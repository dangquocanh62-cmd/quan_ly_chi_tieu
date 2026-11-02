package com.quanlychitieuapp.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.TransactionRepository
import com.quanlychitieuapp.domain.model.CategoryStat
import com.quanlychitieuapp.utils.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.quanlychitieuapp.R

enum class ChartPeriod(val displayNameRes: Int) {
    DAILY(R.string.statistics_period_daily),
    WEEKLY(R.string.statistics_period_weekly),
    MONTHLY(R.string.statistics_period_monthly),
    YEARLY(R.string.statistics_period_yearly)
}

data class StatisticsUiState(
    val period: ChartPeriod = ChartPeriod.MONTHLY,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val categoryStats: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true
) {
    val net: Long get() = totalIncome - totalExpense
}

class StatisticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _period = MutableStateFlow(ChartPeriod.MONTHLY)
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _period.collect { period ->
                loadStats(period)
            }
        }
    }

    fun changePeriod(period: ChartPeriod) {
        _period.value = period
    }

    private suspend fun loadStats(period: ChartPeriod) {
        _uiState.update { it.copy(isLoading = true) }
        val (startDate, endDate) = getPeriodRange(period)

        val income = transactionRepository.sumByType("income", startDate, endDate)
        val expense = transactionRepository.sumByType("expense", startDate, endDate)
        val categoryStats = transactionRepository.getCategoryStats(startDate, endDate)

        _uiState.update {
            StatisticsUiState(
                period = period,
                totalIncome = income,
                totalExpense = expense,
                categoryStats = categoryStats,
                isLoading = false
            )
        }
    }

    private fun getPeriodRange(period: ChartPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (period) {
            ChartPeriod.DAILY -> Pair(DateUtils.startOfDay(now), DateUtils.endOfDay(now))
            ChartPeriod.WEEKLY -> Pair(DateUtils.startOfWeek(), System.currentTimeMillis())
            ChartPeriod.MONTHLY -> Pair(DateUtils.startOfMonth(), DateUtils.endOfMonth())
            ChartPeriod.YEARLY -> Pair(DateUtils.startOfYear(), System.currentTimeMillis())
        }
    }
}
