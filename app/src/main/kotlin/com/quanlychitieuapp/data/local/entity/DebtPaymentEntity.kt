package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.quanlychitieuapp.domain.model.DebtPayment

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(entity = DebtEntity::class, parentColumns = ["id"], childColumns = ["debtId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"])
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val walletId: Long,
    val paymentDate: Long,
    val note: String? = null,
    val createdAt: Long
)

fun DebtPaymentEntity.toDomain() = DebtPayment(
    id, debtId, amount, walletId, paymentDate, note, createdAt
)

fun DebtPayment.toEntity() = DebtPaymentEntity(
    id, debtId, amount, walletId, paymentDate, note, createdAt
)
