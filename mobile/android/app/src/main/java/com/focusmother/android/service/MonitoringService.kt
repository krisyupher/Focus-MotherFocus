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
import com.focusmother.android.ui.conversation.ConversationActivity
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
    private val interventionCooldown = 30 * 1000L // Reduced for testing: 30 seconds

    // List of "wasting" apps to monitor closely
    private val distractionPackages = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.instagram.android",
        "com.tiktok.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.android.chrome", // Browser often used for adult content or wasting time
        "com.sec.android.app.sbrowser"
    )

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
            val currentApp = usageMonitor.getCurrentApp()

            if (currentApp != null && distractionPackages.contains(currentApp.packageName)) {
                // User is in a distraction app
                val dailyUsage = currentApp.totalTimeInForeground

                // CRITICAL: Set to 1 minute (60,000ms) for very aggressive testing
                val usageThreshold = 60 * 1000L

                if (dailyUsage > usageThreshold && shouldTriggerIntervention()) {
                    launchConversation(
                        currentApp = currentApp.packageName,
                        interventionReason = "You've spent ${formatDuration(dailyUsage)} on ${currentApp.appName} today"
                    )
                }
            }

            // General continuous usage check
            val detection = usageMonitor.detectContinuousUsage(thresholdMinutes = 1) // Test: 1 min
            if (detection.isExcessive && shouldTriggerIntervention()) {
                launchConversation(
                    currentApp = null,
                    interventionReason = "You've been on your phone continuously for ${detection.getFormattedScreenTime()}"
                )
            }

            // Update foreground notification
            updateForegroundNotification(detection.getFormattedScreenTime())

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shouldTriggerIntervention(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastIntervention = currentTime - lastInterventionTime
        return timeSinceLastIntervention >= interventionCooldown
    }

    /**
     * Launches ConversationActivity for AI negotiation.
     *
     * @param currentApp Package name of the app triggering intervention (null for general usage)
     * @param interventionReason Human-readable reason for intervention
     */
    private fun launchConversation(currentApp: String?, interventionReason: String) {
        lastInterventionTime = System.currentTimeMillis()

        val intent = Intent(this, ConversationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ConversationActivity.EXTRA_CURRENT_APP, currentApp)
            putExtra(ConversationActivity.EXTRA_INTERVENTION_REASON, interventionReason)
            putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, 1L)
        }
        startActivity(intent)
    }

    private fun formatDuration(ms: Long): String {
        val minutes = ms / 1000 / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            else -> "${minutes}m"
        }
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("⚡ Zordon Watches Over You")
            .setContentText("Observing your digital activities...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateForegroundNotification(screenTime: String) {
        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("⚡ Zordon Watches Over You")
            .setContentText("Digital realm time: $screenTime")
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
        private const val CHECK_INTERVAL_MS = 2000L // Fast check for testing
    }
}
