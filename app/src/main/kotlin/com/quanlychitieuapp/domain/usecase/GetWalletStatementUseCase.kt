package com.quanlychitieuapp.domain.usecase

import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.domain.model.CashFlowDirection
import com.quanlychitieuapp.domain.model.CashFlowItem
import com.quanlychitieuapp.domain.model.CashFlowSource
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.data.local.dao.DebtDao
import com.quanlychitieuapp.data.local.dao.DebtPaymentDao
import com.quanlychitieuapp.data.local.dao.TransactionDao
import com.quanlychitieuapp.data.local.dao.TransferDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetWalletStatementUseCase(
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao
) {
    operator fun invoke(walletId: Long, startTimestamp: Long, endTimestampExclusive: Long): Flow<List<CashFlowItem>> {

        val transactionsFlow = transactionDao.getTransactionsForStatement(walletId, startTimestamp, endTimestampExclusive)
        val transfersFlow = transferDao.getTransfersForStatement(walletId, startTimestamp, endTimestampExclusive)
        val debtsFlow = debtDao.getDebtsForStatement(walletId, startTimestamp, endTimestampExclusive)
        val debtPaymentsFlow = debtPaymentDao.getDebtPaymentsForStatement(walletId, startTimestamp, endTimestampExclusive)

        return combine(
            transactionsFlow,
            transfersFlow,
            debtsFlow,
            debtPaymentsFlow
        ) { transactions, transfers, debts, debtPayments ->
            val items = mutableListOf<CashFlowItem>()

            // 1. Transactions
            transactions.forEach { tx ->
                val direction = if (tx.type == "income") CashFlowDirection.IN else CashFlowDirection.OUT
                items.add(
                    CashFlowItem(
                        sourceType = CashFlowSource.TRANSACTION,
                        sourceId = tx.id,
                        title = com.quanlychitieuapp.utils.IconMapper.categoryKeyToName(tx.categoryKey),
                        description = tx.description.takeIf { !it.isNullOrBlank() } ?: if (tx.type == "income") "Thu nhập" else "Chi tiêu",
                        amount = tx.amount,
                        direction = direction,
                        timestamp = tx.transactionDate,
                        iconKey = tx.categoryKey
                    )
                )
            }

            // 2. Transfers
            transfers.forEach { transfer ->
                val (direction, titleStr, descStr) = if (transfer.fromWalletId == walletId) {
                    Triple(CashFlowDirection.OUT, "\u2197 Chuyển tiền (đến ví khác)", transfer.description ?: "Chuyển tiền đi")
                } else {
                    Triple(CashFlowDirection.IN, "\u2199 Nhận tiền (từ ví khác)", transfer.description ?: "Nhận tiền chuyển đến")
                }
                items.add(
                    CashFlowItem(
                        sourceType = CashFlowSource.TRANSFER,
                        sourceId = transfer.id,
                        title = titleStr,
                        description = descStr,
                        amount = transfer.amount,
                        direction = direction,
                        timestamp = transfer.transferDate,
                        iconKey = "transfer"
                    )
                )
            }

            // 3. Debts
            debts.forEach { dwp ->
                val isLending = dwp.debt.type == DebtType.LENDING.name // Cho vay
                // LENDING -> OUT, BORROWING -> IN
                val direction = if (isLending) CashFlowDirection.OUT else CashFlowDirection.IN
                val titleStr = if (isLending) "\uD83E\uDD1D Cho ${dwp.personName} vay" else "\uD83E\uDD1D Vay ${dwp.personName}"
                val descStr = dwp.debt.description ?: if (isLending) "Khoản cho người khác vay" else "Khoản đi vay"
                
                items.add(
                    CashFlowItem(
                        sourceType = CashFlowSource.DEBT,
                        sourceId = dwp.debt.id,
                        title = titleStr,
                        description = descStr,
                        amount = dwp.debt.principalAmount,
                        direction = direction,
                        timestamp = dwp.debt.createdAt,
                        iconKey = if (isLending) "debt_lending" else "debt_borrowing"
                    )
                )
            }

            // 4. Debt Payments
            debtPayments.forEach { dpwd ->
                val isLending = dpwd.debtType == DebtType.LENDING.name
                // LENDING Payment -> IN (They pay us back), BORROWING Payment -> OUT (We pay back)
                val direction = if (isLending) CashFlowDirection.IN else CashFlowDirection.OUT
                val titleStr = if (isLending) "\u2197 ${dpwd.personName} trả" else "\u2198 Trả ${dpwd.personName}"
                val descStr = dpwd.payment.note ?: if (isLending) "Nhận tiền trả" else "Trả tiền nợ"

                items.add(
                    CashFlowItem(
                        sourceType = CashFlowSource.DEBT_PAYMENT,
                        sourceId = dpwd.payment.id, // Or should it be debtId acting as sourceId to navigate? Yes! Wait...
                        // Actually, sourceId for DebtPayment navigate to DebtDetail, so debtId makes sense for navigation, 
                        // but it wouldn't be unique if there are multiple payments? We only need sourceId for mapping onClick. 
                        // It's fine to pass debtId so clicking goes to the debt. Let's pass debtId:
                        title = titleStr,
                        description = descStr,
                        amount = dpwd.payment.amount,
                        direction = direction,
                        timestamp = dpwd.payment.paymentDate,
                        iconKey = if (isLending) "debt_collection" else "debt_repayment"
                    )
                )
            }

            // Secondary deterministic ordering: timestamp DESC, then sourceType, then sourceId
            items.sortedWith(
                compareByDescending<CashFlowItem> { it.timestamp }
                    .thenBy { it.sourceType }
                    .thenBy { it.sourceId }
            )
        }
    }
}
