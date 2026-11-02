package com.quanlychitieuapp.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps icon keys (stored in DB) to display icons/descriptions.
 *
 * IMPORTANT: Icons in DB are stored as string keys ONLY.
 * Vector icons are resolved here at the PRESENTATION layer only.
 */
object IconMapper {

    /**
     * Map wallet icon key to display vector icon
     */
    fun walletIconToVector(iconKey: String): ImageVector = when (iconKey) {
        "cash" -> Icons.Filled.Money
        "bank" -> Icons.Filled.AccountBalance
        "momo" -> Icons.Filled.Wallet
        "wallet" -> Icons.Filled.AccountBalanceWallet
        else -> Icons.Filled.Savings
    }

    /**
     * Map category icon key to display vector icon
     */
    fun categoryIconToVector(iconKey: String): ImageVector = when (iconKey) {
        // Expense
        "food" -> Icons.Filled.Restaurant
        "shopping" -> Icons.Filled.ShoppingCart
        "transport" -> Icons.Filled.DirectionsCar
        "entertainment" -> Icons.Filled.SportsEsports
        "education" -> Icons.Filled.School
        "living" -> Icons.Filled.Home
        "health" -> Icons.Filled.LocalHospital
        "telecom" -> Icons.Filled.PhoneAndroid
        "other" -> Icons.Filled.Category
        // Income
        "salary" -> Icons.Filled.Work
        "bonus" -> Icons.Filled.CardGiftcard
        "freelance" -> Icons.Filled.Laptop
        "other_income" -> Icons.Filled.Savings
        else -> Icons.Filled.HelpOutline
    }

    /**
     * Map category key to localized Vietnamese display name
     * (Fallback if name not available from DB)
     */
    fun categoryKeyToName(key: String): String = when (key) {
        "food" -> "Ăn uống"
        "shopping" -> "Mua sắm"
        "transport" -> "Di chuyển"
        "entertainment" -> "Giải trí"
        "education" -> "Giáo dục"
        "living" -> "Nhà ở"
        "health" -> "Sức khỏe"
        "telecom" -> "Viễn thông"
        "other" -> "Khác"
        "salary" -> "Lương"
        "bonus" -> "Thưởng"
        "freelance" -> "Freelance"
        "other_income" -> "Thu nhập khác"
        else -> key
    }

    /**
     * Map wallet type key to display name
     */
    fun walletTypeToName(typeKey: String): String = when (typeKey) {
        "cash" -> "Tiền mặt"
        "bank" -> "Ngân hàng"
        "momo" -> "MoMo"
        "other" -> "Khác"
        else -> typeKey
    }
}
