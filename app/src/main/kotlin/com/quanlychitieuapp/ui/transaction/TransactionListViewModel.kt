package com.quanlychitieuapp.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.domain.model.Transaction
import com.quanlychitieuapp.domain.service.TransactionService
import com.quanlychitieuapp.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionFilter(
    val type: String? = null,
    val walletId: Long? = null,
    val categoryKey: String? = null,
    val startDate: Long = DateUtils.startOfMonth(),
    val endDate: Long = DateUtils.endOfMonth()
)

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val transactionService: TransactionService
) : ViewModel() {

    private val _filter = MutableStateFlow(TransactionFilter())
    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_filter, _searchQuery) { filter, query -> Pair(filter, query) }
                .flatMapLatest { (filter, query) ->
                    if (query.isNotEmpty()) {
                        transactionRepository.searchTransactions(query)
                    } else {
                        transactionRepository.getFilteredTransactions(
                            type = filter.type,
                            walletId = filter.walletId,
                            categoryKey = filter.categoryKey,
                            startDate = filter.startDate,
                            endDate = filter.endDate
                        )
                    }
                }
                .collect { transactions ->
                    _uiState.update { state ->
                        state.copy(
                            transactions = transactions,
                            filter = _filter.value,
                            searchQuery = _searchQuery.value,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun applyFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionService.deleteTransaction(transaction).fold(
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message) }
                },
                onSuccess = {}
            )
        }
    }
}
