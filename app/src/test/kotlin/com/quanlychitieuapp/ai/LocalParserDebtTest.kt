package com.quanlychitieuapp.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalParserDebtTest {

    private val parser = LocalParser()

    @Test
    fun `test grammatical roles - borrow`() {
        val cases = listOf(
            "vay Thành 100k",
            "mượn Thành 100k",
            "t vay Thành 100k",
            "t mượn Thành 100k",
            "Thành cho t vay 100k",
            "Thành cho t mượn 100k"
        )
        for (case in cases) {
            val result = parser.parse(case)
            assertTrue("Expected DebtSuccess for '$case', but got $result", result is ParserResult.DebtSuccess)
            val success = result as ParserResult.DebtSuccess
            assertEquals("Failed for case: $case", DebtDirection.BORROW, success.direction)
            assertEquals("Failed for case: $case", "Thành", success.personName)
            assertEquals("Failed for case: $case", 100000L, success.amount)
        }
    }

    @Test
    fun `test grammatical roles - lend`() {
        val cases = listOf(
            "Thành vay t 100k",
            "Thành mượn t 100k",
            "t cho Thành vay 100k",
            "t cho Thành mượn 100k",
            "cho Thành vay 100k",
            "cho Thành mượn 100k"
        )
        for (case in cases) {
            val result = parser.parse(case)
            assertTrue("Expected DebtSuccess for '$case', but got $result", result is ParserResult.DebtSuccess)
            val success = result as ParserResult.DebtSuccess
            assertEquals("Failed for case: $case", DebtDirection.LEND, success.direction)
            assertEquals("Failed for case: $case", "Thành", success.personName)
            assertEquals("Failed for case: $case", 100000L, success.amount)
        }
    }

    @Test
    fun `test missing amount`() {
        val case = "vay Thành"
        val result = parser.parse(case)
        assertTrue("Expected NeedMoreDebtInfo for '$case', but got $result", result is ParserResult.NeedMoreDebtInfo)
        val needMore = result as ParserResult.NeedMoreDebtInfo
        assertEquals(MissingDebtField.AMOUNT, needMore.missingField)
        assertEquals(DebtDirection.BORROW, needMore.debt.direction)
        assertEquals("Thành", needMore.debt.person)
    }

    @Test
    fun `test missing person`() {
        val case = "vay 100k"
        val result = parser.parse(case)
        assertTrue("Expected NeedMoreDebtInfo for '$case', but got $result", result is ParserResult.NeedMoreDebtInfo)
        val needMore = result as ParserResult.NeedMoreDebtInfo
        assertEquals(MissingDebtField.PERSON, needMore.missingField)
        assertEquals(DebtDirection.BORROW, needMore.debt.direction)
        assertEquals(100000L, needMore.debt.amount)
    }
}
