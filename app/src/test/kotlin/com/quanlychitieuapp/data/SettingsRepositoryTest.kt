package com.quanlychitieuapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.quanlychitieuapp.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_settings") }
        )
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun `test default values`() = testScope.runTest {
        val settings = repository.userSettingsFlow.first()
        assertTrue(settings.reminderEnabled)
        assertEquals(21, settings.reminderHour)
        assertEquals(0, settings.reminderMinute)
        assertEquals(false, settings.geminiEnabled)
        assertEquals(1_000_000L, settings.debtThreshold)
    }

    @Test
    fun `test save and read values`() = testScope.runTest {
        repository.updateReminderEnabled(false)
        repository.updateReminderTime(22, 30)
        repository.updateGeminiEnabled(true)
        repository.updateDebtThreshold(500_000L)

        val settings = repository.userSettingsFlow.first()
        
        assertEquals(false, settings.reminderEnabled)
        assertEquals(22, settings.reminderHour)
        assertEquals(30, settings.reminderMinute)
        assertEquals(true, settings.geminiEnabled)
        assertEquals(500_000L, settings.debtThreshold)
    }

    @Test
    fun `test repository recreation preserves values`() = testScope.runTest {
        // Save using first repo
        repository.updateReminderTime(23, 15)
        
        // "Recreate" repository using same DataStore file structure
        val recreatedDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_settings") }
        )
        val recreatedRepository = SettingsRepository(recreatedDataStore)
        
        val settings = recreatedRepository.userSettingsFlow.first()
        assertEquals(23, settings.reminderHour)
        assertEquals(15, settings.reminderMinute)
    }
}
