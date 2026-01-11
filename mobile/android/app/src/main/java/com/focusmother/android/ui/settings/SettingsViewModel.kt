package com.focusmother.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusmother.android.data.preferences.SettingsPreferences
import com.focusmother.android.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages user preferences including daily goals, quiet hours, and strict mode.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * Current settings as a StateFlow for Compose observation.
     */
    val settings: StateFlow<SettingsPreferences> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsPreferences()
        )

    /**
     * Updates the daily screen time goal.
     *
     * @param hours Goal in hours (1-8)
     */
    fun updateDailyGoal(hours: Int) {
        viewModelScope.launch {
            try {
                val goalMs = SettingsPreferences.hoursToMs(hours)
                settingsRepository.updateDailyGoal(goalMs)
            } catch (e: IllegalArgumentException) {
                // Log error or emit to UI state
                android.util.Log.e("SettingsViewModel", "Error updating settings", e)
            }
        }
    }

    /**
     * Updates quiet hours settings.
     *
     * @param enabled Whether quiet hours are enabled
     * @param startHour Start hour (0-23)
     * @param startMinute Start minute (0-59)
     * @param endHour End hour (0-23)
     * @param endMinute End minute (0-59)
     */
    fun updateQuietHours(
        enabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        viewModelScope.launch {
            try {
                val startMinutes = SettingsPreferences.timeToMinutes(startHour, startMinute)
                val endMinutes = SettingsPreferences.timeToMinutes(endHour, endMinute)
                settingsRepository.updateQuietHours(enabled, startMinutes, endMinutes)
            } catch (e: IllegalArgumentException) {
                android.util.Log.e("SettingsViewModel", "Error updating settings", e)
            }
        }
    }

    /**
     * Updates strict mode setting.
     *
     * @param enabled Whether strict mode is enabled
     */
    fun updateStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStrictMode(enabled)
        }
    }

    /**
     * Resets all settings to their default values.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }

    /**
     * Gets the current quiet hours start time as a formatted string.
     *
     * @return Time string in format "HH:MM"
     */
    fun getQuietHoursStartFormatted(): String {
        val minutes = settings.value.quietHoursStart
        val hour = SettingsPreferences.minutesToHour(minutes)
        val minute = SettingsPreferences.minutesToMinute(minutes)
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Gets the current quiet hours end time as a formatted string.
     *
     * @return Time string in format "HH:MM"
     */
    fun getQuietHoursEndFormatted(): String {
        val minutes = settings.value.quietHoursEnd
        val hour = SettingsPreferences.minutesToHour(minutes)
        val minute = SettingsPreferences.minutesToMinute(minutes)
        return String.format("%02d:%02d", hour, minute)
    }
}
