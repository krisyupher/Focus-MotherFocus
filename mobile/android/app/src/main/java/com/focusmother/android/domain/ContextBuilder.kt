package com.focusmother.android.domain

import com.focusmother.android.data.dao.AgreementDao
import com.focusmother.android.data.entity.Agreement
import com.focusmother.android.monitor.UsageMonitor

/**
 * Builds conversation context from various data sources.
 *
 * Gathers information about user's phone usage, current app, recent agreements,
 * and formats it for inclusion in Claude AI prompts. This context enables the
 * AI to have informed conversations about phone usage.
 *
 * @property usageMonitor Provides screen time and app usage statistics
 * @property categoryManager Categorizes apps (social media, games, etc.)
 * @property agreementDao Retrieves recent time agreements
 */
class ContextBuilder(
    private val usageMonitor: UsageMonitor,
    private val categoryManager: CategoryManager,
    private val agreementDao: AgreementDao
) {

    /**
     * Builds comprehensive context for AI conversation.
     *
     * Gathers data from multiple sources:
     * - Total screen time today
     * - Current app being used (if any)
     * - App category
     * - Top apps used today
     * - Recent time agreements
     * - Reason for intervention
     *
     * @param currentApp Package name of current app (null for general usage)
     * @param interventionReason Why the intervention was triggered
     * @return ConversationContext with all gathered information
     */
    suspend fun buildContext(
        currentApp: String?,
        interventionReason: String
    ): ConversationContext {
        // Get today's total screen time
        val screenTimeMs = usageMonitor.getTodayScreenTime()
        val formattedScreenTime = formatTime(screenTimeMs)

        // Get top apps for the day
        val topApps = usageMonitor.getTopApps(5)

        // Build app usage summary
        val appUsageSummary = if (topApps.isEmpty()) {
            "No significant app usage today"
        } else {
            topApps.joinToString("\n") { appInfo ->
                val time = formatTime(appInfo.totalTimeInForeground)
                "• ${appInfo.appName}: $time"
            }
        }

        // Determine current app name and category
        val currentAppName: String?
        val currentAppCategory: String?

        if (currentApp != null) {
            // Try to find app name from top apps list
            val appInfo = topApps.find { it.packageName == currentApp }
            currentAppName = appInfo?.appName ?: currentApp
            currentAppCategory = categoryManager.categorizeApp(currentApp)
        } else {
            currentAppName = null
            currentAppCategory = null
        }

        // Get recent agreements (limit to 5 most recent)
        val recentAgreements = agreementDao.getRecent(5)

        return ConversationContext(
            todayScreenTime = formattedScreenTime,
            currentApp = currentAppName,
            currentAppCategory = currentAppCategory,
            appUsageToday = appUsageSummary,
            recentAgreements = recentAgreements,
            interventionReason = interventionReason
        )
    }

    /**
     * Formats milliseconds to human-readable time string.
     *
     * Examples:
     * - 3h 15m
     * - 45m
     * - <1m
     *
     * @param milliseconds Time in milliseconds
     * @return Formatted time string
     */
    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 1000 / 60).toInt()
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}

/**
 * Context data for AI conversation.
 *
 * Contains all relevant information about the user's phone usage,
 * current activity, and intervention reason. This is used to build
 * informed system prompts for Claude AI.
 *
 * @property todayScreenTime Total screen time today (formatted, e.g., "2h 45m")
 * @property currentApp Name of current app (null if general usage)
 * @property currentAppCategory Category of current app (e.g., "SOCIAL_MEDIA")
 * @property appUsageToday Summary of top apps used today with time spent
 * @property recentAgreements List of recent time agreements (max 5)
 * @property interventionReason Why intervention was triggered
 */
data class ConversationContext(
    val todayScreenTime: String,
    val currentApp: String?,
    val currentAppCategory: String?,
    val appUsageToday: String,
    val recentAgreements: List<Agreement>,
    val interventionReason: String
)
