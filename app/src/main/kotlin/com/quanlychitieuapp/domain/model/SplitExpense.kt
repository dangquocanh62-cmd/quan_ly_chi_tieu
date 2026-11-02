package com.quanlychitieuapp.domain.model

data class SplitExpense(
    val id: Long = 0,
    val totalAmount: Long,
    val walletId: Long,
    val categoryKey: String,
    val description: String,
    val transactionDate: Long,
    val splitMethod: String,
    val linkedTransactionId: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
