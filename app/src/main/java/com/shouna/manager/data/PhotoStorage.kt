package com.shouna.manager.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.max
import kotlin.math.min

object PhotoStorage {
    private const val MAX_EDGE = 1600

    /** 从 content Uri 读取、压缩并保存到应用私有目录，返回相对路径 photos/xxx.jpg */
    fun saveImage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val fileName = "photo-${System.currentTimeMillis()}.jpg"
            val outFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bmp = BitmapFactory.decodeStream(input) ?: return null
                val scale = min(1f, MAX_EDGE.toFloat() / max(bmp.width, bmp.height))
                val w = (bmp.width * scale).toInt().coerceAtLeast(1)
                val h = (bmp.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
                outFile.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                if (scaled != bmp) bmp.recycle()
                scaled.recycle()
            }
            "photos/$fileName"
        } catch (e: Exception) {
            null
        }
    }

    fun loadFile(context: Context, path: String): File = File(context.filesDir, path)

    /** 创建相机拍照用的临时 Uri（cache/capture 下），经 FileProvider 暴露 */
    fun newCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** 按目标边长采样解码缩略图（inSampleSize 为 2 的幂），供列表缩略展示避免 OOM */
    fun decodeThumb(file: File, targetEdge: Int = 400): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > targetEdge || bounds.outHeight / sample > targetEdge) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    fun delete(context: Context, path: String) {
        runCatching { File(context.filesDir, path).delete() }
    }
}
