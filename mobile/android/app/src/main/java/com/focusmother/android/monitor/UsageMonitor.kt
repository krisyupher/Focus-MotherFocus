package com.focusmother.android.monitor

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * UsageMonitor - Monitors phone usage using UsageStatsManager
 * Detects when user spends too much time on phone or specific apps
 */
class UsageMonitor(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val packageManager: PackageManager = context.packageManager

    /**
     * Get total screen time for today (in milliseconds)
     */
    suspend fun getTodayScreenTime(): Long = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        getScreenTimeBetween(startOfDay, endTime)
    }

    /**
     * Get screen time in the last N minutes
     */
    suspend fun getRecentScreenTime(minutes: Int): Long = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (minutes * 60 * 1000L)

        getScreenTimeBetween(startTime, endTime)
    }

    /**
     * Get screen time between two timestamps
     */
    private fun getScreenTimeBetween(startTime: Long, endTime: Long): Long {
        if (usageStatsManager == null) return 0L

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return usageStatsList?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    /**
     * Get current foreground app package name
     */
    suspend fun getCurrentApp(): AppUsageInfo? = withContext(Dispatchers.IO) {
        if (usageStatsManager == null) return@withContext null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (5 * 1000L) // Last 5 seconds

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        )

        // Get the most recently used app
        val currentApp = usageStatsList
            ?.maxByOrNull { it.lastTimeUsed }

        currentApp?.let {
            AppUsageInfo(
                packageName = it.packageName,
                appName = getAppName(it.packageName),
                lastTimeUsed = it.lastTimeUsed,
                totalTimeInForeground = it.totalTimeInForeground
            )
        }
    }

    /**
     * Detect if user has been on phone continuously for too long
     */
    suspend fun detectContinuousUsage(thresholdMinutes: Int = 30): UsageDetection {
        val recentScreenTime = getRecentScreenTime(thresholdMinutes)
        val thresholdMs = thresholdMinutes * 60 * 1000L
        val usagePercentage = (recentScreenTime.toFloat() / thresholdMs) * 100

        return UsageDetection(
            isExcessive = recentScreenTime >= thresholdMs * 0.8f, // 80% threshold
            screenTime = recentScreenTime,
            usagePercentage = usagePercentage.toInt(),
            message = when {
                usagePercentage >= 80 -> "You've been on your phone for ${thresholdMinutes} minutes straight!"
                usagePercentage >= 60 -> "You've been using your phone quite a bit..."
                else -> "Phone usage is normal"
            }
        )
    }

    /**
     * Get top apps by usage today
     */
    suspend fun getTopApps(limit: Int = 5): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        if (usageStatsManager == null) return@withContext emptyList()

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            endTime
        )

        usageStatsList
            ?.filter { it.totalTimeInForeground > 0 }
            ?.sortedByDescending { it.totalTimeInForeground }
            ?.take(limit)
            ?.map { stats ->
                AppUsageInfo(
                    packageName = stats.packageName,
                    appName = getAppName(stats.packageName),
                    lastTimeUsed = stats.lastTimeUsed,
                    totalTimeInForeground = stats.totalTimeInForeground
                )
            } ?: emptyList()
    }

    /**
     * Get human-readable app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        if (usageStatsManager == null) return false

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60) // Last minute

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return !usageStatsList.isNullOrEmpty()
    }

    /**
     * Get screen time for a specific date range.
     *
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Total screen time in milliseconds
     */
    fun getScreenTimeForDateRange(startTime: Long, endTime: Long): Long {
        return getScreenTimeBetween(startTime, endTime)
    }
}

/**
 * Data class for app usage information
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val lastTimeUsed: Long,
    val totalTimeInForeground: Long
) {
    fun getFormattedTime(): String {
        val minutes = totalTimeInForeground / 1000 / 60
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
 * Data class for usage detection results
 */
data class UsageDetection(
    val isExcessive: Boolean,
    val screenTime: Long,
    val usagePercentage: Int,
    val message: String
) {
    fun getFormattedScreenTime(): String {
        val minutes = screenTime / 1000 / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}
