package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Transaction types
 */
enum class TransactionType(val key: String) {
    EXPENSE("expense"),
    INCOME("income");

    companion object {
        fun fromKey(key: String): TransactionType =
            values().find { it.key == key } ?: EXPENSE
    }
}

/**
 * Category keys - stable identifiers used in DB
 * NEVER use Vietnamese names or emoji as keys
 */
object CategoryKeys {
    // Expense categories
    const val FOOD = "food"
    const val SHOPPING = "shopping"
    const val TRANSPORT = "transport"
    const val ENTERTAINMENT = "entertainment"
    const val EDUCATION = "education"
    const val LIVING = "living"
    const val HEALTH = "health"
    const val TELECOM = "telecom"
    const val OTHER_EXPENSE = "other"

    // Income categories
    const val SALARY = "salary"
    const val BONUS = "bonus"
    const val FREELANCE = "freelance"
    const val OTHER_INCOME = "other_income"
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val key: String,            // stable identifier (e.g. "food")
    val name: String,           // display name in Vietnamese
    val iconKey: String,        // icon identifier (NOT emoji)
    val type: String,           // "expense" or "income"
    val isDefault: Boolean = true
)
