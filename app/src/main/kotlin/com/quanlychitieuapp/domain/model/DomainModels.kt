package com.quanlychitieuapp.domain.model

/**
 * Domain model for Wallet (presentation-agnostic)
 */
data class Wallet(
    val id: Long = 0,
    val name: String,
    val type: String,
    val balance: Long,
    val iconKey: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Transaction(
    val id: Long = 0,
    val type: String,
    val amount: Long,
    val categoryKey: String,
    val walletId: Long,
    val walletName: String = "",
    val description: String = "",
    val transactionDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Transfer(
    val id: Long = 0,
    val fromWalletId: Long,
    val fromWalletName: String = "",
    val toWalletId: Long,
    val toWalletName: String = "",
    val amount: Long,
    val description: String = "",
    val transferDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class Category(
    val key: String,
    val name: String,
    val iconKey: String,
    val type: String,
    val isDefault: Boolean = true
)

data class Budget(
    val id: Long = 0,
    val categoryKey: String,
    val amountLimit: Long,
    val month: Int,
    val year: Int,
    val spentAmount: Long = 0L,
    val alert80Sent: Boolean = false,
    val alert100Sent: Boolean = false
) {
    val percentage: Int get() = if (amountLimit > 0) ((spentAmount * 100) / amountLimit).toInt() else 0
    val remaining: Long get() = amountLimit - spentAmount
    val isOverBudget: Boolean get() = spentAmount >= amountLimit
    val isNearLimit: Boolean get() = percentage >= 80 && !isOverBudget
}

data class UserSettings(
    val id: Int = 1,
    val currency: String = "VND",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val reminderMessage: String = "Hôm nay bạn đã nhập đầy đủ chi tiêu chưa?",
    val confirmationThreshold: Long = 1_000_000L,
    val geminiEnabled: Boolean = false,
    val defaultChartPeriod: String = "monthly",
    val isDarkMode: Boolean = false,
    val dbSchemaVersion: Int = 1
)

data class MonthlyStats(
    val totalIncome: Long,
    val totalExpense: Long,
    val net: Long = totalIncome - totalExpense,
    val categoryBreakdown: List<CategoryStat> = emptyList()
)

data class CategoryStat(
    val categoryKey: String,
    val amount: Long,
    val percentage: Float = 0f
)
