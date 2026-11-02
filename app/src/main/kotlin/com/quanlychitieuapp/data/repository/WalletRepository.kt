package com.quanlychitieuapp.data.repository

import com.quanlychitieuapp.data.local.dao.WalletDao
import com.quanlychitieuapp.data.local.entity.WalletEntity
import com.quanlychitieuapp.domain.model.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletRepository(private val walletDao: WalletDao) {

    fun getAllWallets(): Flow<List<Wallet>> =
        walletDao.getAllWallets().map { list -> list.map { it.toDomain() } }

    fun getTotalBalance(): Flow<Long> =
        walletDao.getTotalBalance().map { it ?: 0L }

    suspend fun getWalletById(id: Long): Wallet? =
        walletDao.getWalletById(id)?.toDomain()

    suspend fun getDefaultWallet(): Wallet? =
        walletDao.getDefaultWallet()?.toDomain()

    suspend fun setDefaultWallet(walletId: Long) =
        walletDao.setDefaultWalletTransaction(walletId)

    suspend fun insertWallet(wallet: Wallet): Long =
        walletDao.insertWallet(wallet.toEntity())

    suspend fun updateWallet(wallet: Wallet) =
        walletDao.updateWallet(wallet.toEntity())

    suspend fun updateBalance(id: Long, delta: Long) =
        walletDao.updateBalance(id, delta)

    suspend fun deleteWallet(wallet: Wallet) {
        val txCount = walletDao.countTransactionsForWallet(wallet.id)
        val trCount = walletDao.countTransfersForWallet(wallet.id)
        require(txCount == 0 && trCount == 0) {
            "Không thể xóa ví có giao dịch. Vui lòng xóa giao dịch trước."
        }
        walletDao.deleteWallet(wallet.toEntity())
    }

    suspend fun canDeleteWallet(walletId: Long): Boolean {
        val txCount = walletDao.countTransactionsForWallet(walletId)
        val trCount = walletDao.countTransfersForWallet(walletId)
        return txCount == 0 && trCount == 0
    }

    // Mapping extensions
    private fun WalletEntity.toDomain() = Wallet(
        id = id, name = name, type = type, balance = balance,
        iconKey = iconKey, isDefault = isDefault, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun Wallet.toEntity() = WalletEntity(
        id = id, name = name, type = type, balance = balance,
        iconKey = iconKey, isDefault = isDefault, createdAt = createdAt, updatedAt = updatedAt
    )
}
