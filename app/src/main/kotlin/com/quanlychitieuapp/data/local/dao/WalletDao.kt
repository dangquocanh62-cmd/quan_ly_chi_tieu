package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY isDefault DESC, createdAt ASC")
    fun getAllWallets(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletEntity?

    @Query("SELECT * FROM wallets WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWallet(): WalletEntity?

    @Query("SELECT SUM(balance) FROM wallets")
    fun getTotalBalance(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Transaction
    suspend fun updateBalance(id: Long, delta: Long, now: Long = System.currentTimeMillis()) {
        val wallet = getWalletById(id)
        if (wallet != null) {
            updateWallet(wallet.copy(balance = wallet.balance + delta, updatedAt = now))
        }
    }

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :walletId")
    suspend fun countTransactionsForWallet(walletId: Long): Int

    @Query("SELECT COUNT(*) FROM transfers WHERE fromWalletId = :walletId OR toWalletId = :walletId")
    suspend fun countTransfersForWallet(walletId: Long): Int

    @Query("SELECT * FROM wallets ORDER BY createdAt ASC")
    suspend fun getAllWalletsSnapshot(): List<WalletEntity>

    @Query("DELETE FROM wallets")
    suspend fun deleteAll()

    @Query("UPDATE wallets SET isDefault = 0")
    suspend fun clearAllDefaultWallets()

    @Query("UPDATE wallets SET isDefault = 1 WHERE id = :id")
    suspend fun setWalletAsDefault(id: Long)

    @Transaction
    suspend fun setDefaultWalletTransaction(newDefaultId: Long) {
        clearAllDefaultWallets()
        setWalletAsDefault(newDefaultId)
    }
}
