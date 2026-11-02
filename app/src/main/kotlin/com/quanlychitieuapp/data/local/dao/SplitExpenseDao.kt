package com.quanlychitieuapp.data.local.dao

import androidx.room.*
import com.quanlychitieuapp.data.local.entity.SplitExpenseEntity
import com.quanlychitieuapp.data.local.entity.SplitParticipantEntity
import kotlinx.coroutines.flow.Flow

data class SplitExpenseWithParticipants(
    @Embedded val splitExpense: SplitExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "splitExpenseId"
    )
    val participants: List<SplitParticipantEntity>
)

@Dao
interface SplitExpenseDao {
    @Insert
    suspend fun insertSplitExpense(splitExpense: SplitExpenseEntity): Long

    @Update
    suspend fun updateSplitExpense(splitExpense: SplitExpenseEntity)

    @Delete
    suspend fun deleteSplitExpense(splitExpense: SplitExpenseEntity)

    @Insert
    suspend fun insertParticipants(participants: List<SplitParticipantEntity>)

    @Query("DELETE FROM split_participants WHERE splitExpenseId = :splitExpenseId")
    suspend fun deleteParticipantsBySplitId(splitExpenseId: Long)

    @Transaction
    @Query("SELECT * FROM split_expenses ORDER BY transactionDate DESC")
    fun getAllSplitExpenses(): Flow<List<SplitExpenseWithParticipants>>

    @Transaction
    @Query("SELECT * FROM split_expenses WHERE id = :id")
    suspend fun getSplitExpenseById(id: Long): SplitExpenseWithParticipants?

    @Query("SELECT * FROM split_expenses")
    suspend fun getAllSplitExpensesSnapshot(): List<SplitExpenseEntity>

    @Query("SELECT * FROM split_participants")
    suspend fun getAllSplitParticipantsSnapshot(): List<SplitParticipantEntity>

    @Insert
    suspend fun insertSplitParticipant(participant: SplitParticipantEntity)

    @Query("DELETE FROM split_expenses")
    suspend fun deleteAllSplitExpenses()

    @Query("DELETE FROM split_participants")
    suspend fun deleteAllSplitParticipants()
}
