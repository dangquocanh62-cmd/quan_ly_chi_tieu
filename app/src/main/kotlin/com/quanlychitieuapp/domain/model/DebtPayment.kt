package com.quanlychitieuapp.domain.model

data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val walletId: Long,
    val paymentDate: Long,
    val note: String? = null,
    val createdAt: Long
)
