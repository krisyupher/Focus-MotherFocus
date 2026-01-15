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

    /**
     * Analyzes historical usage patterns to establish a baseline.
     *
     * Looks at the past N days to calculate average daily screen time.
     * This is used to automatically set appropriate thresholds instead of
     * requiring users to manually set daily goals.
     *
     * @param days Number of past days to analyze (default: 7)
     * @return UsageBaseline containing average and peak usage data
     */
    suspend fun analyzeUsageBaseline(days: Int = 7): UsageBaseline = withContext(Dispatchers.IO) {
        if (usageStatsManager == null) {
            return@withContext UsageBaseline(
                averageDailyUsageMs = 2 * 60 * 60 * 1000L, // Default 2 hours
                peakDailyUsageMs = 3 * 60 * 60 * 1000L,    // Default 3 hours
                daysAnalyzed = 0,
                topApps = emptyList()
            )
        }

        val dailyUsages = mutableListOf<Long>()
        val appUsageMap = mutableMapOf<String, Long>()
        val calendar = Calendar.getInstance()

        // Analyze each day
        for (day in 1..days) {
            // Set to start of day (days ago)
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -day)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis

            // Set to end of day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val dayEnd = calendar.timeInMillis

            // Query usage for this day
            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                dayStart,
                dayEnd
            )

            if (usageStatsList != null && usageStatsList.isNotEmpty()) {
                val dailyTotal = usageStatsList.sumOf { it.totalTimeInForeground }
                if (dailyTotal > 0) {
                    dailyUsages.add(dailyTotal)
                }

                // Aggregate app usage
                usageStatsList.forEach { stat ->
                    if (stat.totalTimeInForeground > 0) {
                        val current = appUsageMap[stat.packageName] ?: 0L
                        appUsageMap[stat.packageName] = current + stat.totalTimeInForeground
                    }
                }
            }
        }

        // Calculate statistics
        val averageUsage = if (dailyUsages.isNotEmpty()) {
            dailyUsages.sum() / dailyUsages.size
        } else {
            2 * 60 * 60 * 1000L // Default 2 hours
        }

        val peakUsage = if (dailyUsages.isNotEmpty()) {
            dailyUsages.maxOrNull() ?: averageUsage
        } else {
            3 * 60 * 60 * 1000L // Default 3 hours
        }

        // Get top apps
        val topApps = appUsageMap.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { (packageName, totalTime) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    lastTimeUsed = 0L,
                    totalTimeInForeground = totalTime / days.coerceAtLeast(1)
                )
            }

        UsageBaseline(
            averageDailyUsageMs = averageUsage,
            peakDailyUsageMs = peakUsage,
            daysAnalyzed = dailyUsages.size,
            topApps = topApps
        )
    }

    /**
     * Gets a suggested threshold based on the user's actual usage patterns.
     *
     * Returns a threshold that is slightly lower than average usage to encourage
     * gradual reduction without being unrealistic.
     *
     * @param days Number of days to analyze
     * @return Suggested daily limit in milliseconds
     */
    suspend fun getSuggestedDailyLimit(days: Int = 7): Long {
        val baseline = analyzeUsageBaseline(days)

        // Suggest 80% of average usage as a starting point
        // This encourages reduction while being achievable
        val suggested = (baseline.averageDailyUsageMs * 0.8).toLong()

        // Clamp between 1 hour and 8 hours
        val minLimit = 1 * 60 * 60 * 1000L
        val maxLimit = 8 * 60 * 60 * 1000L

        return suggested.coerceIn(minLimit, maxLimit)
    }
}

/**
 * Data class representing analyzed usage baseline.
 *
 * Contains statistics derived from historical usage data to help
 * the app automatically set appropriate thresholds.
 */
data class UsageBaseline(
    /** Average daily screen time in milliseconds */
    val averageDailyUsageMs: Long,
    /** Peak (highest) daily screen time in milliseconds */
    val peakDailyUsageMs: Long,
    /** Number of days with valid data that were analyzed */
    val daysAnalyzed: Int,
    /** Top apps by average daily usage */
    val topApps: List<AppUsageInfo>
) {
    /**
     * Returns formatted average daily usage.
     */
    fun getFormattedAverageUsage(): String {
        val minutes = averageDailyUsageMs / 1000 / 60
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> "${hours}h ${remainingMinutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    /**
     * Returns formatted peak daily usage.
     */
    fun getFormattedPeakUsage(): String {
        val minutes = peakDailyUsageMs / 1000 / 60
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
