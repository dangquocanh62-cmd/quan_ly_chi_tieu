package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY transferDate DESC")
    fun getAllTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): TransferEntity?

    @Query("""
        SELECT * FROM transfers
        WHERE transferDate BETWEEN :startDate AND :endDate
        ORDER BY transferDate DESC
    """)
    fun getTransfersInRange(startDate: Long, endDate: Long): Flow<List<TransferEntity>>

    @Query("""
        SELECT * FROM transfers
        WHERE (fromWalletId = :walletId OR toWalletId = :walletId)
          AND transferDate >= :startTimestamp 
          AND transferDate < :endTimestampExclusive
    """)
    fun getTransfersForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<TransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity): Long

    @Delete
    suspend fun deleteTransfer(transfer: TransferEntity)

    @Query("SELECT * FROM transfers ORDER BY transferDate DESC")
    suspend fun getAllTransfersSnapshot(): List<TransferEntity>

    @Query("DELETE FROM transfers")
    suspend fun deleteAll()
}
