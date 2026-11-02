package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC, createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions
        WHERE (:type IS NULL OR type = :type)
          AND (:walletId IS NULL OR walletId = :walletId)
          AND (:categoryKey IS NULL OR categoryKey = :categoryKey)
          AND transactionDate BETWEEN :startDate AND :endDate
        ORDER BY transactionDate DESC, createdAt DESC
    """)
    fun getFilteredTransactions(
        type: String? = null,
        walletId: Long? = null,
        categoryKey: String? = null,
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT * FROM transactions
        WHERE walletId = :walletId
          AND transactionDate >= :startTimestamp 
          AND transactionDate < :endTimestampExclusive
    """)
    fun getTransactionsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (description LIKE '%' || :query || '%')
        ORDER BY transactionDate DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
          AND transactionDate BETWEEN :startDate AND :endDate
    """)
    suspend fun sumByType(type: String, startDate: Long, endDate: Long): Long

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = 'expense'
          AND categoryKey = :categoryKey
          AND transactionDate BETWEEN :startDate AND :endDate
    """)
    suspend fun sumExpenseByCategory(categoryKey: String, startDate: Long, endDate: Long): Long

    @Query("""
        SELECT categoryKey, SUM(amount) as total
        FROM transactions
        WHERE type = 'expense'
          AND transactionDate BETWEEN :startDate AND :endDate
        GROUP BY categoryKey
        ORDER BY total DESC
    """)
    suspend fun getCategoryTotals(startDate: Long, endDate: Long): List<CategoryTotal>

    @Query("""
        SELECT * FROM transactions
        ORDER BY transactionDate DESC
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY transactionDate DESC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}


data class CategoryTotal(
    val categoryKey: String,
    val total: Long
)
