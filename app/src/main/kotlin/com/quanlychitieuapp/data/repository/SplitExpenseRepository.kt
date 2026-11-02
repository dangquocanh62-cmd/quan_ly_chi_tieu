package com.quanlychitieuapp.data.repository

import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.toDomain
import com.quanlychitieuapp.domain.model.SplitExpense
import com.quanlychitieuapp.domain.model.SplitParticipant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SplitExpenseRepository(
    private val db: AppDatabase
) {
    private val dao = db.splitExpenseDao()
    private val personDao = db.personDao()

    fun getAllSplitExpenses(): Flow<List<Pair<SplitExpense, List<SplitParticipant>>>> {
        return dao.getAllSplitExpenses().map { list ->
            list.map { item ->
                val expense = item.splitExpense.toDomain()
                val participants = item.participants.map { participantEntity ->
                    val personName = participantEntity.personId?.let { pid ->
                        personDao.getById(pid)?.name
                    } ?: "Bạn (Chủ chi)"
                    participantEntity.toDomain(personName)
                }
                Pair(expense, participants)
            }
        }
    }

    suspend fun getSplitExpenseById(id: Long): Pair<SplitExpense, List<SplitParticipant>>? {
        val item = dao.getSplitExpenseById(id) ?: return null
        val expense = item.splitExpense.toDomain()
        val participants = item.participants.map { participantEntity ->
            val personName = participantEntity.personId?.let { pid ->
                personDao.getById(pid)?.name
            } ?: "Bạn (Chủ chi)"
            participantEntity.toDomain(personName)
        }
        return Pair(expense, participants)
    }
}
