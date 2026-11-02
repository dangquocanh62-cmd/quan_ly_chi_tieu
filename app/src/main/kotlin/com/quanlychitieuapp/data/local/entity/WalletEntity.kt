package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Wallet types - use string keys in DB, map to display in UI layer
 */
enum class WalletType(val key: String, val displayName: String, val iconKey: String) {
    CASH("cash", "Tiền mặt", "cash"),
    BANK("bank", "Ngân hàng", "bank"),
    MOMO("momo", "MoMo", "momo"),
    OTHER("other", "Khác", "wallet");

    companion object {
        fun fromKey(key: String): WalletType =
            values().find { it.key == key } ?: OTHER
    }
}

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,          // WalletType.key
    val balance: Long = 0L,    // VND stored as Long (no float/double)
    val iconKey: String,       // icon identifier, NOT emoji
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
