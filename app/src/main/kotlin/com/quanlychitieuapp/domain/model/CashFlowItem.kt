package com.quanlychitieuapp.domain.model

enum class CashFlowSource {
    TRANSACTION,
    TRANSFER,
    DEBT,
    DEBT_PAYMENT
}

enum class CashFlowDirection {
    IN,
    OUT
}

data class CashFlowItem(
    val sourceType: CashFlowSource,
    val sourceId: Long,
    val title: String,
    val description: String?,
    val amount: Long,
    val direction: CashFlowDirection,
    val timestamp: Long,
    val iconKey: String
)
