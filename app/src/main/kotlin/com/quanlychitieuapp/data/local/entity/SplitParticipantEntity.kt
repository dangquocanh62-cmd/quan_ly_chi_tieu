package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quanlychitieuapp.domain.model.SplitParticipant

@Entity(
    tableName = "split_participants",
    foreignKeys = [
        ForeignKey(
            entity = SplitExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["splitExpenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("splitExpenseId"),
        Index("personId")
    ]
)
data class SplitParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val splitExpenseId: Long,
    val personId: Long?, // Null for owner
    val shareAmount: Long,
    val isOwner: Boolean,
    val linkedDebtId: Long? // The receivable debt created for this non-owner
)

fun SplitParticipantEntity.toDomain(personName: String? = null) = SplitParticipant(
    id = id,
    splitExpenseId = splitExpenseId,
    personId = personId,
    personName = personName,
    shareAmount = shareAmount,
    isOwner = isOwner,
    linkedDebtId = linkedDebtId
)

fun SplitParticipant.toEntity() = SplitParticipantEntity(
    id = id,
    splitExpenseId = splitExpenseId,
    personId = personId,
    shareAmount = shareAmount,
    isOwner = isOwner,
    linkedDebtId = linkedDebtId
)
