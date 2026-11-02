package com.quanlychitieuapp.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInputFilterTest {

    @Test
    fun testValidDigitsAreKept() {
        assertEquals("123", AmountInputFilter.filter("123"))
    }

    @Test
    fun testLettersAreFiltered() {
        assertEquals("123", AmountInputFilter.filter("12abc3"))
    }

    @Test
    fun testDotsAreFiltered() {
        assertEquals("1500000", AmountInputFilter.filter("1.500.000"))
    }

    @Test
    fun testCommasAreFiltered() {
        assertEquals("1500000", AmountInputFilter.filter("1,500,000"))
    }

    @Test
    fun testAllLettersResultsInEmptyString() {
        assertEquals("", AmountInputFilter.filter("abc"))
    }

    @Test
    fun testNegativeSignIsFiltered() {
        assertEquals("100", AmountInputFilter.filter("-100"))
    }

    @Test
    fun testMixedCharactersAreFiltered() {
        assertEquals("1234", AmountInputFilter.filter("12a3b4"))
    }

    @Test
    fun testMaxLengthStripsExcess() {
        assertEquals("123456789012", AmountInputFilter.filter("12345678901234567"))
    }

    @Test
    fun testEmptyInputStaysEmpty() {
        assertEquals("", AmountInputFilter.filter(""))
    }
}
