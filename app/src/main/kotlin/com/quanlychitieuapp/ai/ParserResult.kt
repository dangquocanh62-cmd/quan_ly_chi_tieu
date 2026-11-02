package com.quanlychitieuapp.ai

import com.quanlychitieuapp.data.local.entity.CategoryKeys

enum class DebtDirection {
    BORROW,
    LEND
}

enum class MissingDebtField {
    PERSON,
    AMOUNT
}

data class PendingDebt(
    val direction: DebtDirection,
    val person: String?,
    val amount: Long?,
    val description: String? = null
)

/**
 * Represents the result of parsing a natural language expense input
 */
sealed class ParserResult {
    data class Success(
        val intent: String,         // "expense", "income", "split_expense"
        val amount: Long,           // VND as Long
        val categoryKey: String?,   // stable DB key, nullable for debt
        val walletKey: String? = null, // optional wallet hint
        val description: String = "",
        val date: String = "today", // "today", "yesterday", or ISO date
        val confidence: Float = 1.0f,
        val personName: String? = null,
        val participants: List<String>? = null, // For split_expense
        val splitMethod: String? = null // For split_expense
    ) : ParserResult()

    data class DebtSuccess(
        val direction: DebtDirection,
        val amount: Long,
        val personName: String,
        val walletKey: String? = null,
        val description: String = "",
        val confidence: Float = 1.0f
    ) : ParserResult()

    data class NeedMoreDebtInfo(
        val debt: PendingDebt,
        val missingField: MissingDebtField,
        val message: String
    ) : ParserResult()

    data class LowConfidence(
        val message: String = "⚠️ Mình chưa hiểu khoản này. Bạn có thể nhập lại."
    ) : ParserResult()

    data class ParseError(
        val message: String
    ) : ParserResult()
}
