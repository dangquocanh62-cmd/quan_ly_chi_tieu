package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.domain.model.PaymentStatus

@Entity(
    tableName = "debts",
    foreignKeys = [
        ForeignKey(entity = PersonEntity::class, parentColumns = ["id"], childColumns = ["personId"]),
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["walletId"])
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val type: String,              // LENDING | BORROWING
    val principalAmount: Long,
    val paidAmount: Long = 0,
    val walletId: Long,
    val description: String? = null,
    val dueDate: Long? = null,
    val paymentStatus: String,     // ACTIVE | PARTIALLY_PAID | PAID
    val splitExpenseId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

fun DebtEntity.toDomain() = Debt(
    id = id,
    personId = personId,
// Note: personName is loaded via join and must be populated separately or via DebtWithPerson
    type = DebtType.valueOf(type),
    principalAmount = principalAmount,
    paidAmount = paidAmount,
    walletId = walletId,
    description = description,
    dueDate = dueDate,
    paymentStatus = PaymentStatus.valueOf(paymentStatus),
    splitExpenseId = splitExpenseId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun com.quanlychitieuapp.data.local.dao.DebtWithPerson.toDomain() = debt.toDomain().copy(
    personName = personName
)

fun Debt.toEntity() = DebtEntity(
    id = id,
    personId = personId,
    type = type.name,
    principalAmount = principalAmount,
    paidAmount = paidAmount,
    walletId = walletId,
    description = description,
    dueDate = dueDate,
    paymentStatus = paymentStatus.name,
    splitExpenseId = splitExpenseId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
