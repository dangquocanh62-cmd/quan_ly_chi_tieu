package com.quanlychitieuapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Transaction
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalBalance: Long = 0L,
    val wallets: List<Wallet> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val totalLending: Long = 0L,
    val totalBorrowing: Long = 0L,
    val isLoading: Boolean = true
) {
    val monthlyNet: Long get() = monthlyIncome - monthlyExpense
}

class HomeViewModel(
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
    private val debtRepository: com.quanlychitieuapp.data.repository.DebtRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets(),
                walletRepository.getTotalBalance(),
                transactionRepository.getRecentTransactions(20),
                debtRepository.getAllDebts()
            ) { wallets, totalBalance, recent, debts ->
                // Calculate missing things here, or just wrap in a List and process below
                val totalLending = debts.filter { it.type == com.quanlychitieuapp.domain.model.DebtType.LENDING }.sumOf { it.remainingAmount }
                val totalBorrowing = debts.filter { it.type == com.quanlychitieuapp.domain.model.DebtType.BORROWING }.sumOf { it.remainingAmount }
                
                class StateHolder(val w: List<Wallet>, val tb: Long, val r: List<Transaction>, val tl: Long, val tb2: Long)
                StateHolder(wallets, totalBalance, recent, totalLending, totalBorrowing)
            }.collect { holder ->
                // Calculate monthly stats
                val startOfMonth = DateUtils.startOfMonth()
                val endOfMonth = DateUtils.endOfMonth()
                val income = transactionRepository.sumByType("income", startOfMonth, endOfMonth)
                val expense = transactionRepository.sumByType("expense", startOfMonth, endOfMonth)

                _uiState.update { state ->
                    state.copy(
                        totalBalance = holder.tb,
                        wallets = holder.w,
                        recentTransactions = holder.r,
                        monthlyIncome = income,
                        monthlyExpense = expense,
                        totalLending = holder.tl,
                        totalBorrowing = holder.tb2,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadData()
    }
}
