package com.focusmother.android.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.focusmother.android.data.preferences.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing user settings using DataStore.
 *
 * Provides a reactive Flow-based API for reading and updating settings.
 * All operations are suspend functions and safe for background execution.
 *
 * @param dataStore The DataStore instance to manage settings
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Flow of current settings that emits updates whenever settings change.
     */
    val settingsFlow: Flow<SettingsPreferences> = dataStore.data
        .map { preferences ->
            SettingsPreferences(
                dailyGoalMs = preferences[KEY_DAILY_GOAL_MS] ?: SettingsPreferences().dailyGoalMs,
                quietHoursEnabled = preferences[KEY_QUIET_HOURS_ENABLED] ?: SettingsPreferences().quietHoursEnabled,
                quietHoursStart = preferences[KEY_QUIET_HOURS_START] ?: SettingsPreferences().quietHoursStart,
                quietHoursEnd = preferences[KEY_QUIET_HOURS_END] ?: SettingsPreferences().quietHoursEnd,
                strictModeEnabled = preferences[KEY_STRICT_MODE] ?: SettingsPreferences().strictModeEnabled
            )
        }

    /**
     * Updates the daily screen time goal.
     *
     * @param goalMs Goal in milliseconds
     * @throws IllegalArgumentException if goal is outside valid range
     */
    suspend fun updateDailyGoal(goalMs: Long) {
        require(goalMs >= SettingsPreferences.hoursToMs(SettingsPreferences.MIN_DAILY_GOAL_HOURS)) {
            "Daily goal must be at least ${SettingsPreferences.MIN_DAILY_GOAL_HOURS} hour"
        }
        require(goalMs <= SettingsPreferences.hoursToMs(SettingsPreferences.MAX_DAILY_GOAL_HOURS)) {
            "Daily goal must be at most ${SettingsPreferences.MAX_DAILY_GOAL_HOURS} hours"
        }

        dataStore.edit { preferences ->
            preferences[KEY_DAILY_GOAL_MS] = goalMs
        }
    }

    /**
     * Updates quiet hours settings.
     *
     * @param enabled Whether quiet hours are enabled
     * @param startMinutes Start time in minutes since midnight (0-1439)
     * @param endMinutes End time in minutes since midnight (0-1439)
     * @throws IllegalArgumentException if minutes are out of range
     */
    suspend fun updateQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        require(startMinutes in 0..1439) {
            "Start time must be between 0 and 1439 minutes"
        }
        require(endMinutes in 0..1439) {
            "End time must be between 0 and 1439 minutes"
        }

        dataStore.edit { preferences ->
            preferences[KEY_QUIET_HOURS_ENABLED] = enabled
            preferences[KEY_QUIET_HOURS_START] = startMinutes
            preferences[KEY_QUIET_HOURS_END] = endMinutes
        }
    }

    /**
     * Updates strict mode setting.
     *
     * @param enabled Whether strict mode is enabled
     */
    suspend fun updateStrictMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_STRICT_MODE] = enabled
        }
    }

    /**
     * Resets all settings to defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Sets the daily goal based on automatic usage analysis.
     *
     * This is called during app initialization when no manual goal has been set.
     * The goal is automatically derived from the user's historical usage patterns.
     *
     * @param suggestedLimitMs Suggested limit in milliseconds (from UsageMonitor.getSuggestedDailyLimit)
     */
    suspend fun setAutoDailyGoal(suggestedLimitMs: Long) {
        // Clamp to valid range
        val clampedLimit = suggestedLimitMs.coerceIn(
            SettingsPreferences.hoursToMs(SettingsPreferences.MIN_DAILY_GOAL_HOURS),
            SettingsPreferences.hoursToMs(SettingsPreferences.MAX_DAILY_GOAL_HOURS)
        )

        dataStore.edit { preferences ->
            preferences[KEY_DAILY_GOAL_MS] = clampedLimit
        }
    }

    /**
     * Checks if the current time falls within quiet hours.
     *
     * @param currentMinutes Current time in minutes since midnight
     * @param settings Current settings
     * @return True if currently in quiet hours and they are enabled
     */
    fun isInQuietHours(currentMinutes: Int, settings: SettingsPreferences): Boolean {
        if (!settings.quietHoursEnabled) return false

        val start = settings.quietHoursStart
        val end = settings.quietHoursEnd

        return if (start < end) {
            // Normal case: e.g., 9:00 - 17:00
            currentMinutes in start until end
        } else {
            // Overnight case: e.g., 23:00 - 07:00
            currentMinutes >= start || currentMinutes < end
        }
    }

    companion object {
        private val KEY_DAILY_GOAL_MS = longPreferencesKey("daily_goal_ms")
        private val KEY_QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val KEY_QUIET_HOURS_START = intPreferencesKey("quiet_hours_start")
        private val KEY_QUIET_HOURS_END = intPreferencesKey("quiet_hours_end")
        private val KEY_STRICT_MODE = booleanPreferencesKey("strict_mode")
    }
}
