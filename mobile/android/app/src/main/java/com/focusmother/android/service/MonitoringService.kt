package com.focusmother.android.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusmother.android.FocusMotherApplication
import com.focusmother.android.R
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.repository.AgreementRepository
import com.focusmother.android.domain.AdultContentManager
import com.focusmother.android.domain.AgreementEnforcer
import com.focusmother.android.domain.CategoryManager
import com.focusmother.android.data.preferences.SettingsPreferences
import com.focusmother.android.data.repository.SettingsRepository
import com.focusmother.android.domain.ViolationResult
import com.focusmother.android.monitor.UsageMonitor
import com.focusmother.android.ui.MainActivity
import com.focusmother.android.ui.conversation.ConversationActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * MonitoringService - Background service that continuously monitors phone usage
 * Runs as a foreground service to ensure it's not killed by the system
 */
class MonitoringService : Service() {

    private lateinit var usageMonitor: UsageMonitor
    private lateinit var agreementRepository: AgreementRepository
    private lateinit var agreementEnforcer: AgreementEnforcer
    private lateinit var categoryManager: CategoryManager
    private lateinit var adultContentManager: AdultContentManager
    private lateinit var settingsRepository: SettingsRepository
    private var monitoringJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var lastInterventionTime = 0L
    private var lastSnoozeTime = 0L
    private val interventionCooldown = 30 * 1000L // Reduced for testing: 30 seconds
    private val snoozeDuration = 5 * 60 * 1000L // 5 minutes

    override fun onCreate() {
        super.onCreate()
        usageMonitor = UsageMonitor(this)
        settingsRepository = SettingsRepository(this)

        // Initialize agreement components
        val database = FocusMotherDatabase.getDatabase(this)
        agreementRepository = AgreementRepository(database.agreementDao())
        agreementEnforcer = AgreementEnforcer()

        // Initialize categorization components
        categoryManager = CategoryManager(database.appCategoryDao())
        adultContentManager = AdultContentManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_SNOOZE -> handleSnooze()
        }
        return START_STICKY // Restart if killed
    }

    private fun handleSnooze() {
        lastSnoozeTime = System.currentTimeMillis()
        // Update notification to show snoozed state
        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("💤 Snoozed for 5 minutes")
            .setContentText("Zordon will return shortly...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
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
            // Get current settings
            val settings = settingsRepository.settingsFlow.first()

            // Check quiet hours
            if (isInQuietHours(settings)) {
                updateForegroundNotification("Quiet hours active")
                return
            }

            // Check snooze
            if (isSnoozed()) {
                return
            }

            val currentApp = usageMonitor.getCurrentApp()
            val currentTime = System.currentTimeMillis()

            // PRIORITY 1: Check active agreements first
            val activeAgreements = agreementRepository.getActiveAgreements()
            val violationResult = agreementEnforcer.checkViolations(
                agreements = activeAgreements,
                currentApp = currentApp?.packageName,
                currentTime = currentTime
            )

            when (violationResult.action) {
                ViolationResult.Action.VIOLATION -> {
                    // User broke an agreement - mark it and launch conversation
                    val agreement = violationResult.violatedAgreement
                    if (agreement != null) {
                        agreementRepository.violateAgreement(agreement.id)
                        launchConversation(
                            currentApp = agreement.appPackageName,
                            interventionReason = "You broke your agreement for ${agreement.appName}. You agreed to ${formatDuration(agreement.agreedDuration)}"
                        )
                    }
                }
                ViolationResult.Action.COMPLETION -> {
                    // User honored an agreement - mark it complete and show positive notification
                    val agreement = violationResult.expiredAgreement
                    if (agreement != null) {
                        agreementRepository.completeAgreement(agreement.id)
                        showPositiveNotification(agreement.appName, agreement.agreedDuration)
                    }
                }
                ViolationResult.Action.NONE -> {
                    // No agreement violations - continue with normal monitoring

                    if (currentApp != null) {
                        val packageName = currentApp.packageName
                        val dailyUsage = currentApp.totalTimeInForeground

                        // PRIORITY: Check if app is adult content (strictest threshold)
                        val isAdultContent = adultContentManager.isAdultContent(packageName)

                        // Get threshold from CategoryManager (uses category defaults + custom thresholds)
                        val usageThreshold = if (isAdultContent) {
                            // Adult content gets strict 5-minute threshold
                            AdultContentManager.SUGGESTED_LIMIT_MS
                        } else {
                            // Use category-based threshold from CategoryManager
                            categoryManager.getThreshold(packageName)
                        }

                        // Check if blocked
                        val isBlocked = categoryManager.isBlocked(packageName)

                        if (isBlocked && shouldTriggerIntervention()) {
                            // App is completely blocked
                            launchConversation(
                                currentApp = packageName,
                                interventionReason = "${currentApp.appName} is blocked. Let's talk about why you need access."
                            )
                        } else if (dailyUsage > usageThreshold && shouldTriggerIntervention()) {
                            // App exceeded its category threshold
                            val formattedTime = formatDuration(dailyUsage)
                            val formattedThreshold = formatDuration(usageThreshold)

                            val reason = if (isAdultContent) {
                                // Use non-judgmental adult content messaging
                                "You've been on this app for $formattedTime. That's longer than your $formattedThreshold limit. Let's talk about healthier alternatives."
                            } else {
                                "You've spent $formattedTime on ${currentApp.appName} today. That exceeds your $formattedThreshold threshold."
                            }

                            launchConversation(
                                currentApp = packageName,
                                interventionReason = reason
                            )
                        }
                    }

                    // General continuous usage check (testing: 1 minute)
                    val detection = usageMonitor.detectContinuousUsage(thresholdMinutes = 1)
                    if (detection.isExcessive && shouldTriggerIntervention()) {
                        launchConversation(
                            currentApp = null,
                            interventionReason = "You've been on your phone continuously for ${detection.getFormattedScreenTime()}"
                        )
                    }
                }
            }

            // Update foreground notification
            val detection = usageMonitor.detectContinuousUsage(thresholdMinutes = 1)
            updateForegroundNotification(detection.getFormattedScreenTime())

        } catch (e: Exception) {
            android.util.Log.e("MonitoringService", "Error in monitoring check", e)
        }
    }

    private fun isInQuietHours(settings: SettingsPreferences): Boolean {
        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return settingsRepository.isInQuietHours(currentMinutes, settings)
    }

    private fun isSnoozed(): Boolean {
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastSnoozeTime) < snoozeDuration
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
            .setContentTitle("💬 Zordon is observing")
            .setContentText("Monitoring your digital activities...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateForegroundNotification(screenTime: String) {
        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_SERVICE_ID)
            .setContentTitle("💬 Zordon is observing")
            .setContentText("Digital realm time: $screenTime")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a positive notification when user successfully completes an agreement.
     *
     * @param appName Name of the app the agreement was for
     * @param duration Duration of the agreement
     */
    private fun showPositiveNotification(appName: String, duration: Long) {
        val formattedTime = formatDuration(duration)
        val message = "Well done, ranger! You honored your $formattedTime agreement for $appName. Your discipline grows stronger!"

        val notification = NotificationCompat.Builder(this, FocusMotherApplication.CHANNEL_ALERT_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("✨ Agreement Completed!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
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
        const val ACTION_SNOOZE = "com.focusmother.android.SNOOZE"
        const val ACTION_TAKE_BREAK = "com.focusmother.android.TAKE_BREAK"
        const val ACTION_REQUEST_TIME = "com.focusmother.android.REQUEST_TIME"
        private const val NOTIFICATION_ID = 1001
        private const val INTERVENTION_NOTIFICATION_ID = 1002
        private const val COMPLETION_NOTIFICATION_ID = 1003
        private const val CHECK_INTERVAL_MS = 2000L // Fast check for testing
    }
}
