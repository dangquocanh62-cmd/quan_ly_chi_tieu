package com.quanlychitieuapp.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.CashFlowItem
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.usecase.GetWalletStatementUseCase
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.domain.service.TransactionService
import com.quanlychitieuapp.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class WalletDetailUiState(
    val wallet: Wallet? = null,
    val items: List<CashFlowItem> = emptyList(),
    val currentYear: Int,
    val currentMonth: Int,
    val isLoading: Boolean = true
)

class WalletDetailViewModel(
    private val walletId: Long,
    private val walletRepository: WalletRepository,
    private val db: AppDatabase
) : ViewModel() {

    private val getWalletStatementUseCase = GetWalletStatementUseCase(
        transactionDao = db.transactionDao(),
        transferDao = db.transferDao(),
        debtDao = db.debtDao(),
        debtPaymentDao = db.debtPaymentDao()
    )

    private val calendar = Calendar.getInstance()
    private val _year = MutableStateFlow(calendar.get(Calendar.YEAR))
    private val _month = MutableStateFlow(calendar.get(Calendar.MONTH)) // 0-based
    
    private val _uiState = MutableStateFlow(WalletDetailUiState(
        currentYear = _year.value,
        currentMonth = _month.value
    ))
    val uiState: StateFlow<WalletDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets().map { wallets -> wallets.find { it.id == walletId } },
                _year,
                _month
            ) { wallet, year, month ->
                Triple(wallet, year, month)
            }.flatMapLatest { (wallet, year, month) ->
                val startDate = DateUtils.startOfMonth(month + 1, year)
                val endDate = DateUtils.endOfMonth(month + 1, year)
                
                getWalletStatementUseCase(
                    walletId = walletId,
                    startTimestamp = startDate,
                    endTimestampExclusive = endDate + 1 // Add 1ms to make it exclusive if it includes 23:59:59.999
                ).map { items ->
                    Triple(wallet, items, Pair(year, month))
                }
            }.collect { (wallet, cashFlowItems, dateMap) ->
                _uiState.update { 
                    it.copy(
                        wallet = wallet,
                        items = cashFlowItems,
                        currentYear = dateMap.first,
                        currentMonth = dateMap.second,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun nextMonth() {
        if (_month.value == 11) {
            _month.value = 0
            _year.value += 1
        } else {
            _month.value += 1
        }
    }

    fun previousMonth() {
        if (_month.value == 0) {
            _month.value = 11
            _year.value -= 1
        } else {
            _month.value -= 1
        }
    }

    /* We no longer deal with single transaction deletion here unless it's handled differently */
}
