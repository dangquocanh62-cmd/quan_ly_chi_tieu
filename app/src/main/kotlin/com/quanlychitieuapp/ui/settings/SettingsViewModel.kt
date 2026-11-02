package com.quanlychitieuapp.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quanlychitieuapp.data.repository.GeminiKeyRepository
import com.quanlychitieuapp.data.repository.SettingsRepository
import com.quanlychitieuapp.data.repository.UserSettings
import com.quanlychitieuapp.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(true, 21, 0, false, 1_000_000L),
    val hasApiKey: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val geminiKeyRepository: GeminiKeyRepository,
    private val applicationContext: Context
) : ViewModel() {

    private val _hasApiKey = MutableStateFlow(geminiKeyRepository.hasApiKey())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userSettingsFlow,
        _hasApiKey
    ) { settings, hasKey ->
        SettingsUiState(
            userSettings = settings,
            hasApiKey = hasKey
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(hasApiKey = geminiKeyRepository.hasApiKey())
    )

    fun updateReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateReminderEnabled(enabled)
            val settings = uiState.value.userSettings
            if (enabled) {
                ReminderScheduler.schedule(
                    context = applicationContext,
                    hour = settings.reminderHour,
                    minute = settings.reminderMinute,
                    enabled = true
                )
            } else {
                ReminderScheduler.schedule(
                    context = applicationContext,
                    enabled = false
                )
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderTime(hour, minute)
            if (uiState.value.userSettings.reminderEnabled) {
                // Cancel and replaced due to ExistingPeriodicWorkPolicy.REPLACE in scheduler
                ReminderScheduler.schedule(
                    context = applicationContext,
                    hour = hour,
                    minute = minute,
                    enabled = true
                )
            }
        }
    }

    fun updateGeminiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateGeminiEnabled(enabled)
        }
    }

    fun updateDebtThreshold(threshold: Long) {
        viewModelScope.launch {
            settingsRepository.updateDebtThreshold(threshold)
        }
    }

    fun saveGeminiApiKey(key: String) {
        geminiKeyRepository.saveApiKey(key)
        _hasApiKey.update { true }
    }
}
