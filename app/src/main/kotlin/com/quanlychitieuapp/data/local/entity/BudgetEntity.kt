package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Budget tracking per category per month/year.
 * alert80Sent and alert100Sent prevent repeated notifications.
 * Reset at start of new month.
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryKey", "month", "year"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryKey: String,    // stable key
    val amountLimit: Long,      // VND as Long
    val month: Int,             // 1-12
    val year: Int,              // e.g. 2026
    val alert80Sent: Boolean = false,
    val alert100Sent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
