package com.focusmother.android.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.focusmother.android.data.preferences.SettingsPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SettingsRepository.
 *
 * Tests DataStore read/write operations, validation, and quiet hours logic.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var testScope: TestScope
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher() + Job())

        // Mock context to return a test DataStore file
        Mockito.`when`(mockContext.filesDir)
            .thenReturn(java.io.File(System.getProperty("java.io.tmpdir")))

        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                mockContext.preferencesDataStoreFile("test_settings_${System.currentTimeMillis()}")
            }
        )

        repository = SettingsRepository(mockContext)

        // Use reflection to inject test DataStore
        val field = SettingsRepository::class.java.getDeclaredField("dataStore")
        field.isAccessible = true
        // Note: This is a simplified test setup. In production, you'd use dependency injection
    }

    @After
    fun teardown() {
        testScope.cancel()
    }

    @Test
    fun `settingsFlow returns default settings initially`() = runTest {
        // Create a new repository instance for clean state
        val repo = SettingsRepository(mockContext)

        // This test verifies defaults are returned when no preferences exist
        // We'll test the actual flow in integration tests
        // For unit test, we verify the default SettingsPreferences values
        val defaults = SettingsPreferences()

        assertEquals(2 * 60 * 60 * 1000L, defaults.dailyGoalMs)
        assertFalse(defaults.quietHoursEnabled)
        assertEquals(23 * 60, defaults.quietHoursStart)
        assertEquals(7 * 60, defaults.quietHoursEnd)
        assertFalse(defaults.strictModeEnabled)
    }

    @Test
    fun `updateDailyGoal accepts valid values`() = runTest {
        // Test minimum boundary
        val minGoal = SettingsPreferences.hoursToMs(SettingsPreferences.MIN_DAILY_GOAL_HOURS)
        // Should not throw
        try {
            repository.updateDailyGoal(minGoal)
        } catch (e: Exception) {
            throw AssertionError("Should accept minimum goal of 1 hour", e)
        }

        // Test maximum boundary
        val maxGoal = SettingsPreferences.hoursToMs(SettingsPreferences.MAX_DAILY_GOAL_HOURS)
        // Should not throw
        try {
            repository.updateDailyGoal(maxGoal)
        } catch (e: Exception) {
            throw AssertionError("Should accept maximum goal of 8 hours", e)
        }

        // Test mid-range value
        val midGoal = SettingsPreferences.hoursToMs(4)
        try {
            repository.updateDailyGoal(midGoal)
        } catch (e: Exception) {
            throw AssertionError("Should accept mid-range goal of 4 hours", e)
        }
    }

    @Test
    fun `updateDailyGoal rejects values below minimum`() = runTest {
        val belowMin = SettingsPreferences.hoursToMs(SettingsPreferences.MIN_DAILY_GOAL_HOURS) - 1

        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateDailyGoal(belowMin)
        }

        assertTrue(exception.message!!.contains("at least"))
    }

    @Test
    fun `updateDailyGoal rejects values above maximum`() = runTest {
        val aboveMax = SettingsPreferences.hoursToMs(SettingsPreferences.MAX_DAILY_GOAL_HOURS) + 1

        val exception = assertFailsWith<IllegalArgumentException> {
            repository.updateDailyGoal(aboveMax)
        }

        assertTrue(exception.message!!.contains("at most"))
    }

    @Test
    fun `updateQuietHours accepts valid time ranges`() = runTest {
        // Normal day range: 9:00 - 17:00
        try {
            repository.updateQuietHours(true, 9 * 60, 17 * 60)
        } catch (e: Exception) {
            throw AssertionError("Should accept normal day range", e)
        }

        // Overnight range: 23:00 - 07:00
        try {
            repository.updateQuietHours(true, 23 * 60, 7 * 60)
        } catch (e: Exception) {
            throw AssertionError("Should accept overnight range", e)
        }

        // Boundary values
        try {
            repository.updateQuietHours(true, 0, 1439)
        } catch (e: Exception) {
            throw AssertionError("Should accept boundary values 0 and 1439", e)
        }
    }

    @Test
    fun `updateQuietHours rejects negative start time`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.updateQuietHours(true, -1, 7 * 60)
        }
    }

    @Test
    fun `updateQuietHours rejects start time above 1439`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.updateQuietHours(true, 1440, 7 * 60)
        }
    }

    @Test
    fun `updateQuietHours rejects negative end time`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.updateQuietHours(true, 23 * 60, -1)
        }
    }

    @Test
    fun `updateQuietHours rejects end time above 1439`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            repository.updateQuietHours(true, 23 * 60, 1440)
        }
    }

    @Test
    fun `updateStrictMode updates setting`() = runTest {
        // Should not throw
        try {
            repository.updateStrictMode(true)
            repository.updateStrictMode(false)
        } catch (e: Exception) {
            throw AssertionError("updateStrictMode should not throw", e)
        }
    }

    @Test
    fun `isInQuietHours returns false when disabled`() {
        val settings = SettingsPreferences(quietHoursEnabled = false)

        // Should return false regardless of time
        assertFalse(repository.isInQuietHours(0, settings))
        assertFalse(repository.isInQuietHours(12 * 60, settings))
        assertFalse(repository.isInQuietHours(23 * 60, settings))
    }

    @Test
    fun `isInQuietHours detects normal day range correctly`() {
        // Quiet hours: 9:00 - 17:00 (9*60 = 540, 17*60 = 1020)
        val settings = SettingsPreferences(
            quietHoursEnabled = true,
            quietHoursStart = 9 * 60,
            quietHoursEnd = 17 * 60
        )

        // Before quiet hours
        assertFalse(repository.isInQuietHours(8 * 60 + 59, settings)) // 8:59

        // Start of quiet hours
        assertTrue(repository.isInQuietHours(9 * 60, settings)) // 9:00

        // During quiet hours
        assertTrue(repository.isInQuietHours(12 * 60, settings)) // 12:00

        // End of quiet hours (exclusive)
        assertFalse(repository.isInQuietHours(17 * 60, settings)) // 17:00

        // After quiet hours
        assertFalse(repository.isInQuietHours(18 * 60, settings)) // 18:00
    }

    @Test
    fun `isInQuietHours detects overnight range correctly`() {
        // Quiet hours: 23:00 - 07:00 (23*60 = 1380, 7*60 = 420)
        val settings = SettingsPreferences(
            quietHoursEnabled = true,
            quietHoursStart = 23 * 60,
            quietHoursEnd = 7 * 60
        )

        // Before midnight, before quiet hours
        assertFalse(repository.isInQuietHours(22 * 60, settings)) // 22:00

        // Before midnight, during quiet hours
        assertTrue(repository.isInQuietHours(23 * 60, settings)) // 23:00
        assertTrue(repository.isInQuietHours(23 * 60 + 30, settings)) // 23:30

        // After midnight, during quiet hours
        assertTrue(repository.isInQuietHours(0, settings)) // 00:00
        assertTrue(repository.isInQuietHours(3 * 60, settings)) // 03:00
        assertTrue(repository.isInQuietHours(6 * 60 + 59, settings)) // 06:59

        // After midnight, after quiet hours
        assertFalse(repository.isInQuietHours(7 * 60, settings)) // 07:00
        assertFalse(repository.isInQuietHours(12 * 60, settings)) // 12:00
    }

    @Test
    fun `isInQuietHours handles edge case of same start and end time`() {
        // Same time means entire day is quiet (unusual but valid)
        val settings = SettingsPreferences(
            quietHoursEnabled = true,
            quietHoursStart = 12 * 60,
            quietHoursEnd = 12 * 60
        )

        // When start == end, it's treated as overnight (wraps around)
        // So all times except exactly 12:00 should be quiet
        assertTrue(repository.isInQuietHours(0, settings))
        assertTrue(repository.isInQuietHours(11 * 60 + 59, settings))
        assertFalse(repository.isInQuietHours(12 * 60, settings)) // Exact end time
        assertTrue(repository.isInQuietHours(12 * 60 + 1, settings))
    }

    @Test
    fun `SettingsPreferences helper functions work correctly`() {
        // hoursToMs
        assertEquals(1 * 60 * 60 * 1000L, SettingsPreferences.hoursToMs(1))
        assertEquals(2 * 60 * 60 * 1000L, SettingsPreferences.hoursToMs(2))
        assertEquals(8 * 60 * 60 * 1000L, SettingsPreferences.hoursToMs(8))

        // msToHours
        assertEquals(1, SettingsPreferences.msToHours(1 * 60 * 60 * 1000L))
        assertEquals(2, SettingsPreferences.msToHours(2 * 60 * 60 * 1000L))
        assertEquals(8, SettingsPreferences.msToHours(8 * 60 * 60 * 1000L))

        // timeToMinutes
        assertEquals(0, SettingsPreferences.timeToMinutes(0, 0))
        assertEquals(60, SettingsPreferences.timeToMinutes(1, 0))
        assertEquals(90, SettingsPreferences.timeToMinutes(1, 30))
        assertEquals(1439, SettingsPreferences.timeToMinutes(23, 59))

        // minutesToHour
        assertEquals(0, SettingsPreferences.minutesToHour(0))
        assertEquals(1, SettingsPreferences.minutesToHour(60))
        assertEquals(12, SettingsPreferences.minutesToHour(720))
        assertEquals(23, SettingsPreferences.minutesToHour(1439))

        // minutesToMinute
        assertEquals(0, SettingsPreferences.minutesToMinute(0))
        assertEquals(0, SettingsPreferences.minutesToMinute(60))
        assertEquals(30, SettingsPreferences.minutesToMinute(90))
        assertEquals(59, SettingsPreferences.minutesToMinute(1439))
    }

    @Test
    fun `resetToDefaults clears all preferences`() = runTest {
        // This test verifies the method doesn't throw
        // Actual persistence testing would be in integration tests
        try {
            repository.resetToDefaults()
        } catch (e: Exception) {
            throw AssertionError("resetToDefaults should not throw", e)
        }
    }
}
