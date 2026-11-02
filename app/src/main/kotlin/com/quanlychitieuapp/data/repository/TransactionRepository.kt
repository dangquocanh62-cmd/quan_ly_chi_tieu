package com.quanlychitieuapp.data.repository

import com.quanlychitieuapp.data.local.dao.TransactionDao
import com.quanlychitieuapp.data.local.dao.WalletDao
import com.quanlychitieuapp.data.local.entity.TransactionEntity
import com.quanlychitieuapp.domain.model.CategoryStat
import com.quanlychitieuapp.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao
) {

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    fun getFilteredTransactions(
        type: String? = null,
        walletId: Long? = null,
        categoryKey: String? = null,
        startDate: Long,
        endDate: Long
    ): Flow<List<Transaction>> =
        transactionDao.getFilteredTransactions(type, walletId, categoryKey, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }

    fun searchTransactions(query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(query).map { list -> list.map { it.toDomain() } }

    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }

    suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    suspend fun sumByType(type: String, startDate: Long, endDate: Long): Long =
        transactionDao.sumByType(type, startDate, endDate)

    suspend fun getCategoryStats(startDate: Long, endDate: Long): List<CategoryStat> {
        val totals = transactionDao.getCategoryTotals(startDate, endDate)
        val totalAmount = totals.sumOf { it.total }
        return totals.map { ct ->
            CategoryStat(
                categoryKey = ct.categoryKey,
                amount = ct.total,
                percentage = if (totalAmount > 0) (ct.total.toFloat() / totalAmount) * 100f else 0f
            )
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction.toEntity())

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction.toEntity())

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction.toEntity())

    // Mapping
    private fun TransactionEntity.toDomain() = Transaction(
        id = id, type = type, amount = amount, categoryKey = categoryKey,
        walletId = walletId, description = description,
        transactionDate = transactionDate, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id, type = type, amount = amount, categoryKey = categoryKey,
        walletId = walletId, description = description,
        transactionDate = transactionDate, createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}
