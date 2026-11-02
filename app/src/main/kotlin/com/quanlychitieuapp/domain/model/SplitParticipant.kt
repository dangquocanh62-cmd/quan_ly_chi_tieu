package com.quanlychitieuapp.domain.model

data class SplitParticipant(
    val id: Long = 0,
    val splitExpenseId: Long,
    val personId: Long?,
    val personName: String? = null,
    val shareAmount: Long,
    val isOwner: Boolean,
    val linkedDebtId: Long?
)
