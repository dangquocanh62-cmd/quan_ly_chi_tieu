package com.quanlychitieuapp.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object Statistics : Screen("statistics")
    object Budget : Screen("budget")
    object Settings : Screen("settings")
    object Chat : Screen("chat")
    object AddTransaction : Screen("add_transaction/{type}?walletId={walletId}") {
        fun createRoute(type: String, walletId: Long? = null) = 
            if (walletId != null) "add_transaction/$type?walletId=$walletId"
            else "add_transaction/$type"
    }
    object EditTransaction : Screen("edit_transaction/{id}") {
        fun createRoute(id: Long) = "edit_transaction/$id"
    }
    object WalletList : Screen("wallets")
    object AddWallet : Screen("add_wallet")
    object Transfer : Screen("transfer")
    
    object WalletDetail : Screen("wallet_detail/{walletId}") {
        fun createRoute(walletId: Long) = "wallet_detail/$walletId"
    }
    object BudgetDetail : Screen("budget_detail/{id}") {
        fun createRoute(id: Long) = "budget_detail/$id"
    }
    object Backup : Screen("backup")
    object DebtList : Screen("debt_list")
    object DebtDetail : Screen("debt_detail/{id}") {
        fun createRoute(id: Long) = "debt_detail/$id"
    }
    object AddDebt : Screen("add_debt")
    object EditDebt : Screen("edit_debt/{debtId}") {
        fun createRoute(debtId: Long) = "edit_debt/$debtId"
    }
    object AddDebtPayment : Screen("add_debt_payment/{debtId}") {
        fun createRoute(debtId: Long) = "add_debt_payment/$debtId"
    }
    object PersonDebtDetail : Screen("person_debt_detail/{personId}") {
        fun createRoute(personId: Long) = "person_debt_detail/$personId"
    }
}
