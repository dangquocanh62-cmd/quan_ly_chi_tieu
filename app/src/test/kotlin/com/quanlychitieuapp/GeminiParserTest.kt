package com.quanlychitieuapp

import com.quanlychitieuapp.ai.GeminiParser
import com.quanlychitieuapp.ai.ParserResult
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GeminiParser validation logic.
 * Uses mock JSON responses - NO real network calls.
 * 
 * Tests the parseJsonOutput() method directly to validate:
 * - Valid output parsed correctly
 * - Invalid/malformed output rejected
 * - Low confidence output triggers LowConfidence result
 * - Negative amounts rejected
 */
class GeminiParserTest {

    // We test only parseJsonOutput() - no network, no Context needed
    private val parser = GeminiParser(
        context = mockk(relaxed = true),
        apiKeyProvider = { null }
    )

    @Test
    fun `valid JSON output returns Success`() {
        val json = """
            {
              "intent": "expense",
              "amount": 420000,
              "category": "food",
              "wallet": "momo",
              "description": "ăn với bạn",
              "date": "today",
              "confidence": 0.95
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNotNull(result)
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("expense", success.intent)
        assertEquals(420_000L, success.amount)
        assertEquals("food", success.categoryKey)
        assertEquals("momo", success.walletKey)
        assertEquals(0.95f, success.confidence, 0.01f)
    }

    @Test
    fun `income JSON returns Success with correct intent`() {
        val json = """
            {
              "intent": "income",
              "amount": 8000000,
              "category": "salary",
              "wallet": null,
              "description": "lương tháng 8",
              "date": "today",
              "confidence": 0.98
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertTrue(result is ParserResult.Success)
        val success = result as ParserResult.Success
        assertEquals("income", success.intent)
        assertEquals(8_000_000L, success.amount)
        assertEquals("salary", success.categoryKey)
    }

    @Test
    fun `low confidence returns LowConfidence`() {
        val json = """
            {
              "intent": "expense",
              "amount": 100000,
              "category": "other",
              "wallet": null,
              "description": "",
              "date": "today",
              "confidence": 0.3
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertTrue("Expected LowConfidence but got: $result", result is ParserResult.LowConfidence)
    }

    @Test
    fun `invalid intent returns null`() {
        val json = """
            {
              "intent": "transfer",
              "amount": 100000,
              "category": "food",
              "confidence": 0.9
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNull(result)
    }

    @Test
    fun `negative amount returns null`() {
        val json = """
            {
              "intent": "expense",
              "amount": -50000,
              "category": "food",
              "confidence": 0.9
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNull(result)
    }

    @Test
    fun `zero amount returns null`() {
        val json = """
            {
              "intent": "expense",
              "amount": 0,
              "category": "food",
              "confidence": 0.9
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNull(result)
    }

    @Test
    fun `invalid category returns null`() {
        val json = """
            {
              "intent": "expense",
              "amount": 100000,
              "category": "invalid_category_xyz",
              "confidence": 0.9
            }
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNull(result)
    }

    @Test
    fun `malformed JSON returns null`() {
        val result = parser.parseJsonOutput("{invalid json}")
        assertNull(result)
    }

    @Test
    fun `empty string returns null`() {
        val result = parser.parseJsonOutput("")
        assertNull(result)
    }

    @Test
    fun `markdown wrapped JSON is parsed correctly`() {
        val json = """
            ```json
            {
              "intent": "expense",
              "amount": 35000,
              "category": "food",
              "wallet": null,
              "description": "ăn sáng",
              "date": "today",
              "confidence": 0.9
            }
            ```
        """.trimIndent()

        val result = parser.parseJsonOutput(json)
        assertNotNull(result)
        assertTrue(result is ParserResult.Success)
    }

    @Test
    fun `missing required fields returns null`() {
        val json = """{"description": "test"}"""
        val result = parser.parseJsonOutput(json)
        assertNull(result)
    }
}
