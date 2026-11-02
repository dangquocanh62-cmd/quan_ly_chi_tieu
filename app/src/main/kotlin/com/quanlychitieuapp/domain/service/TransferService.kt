package com.quanlychitieuapp.domain.service

import androidx.room.withTransaction
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.TransferEntity
import com.quanlychitieuapp.domain.model.Transfer

/**
 * TransferService - atomic wallet-to-wallet transfers.
 * 
 * CRITICAL: source.balance -= amount AND destination.balance += amount
 * must be atomic. Uses Room @Transaction to ensure this.
 * Transfer does NOT change total asset value.
 */
class TransferService(private val db: AppDatabase) {

    /**
     * Execute a transfer atomically.
     * Both balance updates happen in the same Room transaction.
     */
    suspend fun transfer(
        fromWalletId: Long,
        toWalletId: Long,
        amount: Long,
        description: String,
        transferDate: Long
    ): Result<Long> = runCatching {
        require(amount > 0) { "Số tiền chuyển phải lớn hơn 0" }
        require(fromWalletId != toWalletId) { "Không thể chuyển tiền cho cùng một ví" }

        val fromWallet = db.walletDao().getWalletById(fromWalletId)
            ?: error("Không tìm thấy ví nguồn")
        db.walletDao().getWalletById(toWalletId)
            ?: error("Không tìm thấy ví đích")

        // Use Room transaction for atomicity
        db.withTransaction {
            // Deduct from source
            db.walletDao().updateBalance(fromWalletId, -amount)
            // Add to destination
            db.walletDao().updateBalance(toWalletId, +amount)
            // Record the transfer
            db.transferDao().insertTransfer(
                TransferEntity(
                    fromWalletId = fromWalletId,
                    toWalletId = toWalletId,
                    amount = amount,
                    description = description,
                    transferDate = transferDate
                )
            )
        }
    }

    /**
     * Delete a transfer and reverse balance changes atomically.
     */
    suspend fun deleteTransfer(transfer: Transfer): Result<Unit> = runCatching {
        db.withTransaction {
            // Reverse: add back to source, deduct from destination
            db.walletDao().updateBalance(transfer.fromWalletId, +transfer.amount)
            db.walletDao().updateBalance(transfer.toWalletId, -transfer.amount)
            db.transferDao().deleteTransfer(
                com.quanlychitieuapp.data.local.entity.TransferEntity(
                    id = transfer.id,
                    fromWalletId = transfer.fromWalletId,
                    toWalletId = transfer.toWalletId,
                    amount = transfer.amount,
                    description = transfer.description,
                    transferDate = transfer.transferDate,
                    createdAt = transfer.createdAt
                )
            )
        }
    }
}
