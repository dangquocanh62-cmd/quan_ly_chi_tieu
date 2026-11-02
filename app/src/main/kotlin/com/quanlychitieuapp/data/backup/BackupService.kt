package com.quanlychitieuapp.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.quanlychitieuapp.R
import com.quanlychitieuapp.data.local.AppDatabase
import com.quanlychitieuapp.data.local.entity.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val schemaVersion: Int = 1,
    val exportedAt: String = "",
    val wallets: List<WalletEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val transfers: List<TransferEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val settings: UserSettingsEntity? = null,
    val splitExpenses: List<SplitExpenseEntity> = emptyList(),
    val splitParticipants: List<SplitParticipantEntity> = emptyList()
)

sealed class BackupResult {
    data class Success(val message: String) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

/**
 * BackupService - handles JSON export and import.
 * 
 * CRITICAL RULES:
 * - Validate schema before any import
 * - Validate all amounts are positive
 * - Never crash on bad input
 * - Offer Merge (add) or Replace (overwrite) on import
 */
class BackupService(
    private val context: Context,
    private val db: AppDatabase
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export all data to a JSON file at the given URI.
     */
    suspend fun exportToJson(uri: Uri): BackupResult {
        return try {
            val wallets = db.walletDao().getAllWalletsSnapshot()
            val categories = db.categoryDao().getAllCategoriesSnapshot()
            val transactions = db.transactionDao().getAllTransactionsSnapshot()
            val transfers = db.transferDao().getAllTransfersSnapshot()
            val budgets = db.budgetDao().getAllBudgetsSnapshot()
            val settings = db.userSettingsDao().getSettingsOnce()
            val splitExpenses = db.splitExpenseDao().getAllSplitExpensesSnapshot()
            val splitParticipants = db.splitExpenseDao().getAllSplitParticipantsSnapshot()

            val backup = BackupData(
                schemaVersion = 1,
                exportedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                wallets = wallets,
                categories = categories,
                transactions = transactions,
                transfers = transfers,
                budgets = budgets,
                settings = settings,
                splitExpenses = splitExpenses,
                splitParticipants = splitParticipants
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(gson.toJson(backup))
                }
            } ?: return BackupResult.Error(context.getString(R.string.backup_err_file_write))

            BackupResult.Success(context.getString(R.string.backup_success_export_json, transactions.size))
        } catch (e: Exception) {
            BackupResult.Error(context.getString(R.string.backup_err_export_json, e.message ?: ""))
        }
    }

    /**
     * Export transactions to CSV file.
     */
    suspend fun exportToCsv(uri: Uri): BackupResult {
        return try {
            val transactions = db.transactionDao().getAllTransactionsSnapshot()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    // BOM for Excel UTF-8 compatibility
                    writer.write("\uFEFF")
                    writer.write("Ngày,Loại,Số tiền,Danh mục,Ghi chú\n")
                    transactions.forEach { tx ->
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(Date(tx.transactionDate))
                        val type = if (tx.type == "expense") "Chi tiêu" else "Thu nhập"
                        val categoryName = categoryKeyToName(tx.categoryKey)
                        writer.write("$date,$type,${tx.amount},$categoryName,\"${tx.description}\"\n")
                    }
                }
            } ?: return BackupResult.Error(context.getString(R.string.backup_err_csv_open))

            BackupResult.Success(context.getString(R.string.backup_success_export_csv, transactions.size))
        } catch (e: Exception) {
            BackupResult.Error(context.getString(R.string.backup_err_export_csv, e.message ?: ""))
        }
    }

    /**
     * Import from JSON backup.
     * @param merge: true = add to existing data, false = replace all
     */
    suspend fun importFromJson(uri: Uri, merge: Boolean): BackupResult {
        return try {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: return BackupResult.Error(context.getString(R.string.backup_err_file_read))

            // Parse and validate
            val backup = parseAndValidate(jsonText)
                ?: return BackupResult.Error(context.getString(R.string.backup_err_invalid_file))

            if (!merge) {
                // Replace: clear existing data
                // NOTE: This is dangerous - ask for confirmation in UI first
                clearAllData()
            }

            // Import data
            var importedCount = 0

            backup.wallets.forEach { wallet ->
                try { db.walletDao().insertWallet(wallet) } catch (_: Exception) {}
            }

            backup.categories.forEach { cat ->
                try { db.categoryDao().insertCategory(cat) } catch (_: Exception) {}
            }

            backup.transactions.forEach { tx ->
                if (validateTransaction(tx)) {
                    try { db.transactionDao().insertTransaction(tx); importedCount++ } catch (_: Exception) {}
                }
            }

            backup.transfers.forEach { transfer ->
                if (transfer.amount > 0) {
                    try { db.transferDao().insertTransfer(transfer) } catch (_: Exception) {}
                }
            }

            backup.budgets.forEach { budget ->
                if (budget.amountLimit > 0) {
                    try { db.budgetDao().insertBudget(budget) } catch (_: Exception) {}
                }
            }

            backup.settings?.let { settings ->
                try { db.userSettingsDao().insertSettings(settings) } catch (_: Exception) {}
            }
            
            backup.splitExpenses.forEach { split ->
                try { db.splitExpenseDao().insertSplitExpense(split) } catch (_: Exception) {}
            }
            
            backup.splitParticipants.forEach { participant ->
                try { db.splitExpenseDao().insertSplitParticipant(participant) } catch (_: Exception) {}
            }

            BackupResult.Success(context.getString(R.string.backup_success_import, importedCount))
        } catch (e: Exception) {
            BackupResult.Error(context.getString(R.string.backup_err_invalid_file_with_msg, e.message ?: ""))
        }
    }

    private fun parseAndValidate(jsonText: String): BackupData? {
        return try {
            val json = JsonParser.parseString(jsonText).asJsonObject
            val schemaVersion = json.get("schemaVersion")?.asInt ?: return null
            if (schemaVersion < 1) return null // Unsupported schema

            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun validateTransaction(tx: TransactionEntity): Boolean {
        return tx.amount > 0 &&
                (tx.type == "expense" || tx.type == "income") &&
                tx.categoryKey.isNotEmpty() &&
                tx.walletId > 0
    }

    private suspend fun clearAllData() {
        // Safe delete order to avoid FK violations
        db.splitExpenseDao().deleteAllSplitParticipants()
        db.splitExpenseDao().deleteAllSplitExpenses()
        db.transferDao().deleteAll()
        db.transactionDao().deleteAll()
        db.budgetDao().deleteAll()
        db.walletDao().deleteAll()
        db.categoryDao().deleteAll()
    }

    private fun categoryKeyToName(key: String): String = when (key) {
        "food" -> "Ăn uống"; "shopping" -> "Mua sắm"; "transport" -> "Di chuyển"
        "entertainment" -> "Giải trí"; "education" -> "Giáo dục"; "living" -> "Nhà ở"
        "health" -> "Sức khỏe"; "telecom" -> "Viễn thông"; "salary" -> "Lương"
        "bonus" -> "Thưởng"; "freelance" -> "Freelance"; else -> key
    }
}
