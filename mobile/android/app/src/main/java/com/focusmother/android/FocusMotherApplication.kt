package com.focusmother.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.repository.SettingsRepository
import com.focusmother.android.domain.CategoryManager
import com.focusmother.android.monitor.UsageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Property delegate for DataStore (singleton)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_mother_settings")

/**
 * FocusMotherFocus Application Class
 * Initializes app-wide components and notification channels
 */
class FocusMotherApplication : Application() {

    /**
     * Room database instance (singleton)
     */
    lateinit var database: FocusMotherDatabase
        private set

    /**
     * Category manager for app categorization
     */
    lateinit var categoryManager: CategoryManager
        private set

    /**
     * Application-scoped coroutine scope for background operations
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        createNotificationChannels()

        // Initialize database
        database = FocusMotherDatabase.getDatabase(this)
        categoryManager = CategoryManager(database.appCategoryDao())

        // Seed database on first launch
        seedDatabaseIfNeeded()

        // Auto-set daily goal based on usage analysis
        initializeUsageBasedGoal()
    }

    /**
     * Seeds the database with app categories on first launch.
     *
     * Uses SharedPreferences to track whether seeding has occurred.
     * This ensures we only seed once and preserve user customizations.
     */
    private fun seedDatabaseIfNeeded() {
        applicationScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_DATABASE_SEEDED, false)) {
                categoryManager.seedDatabase()
                prefs.edit().putBoolean(KEY_DATABASE_SEEDED, true).apply()
            }
        }
    }

    /**
     * Automatically sets the daily goal based on the user's actual usage patterns.
     *
     * This analyzes the past 7 days of usage data and sets a goal that is
     * 80% of the average daily usage (encouraging gradual reduction).
     * Only runs once after onboarding is complete.
     */
    private fun initializeUsageBasedGoal() {
        applicationScope.launch {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Only set auto-goal once, after onboarding and if not already set
            val onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
            val autoGoalSet = prefs.getBoolean(KEY_AUTO_GOAL_SET, false)

            if (onboardingComplete && !autoGoalSet) {
                val usageMonitor = UsageMonitor(this@FocusMotherApplication)

                // Check if we have usage stats permission
                if (usageMonitor.hasUsageStatsPermission()) {
                    val suggestedLimit = usageMonitor.getSuggestedDailyLimit(days = 7)
                    val settingsRepository = SettingsRepository(dataStore)
                    settingsRepository.setAutoDailyGoal(suggestedLimit)

                    // Mark as set so we don't overwrite user changes later
                    prefs.edit().putBoolean(KEY_AUTO_GOAL_SET, true).apply()

                    android.util.Log.i(
                        "FocusMotherApp",
                        "Auto-set daily goal based on usage analysis: ${suggestedLimit / 1000 / 60} minutes"
                    )
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when FocusMother is actively monitoring"
                setShowBadge(false)
            }

            // Alert Channel (for interventions)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Usage Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you've been using your phone too long"
                setShowBadge(true)
                enableVibration(true)
            }

            // Agreement Channel (for time agreements)
            val agreementChannel = NotificationChannel(
                CHANNEL_AGREEMENT_ID,
                "Time Agreements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders about your time commitments"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(serviceChannel, alertChannel, agreementChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "monitoring_service"
        const val CHANNEL_ALERT_ID = "usage_alerts"
        const val CHANNEL_AGREEMENT_ID = "time_agreements"

        private const val PREFS_NAME = "focus_mother_prefs"
        private const val KEY_DATABASE_SEEDED = "database_seeded"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_AUTO_GOAL_SET = "auto_goal_set"
    }
}
