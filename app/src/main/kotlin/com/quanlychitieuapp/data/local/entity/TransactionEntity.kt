package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("walletId"),
        Index("categoryKey"),
        Index("transactionDate"),
        Index("type")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,               // "expense" or "income"
    val amount: Long,               // VND as Long - NO Float/Double
    val categoryKey: String,        // stable category key
    val walletId: Long,
    val description: String = "",
    val transactionDate: Long,      // epoch millis
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
