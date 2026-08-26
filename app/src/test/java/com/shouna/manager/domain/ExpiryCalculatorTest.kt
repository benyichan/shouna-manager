package com.shouna.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiryCalculatorTest {

    private val DAY = 24 * 60 * 60 * 1000L

    @Test
    fun directExpiryWins() {
        val direct = 200L * DAY
        assertEquals(direct, ExpiryCalculator.computeExpiry(100L * DAY, 30L, direct))
    }

    @Test
    fun computesFromProductionPlusShelfLife() {
        assertEquals(130L * DAY, ExpiryCalculator.computeExpiry(100L * DAY, 30L, null))
    }

    @Test
    fun returnsNullWhenNoInfo() {
        assertNull(ExpiryCalculator.computeExpiry(null, null, null))
        assertNull(ExpiryCalculator.computeExpiry(100L * DAY, null, null))
    }

    @Test
    fun statusClasses() {
        val now = System.currentTimeMillis()
        val today = ExpiryCalculator.startOfDay(now)
        // 已处理优先
        assertEquals(ExpiryStatus.HANDLED, ExpiryCalculator.statusOf(today - DAY, true, now = now))
        // 已过期
        assertEquals(ExpiryStatus.EXPIRED, ExpiryCalculator.statusOf(today - DAY, false, now = now))
        // 临期（≤7 天）
        assertEquals(ExpiryStatus.EXPIRING, ExpiryCalculator.statusOf(today + 3 * DAY, false, now = now))
        assertEquals(ExpiryStatus.EXPIRING, ExpiryCalculator.statusOf(today + 7 * DAY, false, now = now))
        // 正常
        assertEquals(ExpiryStatus.NORMAL, ExpiryCalculator.statusOf(today + 8 * DAY, false, now = now))
        // 无有效期
        assertEquals(ExpiryStatus.NORMAL, ExpiryCalculator.statusOf(null, false, now = now))
    }
}

