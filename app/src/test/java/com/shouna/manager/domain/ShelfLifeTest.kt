package com.shouna.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShelfLifeTest {

    @Test
    fun parsesDays() {
        assertEquals(15L, ShelfLife.parseDays("15天"))
        assertEquals(15L, ShelfLife.parseDays("15 天"))
        assertEquals(7L, ShelfLife.parseDays("1周"))
        assertEquals(30L, ShelfLife.parseDays("1个月"))
        assertEquals(270L, ShelfLife.parseDays("9个月"))
        assertEquals(1095L, ShelfLife.parseDays("3年"))
        assertEquals(30L, ShelfLife.parseDays("30日"))
    }

    @Test
    fun handlesFractions() {
        assertEquals(547L, ShelfLife.parseDays("1.5年"))
        assertEquals(15L, ShelfLife.parseDays("0.5个月"))
    }

    @Test
    fun returnsNullOnBlankOrGarbage() {
        assertNull(ShelfLife.parseDays(null))
        assertNull(ShelfLife.parseDays(""))
        assertNull(ShelfLife.parseDays("无保质期"))
        assertNull(ShelfLife.parseDays("abc"))
    }
}
