package com.quanlychitieuapp.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * MoneyParser: Parse Vietnamese money strings to Long (VND).
 * 
 * RULES:
 * - NEVER use Float/Double internally
 * - All amounts stored as Long (smallest unit = đồng)
 * - Return null if cannot parse
 * 
 * Examples:
 *   "35k"         -> 35000
 *   "1.5tr"       -> 1500000
 *   "1,5 triệu"   -> 1500000
 *   "2 triệu"     -> 2000000
 *   "1.200.000đ"  -> 1200000
 *   "50000"       -> 50000
 *   "500000"      -> 500000
 */
object MoneyParser {

    private val TRIỆU_REGEX = Regex("""(\d+)[.,](\d+)\s*(?:tr|triệu|trieu|t\b)""", RegexOption.IGNORE_CASE)
    private val WHOLE_TRIỆU_REGEX = Regex("""(\d+)\s*(?:tr\b|triệu|trieu)""", RegexOption.IGNORE_CASE)
    private val K_REGEX = Regex("""(\d+)[.,](\d+)\s*k\b""", RegexOption.IGNORE_CASE)
    private val WHOLE_K_REGEX = Regex("""(\d+)\s*k\b""", RegexOption.IGNORE_CASE)
    private val PURE_NUMBER_REGEX = Regex("""^[\d.,]+đ?$""")

    /**
     * Parse a money string to Long VND.
     * @return amount in VND as Long, or null if unparseable
     */
    fun parse(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // Try decimal triệu: "1.5tr", "1,5 triệu"
        TRIỆU_REGEX.find(trimmed)?.let { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return null
            val decimalStr = match.groupValues[2]
            val decimal = parseDecimalPart(decimalStr, multiplier = 1_000_000L)
            return whole * 1_000_000L + decimal
        }

        // Try whole triệu: "2 triệu", "3tr"
        WHOLE_TRIỆU_REGEX.find(trimmed)?.let { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return null
            return whole * 1_000_000L
        }

        // Try decimal k: "1.5k", "1,5k"
        K_REGEX.find(trimmed)?.let { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return null
            val decimalStr = match.groupValues[2]
            val decimal = parseDecimalPart(decimalStr, multiplier = 1_000L)
            return whole * 1_000L + decimal
        }

        // Try whole k: "35k", "300k"
        WHOLE_K_REGEX.find(trimmed)?.let { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return null
            return whole * 1_000L
        }

        // Try pure number (possibly with dots/commas as thousand separator and optional đ)
        if (PURE_NUMBER_REGEX.matches(trimmed)) {
            return parsePureNumber(trimmed)
        }

        // Extract any numeric portion with unit suffix from complex text
        return extractFromText(trimmed)
    }

    /**
     * Parse decimal part into VND.
     * Example: "5" with multiplier 1_000_000 -> 500_000
     *          "25" with multiplier 1_000_000 -> 250_000
     */
    private fun parseDecimalPart(decimalStr: String, multiplier: Long): Long {
        val padded = decimalStr.padEnd(1, '0')
        val ratio = padded[0].toString().toLongOrNull() ?: 0L
        return ratio * (multiplier / 10L)
    }

    /**
     * Parse "1.200.000" or "1,200,000" or "1200000" as pure number VND
     */
    private fun parsePureNumber(input: String): Long? {
        // Remove currency symbol
        val cleaned = input.replace("đ", "").trim()

        // Determine separator usage
        val dotCount = cleaned.count { it == '.' }
        val commaCount = cleaned.count { it == ',' }

        return when {
            dotCount == 0 && commaCount == 0 -> {
                // Plain integer: "50000"
                cleaned.toLongOrNull()
            }
            dotCount > 1 -> {
                // Multiple dots = thousand separator: "1.200.000"
                cleaned.replace(".", "").replace(",", "").toLongOrNull()
            }
            commaCount > 1 -> {
                // Multiple commas = thousand separator: "1,200,000"
                cleaned.replace(",", "").replace(".", "").toLongOrNull()
            }
            dotCount == 1 && commaCount == 0 -> {
                // One dot: could be decimal "1.5" or separator "50.000"
                val parts = cleaned.split(".")
                if (parts.size == 2) {
                    val afterDot = parts[1]
                    if (afterDot.length == 3) {
                        // Thousand separator: "50.000" -> 50000
                        cleaned.replace(".", "").toLongOrNull()
                    } else {
                        // Decimal - round to nearest dong (no fractions in VND)
                        null // We don't support fractions of VND
                    }
                } else null
            }
            commaCount == 1 && dotCount == 0 -> {
                // One comma: "50,000" (thousand sep) or "1,5" (decimal - explicitly rejected in VND)
                val parts = cleaned.split(",")
                if (parts.size == 2 && parts[1].length == 3) {
                    // Thousand separator: "50,000"
                    cleaned.replace(",", "").toLongOrNull()
                } else {
                    null
                }
            }
            else -> null
        }
    }

    /**
     * Try to extract money amount from natural language text.
     * Returns first parseable amount found.
     */
    private fun extractFromText(text: String): Long? {
        // Already tried all patterns on full text - look for embedded amounts
        val patterns = listOf(
            TRIỆU_REGEX, WHOLE_TRIỆU_REGEX, K_REGEX, WHOLE_K_REGEX
        )
        for (pattern in patterns) {
            val result = pattern.find(text)
            if (result != null) {
                return parse(result.value)
            }
        }
        return null
    }
}

/**
 * MoneyFormatter: Format Long VND amount for display.
 * 
 * Examples:
 *   35000   -> "35.000đ"
 *   1500000 -> "1.500.000đ"
 */
object MoneyFormatter {

    /**
     * Format amount to Vietnamese currency display format.
     * Outputs: "35.000đ", "1.500.000đ"
     */
    fun format(amount: Long): String {
        val formatted = String.format(Locale("vi", "VN"), "%,d", amount)
            .replace(",", ".")
        return "${formatted}đ"
    }

    /**
     * Alias for format.
     */
    fun formatVND(amount: Long): String = format(amount)

    /**
     * Format without the đ symbol
     */
    fun formatNoSymbol(amount: Long): String {
        return String.format(Locale("vi", "VN"), "%,d", amount)
            .replace(",", ".")
    }

    /**
     * Compact format: 1.500.000đ -> "1,5tr" | 35.000đ -> "35k"
     */
    fun formatCompact(amount: Long): String {
        return when {
            amount >= 1_000_000L && amount % 1_000_000L == 0L -> {
                "${amount / 1_000_000L} triệu"
            }
            amount >= 1_000_000L -> {
                val mil = amount / 1_000_000L
                val rem = (amount % 1_000_000L) / 100_000L
                if (rem == 0L) "${mil}tr" else "${mil},${rem}tr"
            }
            amount >= 1_000L && amount % 1_000L == 0L -> {
                "${amount / 1_000L}k"
            }
            else -> format(amount)
        }
    }
}
