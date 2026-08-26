package com.shouna.manager.domain

import java.util.Calendar

/**
 * 从 OCR 识别文本中解析出「生产日期 / 保质期 / 有效日期」。
 * 支持非标准格式：生产日期 2025.11.22、2025-11-22、2025年11月22日、2025.11；
 * 有效期至 2027.10.、2027.10、2027-10；保质期 9个月 / 15天 / 3年。
 *
 * 有效期只给到「年-月」缺日时，不在此处定默认日期，由调用方结合生产日期补齐（保持与生产日期同日）。
 */
object OcrParse {

    data class Result(
        val productionDate: Long? = null,
        val shelfLifeText: String? = null,
        /** 有效日期完整（含日）时的时间戳 */
        val expiryDirect: Long? = null,
        /** 有效日期缺日、只到年月时的年 */
        val expiryYmOnlyYear: Int? = null,
        /** 有效日期缺日、只到年月时的月 */
        val expiryYmOnlyMonth: Int? = null
    )

    private val DATE_RE = Regex("""(20\d{2})\s*[.\-年/]\s*(\d{1,2})\s*(?:[.\-月/]\s*(\d{1,2}))?""")
    private val SHELF_RE = Regex("""(\d+(?:\.\d+)?)\s*(天|日|周|个月|月|年)""")

    fun parse(texts: List<String>): Result {
        var productionDate: Long? = null
        var shelfLifeText: String? = null
        var expiryDirect: Long? = null
        var expiryY: Int? = null
        var expiryM: Int? = null

        for (raw in texts) {
            val line = raw.trim()
            if (line.isBlank()) continue

            if (productionDate == null && (line.contains("生产日期") || line.contains("生产"))) {
                extractYmd(afterKeyword(line, "生产日期"))?.let { (y, m, d) ->
                    productionDate = ymdToMillis(y, m, d)
                }
            }
            if (shelfLifeText == null && line.contains("保质期")) {
                shelfLifeText = extractShelfLifeText(line)
            }
            if (expiryDirect == null && expiryY == null && (line.contains("有效期"))) {
                val tail = afterKeyword(line, "有效期至") ?: line
                extractYmd(tail)?.let { (y, m, d) ->
                    if (d == null) {
                        expiryY = y; expiryM = m
                    } else {
                        expiryDirect = ymdToMillis(y, m, d)
                    }
                }
            }
        }
        return Result(productionDate, shelfLifeText, expiryDirect, expiryY, expiryM)
    }

    private fun afterKeyword(line: String, keyword: String): String {
        val idx = line.indexOf(keyword)
        return if (idx >= 0) line.substring(idx + keyword.length) else line
    }

    /** 提取「年-月(-日)」，返回 (year, month, day?)；月缺省 1，日可空 */
    private fun extractYmd(input: String): Triple<Int, Int, Int?>? {
        val m = DATE_RE.find(input) ?: return null
        return Triple(
            m.groupValues[1].toInt(),
            m.groupValues[2].toIntOrNull() ?: 1,
            m.groupValues[3].toIntOrNull()
        )
    }

    /** 供外部按年月日构造时间戳（如回填时用生产日期补缺的日） */
    fun makeMillis(year: Int, month: Int, day: Int): Long =
        ymdToMillis(year, month, day)

    private fun ymdToMillis(year: Int, month: Int, day: Int?): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        val m = month.coerceIn(1, 12)
        cal.set(Calendar.MONTH, m - 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val d = (day ?: maxDay).coerceIn(1, maxDay)
        cal.set(Calendar.DAY_OF_MONTH, d)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 提取保质期描述原文（如「9个月」「15天」），用于回填保质期字段 */
    private fun extractShelfLifeText(line: String): String? {
        return SHELF_RE.find(line)?.value
    }
}
