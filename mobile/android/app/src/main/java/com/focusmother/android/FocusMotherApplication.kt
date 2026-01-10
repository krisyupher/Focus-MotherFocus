package com.focusmother.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.domain.CategoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
    }
}
