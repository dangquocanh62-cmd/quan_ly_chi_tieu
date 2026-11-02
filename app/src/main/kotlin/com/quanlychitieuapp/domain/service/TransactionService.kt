package com.quanlychitieuapp.domain.service

import com.quanlychitieuapp.data.local.dao.TransactionDao
import com.quanlychitieuapp.data.local.dao.TransferDao
import com.quanlychitieuapp.data.local.dao.WalletDao
import com.quanlychitieuapp.data.local.entity.TransactionEntity
import com.quanlychitieuapp.data.local.entity.TransferEntity
import com.quanlychitieuapp.domain.model.Transaction
import com.quanlychitieuapp.domain.model.Transfer

/**
 * TransactionService - core financial logic.
 * 
 * ALL operations must maintain balance integrity:
 * - Add expense: wallet.balance -= amount
 * - Add income: wallet.balance += amount
 * - Edit: reverse old balance change, apply new
 * - Delete: reverse balance change
 * - Transfer: source -= amount, destination += amount (atomic)
 * 
 * NO floating point. NO double-subtract. NO double-add.
 */
class TransactionService(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao
) {

    /**
     * Add a new expense transaction.
     * wallet.balance -= amount
     */
    suspend fun addExpense(
        amount: Long,
        categoryKey: String,
        walletId: Long,
        description: String,
        transactionDate: Long
    ): Result<Long> = runCatching {
        require(amount > 0) { "Số tiền phải lớn hơn 0" }
        val wallet = walletDao.getWalletById(walletId)
            ?: error("Không tìm thấy ví")

        val entity = TransactionEntity(
            type = "expense",
            amount = amount,
            categoryKey = categoryKey,
            walletId = walletId,
            description = description,
            transactionDate = transactionDate
        )
        val txId = transactionDao.insertTransaction(entity)
        // Update balance: expense decreases balance
        walletDao.updateBalance(walletId, -amount)
        txId
    }

    /**
     * Add a new income transaction.
     * wallet.balance += amount
     */
    suspend fun addIncome(
        amount: Long,
        categoryKey: String,
        walletId: Long,
        description: String,
        transactionDate: Long
    ): Result<Long> = runCatching {
        require(amount > 0) { "Số tiền phải lớn hơn 0" }
        walletDao.getWalletById(walletId) ?: error("Không tìm thấy ví")

        val entity = TransactionEntity(
            type = "income",
            amount = amount,
            categoryKey = categoryKey,
            walletId = walletId,
            description = description,
            transactionDate = transactionDate
        )
        val txId = transactionDao.insertTransaction(entity)
        // Update balance: income increases balance
        walletDao.updateBalance(walletId, +amount)
        txId
    }

    /**
     * Edit a transaction.
     * MUST reverse old balance change, then apply new balance change.
     * 
     * Old expense (amount=100, walletId=1) -> balance[1] was decreased by 100
     * New expense (amount=200, walletId=1) -> reverse (+100), apply (-200) = net -100 more
     */
    suspend fun editTransaction(
        old: Transaction,
        newAmount: Long,
        newCategoryKey: String,
        newWalletId: Long,
        newDescription: String,
        newDate: Long
    ): Result<Unit> = runCatching {
        require(newAmount > 0) { "Số tiền phải lớn hơn 0" }
        walletDao.getWalletById(newWalletId) ?: error("Không tìm thấy ví mới")

        // Step 1: Reverse old balance change
        val oldDelta = if (old.type == "expense") +old.amount else -old.amount
        walletDao.updateBalance(old.walletId, oldDelta)

        // Step 2: If wallet changed, old wallet already reversed above, apply new
        // Step 3: Apply new balance change
        val newDelta = if (old.type == "expense") -newAmount else +newAmount
        // Note: if wallet changed, handle both wallets
        if (old.walletId != newWalletId) {
            // Old wallet already reversed; now apply new wallet
            walletDao.updateBalance(newWalletId, newDelta)
        } else {
            walletDao.updateBalance(newWalletId, newDelta)
        }

        // Step 4: Update transaction record
        val updated = TransactionEntity(
            id = old.id,
            type = old.type,
            amount = newAmount,
            categoryKey = newCategoryKey,
            walletId = newWalletId,
            description = newDescription,
            transactionDate = newDate,
            createdAt = old.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updated)
    }

    /**
     * Delete a transaction.
     * MUST reverse the balance change.
     */
    suspend fun deleteTransaction(transaction: Transaction): Result<Unit> = runCatching {
        // Reverse balance change
        val delta = if (transaction.type == "expense") +transaction.amount else -transaction.amount
        walletDao.updateBalance(transaction.walletId, delta)

        val entity = TransactionEntity(
            id = transaction.id,
            type = transaction.type,
            amount = transaction.amount,
            categoryKey = transaction.categoryKey,
            walletId = transaction.walletId,
            description = transaction.description,
            transactionDate = transaction.transactionDate,
            createdAt = transaction.createdAt
        )
        transactionDao.deleteTransaction(entity)
    }
}
