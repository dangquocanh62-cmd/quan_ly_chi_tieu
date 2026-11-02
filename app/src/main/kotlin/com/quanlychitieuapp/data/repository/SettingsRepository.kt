package com.quanlychitieuapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserSettings(
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val geminiEnabled: Boolean,
    val debtThreshold: Long
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    // Defined keys
    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
        val DEBT_THRESHOLD = longPreferencesKey("debt_threshold")
    }

    val userSettingsFlow: Flow<UserSettings> = dataStore.data.map { preferences ->
        UserSettings(
            reminderEnabled = preferences[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = preferences[Keys.REMINDER_HOUR] ?: 21,
            reminderMinute = preferences[Keys.REMINDER_MINUTE] ?: 0,
            geminiEnabled = preferences[Keys.GEMINI_ENABLED] ?: false,
            // migrate from old value if not found
            debtThreshold = preferences[Keys.DEBT_THRESHOLD] ?: 1_000_000L
        )
    }

    suspend fun updateReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateGeminiEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GEMINI_ENABLED] = enabled
        }
    }

    suspend fun updateDebtThreshold(threshold: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.DEBT_THRESHOLD] = threshold
        }
    }

    companion object {
        const val DATASTORE_NAME = "user_settings"
    }
}
