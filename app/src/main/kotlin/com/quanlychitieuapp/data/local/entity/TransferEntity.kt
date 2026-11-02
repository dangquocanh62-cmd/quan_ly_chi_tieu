package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transfer between wallets.
 * Transfer is NOT expense or income - it does NOT change total assets.
 * source.balance -= amount, destination.balance += amount (atomic via @Transaction)
 */
@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromWalletId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("fromWalletId"),
        Index("toWalletId"),
        Index("transferDate")
    ]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromWalletId: Long,
    val toWalletId: Long,
    val amount: Long,           // VND as Long - NO Float/Double
    val description: String = "",
    val transferDate: Long,     // epoch millis
    val createdAt: Long = System.currentTimeMillis()
)
