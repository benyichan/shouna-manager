package com.shouna.manager.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TagUtilsTest {

    @Test
    fun splitsAndTrims() {
        assertEquals(listOf("食品", "电子产品"), TagUtils.split("食品,电子产品"))
        assertEquals(listOf("食品", "电子产品"), TagUtils.split("食品，电子产品"))
        assertEquals(listOf("食品"), TagUtils.split(" 食品 ,  "))
    }

    @Test
    fun removesDuplicatesAndBlanks() {
        assertEquals(listOf("食品", "电子产品"), TagUtils.split("食品,食品,电子产品"))
        assertEquals(emptyList<String>(), TagUtils.split(""))
        assertEquals(emptyList<String>(), TagUtils.split(null))
        assertEquals(emptyList<String>(), TagUtils.split(",,,  ,"))
    }

    @Test
    fun joins() {
        assertEquals("食品,电子产品", TagUtils.join(listOf("食品", "电子产品", "食品")))
        assertEquals("", TagUtils.join(emptyList()))
    }
}
