package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryKey = :categoryKey AND month = :month AND year = :year")
    suspend fun getBudgetForCategory(categoryKey: String, month: Int, year: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    /**
     * Reset alert flags at the start of a new month
     */
    @Query("""
        UPDATE budgets 
        SET alert80Sent = 0, alert100Sent = 0
        WHERE month = :month AND year = :year
    """)
    suspend fun resetAlerts(month: Int, year: Int)

    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    suspend fun getAllBudgetsSnapshot(): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
