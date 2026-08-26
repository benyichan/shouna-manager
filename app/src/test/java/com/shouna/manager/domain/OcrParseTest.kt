package com.shouna.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class OcrParseTest {

    private fun millis(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance()
        c.clear(); c.set(y, m - 1, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun parsesDotSeparatedProductionDate() {
        val r = OcrParse.parse(listOf("生产日期：2025.11.22"))
        assertEquals(millis(2025, 11, 22), r.productionDate)
    }

    @Test
    fun parsesDashAndChineseDate() {
        assertEquals(millis(2025, 11, 22), OcrParse.parse(listOf("生产日期：2025-11-22")).productionDate)
        assertEquals(millis(2025, 11, 22), OcrParse.parse(listOf("生产日期：2025年11月22日")).productionDate)
        assertEquals(millis(2025, 11, 22), OcrParse.parse(listOf("生产日期：2025/11/22")).productionDate)
    }

    @Test
    fun expiryWithoutDayIsFlaggedNotDefaulted() {
        // 有效期至：2027.10. 缺日 → 不默认月末，标记年月
        val r = OcrParse.parse(listOf("有效期至：2027.10."))
        assertNull(r.expiryDirect)
        assertEquals(2027, r.expiryYmOnlyYear)
        assertEquals(10, r.expiryYmOnlyMonth)
    }

    @Test
    fun expiryFullDateParsed() {
        assertEquals(millis(2027, 10, 15), OcrParse.parse(listOf("有效期至 2027.10.15")).expiryDirect)
    }

    @Test
    fun expiryWithoutTrailingDotYearMonth() {
        val r = OcrParse.parse(listOf("有效期至2027-10"))
        assertEquals(2027, r.expiryYmOnlyYear)
        assertEquals(10, r.expiryYmOnlyMonth)
    }

    @Test
    fun makeMillisUsesGivenDay() {
        // 回填时用生产日期的日补缺：生产 2025-11-22，有效期 2027-10 缺日 → 2027-10-22
        val prod = OcrParse.parse(listOf("生产日期：2025.11.22")).productionDate!!
        val expiry = OcrParse.parse(listOf("有效期至：2027.10."))
        val day = Calendar.getInstance().apply { timeInMillis = prod }.get(Calendar.DAY_OF_MONTH)
        val merged = OcrParse.makeMillis(expiry.expiryYmOnlyYear!!, expiry.expiryYmOnlyMonth!!, day)
        assertEquals(millis(2027, 10, 22), merged)
    }

    @Test
    fun parsesShelfLifeText() {
        assertEquals("9个月", OcrParse.parse(listOf("保质期：9个月")).shelfLifeText)
        assertEquals("15天", OcrParse.parse(listOf("保质期 15天")).shelfLifeText)
    }

    @Test
    fun extractsFieldsFromMixedLines() {
        val r = OcrParse.parse(listOf("生产日期：2025.11.22", "保质期：9个月", "有效期至：2027.10."))
        assertEquals(millis(2025, 11, 22), r.productionDate)
        assertEquals("9个月", r.shelfLifeText)
        assertEquals(2027, r.expiryYmOnlyYear)
        assertEquals(10, r.expiryYmOnlyMonth)
    }

    @Test
    fun handlesGarbage() {
        val r = OcrParse.parse(listOf("这是无关文字", "abc 123"))
        assertNull(r.productionDate)
        assertNull(r.shelfLifeText)
        assertNull(r.expiryDirect)
        assertNull(r.expiryYmOnlyYear)
    }
}
