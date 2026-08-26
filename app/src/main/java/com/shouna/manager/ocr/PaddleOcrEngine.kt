package com.shouna.manager.ocr

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

/** PP-OCRv4 移动端全本地 OCR：OnnxRuntime(det+rec) + OpenCV(DB 后处理)，字符表从 assets 键文件读取 */
class PaddleOcrEngine(private val context: Context) {

    private val env = OrtEnvironment.getEnvironment()
    private var det: OrtSession? = null
    private var rec: OrtSession? = null
    private var characters: List<String> = emptyList()

    private val detLimitSide = 736
    private val detMaxSide = 1280
    private val detThresh = 0.3f
    private val detBoxThresh = 0.5f
    private val detUnclipRatio = 1.6f
    private val detMinSize = 3
    private val detMaxCandidates = 1000
    private val detMean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val detStd = floatArrayOf(0.229f, 0.224f, 0.225f)

    private var initialized = false

    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (initialized) return@withContext true
        try {
            OpenCVLoader.initLocal()
            val detBytes = context.assets.open("ppocr/det.onnx").use { it.readBytes() }
            val recBytes = context.assets.open("ppocr/rec.onnx").use { it.readBytes() }
            val opts = OrtSession.SessionOptions()
            det = env.createSession(detBytes, opts)
            rec = env.createSession(recBytes, opts)
            characters = buildCharacters()
            initialized = true
            true
        } catch (e: Throwable) {
            android.util.Log.e("PaddleOcr", "init failed", e)
            false
        }
    }

    /** 字符表：['blank'] + 官方 keys(6623) + ' ' = 6625，匹配 rec 输出维度 */
    private fun buildCharacters(): List<String> {
        val raw = context.assets.open("ppocr/keys.txt").use { it.readBytes().toString(Charsets.UTF_8) }
        val list = raw.split('\n').map { it.trimEnd('\r') }.filter { it.isNotEmpty() }
        if (list.isEmpty()) return emptyList()
        return listOf("blank") + list + listOf(" ")
    }

    fun recognize(bitmap: Bitmap): List<String> {
        val detSession = det ?: return emptyList()
        val recSession = rec ?: return emptyList()
        if (characters.size < 2) return emptyList()

        val bgra = Mat()
        Utils.bitmapToMat(bitmap, bgra)
        val rgb = Mat()
        Imgproc.cvtColor(bgra, rgb, Imgproc.COLOR_BGRA2RGB)
        bgra.release()

        try {
            android.util.Log.i("Ocr", "recognize: bitmap ${bitmap.width}x${bitmap.height}")
            // 双 det：原图 + 反相图，覆盖反白文字（白字深底）与常规黑字
            val inv = Mat()
            Core.bitwise_not(rgb, inv)
            val items = ArrayList<Pair<List<Pair<Float, Float>>, Mat>>()
            runCatching { detect(rgb) }.onSuccess { b0 -> b0.forEach { items.add(it to rgb) } }
            runCatching { detect(inv) }.onSuccess { b1 -> b1.forEach { items.add(it to inv) } }
            android.util.Log.i("Ocr", "det boxes total=${items.size}")

            val texts = mutableListOf<String>()
            items.forEachIndexed { bi, (box, src) ->
                android.util.Log.i("Ocr", "crop+rec box $bi")
                val crop = cropRgb(src, box) ?: return@forEachIndexed
                val text = recognizeCrop(recSession, crop)
                crop.release()
                if (text.isNotBlank() && !texts.contains(text)) texts.add(text)
            }
            inv.release()
            android.util.Log.i("Ocr", "texts=${texts.joinToString("|")}")
            return texts
        } finally {
            rgb.release()
        }
    }

    /** 单通道检测：返回原图坐标的文本框集合 */
    private fun detect(src: Mat): List<List<Pair<Float, Float>>> {
        val detSession = det ?: return emptyList()
        val hold = detPreprocess(src)
        val resized = hold.first
        val cols = resized.cols()
        val rows = resized.rows()
        val pix = ByteArray(cols * rows * 3)
        resized.get(0, 0, pix)
        val f = FloatArray(cols * rows * 3)
        for (i in pix.indices) f[i] = (pix[i].toInt() and 0xFF) / 255f
        val out = FloatArray(3 * rows * cols)
        for (c in 0 until 3) {
            val mean = detMean[c]; val std = detStd[c]
            for (i in 0 until rows * cols) out[c * rows * cols + i] = (f[i * 3 + c] - mean) / std
        }
        resized.release()

        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(out), longArrayOf(1, 3, rows.toLong(), cols.toLong()))
        val result = detSession.run(mapOf(detSession.inputNames.iterator().next() to tensor))
        tensor.close()
        val pred = result.get(0).value as Array<Array<Array<FloatArray>>>
        val last = pred[0][0]
        val hm = last.size
        val wm = last[0].size
        result.close()
        return dbPostprocess(pred[0][0], hm, wm, src.cols(), src.rows())
    }

    // ======== det 预处理 ========
    private fun detPreprocess(rgb: Mat): Pair<Mat, Float> {
        var h = rgb.rows(); var w = rgb.cols()
        // 先限制最大边，避免高分辨率相机照片导致内存暴涨
        if (max(h, w) > detMaxSide) {
            val sc = detMaxSide.toFloat() / max(h, w)
            h = (h * sc).toInt().coerceAtLeast(1)
            w = (w * sc).toInt().coerceAtLeast(1)
        }
        val ratio = if (min(h, w) < detLimitSide) detLimitSide.toFloat() / min(h, w) else 1f
        var rh = (h * ratio).toInt(); var rw = (w * ratio).toInt()
        rh = (round(rh / 32.0) * 32).toInt().coerceAtLeast(32)
        rw = (round(rw / 32.0) * 32).toInt().coerceAtLeast(32)
        val out = Mat()
        Imgproc.resize(rgb, out, Size(rw.toDouble(), rh.toDouble()))
        return out to (rh.toFloat() / h)
    }

    // ======== DB 后处理 ========
    private fun dbPostprocess(
        pred: Array<FloatArray>, hm: Int, wm: Int, srcW: Int, srcH: Int
    ): List<List<Pair<Float, Float>>> {
        val seg = Mat(hm, wm, CvType.CV_32F)
        for (i in 0 until hm) for (j in 0 until wm) seg.put(i, j, if (pred[i][j] > detThresh) 1.0 else 0.0)
        val seg8 = Mat()
        seg.convertTo(seg8, CvType.CV_8U, 255.0)
        seg.release()
        val kernel = Mat(2, 2, CvType.CV_8U, org.opencv.core.Scalar(1.0))
        val dilated = Mat()
        Imgproc.dilate(seg8, dilated, kernel)
        kernel.release(); seg8.release()

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        hierarchy.release(); dilated.release()

        val boxes = mutableListOf<List<Pair<Float, Float>>>()
        val count = min(contours.size, detMaxCandidates)
        for (ci in 0 until count) {
            val contour = contours[ci]
            val mini = getMiniBoxes(contour)
            if (mini == null) { contour.release(); continue }
            val (pts, sside) = mini
            if (sside < detMinSize) { contour.release(); continue }
            val score = boxScoreFast(pred, hm, wm, pts)
            if (score < detBoxThresh) { contour.release(); continue }
            val dist = scoreUnclip(pts)
            val expanded = expandPolygon(pts, dist)
            val (pts2, sside2) = getMiniBoxes2(expanded)
            if (sside2 < detMinSize + 2) { contour.release(); continue }
            val finalPts = pts2.map {
                (Math.round(it.first / wm * srcW).toFloat().coerceIn(0f, srcW.toFloat())) to
                    (Math.round(it.second / hm * srcH).toFloat().coerceIn(0f, srcH.toFloat()))
            }
            boxes.add(finalPts)
            contour.release()
        }
        contours.forEach { it.release() }
        return boxes
    }

    private fun getMiniBoxes(contour: MatOfPoint): Pair<List<Pair<Float, Float>>, Float>? {
        if (contour.rows() < 3) return null
        val pts2f = MatOfPoint2f(*contour.toArray())
        val rect = Imgproc.minAreaRect(pts2f)
        pts2f.release()
        val sside = min(rect.size.width, rect.size.height).toFloat()
        return reorderCorners(roiCorners(rect)) to sside
    }

    private fun getMiniBoxes2(expanded: List<Pair<Float, Float>>): Pair<List<Pair<Float, Float>>, Float> {
        val pts = expanded.map { Point(it.first.toDouble(), it.second.toDouble()) }.toTypedArray()
        val pts2f = MatOfPoint2f(*pts)
        val rect = Imgproc.minAreaRect(pts2f)
        pts2f.release()
        val sside = min(rect.size.width, rect.size.height).toFloat()
        return reorderCorners(roiCorners(rect)) to sside
    }

    /** 由 RotatedRect 中心/宽高/角度手算 4 角（顺序后续统一重排，几何等效 boxPoints） */
    private fun roiCorners(rect: org.opencv.core.RotatedRect): Array<Pair<Double, Double>> {
        val theta = Math.toRadians(rect.angle)
        val cos = Math.cos(theta); val sin = Math.sin(theta)
        val cx = rect.center.x; val cy = rect.center.y
        val hw = rect.size.width / 2.0; val hh = rect.size.height / 2.0
        val corners = arrayOf(-hw to -hh, hw to -hh, hw to hh, -hw to hh)
        return Array(4) { i ->
            val (x0, y0) = corners[i]
            (cx + x0 * cos - y0 * sin) to (cy + x0 * sin + y0 * cos)
        }
    }

    /** 4 角按 x 排序后按 y 重排（与 PaddleOCR boxPoints 顺序一致），转 Float */
    private fun reorderCorners(arr: Array<Pair<Double, Double>>): List<Pair<Float, Float>> {
        val byX = arr.sortedBy { it.first }
        val p0 = byX[0]; val p1 = byX[1]; val p2 = byX[2]; val p3 = byX[3]
        var i1 = 0; var i2 = 1; var i3 = 2; var i4 = 3
        if (p1.second > p0.second) { i1 = 0; i4 = 1 } else { i1 = 1; i4 = 0 }
        if (p3.second > p2.second) { i2 = 2; i3 = 3 } else { i2 = 3; i3 = 2 }
        val ord = listOf(byX[i1], byX[i2], byX[i3], byX[i4])
        return ord.map { it.first.toFloat() to it.second.toFloat() }
    }

    private fun boxScoreFast(pred: Array<FloatArray>, hm: Int, wm: Int, box: List<Pair<Float, Float>>): Float {
        val xmin = floor(box.minOf { it.first }).toInt().coerceIn(0, wm - 1)
        val xmax = ceil(box.maxOf { it.first }).toInt().coerceIn(0, wm - 1)
        val ymin = floor(box.minOf { it.second }).toInt().coerceIn(0, hm - 1)
        val ymax = ceil(box.maxOf { it.second }).toInt().coerceIn(0, hm - 1)
        val xa = maxOf(xmin, xmax); val ya = maxOf(ymin, ymax)
        val mask = Mat(ya - ymin + 1, xa - xmin + 1, CvType.CV_8U, org.opencv.core.Scalar(0.0))
        val poly = box.map { Point(it.first.toDouble() - xmin.toDouble(), it.second.toDouble() - ymin.toDouble()) }
        val mp = MatOfPoint(*poly.toTypedArray())
        Imgproc.fillConvexPoly(mask, mp, org.opencv.core.Scalar(1.0))
        mp.release()
        var sum = 0.0; var cnt = 0
        for (i in 0..(ya - ymin)) for (j in 0..(xa - xmin)) {
            if (mask.get(i, j)[0] > 0.5) { sum += pred[ymin + i][xmin + j]; cnt++ }
        }
        mask.release()
        return if (cnt > 0) (sum / cnt).toFloat() else 0f
    }

    private fun scoreUnclip(box: List<Pair<Float, Float>>): Float {
        val area = polygonArea(box); val perim = polygonPerimeter(box)
        if (perim <= 0) return 0f
        return area * detUnclipRatio / perim
    }

    private fun expandPolygon(box: List<Pair<Float, Float>>, distance: Float): List<Pair<Float, Float>> {
        if (distance <= 0) return box
        val cx = (box.map { it.first }.average()).toFloat()
        val cy = (box.map { it.second }.average()).toFloat()
        return box.map { (x, y) ->
            var dx = x - cx; var dy = y - cy
            val len = sqrt(dx * dx + dy * dy)
            if (len < 1e-6f) return@map x to y
            dx /= len; dy /= len
            (x + dx * distance) to (y + dy * distance)
        }
    }

    private fun polygonArea(box: List<Pair<Float, Float>>): Float {
        var s = 0.0
        for (i in box.indices) {
            val (x1, y1) = box[i]; val (x2, y2) = box[(i + 1) % box.size]
            s += x1 * y2 - x2 * y1
        }
        return (abs(s) / 2).toFloat()
    }

    private fun polygonPerimeter(box: List<Pair<Float, Float>>): Float {
        var p = 0.0
        for (i in box.indices) {
            val (x1, y1) = box[i]; val (x2, y2) = box[(i + 1) % box.size]
            val dx = x2 - x1; val dy = y2 - y1
            p += sqrt(dx * dx + dy * dy)
        }
        return p.toFloat()
    }

    // ======== rec ========
    private fun recognizeCrop(session: OrtSession, crop: Mat): String {
        val h = crop.rows(); val w = crop.cols()
        if (h < 4 || w < 4) return ""
        val resizedW = ceil(w * 48.0 / h).toInt().coerceAtLeast(8)
        val resized = Mat()
        Imgproc.resize(crop, resized, Size(resizedW.toDouble(), 48.0))
        val pix = ByteArray(resizedW * 48 * 3)
        resized.get(0, 0, pix)
        resized.release()

        val chw = FloatArray(3 * 48 * resizedW)
        for (i in 0 until resizedW * 48) {
            val v = (pix[i * 3].toInt() and 0xFF) / 255f
            val norm = (v - 0.5f) / 0.5f
            chw[i] = norm
            chw[48 * resizedW + i] = norm
            chw[2 * 48 * resizedW + i] = norm
        }
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw), longArrayOf(1, 3, 48, resizedW.toLong()))
        val result = session.run(mapOf(session.inputNames.iterator().next() to tensor))
        tensor.close()
        val probs = result.get(0).value as Array<Array<FloatArray>>
        result.close()
        return ctcDecode(probs[0])
    }

    private fun ctcDecode(seq: Array<FloatArray>): String {
        val sb = StringBuilder()
        var prevIdx = -1
        for (step in seq) {
            var best = 0; var bestV = -Float.MAX_VALUE
            for (k in seq.indices) if (step[k] > bestV) { bestV = step[k]; best = k }
            if (best == 0 || best >= characters.size) { prevIdx = -1; continue }
            if (best == prevIdx) continue
            prevIdx = best
            sb.append(characters[best])
        }
        return sb.toString().trim()
    }

    private fun floor(v: Float): Int = kotlin.math.floor(v.toDouble()).toInt()
    private fun ceil(v: Float): Int = kotlin.math.ceil(v.toDouble()).toInt()

    private fun cropRgb(src: Mat, box: List<Pair<Float, Float>>): Mat? {
        val pts = box.map { Point(it.first.toDouble(), it.second.toDouble()) }
        val mp = MatOfPoint2f(*pts.toTypedArray())
        val rect = Imgproc.minAreaRect(mp)
        mp.release()
        val center = rect.center; val size = rect.size
        val rotMat = Imgproc.getRotationMatrix2D(center, rect.angle, 1.0)
        val rotated = Mat()
        Imgproc.warpAffine(src, rotated, rotMat, Size(src.cols().toDouble(), src.rows().toDouble()))
        rotMat.release()
        val w = size.width.toInt().coerceAtLeast(8)
        val h = size.height.toInt().coerceAtLeast(8)
        val left = (center.x - w / 2.0).toInt(); val top = (center.y - h / 2.0).toInt()
        val cl = left.coerceIn(0, rotated.cols() - 1); val ct = top.coerceIn(0, rotated.rows() - 1)
        val cr = (left + w).coerceIn(cl + 1, rotated.cols()); val cb = (top + h).coerceIn(ct + 1, rotated.rows())
        val crop = Mat(rotated, org.opencv.core.Rect(cl, ct, cr - cl, cb - ct))
        val out = crop.clone()
        crop.release(); rotated.release()
        return out
    }
}
