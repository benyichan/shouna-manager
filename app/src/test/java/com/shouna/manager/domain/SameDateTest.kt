package com.shouna.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SameDateTest {

    private fun ts(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun matchesSameMonthDayDifferentYears() {
        assertTrue(SameDate.isSameMonthDay(ts(2023, 3, 15), ts(2025, 3, 15)))
        assertTrue(SameDate.isSameMonthDay(ts(2020, 12, 1), ts(2024, 12, 1)))
    }

    @Test
    fun rejectsDifferentMonthOrDay() {
        assertFalse(SameDate.isSameMonthDay(ts(2023, 3, 15), ts(2025, 3, 16)))
        assertFalse(SameDate.isSameMonthDay(ts(2023, 3, 15), ts(2025, 4, 15)))
    }

    @Test
    fun rejectsNull() {
        assertFalse(SameDate.isSameMonthDay(null, ts(2025, 3, 15)))
        assertFalse(SameDate.isSameMonthDay(ts(2025, 3, 15), null))
        assertFalse(SameDate.isSameMonthDay(null, null))
    }

    @Test
    fun leapDayFeb29() {
        // 闰年 2/29 应匹配到另一个闰年的 2/29；平年无 2/29 正常不参与
        assertTrue(SameDate.isSameMonthDay(ts(2020, 2, 29), ts(2024, 2, 29)))
        assertFalse(SameDate.isSameMonthDay(ts(2020, 2, 29), ts(2023, 2, 28)))
    }

    @Test
    fun extractsYear() {
        assertEquals(2023, SameDate.year(ts(2023, 1, 1)))
    }
}
