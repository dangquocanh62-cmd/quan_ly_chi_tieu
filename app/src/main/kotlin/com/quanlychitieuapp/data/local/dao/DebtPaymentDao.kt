package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

data class DebtPaymentWithDebtAndPerson(
    @Embedded val payment: DebtPaymentEntity,
    @ColumnInfo(name = "debtType") val debtType: String,
    @ColumnInfo(name = "personName") val personName: String
)

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC, createdAt DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>
    
    @Query("""
        SELECT dp.*, d.type AS debtType, p.name AS personName 
        FROM debt_payments dp 
        INNER JOIN debts d ON dp.debtId = d.id 
        INNER JOIN persons p ON d.personId = p.id 
        WHERE dp.walletId = :walletId 
          AND dp.paymentDate >= :startTimestamp 
          AND dp.paymentDate < :endTimestampExclusive
    """)
    fun getDebtPaymentsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<DebtPaymentWithDebtAndPerson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: DebtPaymentEntity): Long
}
