package com.quanlychitieuapp.domain.usecase

import com.quanlychitieuapp.data.local.dao.*
import com.quanlychitieuapp.data.local.entity.*
import com.quanlychitieuapp.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GetWalletStatementUseCaseTest {

    @Test
    fun `Test unified wallet statement aggregation and balance calculation`() = runBlocking {
        val walletId = 1L
        
        // Initial setup for the scenario
        // Initial wallet balance = 10_000_000 is maintained OUTSIDE this usecase. 
        // This use case only tests the 5 cashflows generated.

        // 1. Income: +5,000,000
        val txDao = object : TransactionDao {
            override fun getTransactionsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<TransactionEntity>> {
                return flowOf(listOf(
                    TransactionEntity(id = 1, type = "income", amount = 5_000_000, categoryKey = "salary", walletId = walletId, description = "Lương", transactionDate = 1000L, createdAt = 1000L),
                    TransactionEntity(id = 2, type = "expense", amount = 1_000_000, categoryKey = "food", walletId = walletId, description = "Ăn uống", transactionDate = 2000L, createdAt = 2000L)
                ))
            }
            override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf()
            override suspend fun getTransactionById(id: Long): TransactionEntity? = null
            override fun getFilteredTransactions(type: String?, walletId: Long?, categoryKey: String?, startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = flowOf()
            override fun searchTransactions(query: String): Flow<List<TransactionEntity>> = flowOf()
            override suspend fun sumByType(type: String, startDate: Long, endDate: Long): Long = 0
            override suspend fun sumExpenseByCategory(categoryKey: String, startDate: Long, endDate: Long): Long = 0
            override suspend fun getCategoryTotals(startDate: Long, endDate: Long): List<CategoryTotal> = emptyList()
            override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> = flowOf()
            override suspend fun insertTransaction(transaction: TransactionEntity): Long = 0
            override suspend fun updateTransaction(transaction: TransactionEntity) {}
            override suspend fun deleteTransaction(transaction: TransactionEntity) {}
            override suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> = emptyList()
            override suspend fun deleteAll() {}
        }

        // 3. Transfer OUT: -2,000,000
        val tfDao = object : TransferDao {
            override fun getTransfersForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<TransferEntity>> {
                return flowOf(listOf(
                    TransferEntity(id = 1, fromWalletId = walletId, toWalletId = 2L, amount = 2_000_000, description = "Chuyển tiền", transferDate = 3000L, createdAt = 3000L)
                ))
            }
            override fun getAllTransfers(): Flow<List<TransferEntity>> = flowOf()
            override suspend fun getTransferById(id: Long): TransferEntity? = null
            override fun getTransfersInRange(startDate: Long, endDate: Long): Flow<List<TransferEntity>> = flowOf()
            override suspend fun insertTransfer(transfer: TransferEntity): Long = 0
            override suspend fun deleteTransfer(transfer: TransferEntity) {}
            override suspend fun getAllTransfersSnapshot(): List<TransferEntity> = emptyList()
            override suspend fun deleteAll() {}
        }

        // 4. Lending: -500,000
        val dDao = object : DebtDao {
            override fun getDebtsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<DebtWithPerson>> {
                return flowOf(listOf(
                    DebtWithPerson(
                        debt = DebtEntity(id = 1, personId = 1, type = "LENDING", principalAmount = 500_000, paidAmount = 0, walletId = walletId, description = null, paymentStatus = "ACTIVE", createdAt = 4000L, updatedAt = 4000L),
                        personName = "Thành"
                    )
                ))
            }
            override fun getAllDebtsWithPerson(): Flow<List<DebtWithPerson>> = flowOf()
            override suspend fun getDebtWithPersonById(id: Long): DebtWithPerson? = null
            override suspend fun getById(id: Long): DebtEntity? = null
            override suspend fun insert(debt: DebtEntity): Long = 0
            override suspend fun update(debt: DebtEntity) {}
            override suspend fun delete(debt: DebtEntity) {}
        }

        // 5. Debt Payment Received: +200,000
        val dpDao = object : DebtPaymentDao {
            override fun getDebtPaymentsForStatement(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<DebtPaymentWithDebtAndPerson>> {
                return flowOf(listOf(
                    DebtPaymentWithDebtAndPerson(
                        payment = DebtPaymentEntity(id = 1, debtId = 1, amount = 200_000, walletId = walletId, paymentDate = 5000L, createdAt = 5000L),
                        debtType = "LENDING",
                        personName = "Thành"
                    )
                ))
            }
            override fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>> = flowOf()
            override suspend fun insert(payment: DebtPaymentEntity): Long = 0
        }

        val useCase = GetWalletStatementUseCase(txDao, tfDao, dDao, dpDao)
        val statement = useCase(walletId, 0L, 9999L).first()

        // Verify count
        assertEquals("CashFlowItem count should be 5", 5, statement.size)

        // Verify types
        val transactions = statement.filter { it.sourceType == CashFlowSource.TRANSACTION }
        val transfers = statement.filter { it.sourceType == CashFlowSource.TRANSFER }
        val debts = statement.filter { it.sourceType == CashFlowSource.DEBT }
        val payments = statement.filter { it.sourceType == CashFlowSource.DEBT_PAYMENT }

        assertEquals(2, transactions.size)
        assertEquals(1, transfers.size)
        assertEquals(1, debts.size)
        assertEquals(1, payments.size)

        // Verify amounts and directions
        val income = transactions.find { it.amount == 5_000_000L }!!
        assertEquals(CashFlowDirection.IN, income.direction)

        val expense = transactions.find { it.amount == 1_000_000L }!!
        assertEquals(CashFlowDirection.OUT, expense.direction)

        val transfer = transfers[0]
        assertEquals(2_000_000L, transfer.amount)
        assertEquals(CashFlowDirection.OUT, transfer.direction)

        val lending = debts[0]
        assertEquals(500_000L, lending.amount)
        assertEquals(CashFlowDirection.OUT, lending.direction)

        val payment = payments[0]
        assertEquals(200_000L, payment.amount)
        assertEquals(CashFlowDirection.IN, payment.direction)

        // Verify Net Cash Flow (+1_700_000)
        var netCashFlow = 0L
        for (item in statement) {
            if (item.direction == CashFlowDirection.IN) netCashFlow += item.amount
            else netCashFlow -= item.amount
        }
        assertEquals(1_700_000L, netCashFlow)

        // Verify sorting (descending timestamp)
        assertTrue(statement[0].timestamp >= statement[1].timestamp)
        assertTrue(statement[1].timestamp >= statement[2].timestamp)
        assertTrue(statement[2].timestamp >= statement[3].timestamp)
        assertTrue(statement[3].timestamp >= statement[4].timestamp)
    }
}
