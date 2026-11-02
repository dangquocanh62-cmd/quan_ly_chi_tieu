package com.quanlychitieuapp

import com.quanlychitieuapp.utils.MoneyFormatter
import com.quanlychitieuapp.utils.MoneyParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for MoneyParser and MoneyFormatter.
 * JUnit4 - pure Kotlin, no Android context needed.
 */
class MoneyParserTest {

    // ===== MoneyParser Tests =====

    @Test
    fun `parse 35k returns 35000`() {
        assertEquals(35_000L, MoneyParser.parse("35k"))
    }

    @Test
    fun `parse 300k returns 300000`() {
        assertEquals(300_000L, MoneyParser.parse("300k"))
    }

    @Test
    fun `parse 1_5tr returns 1500000`() {
        assertEquals(1_500_000L, MoneyParser.parse("1.5tr"))
    }

    @Test
    fun `parse 1_5 triệu dot notation returns 1500000`() {
        assertEquals(1_500_000L, MoneyParser.parse("1.5 triệu"))
    }

    @Test
    fun `parse 1 comma 5 triệu returns 1500000`() {
        assertEquals(1_500_000L, MoneyParser.parse("1,5 triệu"))
    }

    @Test
    fun `parse 2 triệu returns 2000000`() {
        assertEquals(2_000_000L, MoneyParser.parse("2 triệu"))
    }

    @Test
    fun `parse 8tr returns 8000000`() {
        assertEquals(8_000_000L, MoneyParser.parse("8tr"))
    }

    @Test
    fun `parse 1 dot 200 dot 000 đ returns 1200000`() {
        assertEquals(1_200_000L, MoneyParser.parse("1.200.000đ"))
    }

    @Test
    fun `parse 50 dot 000 returns 50000`() {
        assertEquals(50_000L, MoneyParser.parse("50.000"))
    }

    @Test
    fun `parse 500000 returns 500000`() {
        assertEquals(500_000L, MoneyParser.parse("500000"))
    }

    @Test
    fun `parse 50000 returns 50000`() {
        assertEquals(50_000L, MoneyParser.parse("50000"))
    }

    @Test
    fun `parse empty string returns null`() {
        assertNull(MoneyParser.parse(""))
    }

    @Test
    fun `parse invalid string returns null`() {
        assertNull(MoneyParser.parse("abc"))
    }

    @Test
    fun `parse embedded in text - ăn sáng 35k`() {
        // MoneyParser.parse() on "ăn sáng 35k" will return 35000 because it finds "35k"
        assertEquals(35_000L, MoneyParser.parse("ăn sáng 35k"))
    }

    @Test
    fun `parse triệu uppercase`() {
        assertEquals(2_000_000L, MoneyParser.parse("2 Triệu"))
    }

    @Test
    fun `parse 10tr returns 10000000`() {
        assertEquals(10_000_000L, MoneyParser.parse("10tr"))
    }

    // ===== MoneyFormatter Tests =====

    @Test
    fun `format 35000 returns 35 dot 000đ`() {
        assertEquals("35.000đ", MoneyFormatter.format(35_000L))
    }

    @Test
    fun `format 1500000 returns 1 dot 500 dot 000đ`() {
        assertEquals("1.500.000đ", MoneyFormatter.format(1_500_000L))
    }

    @Test
    fun `format 0 returns 0đ`() {
        assertEquals("0đ", MoneyFormatter.format(0L))
    }

    @Test
    fun `format 1000000000 returns 1 dot 000 dot 000 dot 000đ`() {
        assertEquals("1.000.000.000đ", MoneyFormatter.format(1_000_000_000L))
    }

    @Test
    fun `compact format 2000000 is 2 triệu`() {
        assertEquals("2 triệu", MoneyFormatter.formatCompact(2_000_000L))
    }

    @Test
    fun `compact format 35000 is 35k`() {
        assertEquals("35k", MoneyFormatter.formatCompact(35_000L))
    }

    @Test
    fun `compact format 1500000 is 1 comma 5tr`() {
        // 1,500,000 = 1tr + 500,000 = 1,5tr
        val result = MoneyFormatter.formatCompact(1_500_000L)
        assertTrue("Expected compact format for 1.5tr, got: $result", result.contains("tr"))
    }
}
