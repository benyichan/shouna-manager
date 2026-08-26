package com.shouna.manager.domain

/** 多标签工具：逗号分隔字符串 ↔ 标签列表 */
object TagUtils {

    private val DEFAULT_TAGS = listOf(
        "食品", "电子产品", "日用品", "衣物", "药品", "化妆品", "工具", "文具", "其他"
    )

    /** "食品,电子产品" → ["食品","电子产品"]，去空白、去重、去空串 */
    fun split(tags: String?): List<String> =
        tags.orEmpty()
            .split(',', '，')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun join(tags: List<String>): String = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

    fun defaultTags(): List<String> = DEFAULT_TAGS
}
