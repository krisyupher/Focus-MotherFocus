package com.focusmother.android.data.preferences

/**
 * Data class representing user settings stored in DataStore.
 *
 * Contains all user-configurable preferences for the FocusMother app.
 */
data class SettingsPreferences(
    /**
     * Daily screen time goal in milliseconds.
     * Default: 2 hours (7,200,000 ms)
     */
    val dailyGoalMs: Long = 2 * 60 * 60 * 1000L,

    /**
     * Whether quiet hours are enabled.
     * When enabled, interventions are suppressed during specified time range.
     */
    val quietHoursEnabled: Boolean = false,

    /**
     * Start of quiet hours in minutes since midnight (0-1439).
     * Default: 23:00 (1380 minutes = 23 * 60)
     */
    val quietHoursStart: Int = 23 * 60,

    /**
     * End of quiet hours in minutes since midnight (0-1439).
     * Default: 07:00 (420 minutes = 7 * 60)
     */
    val quietHoursEnd: Int = 7 * 60,

    /**
     * Strict mode - if enabled, user cannot snooze interventions.
     * When true, all interventions must be addressed immediately.
     */
    val strictModeEnabled: Boolean = false
) {
    companion object {
        /**
         * Minimum daily goal in hours
         */
        const val MIN_DAILY_GOAL_HOURS = 1

        /**
         * Maximum daily goal in hours
         */
        const val MAX_DAILY_GOAL_HOURS = 8

        /**
         * Converts hours to milliseconds
         */
        fun hoursToMs(hours: Int): Long = hours * 60 * 60 * 1000L

        /**
         * Converts milliseconds to hours (rounded)
         */
        fun msToHours(ms: Long): Int = (ms / (60 * 60 * 1000)).toInt()

        /**
         * Converts time (hour, minute) to minutes since midnight
         */
        fun timeToMinutes(hour: Int, minute: Int): Int = hour * 60 + minute

        /**
         * Extracts hour from minutes since midnight
         */
        fun minutesToHour(minutes: Int): Int = minutes / 60

        /**
         * Extracts minute from minutes since midnight
         */
        fun minutesToMinute(minutes: Int): Int = minutes % 60
    }
}
