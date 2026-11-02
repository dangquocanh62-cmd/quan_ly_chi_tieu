package com.quanlychitieuapp.data.repository

import androidx.room.withTransaction
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.DebtEntity
import com.quanlychitieuapp.data.local.entity.DebtPaymentEntity
import com.quanlychitieuapp.data.local.entity.PersonEntity
import com.quanlychitieuapp.domain.model.Debt
import com.quanlychitieuapp.domain.model.DebtPayment
import com.quanlychitieuapp.domain.model.DebtType
import com.quanlychitieuapp.domain.model.PaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.quanlychitieuapp.data.local.entity.toDomain

class DebtRepository(
    private val db: AppDatabase
) {
    private val debtDao = db.debtDao()
    private val debtPaymentDao = db.debtPaymentDao()
    private val personDao = db.personDao()
    private val walletDao = db.walletDao()

    fun getAllDebts(): Flow<List<Debt>> =
        debtDao.getAllDebtsWithPerson().map { list -> list.map { it.toDomain() } }

    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPayment>> =
        debtPaymentDao.getPaymentsForDebt(debtId).map { list -> list.map { it.toDomain() } }

    private suspend fun getOrCreatePersonId(personName: String): Long {
        val trimmed = personName.trim().replace(Regex("\\s+"), " ")
        val normalized = trimmed.lowercase()
        val existing = personDao.findByNormalizedName(normalized)
        if (existing != null) return existing.id
        val newPerson = PersonEntity(name = trimmed, normalizedName = normalized, createdAt = System.currentTimeMillis())
        return personDao.insert(newPerson)
    }

    suspend fun getDebtById(id: Long): Debt? = debtDao.getById(id)?.toDomain()
    
    suspend fun getPersonById(id: Long): com.quanlychitieuapp.domain.model.Person? = personDao.getById(id)?.toDomain()

    suspend fun createLending(personName: String, amount: Long, walletId: Long, dueDate: Long?, description: String?, splitExpenseId: Long? = null): Long {
        return db.withTransaction {
            val personId = getOrCreatePersonId(personName)
            val debt = DebtEntity(
                personId = personId,
                type = DebtType.LENDING.name,
                principalAmount = amount,
                paidAmount = 0,
                walletId = walletId,
                description = description,
                dueDate = dueDate,
                paymentStatus = PaymentStatus.ACTIVE.name,
                splitExpenseId = splitExpenseId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            walletDao.updateBalance(walletId, -amount) // Lending money decreases your wallet balance
            debtDao.insert(debt)
        }
    }

    suspend fun recordLendingRepayment(debtId: Long, amount: Long, walletId: Long, note: String?) {
        db.withTransaction {
            val debt = debtDao.getById(debtId) ?: throw Exception("Không tìm thấy khoản cho vay")
            require(debt.type == DebtType.LENDING.name) { "Loại khoản vay không khớp" }
            val remaining = debt.principalAmount - debt.paidAmount
            require(amount in 1..remaining) { "Số tiền trả ($amount) không hợp lệ so với dư nợ ($remaining)" }

            val payment = DebtPaymentEntity(
                debtId = debtId, amount = amount, walletId = walletId,
                paymentDate = System.currentTimeMillis(), note = note,
                createdAt = System.currentTimeMillis()
            )
            walletDao.updateBalance(walletId, +amount) // Receiving repayment increases your wallet balance
            debtPaymentDao.insert(payment)

            val newPaid = debt.paidAmount + amount
            val status = if (newPaid >= debt.principalAmount) PaymentStatus.PAID.name else PaymentStatus.PARTIALLY_PAID.name
            debtDao.update(debt.copy(
                paidAmount = newPaid, paymentStatus = status, updatedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun createBorrowing(personName: String, amount: Long, walletId: Long, dueDate: Long?, description: String?): Long {
        return db.withTransaction {
            val personId = getOrCreatePersonId(personName)
            val debt = DebtEntity(
                personId = personId,
                type = DebtType.BORROWING.name,
                principalAmount = amount,
                paidAmount = 0,
                walletId = walletId,
                description = description,
                dueDate = dueDate,
                paymentStatus = PaymentStatus.ACTIVE.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            walletDao.updateBalance(walletId, +amount) // Borrowing money increases your wallet balance
            debtDao.insert(debt)
        }
    }

    suspend fun recordBorrowingRepayment(debtId: Long, amount: Long, walletId: Long, note: String?) {
        db.withTransaction {
            val debt = debtDao.getById(debtId) ?: throw Exception("Không tìm thấy khoản nợ")
            require(debt.type == DebtType.BORROWING.name) { "Loại nợ không khớp" }
            val remaining = debt.principalAmount - debt.paidAmount
            require(amount in 1..remaining) { "Số tiền trả ($amount) không hợp lệ so với dư nợ ($remaining)" }

            val payment = DebtPaymentEntity(
                debtId = debtId, amount = amount, walletId = walletId,
                paymentDate = System.currentTimeMillis(), note = note,
                createdAt = System.currentTimeMillis()
            )
            walletDao.updateBalance(walletId, -amount) // Repaying money decreases your wallet balance
            debtPaymentDao.insert(payment)

            val newPaid = debt.paidAmount + amount
            val status = if (newPaid >= debt.principalAmount) PaymentStatus.PAID.name else PaymentStatus.PARTIALLY_PAID.name
            debtDao.update(debt.copy(
                paidAmount = newPaid, paymentStatus = status, updatedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun editDebt(debtId: Long, newPrincipalAmount: Long, newWalletId: Long, newDueDate: Long?, newDescription: String?) {
        db.withTransaction {
            val debt = debtDao.getById(debtId) ?: throw Exception("Không tìm thấy khoản ghi")
            if (debt.paidAmount > 0) {
                // If payment history exists, changing amount or wallet violates constraints. User can ONLY change description/dueDate.
                require(debt.principalAmount == newPrincipalAmount && debt.walletId == newWalletId) {
                    "Không thể sửa số tiền gốc hoặc ví sau khi đã có lịch sử thanh toán"
                }
                debtDao.update(debt.copy(
                    dueDate = newDueDate, 
                    description = newDescription, 
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                require(newPrincipalAmount > 0) { "Số tiền phải lớn hơn 0" }
                // Reverse old operation
                val oldDelta = if (debt.type == DebtType.LENDING.name) +debt.principalAmount else -debt.principalAmount
                walletDao.updateBalance(debt.walletId, oldDelta)
                
                // Apply new operation
                val newDelta = if (debt.type == DebtType.LENDING.name) -newPrincipalAmount else +newPrincipalAmount
                walletDao.updateBalance(newWalletId, newDelta)

                debtDao.update(debt.copy(
                    principalAmount = newPrincipalAmount,
                    walletId = newWalletId,
                    dueDate = newDueDate,
                    description = newDescription,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }
    }

    suspend fun deleteDebt(debtId: Long) {
        db.withTransaction {
            val debt = debtDao.getById(debtId) ?: throw Exception("Không tìm thấy khoản ghi")
            require(debt.paidAmount == 0L) { "Không thể xóa khoản vay/nợ đã có lịch sử thanh toán" }
            
            // Refund the original operation to the wallet
            val delta = if (debt.type == DebtType.LENDING.name) +debt.principalAmount else -debt.principalAmount
            walletDao.updateBalance(debt.walletId, delta)
            
            debtDao.delete(debt)
        }
    }
}
