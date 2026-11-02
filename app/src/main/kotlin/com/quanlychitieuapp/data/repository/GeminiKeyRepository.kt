package com.quanlychitieuapp.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class GeminiKeyRepository(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "gemini_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // If Keystore is corrupted, clear old preferences and recreate
        context.getSharedPreferences("gemini_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val file = java.io.File("${context.filesDir.parent}/shared_prefs/gemini_prefs.xml")
        if (file.exists()) file.delete()
        
        EncryptedSharedPreferences.create(
            context,
            "gemini_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasApiKey(): Boolean {
        return !prefs.getString("gemini_api_key", "").isNullOrEmpty()
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun deleteApiKey() {
        prefs.edit().remove("gemini_api_key").apply()
    }

    fun getApiKey(): String? {
        val key = prefs.getString("gemini_api_key", "")
        return if (key.isNullOrEmpty()) null else key
    }
}
