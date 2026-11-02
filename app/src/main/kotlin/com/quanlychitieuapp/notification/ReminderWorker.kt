package com.quanlychitieuapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.quanlychitieuapp.MainActivity
import com.quanlychitieuapp.R
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.firstOrNull

const val CHANNEL_ID = "expense_reminder"
const val NOTIFICATION_ID = 1001
const val WORK_TAG = "daily_reminder"

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = context.applicationContext as com.quanlychitieuapp.QuanLyChiTieuApp
        val db = app.database
        
        // 1. Get default message
        var message = inputData.getString("message")
            ?: context.getString(R.string.notification_body)

        // 2. Check debts
        val now = System.currentTimeMillis()
        val debts = db.debtDao().getAllDebtsWithPerson().firstOrNull() ?: emptyList()
        val dueDebts = debts.filter { it.debt.paymentStatus == "PENDING" && it.debt.dueDate != null }
        
        var debtMessage = ""
        for (d in dueDebts) {
            val due = d.debt.dueDate!!
            if (now > due) {
                val overdueDays = ((now - due) / (1000 * 60 * 60 * 24)).toInt()
                debtMessage += "\n- Nợ ${d.personName} quá hạn $overdueDays ngày!"
            } else if (due - now < 3L * 24 * 60 * 60 * 1000) {
                // Sắp đến hạn (dưới 3 ngày)
                debtMessage += "\n- Nợ ${d.personName} sắp đến hạn!"
            }
        }
        
        if (debtMessage.isNotEmpty()) {
            message = "Cảnh báo Vay/Nợ: $debtMessage\n\n$message"
        }

        showNotification(message)
        return Result.success()
    }

    private fun showNotification(message: String) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted - will be requested at runtime
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

/**
 * Schedule or reschedule the daily reminder WorkManager job.
 * Note: WorkManager is a best-effort scheduler and does not guarantee execution exactly
 * at the precise minute due to Android Doze mode and battery optimizations.
 * Behavior:
 * - Cancels any existing notification worker before re-scheduling.
 * - Uses ExistingPeriodicWorkPolicy.REPLACE to guarantee no duplicates.
 */
object ReminderScheduler {

    fun schedule(
        context: Context,
        hour: Int = 21,
        minute: Int = 0,
        message: String = "Hôm nay bạn đã nhập đầy đủ chi tiêu chưa?",
        enabled: Boolean = true
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(WORK_TAG)

        if (!enabled) return

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time already passed today, schedule for tomorrow
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis

        val inputData = Data.Builder()
            .putString("message", message)
            .build()

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(WORK_TAG)
            .setConstraints(Constraints.NONE)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }
}
