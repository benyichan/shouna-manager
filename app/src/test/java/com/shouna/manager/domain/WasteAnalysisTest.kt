package com.shouna.manager.domain

import com.shouna.manager.data.db.entity.ItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WasteAnalysisTest {

    /** 固定「今天」：2026-08-26 0 点 */
    private val now: Long by lazy {
        Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 26, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun item(
        handled: Boolean,
        expiryDate: Long?,
        handledAt: Long? = null,
        amount: Double? = null,
        tags: String = ""
    ) = ItemEntity(
        name = "测试物品",
        tags = tags,
        expiryDate = expiryDate,
        amount = amount,
        handled = handled,
        handledAt = handledAt,
        createdAt = now
    )

    @Test
    fun `未处理或处理时未过期的不计入浪费`() {
        val list = listOf(
            item(handled = false, expiryDate = now - DAY_MS),          // 过期但未处理
            item(handled = true, expiryDate = now + DAY_MS, handledAt = now), // 处理时还没过期
            item(handled = true, expiryDate = null, handledAt = now)   // 无有效期
        )
        assertFalse(WasteAnalysis.hasAny(list, now))
        assertTrue(WasteAnalysis.monthly(list, now).all { it.count == 0 })
    }

    @Test
    fun `处理时已过期的计入当月浪费`() {
        val cal = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 10) }
        val lastMonthHandledAt = cal.timeInMillis
        val list = listOf(
            item(handled = true, expiryDate = lastMonthHandledAt - DAY_MS, handledAt = lastMonthHandledAt),
            item(handled = true, expiryDate = lastMonthHandledAt - DAY_MS, handledAt = lastMonthHandledAt)
        )
        val stats = WasteAnalysis.monthly(list, now)
        assertEquals(3, stats.size)
        assertEquals("2026-07", stats[1].yearMonth)
        assertEquals(2, stats[1].count)
        assertEquals(0, stats[0].count)
    }

    @Test
    fun `金额合计与最多标签`() {
        val list = listOf(
            item(handled = true, expiryDate = now - DAY_MS, handledAt = now, amount = 12.5, tags = "食品"),
            item(handled = true, expiryDate = now - DAY_MS, handledAt = now, amount = 30.0, tags = "食品,调味品")
        )
        val stat = WasteAnalysis.monthly(list, now)[0]
        assertEquals(2, stat.count)
        assertEquals(42.5, stat.amountSum, 0.001)
        assertEquals("食品", stat.topTag)
    }
}
