package com.quanlychitieuapp

import android.app.Application
import com.quanlychitieuapp.data.local.AppDatabase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.quanlychitieuapp.data.repository.GeminiKeyRepository
import com.quanlychitieuapp.data.repository.SettingsRepository

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SettingsRepository.DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "settings"))
    }
)

class QuanLyChiTieuApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
    
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(dataStore)
    }
    
    val geminiKeyRepository: GeminiKeyRepository by lazy {
        GeminiKeyRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        // WorkManager is auto-initialized via manifest
    }
}
