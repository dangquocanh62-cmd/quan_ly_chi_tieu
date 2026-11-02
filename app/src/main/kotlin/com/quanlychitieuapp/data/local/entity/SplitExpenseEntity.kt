package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quanlychitieuapp.domain.model.SplitExpense

@Entity(
    tableName = "split_expenses",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("walletId")]
)
data class SplitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalAmount: Long,
    val walletId: Long,
    val categoryKey: String,
    val description: String,
    val transactionDate: Long,
    val splitMethod: String,
    val linkedTransactionId: Long?, // Actual transaction of the owner
    val createdAt: Long,
    val updatedAt: Long
)

fun SplitExpenseEntity.toDomain() = SplitExpense(
    id = id,
    totalAmount = totalAmount,
    walletId = walletId,
    categoryKey = categoryKey,
    description = description,
    transactionDate = transactionDate,
    splitMethod = splitMethod,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SplitExpense.toEntity() = SplitExpenseEntity(
    id = id,
    totalAmount = totalAmount,
    walletId = walletId,
    categoryKey = categoryKey,
    description = description,
    transactionDate = transactionDate,
    splitMethod = splitMethod,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
