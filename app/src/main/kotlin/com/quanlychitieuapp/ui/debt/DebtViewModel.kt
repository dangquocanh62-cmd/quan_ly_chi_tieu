package com.quanlychitieuapp.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.DebtRepository
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.domain.model.Wallet
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.quanlychitieuapp.R

data class PersonDebtSummary(
    val personId: Long,
    val personName: String,
    val lendingTotal: Long,
    val borrowingTotal: Long,
    val netDebt: Long
)

data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val personSummaries: List<PersonDebtSummary> = emptyList(),
    val currentFilter: DebtFilter = DebtFilter.ALL,
    val isLendingTotal: Long = 0,
    val isBorrowingTotal: Long = 0,
    val error: String? = null
)

enum class DebtFilter(val titleRes: Int) {
    ALL(R.string.debt_filter_all),
    LENDING(R.string.debt_filter_lending),
    BORROWING(R.string.debt_filter_borrowing),
    OVERDUE(R.string.debt_filter_overdue)
}

class DebtViewModel(
    private val debtRepository: DebtRepository,
    private val walletRepository: WalletRepository,
    private val splitExpenseRepository: com.quanlychitieuapp.data.repository.SplitExpenseRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(debtRepository.getAllDebts(), walletRepository.getAllWallets()) { debts, wallets ->
                Pair(debts, wallets)
            }.collect { (debts, wallets) ->
                updateStateWithDebts(debts, wallets, _uiState.value.currentFilter)
            }
        }
    }

    fun setFilter(filter: DebtFilter) {
        val currentState = _uiState.value
        updateStateWithDebts(currentState.debts, currentState.wallets, filter)
    }

    private fun updateStateWithDebts(debts: List<Debt>, wallets: List<Wallet>, filter: DebtFilter) {
        val lendingTotal = debts.filter { it.type == DebtType.LENDING }.sumOf { it.remainingAmount }
        val borrowingTotal = debts.filter { it.type == DebtType.BORROWING }.sumOf { it.remainingAmount }

        val activeDebts = debts.filter { it.remainingAmount > 0 || it.paymentStatus != com.quanlychitieuapp.domain.model.PaymentStatus.PAID }
        
        val summaries = activeDebts.groupBy { it.personId }.map { (pId, list) ->
            val pName = list.first().personName
            val lent = list.filter { it.type == DebtType.LENDING }.sumOf { it.remainingAmount }
            val borrowed = list.filter { it.type == DebtType.BORROWING }.sumOf { it.remainingAmount }
            PersonDebtSummary(
                personId = pId,
                personName = pName,
                lendingTotal = lent,
                borrowingTotal = borrowed,
                netDebt = lent - borrowed
            )
        }

        val filteredSummaries = when (filter) {
            DebtFilter.ALL -> summaries
            DebtFilter.LENDING -> summaries.filter { it.lendingTotal > 0 }
            DebtFilter.BORROWING -> summaries.filter { it.borrowingTotal > 0 }
            DebtFilter.OVERDUE -> {
                val overduePersonIds = debts.filter { it.isOverdue }.map { it.personId }.toSet()
                summaries.filter { overduePersonIds.contains(it.personId) }
            }
        }.sortedByDescending { Math.abs(it.netDebt) }

        _uiState.update { 
            it.copy(
                debts = debts,
                wallets = wallets,
                personSummaries = filteredSummaries,
                currentFilter = filter,
                isLendingTotal = lendingTotal,
                isBorrowingTotal = borrowingTotal,
                error = null
            )
        }
    }

    // A helper method for fetching detailed info in the detail screen
    suspend fun getDebtWithDetails(debtId: Long): Triple<Debt?, com.quanlychitieuapp.domain.model.Person?, List<com.quanlychitieuapp.domain.model.DebtPayment>> {
        val debt = debtRepository.getDebtById(debtId) ?: return Triple(null, null, emptyList())
        val person = debtRepository.getPersonById(debt.personId)
        val payments = debtRepository.getPaymentsForDebt(debtId).first()
        return Triple(debt, person, payments)
    }

    suspend fun getSplitExpenseDetails(splitExpenseId: Long): Pair<com.quanlychitieuapp.domain.model.SplitExpense, List<com.quanlychitieuapp.domain.model.SplitParticipant>>? {
        return splitExpenseRepository?.getSplitExpenseById(splitExpenseId)
    }

    fun deleteDebt(debtId: Long, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                debtRepository.deleteDebt(debtId)
                onSuccess()
            } catch (e: Exception) {
                onError(R.string.debt_error_delete_failed)
            }
        }
    }

    fun addDebt(personName: String, type: DebtType, amount: Long, walletId: Long, dueDate: Long?, description: String?, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                if (type == DebtType.LENDING) {
                    debtRepository.createLending(personName, amount, walletId, dueDate, description)
                } else {
                    debtRepository.createBorrowing(personName, amount, walletId, dueDate, description)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(R.string.debt_error_add_failed)
            }
        }
    }

    fun editDebt(debtId: Long, principalAmount: Long, walletId: Long, dueDate: Long?, description: String?, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                debtRepository.editDebt(debtId, principalAmount, walletId, dueDate, description)
                onSuccess()
            } catch (e: Exception) {
                onError(R.string.debt_error_edit_failed)
            }
        }
    }
    
    fun recordPayment(debtId: Long, amount: Long, walletId: Long, note: String?, type: DebtType, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                if(type == DebtType.LENDING) {
                    debtRepository.recordLendingRepayment(debtId, amount, walletId, note)
                } else {
                    debtRepository.recordBorrowingRepayment(debtId, amount, walletId, note)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(R.string.debt_error_payment_failed)
            }
        }
    }
}
