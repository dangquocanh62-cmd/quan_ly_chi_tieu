package com.quanlychitieuapp

import com.quanlychitieuapp.ai.LocalParser
import com.quanlychitieuapp.ai.ParserResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocalParser - Vietnamese NLP rule-based parser.
 * JUnit4, no Android context needed.
 */
class LocalParserTest {

    private lateinit var parser: LocalParser

    @Before
    fun setUp() {
        parser = LocalParser()
    }

    @Test
    fun `ăn sáng 35k is expense food 35000`() {
        val result = parser.parse("ăn sáng 35k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(35_000L, success.amount)
        assertEquals("food", success.categoryKey)
    }

    @Test
    fun `cafe 40k is expense food 40000`() {
        val result = parser.parse("cafe 40k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(40_000L, success.amount)
        assertEquals("food", success.categoryKey)
    }

    @Test
    fun `grab 50k is expense transport 50000`() {
        val result = parser.parse("grab 50k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(50_000L, success.amount)
        assertEquals("transport", success.categoryKey)
    }

    @Test
    fun `mua áo 300k is expense shopping 300000`() {
        val result = parser.parse("mua áo 300k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(300_000L, success.amount)
        assertEquals("shopping", success.categoryKey)
    }

    @Test
    fun `lương 8tr is income salary 8000000`() {
        val result = parser.parse("lương 8tr")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("income", success.intent)
        assertEquals(8_000_000L, success.amount)
        assertEquals("salary", success.categoryKey)
    }

    @Test
    fun `nhận thưởng 2tr is income bonus 2000000`() {
        val result = parser.parse("nhận thưởng 2tr")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("income", success.intent)
        assertEquals(2_000_000L, success.amount)
        assertEquals("bonus", success.categoryKey)
    }

    @Test
    fun `empty string returns low confidence`() {
        val result = parser.parse("")
        assertTrue(result is ParserResult.LowConfidence)
    }

    @Test
    fun `gibberish returns low confidence`() {
        val result = parser.parse("xyzabc")
        assertTrue(result is ParserResult.LowConfidence)
    }

    @Test
    fun `thuốc 150k is expense health`() {
        val result = parser.parse("thuốc 150k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(150_000L, success.amount)
        assertEquals("health", success.categoryKey)
    }

    @Test
    fun `học phí 2 triệu is expense education`() {
        val result = parser.parse("học phí 2 triệu")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(2_000_000L, success.amount)
        assertEquals("education", success.categoryKey)
    }

    @Test
    fun `confidence is positive`() {
        val result = parser.parse("ăn trưa 50k")
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertTrue(success.confidence > 0f)
    }
}
