package com.quanlychitieuapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.BudgetEntity
import com.quanlychitieuapp.domain.model.Budget
import com.quanlychitieuapp.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BudgetUiState(
    val budgets: List<Budget> = emptyList(),
    val showAddDialog: Boolean = false,
    val isLoading: Boolean = true
)

class BudgetViewModel(private val db: AppDatabase) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val currentMonth = DateUtils.currentMonth()
    private val currentYear = DateUtils.currentYear()

    init {
        viewModelScope.launch {
            db.budgetDao().getBudgetsForMonth(currentMonth, currentYear)
                .collect { budgetEntities ->
                    val budgets = budgetEntities.map { entity ->
                        val startDate = DateUtils.startOfMonth(currentMonth, currentYear)
                        val endDate = DateUtils.endOfMonth(currentMonth, currentYear)
                        val spent = db.transactionDao().sumExpenseByCategory(
                            entity.categoryKey, startDate, endDate
                        )
                        Budget(
                            id = entity.id,
                            categoryKey = entity.categoryKey,
                            amountLimit = entity.amountLimit,
                            month = entity.month,
                            year = entity.year,
                            spentAmount = spent,
                            alert80Sent = entity.alert80Sent,
                            alert100Sent = entity.alert100Sent
                        )
                    }
                    _uiState.update { it.copy(budgets = budgets, isLoading = false) }
                }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun saveBudget(categoryKey: String, amountLimit: Long) {
        viewModelScope.launch {
            val existing = db.budgetDao().getBudgetForCategory(categoryKey, currentMonth, currentYear)
            if (existing != null) {
                db.budgetDao().updateBudget(
                    existing.copy(amountLimit = amountLimit, updatedAt = System.currentTimeMillis())
                )
            } else {
                db.budgetDao().insertBudget(
                    BudgetEntity(
                        categoryKey = categoryKey,
                        amountLimit = amountLimit,
                        month = currentMonth,
                        year = currentYear
                    )
                )
            }
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            val entity = db.budgetDao().getBudgetById(budget.id) ?: return@launch
            db.budgetDao().deleteBudget(entity)
        }
    }
}
