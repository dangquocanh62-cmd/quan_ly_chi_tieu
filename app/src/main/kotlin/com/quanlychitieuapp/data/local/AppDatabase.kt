package com.quanlychitieuapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quanlychitieuapp.data.local.dao.*
import com.quanlychitieuapp.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WalletEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        BudgetEntity::class,
        UserSettingsEntity::class,
        PersonEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        SplitExpenseEntity::class,
        SplitParticipantEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun personDao(): PersonDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao
    abstract fun splitExpenseDao(): SplitExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quan_ly_chi_tieu.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback to seed default data on first creation
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaultData(database)
                    }
                }
            }
        }

        private suspend fun seedDefaultData(db: AppDatabase) {
            // Seed default wallets
            val walletDao = db.walletDao()
            walletDao.insertWallet(
                WalletEntity(name = "Tiền mặt", type = WalletType.CASH.key, iconKey = "cash", isDefault = true)
            )
            walletDao.insertWallet(
                WalletEntity(name = "Ngân hàng", type = WalletType.BANK.key, iconKey = "bank")
            )
            walletDao.insertWallet(
                WalletEntity(name = "MoMo", type = WalletType.MOMO.key, iconKey = "momo")
            )

            // Seed default categories
            val categoryDao = db.categoryDao()
            val expenseCategories = listOf(
                CategoryEntity(key = CategoryKeys.FOOD, name = "Ăn uống", iconKey = "food", type = "expense"),
                CategoryEntity(key = CategoryKeys.SHOPPING, name = "Mua sắm", iconKey = "shopping", type = "expense"),
                CategoryEntity(key = CategoryKeys.TRANSPORT, name = "Di chuyển", iconKey = "transport", type = "expense"),
                CategoryEntity(key = CategoryKeys.ENTERTAINMENT, name = "Giải trí", iconKey = "entertainment", type = "expense"),
                CategoryEntity(key = CategoryKeys.EDUCATION, name = "Giáo dục", iconKey = "education", type = "expense"),
                CategoryEntity(key = CategoryKeys.LIVING, name = "Nhà ở", iconKey = "living", type = "expense"),
                CategoryEntity(key = CategoryKeys.HEALTH, name = "Sức khỏe", iconKey = "health", type = "expense"),
                CategoryEntity(key = CategoryKeys.TELECOM, name = "Viễn thông", iconKey = "telecom", type = "expense"),
                CategoryEntity(key = CategoryKeys.OTHER_EXPENSE, name = "Khác", iconKey = "other", type = "expense")
            )
            val incomeCategories = listOf(
                CategoryEntity(key = CategoryKeys.SALARY, name = "Lương", iconKey = "salary", type = "income"),
                CategoryEntity(key = CategoryKeys.BONUS, name = "Thưởng", iconKey = "bonus", type = "income"),
                CategoryEntity(key = CategoryKeys.FREELANCE, name = "Freelance", iconKey = "freelance", type = "income"),
                CategoryEntity(key = CategoryKeys.OTHER_INCOME, name = "Thu nhập khác", iconKey = "other_income", type = "income")
            )
            categoryDao.insertCategories(expenseCategories + incomeCategories)

            // Seed default settings
            db.userSettingsDao().insertSettings(UserSettingsEntity())
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create persons table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `persons` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `normalizedName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create debts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `principalAmount` INTEGER NOT NULL,
                        `paidAmount` INTEGER NOT NULL,
                        `walletId` INTEGER NOT NULL,
                        `description` TEXT,
                        `dueDate` INTEGER,
                        `paymentStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_personId` ON `debts` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_walletId` ON `debts` (`walletId`)")

                // Create debt_payments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debt_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `debtId` INTEGER NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `walletId` INTEGER NOT NULL,
                        `paymentDate` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_walletId` ON `debt_payments` (`walletId`)")

                // Add column debtConfirmationThreshold
                db.execSQL("ALTER TABLE user_settings ADD COLUMN debtConfirmationThreshold INTEGER NOT NULL DEFAULT 1000000")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add splitExpenseId to debts
                db.execSQL("ALTER TABLE debts ADD COLUMN splitExpenseId INTEGER")

                // Create split_expenses table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `walletId` INTEGER NOT NULL,
                        `categoryKey` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `transactionDate` INTEGER NOT NULL,
                        `splitMethod` TEXT NOT NULL,
                        `linkedTransactionId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`walletId`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_expenses_walletId` ON `split_expenses` (`walletId`)")

                // Create split_participants table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `split_participants` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `splitExpenseId` INTEGER NOT NULL,
                        `personId` INTEGER,
                        `shareAmount` INTEGER NOT NULL,
                        `isOwner` INTEGER NOT NULL,
                        `linkedDebtId` INTEGER,
                        FOREIGN KEY(`splitExpenseId`) REFERENCES `split_expenses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_participants_splitExpenseId` ON `split_participants` (`splitExpenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_split_participants_personId` ON `split_participants` (`personId`)")
            }
        }
    }
}
