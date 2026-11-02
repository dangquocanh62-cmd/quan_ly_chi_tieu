package com.quanlychitieuapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.WalletEntity
import com.quanlychitieuapp.data.repository.DebtRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebtRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DebtRepository
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DebtRepository(db)
        
        // Setup initial wallet for tests
        runBlocking {
            db.walletDao().insertWallet(WalletEntity(id = 1, name = "Cash", balance = 10000L, type = "cash", iconKey = "cash", isDefault = true))
        }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test dedupe logic - creating debt with same normalized name reuses person`() = runBlocking {
        // Create first debt for "Nguyễn Văn A" 
        repository.createLending("Nguyễn Văn A", 1000L, 1, null, "")
        
        // Create second debt for "nguyen van a" (different casing)
        repository.createLending("nguyen van a", 2000L, 1, null, "")
        
        // Assert there is only one person
        val cursor = db.query("SELECT COUNT(*) FROM persons", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        
        assertEquals("Should reuse existing person", 1, count)
        
        val debts = repository.getAllDebts().first()
        assertEquals(2, debts.size)
        assertEquals(debts[0].personId, debts[1].personId)
    }

    @Test
    fun `test Case 24 - atomic transaction rejects over-payment`() = runBlocking {
        repository.createBorrowing("Nam", 5000L, 1, null, "")
        val debts = repository.getAllDebts().first()
        val debt = debts.first()
        
        // Try to pay 6000 but only owe 5000
        var error: Exception? = null
        try {
            repository.recordBorrowingRepayment(debt.id, 6000L, 1, "")
        } catch (e: Exception) {
            error = e
        }
        
        assertNotNull("Should throw exception on over-payment", error)
        
        // Balance should remain unchanged if transaction rolled back
        val wallet = db.walletDao().getWalletById(1)
        assertEquals("Wallet balance should not change if over-payment fails", 15000L, wallet?.balance)
        
        val record = db.debtDao().getById(debt.id)
        assertEquals("Paid amount should remain 0", 0L, record?.paidAmount)
    }

    @Test
    fun `test lock deletes and edits when paidAmount is greater than 0`() = runBlocking {
        repository.createLending("Minh", 5000L, 1, null, "")
        val debt = repository.getAllDebts().first().first()
        
        repository.recordLendingRepayment(debt.id, 2000L, 1, "")
        
        var error: Exception? = null
        try {
            repository.deleteDebt(debt.id)
        } catch (e: Exception) {
            error = e
        }
        
        assertNotNull("Should throw Exception when deleting partially paid debt", error)
        assertTrue(error?.message?.contains("paidAmount > 0") == true)
    }
}
