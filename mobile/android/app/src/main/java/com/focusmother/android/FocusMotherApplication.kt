package com.focusmother.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * FocusMotherFocus Application Class
 * Initializes app-wide components and notification channels
 */
class FocusMotherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channels
        createNotificationChannels()
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
    }
}
