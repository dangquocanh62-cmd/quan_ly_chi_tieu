package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

data class DebtWithPerson(
    @Embedded val debt: DebtEntity,
    @ColumnInfo(name = "personName") val personName: String
)

@Dao
interface DebtDao {
    @Query("""
        SELECT d.*, p.name AS personName 
        FROM debts d 
        INNER JOIN persons p ON d.personId = p.id 
        ORDER BY d.createdAt DESC
    """)
    fun getAllDebtsWithPerson(): Flow<List<DebtWithPerson>>

    @Query("""
        SELECT d.*, p.name AS personName 
        FROM debts d 
        INNER JOIN persons p ON d.personId = p.id 
        WHERE d.walletId = :walletId 
          AND (
               (d.createdAt >= :startTimestamp AND d.createdAt < :endTimestampExclusive)
          )
    """)
    fun getDebtsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<DebtWithPerson>>

    @Query("""
        SELECT d.*, p.name AS personName 
        FROM debts d 
        INNER JOIN persons p ON d.personId = p.id 
        WHERE d.id = :id
    """)
    suspend fun getDebtWithPersonById(id: Long): DebtWithPerson?

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity): Long

    @Update
    suspend fun update(debt: DebtEntity)

    @Delete
    suspend fun delete(debt: DebtEntity)
}
