package com.quanlychitieuapp.domain.model

data class Debt(
    val id: Long = 0,
    val personId: Long,
    val personName: String = "", // Loaded via JOIN
    val type: DebtType,
    val principalAmount: Long,
    val paidAmount: Long = 0,
    val walletId: Long,
    val description: String? = null,
    val dueDate: Long? = null,
    val paymentStatus: PaymentStatus,
    val splitExpenseId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val remainingAmount: Long get() = principalAmount - paidAmount
    val isOverdue: Boolean get() =
        dueDate != null && dueDate < System.currentTimeMillis() && paymentStatus != PaymentStatus.PAID
}

enum class DebtType { LENDING, BORROWING }
enum class PaymentStatus { ACTIVE, PARTIALLY_PAID, PAID }
