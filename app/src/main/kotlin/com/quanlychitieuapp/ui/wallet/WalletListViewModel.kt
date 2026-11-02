package com.quanlychitieuapp.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.WalletRepository
import com.quanlychitieuapp.domain.model.Wallet
import com.quanlychitieuapp.domain.service.TransferService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WalletListUiState(
    val wallets: List<Wallet> = emptyList(),
    val totalBalance: Long = 0L,
    val deleteErrorRes: Int? = null,
    val isLoading: Boolean = true
)

class WalletListViewModel(
    private val walletRepository: WalletRepository,
    private val transferService: TransferService
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletListUiState())
    val uiState: StateFlow<WalletListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                walletRepository.getAllWallets(),
                walletRepository.getTotalBalance()
            ) { wallets, totalBalance ->
                Pair(wallets, totalBalance)
            }.collect { (wallets, totalBalance) ->
                _uiState.update {
                    it.copy(
                        wallets = wallets,
                        totalBalance = totalBalance,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setDefaultWallet(walletId: Long) {
        viewModelScope.launch {
            walletRepository.setDefaultWallet(walletId)
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            if (walletRepository.canDeleteWallet(wallet.id)) {
                walletRepository.deleteWallet(wallet)
            } else {
                _uiState.update {
                    it.copy(deleteErrorRes = com.quanlychitieuapp.R.string.wallet_delete_error_generic)
                }
            }
        }
    }

    fun dismissDeleteError() {
        _uiState.update { it.copy(deleteErrorRes = null) }
    }
}
