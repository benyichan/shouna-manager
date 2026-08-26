package com.shouna.manager.domain

import com.shouna.manager.data.db.entity.ItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 单月浪费统计 */
data class MonthWaste(
    /** 如 2026-08 */
    val yearMonth: String,
    val count: Int,
    val amountSum: Double,
    /** 该月丢弃物品中出现最多的标签（可能为 null） */
    val topTag: String?
)

/**
 * 浪费分析：统计「已处理 且 处理时已过期」的物品。
 * 口径：handled=true、expiryDate 非空、expiryDate < (handledAt ?: now)。
 * 按处理时间所在月份归组。
 */
object WasteAnalysis {

    fun monthly(
        items: List<ItemEntity>,
        now: Long = System.currentTimeMillis(),
        months: Int = 3
    ): List<MonthWaste> {
        val fmt = SimpleDateFormat("yyyy-MM", Locale.CHINA)
        val wasted = items.filter {
            it.handled && it.expiryDate != null && it.expiryDate < (it.handledAt ?: now)
        }
        val grouped = wasted.groupBy { fmt.format(Date(it.handledAt ?: now)) }
        return (0 until months).map { offset ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.MONTH, -offset)
            }
            val key = fmt.format(cal.time)
            val list = grouped[key].orEmpty()
            val topTag = list
                .flatMap { TagUtils.split(it.tags) }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            MonthWaste(
                yearMonth = key,
                count = list.size,
                amountSum = list.sumOf { it.amount ?: 0.0 },
                topTag = topTag
            )
        }
    }

    /** 是否存在任何浪费记录（用于决定是否展示卡片） */
    fun hasAny(items: List<ItemEntity>, now: Long = System.currentTimeMillis()): Boolean =
        items.any { it.handled && it.expiryDate != null && it.expiryDate < (it.handledAt ?: now) }
}
