package com.shouna.manager.domain

/**
 * 保质期描述解析：把「15天 / 9个月 / 3年 / 1周 / 1年半」这类文本解析成天数。
 * 无法解析时返回 null（表示无保质期信息）。
 */
object ShelfLife {
    private val PATTERN = Regex("""(\d+(?:\.\d+)?)\s*(天|日|周|个月|月|年)""")

    fun parseDays(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val m = PATTERN.find(text.trim()) ?: return null
        val value = m.groupValues[1].toDouble()
        return when (m.groupValues[2]) {
            "天", "日" -> (value * 1).toLong()
            "周" -> (value * 7).toLong()
            "个月", "月" -> (value * 30).toLong()
            "年" -> (value * 365).toLong()
            else -> null
        }
    }
}
