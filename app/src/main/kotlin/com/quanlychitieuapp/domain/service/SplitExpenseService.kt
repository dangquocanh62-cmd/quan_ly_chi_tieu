package com.quanlychitieuapp.domain.service

import androidx.room.withTransaction
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.SplitExpenseEntity
import com.quanlychitieuapp.data.local.entity.SplitParticipantEntity
import com.quanlychitieuapp.data.local.entity.toDomain
import com.quanlychitieuapp.data.repository.DebtRepository
import com.quanlychitieuapp.domain.model.SplitExpense
import com.quanlychitieuapp.domain.model.SplitParticipant

/**
 * Service for Split Expense operations ensuring atomicity and data integrity.
 * Integrates directly with TransactionService (for actual expense) and DebtRepository (for receivables).
 */
class SplitExpenseService(
    private val db: AppDatabase,
    private val transactionService: TransactionService,
    private val debtRepository: DebtRepository
) {
    private val splitDao = db.splitExpenseDao()
    private val debtDao = db.debtDao()
    private val personDao = db.personDao()

    data class ParticipantInput(
        val personName: String?, // null if Owner
        val manuallyEnteredShare: Long? // null if EQUAL mode
    )

    /**
     * Creates a Split Expense.
     * Calculates shares if EQUAL mode, validates if CUSTOM mode.
     * Records the actual Expense for the Owner, and Lending Debts for others.
     */
    suspend fun createSplitExpense(
        totalAmount: Long,
        categoryKey: String,
        walletId: Long,
        description: String,
        splitMethod: String, // "equal" or "custom"
        participants: List<ParticipantInput>,
        transactionDate: Long
    ): Result<Long> = runCatching {
        require(totalAmount > 0) { "Tổng số tiền phải lớn hơn 0" }
        require(participants.isNotEmpty()) { "Phải có người tham gia chia" }
        
        val hasOwner = participants.any { it.personName == null }
        require(hasOwner) { "Bắt buộc phải có Bạn (Chủ chi) trong danh sách" }

        val shares = calculateShares(totalAmount, splitMethod, participants)
        
        db.withTransaction {
            val now = System.currentTimeMillis()
            // 1. Create SplitExpense shell
            val splitExpenseId = splitDao.insertSplitExpense(
                SplitExpenseEntity(
                    totalAmount = totalAmount,
                    walletId = walletId,
                    categoryKey = categoryKey,
                    description = description,
                    transactionDate = transactionDate,
                    splitMethod = splitMethod,
                    linkedTransactionId = null,
                    createdAt = now,
                    updatedAt = now
                )
            )

            val participantEntities = mutableListOf<SplitParticipantEntity>()
            var linkedTransactionId: Long? = null

            for (shareInfo in shares) {
                if (shareInfo.isOwner) {
                    if (shareInfo.amount > 0) {
                        val ownerExpenseId = transactionService.addExpense(
                            amount = shareInfo.amount,
                            categoryKey = categoryKey,
                            walletId = walletId,
                            description = description,
                            transactionDate = transactionDate
                        ).getOrThrow()
                        linkedTransactionId = ownerExpenseId
                    }
                    participantEntities.add(
                        SplitParticipantEntity(
                            splitExpenseId = splitExpenseId,
                            personId = null,
                            shareAmount = shareInfo.amount,
                            isOwner = true,
                            linkedDebtId = null
                        )
                    )
                } else {
                    if (shareInfo.amount > 0) {
                        val linkedDebtId = debtRepository.createLending(
                            personName = shareInfo.personName!!,
                            amount = shareInfo.amount,
                            walletId = walletId,
                            dueDate = null,
                            description = "Từ chi tiêu chung: $description",
                            splitExpenseId = splitExpenseId
                        )
                        val personId = personDao.findByNormalizedName(shareInfo.personName.trim().replace(Regex("\\s+"), " ").lowercase())?.id
                            ?: throw Exception("Lỗi không tìm thấy Person")
                            
                        participantEntities.add(
                            SplitParticipantEntity(
                                splitExpenseId = splitExpenseId,
                                personId = personId,
                                shareAmount = shareInfo.amount,
                                isOwner = false,
                                linkedDebtId = linkedDebtId
                            )
                        )
                    }
                }
            }

            // Update split expense with linkedTransactionId
            val finalSplit = splitDao.getSplitExpenseById(splitExpenseId)?.splitExpense
                ?.copy(linkedTransactionId = linkedTransactionId)
            finalSplit?.let { splitDao.updateSplitExpense(it) }

            splitDao.insertParticipants(participantEntities)
            splitExpenseId
        }
    }

    /**
     * Delete Split Expense reverting all linked data.
     * Fails if any linked Debt has payment > 0.
     */
    suspend fun deleteSplitExpense(splitExpenseId: Long): Result<Unit> = runCatching {
        db.withTransaction {
            val splitWithPart = splitDao.getSplitExpenseById(splitExpenseId)
                ?: throw Exception("Không tìm thấy khoản chia chi tiêu")

            // Validate payments
            for (p in splitWithPart.participants) {
                if (!p.isOwner && p.linkedDebtId != null) {
                    val debt = debtDao.getById(p.linkedDebtId)
                    if (debt != null && debt.paidAmount > 0) {
                        throw IllegalStateException("Không thể xóa khoản chi này vì khoản nợ liên quan đã có lịch sử thanh toán.")
                    }
                }
            }

            // Perform rollback
            for (p in splitWithPart.participants) {
                if (!p.isOwner && p.linkedDebtId != null) {
                    debtRepository.deleteDebt(p.linkedDebtId)
                }
            }
            if (splitWithPart.splitExpense.linkedTransactionId != null) {
                val tx = db.transactionDao().getTransactionById(splitWithPart.splitExpense.linkedTransactionId)
                if (tx != null) {
                    val domainTx = com.quanlychitieuapp.domain.model.Transaction(
                        id = tx.id, type = tx.type, amount = tx.amount, categoryKey = tx.categoryKey,
                        walletId = tx.walletId, description = tx.description,
                        transactionDate = tx.transactionDate, createdAt = tx.createdAt, updatedAt = tx.updatedAt
                    )
                    transactionService.deleteTransaction(domainTx)
                }
            }

            splitDao.deleteSplitExpense(splitWithPart.splitExpense)
        }
    }

    /**
     * Edits Split Expense by atomically rolling back old, and creating new.
     * Fails if any linked Debt has payment > 0.
     */
    suspend fun editSplitExpense(
        splitExpenseId: Long,
        totalAmount: Long,
        categoryKey: String,
        walletId: Long,
        description: String,
        splitMethod: String,
        participants: List<ParticipantInput>,
        transactionDate: Long
    ): Result<Long> = runCatching {
        db.withTransaction {
            // This natively validates paidAmount = 0
            deleteSplitExpense(splitExpenseId).getOrThrow()
            
            createSplitExpense(
                totalAmount = totalAmount,
                categoryKey = categoryKey,
                walletId = walletId,
                description = description,
                splitMethod = splitMethod,
                participants = participants,
                transactionDate = transactionDate
            ).getOrThrow()
        }
    }

    data class ShareOutput(val personName: String?, val isOwner: Boolean, val amount: Long)

    private fun calculateShares(
        total: Long,
        method: String,
        inputs: List<ParticipantInput>
    ): List<ShareOutput> {
        val outputs = mutableListOf<ShareOutput>()
        
        if (method == "equal") {
            val baseShare = total / inputs.size
            val remainder = total % inputs.size
            
            inputs.forEach { input ->
                val isOwner = input.personName == null
                // owner gets the remainder cents to balance precisely
                val finalShare = if (isOwner) baseShare + remainder else baseShare
                outputs.add(ShareOutput(personName = input.personName, isOwner = isOwner, amount = finalShare))
            }
        } else if (method == "custom") {
            var sum = 0L
            inputs.forEach { input ->
                val amount = input.manuallyEnteredShare ?: throw Exception("Thiếu số tiền trong Custom Split")
                require(amount >= 0) { "Số tiền không được âm" }
                sum += amount
                outputs.add(ShareOutput(personName = input.personName, isOwner = (input.personName == null), amount = amount))
            }
            require(sum == total) { "Tổng số tiền chia ($sum) không khớp với hóa đơn ($total)" }
        } else {
            throw IllegalArgumentException("Phương thức chia '$method' không đúng")
        }
        
        return outputs
    }
}
