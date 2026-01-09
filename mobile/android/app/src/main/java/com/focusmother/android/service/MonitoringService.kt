package com.focusmother.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusmother.android.FocusMotherApplication
import com.focusmother.android.R
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.ui.MainActivity
import kotlinx.coroutines.*

/**
 * MonitoringService - Background service that continuously monitors phone usage
 * Runs as a foreground service to ensure it's not killed by the system
 */
class MonitoringService : Service() {

    private lateinit var usageMonitor: UsageMonitor
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var lastInterventionTime = 0L
    private val interventionCooldown = 15 * 60 * 1000L // 15 minutes cooldown

    override fun onCreate() {
        super.onCreate()
        usageMonitor = UsageMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY // Restart if killed
    }

    private fun startMonitoring() {
        // Show foreground notification
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start monitoring loop
        monitoringJob = serviceScope.launch {
            while (isActive) {
                performMonitoringCheck()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun performMonitoringCheck() {
        try {
            // Check if user has been on phone for too long
            val detection = usageMonitor.detectContinuousUsage(
                thresholdMinutes = 30
            )

            // Trigger intervention if needed (with cooldown)
            if (detection.isExcessive && shouldTriggerIntervention()) {
                triggerIntervention(detection)
            }

            // Update foreground notification with current stats
            updateForegroundNotification(detection.getFormattedScreenTime())

        } catch (e: Exception) {
            // Log error but continue monitoring
            e.printStackTrace()
        }
    }

    private fun shouldTriggerIntervention(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastIntervention = currentTime - lastInterventionTime

        return timeSinceLastIntervention >= interventionCooldown
    }

    private fun triggerIntervention(detection: com.focusmother.android.monitor.UsageDetection) {
        lastInterventionTime = System.currentTimeMillis()

        // Create high-priority alert notification
        val alertIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_intervention", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            INTERVENTION_REQUEST_CODE,
            alertIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for a Break?")
            .setContentText(detection.message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${detection.message}\n\nYou've been active for ${detection.getFormattedScreenTime()}. Maybe it's time to take a break?"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .addAction(
                R.drawable.ic_check,
                "I'll take a break",
                createActionPendingIntent(ACTION_TAKE_BREAK)
            )
            .addAction(
                R.drawable.ic_time,
                "5 more minutes",
                createActionPendingIntent(ACTION_REQUEST_TIME)
            )
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(INTERVENTION_NOTIFICATION_ID, notification)
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, com.focusmother.android.receiver.NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("FocusMother is Monitoring")
            .setContentText("Tracking your phone usage...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateForegroundNotification(screenTime: String) {
        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("FocusMother is Monitoring")
            .setContentText("Screen time: $screenTime")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "com.focusmother.android.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.focusmother.android.STOP_MONITORING"
        const val ACTION_TAKE_BREAK = "com.focusmother.android.TAKE_BREAK"
        const val ACTION_REQUEST_TIME = "com.focusmother.android.REQUEST_TIME"

        private const val NOTIFICATION_ID = 1001
        private const val INTERVENTION_NOTIFICATION_ID = 1002
        private const val INTERVENTION_REQUEST_CODE = 100
        private const val CHECK_INTERVAL_MS = 60_000L // Check every minute
    }
}
