package com.focusmother.android.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusmother.android.data.entity.Agreement
import com.focusmother.android.data.repository.AgreementRepository
import com.focusmother.android.monitor.AppUsageInfo
import com.focusmother.android.monitor.UsageMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel for the Analytics screen.
 *
 * Provides weekly statistics including:
 * - Agreement success rate
 * - Top apps usage
 * - Daily screen time trends
 */
class AnalyticsViewModel(
    private val agreementRepository: AgreementRepository,
    private val usageMonitor: UsageMonitor
) : ViewModel() {

    private val _weeklyStats = MutableStateFlow<WeeklyStats?>(null)
    val weeklyStats: StateFlow<WeeklyStats?> = _weeklyStats.asStateFlow()

    // Individual StateFlows for UI components
    private val _agreementStats = MutableStateFlow(AgreementStats(total = 0, completed = 0, violated = 0))
    val agreementStats: StateFlow<AgreementStats> = _agreementStats.asStateFlow()

    private val _topApps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val topApps: StateFlow<List<AppUsageInfo>> = _topApps.asStateFlow()

    private val _weeklyScreenTime = MutableStateFlow<List<Long>>(List(7) { 0L })
    val weeklyScreenTime: StateFlow<List<Long>> = _weeklyScreenTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadWeeklyStats()
    }

    /**
     * Loads weekly statistics from the last 7 days.
     */
    fun loadWeeklyStats() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val calendar = Calendar.getInstance()
                val endTime = calendar.timeInMillis

                // Calculate start time (7 days ago at midnight)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                // Load agreements from the last 7 days
                val agreements = agreementRepository.getAgreementsByDateRange(startTime, endTime)

                // Calculate agreement stats
                val total = agreements.size
                val completed = agreements.count { it.status == Agreement.STATUS_COMPLETED }
                val violated = agreements.count { it.status == Agreement.STATUS_VIOLATED }
                val successRate = if (total > 0) completed.toFloat() / total.toFloat() else 0f

                // Load daily screen time for the last 7 days
                val dailyScreenTime = getDailyScreenTime(startTime, endTime)

                // Load top apps for the week
                val topApps = usageMonitor.getTopApps(5)

                val stats = WeeklyStats(
                    totalAgreements = total,
                    completedAgreements = completed,
                    violatedAgreements = violated,
                    successRate = successRate,
                    dailyScreenTime = dailyScreenTime,
                    topApps = topApps
                )

                _weeklyStats.value = stats

                // Update individual StateFlows
                _agreementStats.value = AgreementStats(
                    total = total,
                    completed = completed,
                    violated = violated
                )
                _topApps.value = topApps
                _weeklyScreenTime.value = dailyScreenTime
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Error loading weekly stats", e)
                // Emit error state or default stats
                val defaultStats = WeeklyStats(
                    totalAgreements = 0,
                    completedAgreements = 0,
                    violatedAgreements = 0,
                    successRate = 0f,
                    dailyScreenTime = listOf(0, 0, 0, 0, 0, 0, 0),
                    topApps = emptyList()
                )

                _weeklyStats.value = defaultStats
                _agreementStats.value = AgreementStats(total = 0, completed = 0, violated = 0)
                _topApps.value = emptyList()
                _weeklyScreenTime.value = List(7) { 0L }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Gets daily screen time for each of the last 7 days.
     *
     * @param startTime Start timestamp (7 days ago at midnight)
     * @param endTime End timestamp (now)
     * @return List of 7 screen time values in milliseconds (oldest to newest)
     */
    private fun getDailyScreenTime(startTime: Long, endTime: Long): List<Long> {
        val dailyTimes = mutableListOf<Long>()
        val calendar = Calendar.getInstance()

        for (dayOffset in 0..6) {
            calendar.timeInMillis = startTime
            calendar.add(Calendar.DAY_OF_YEAR, dayOffset)

            val dayStart = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = calendar.timeInMillis

            // Get screen time for this specific day
            val screenTime = usageMonitor.getScreenTimeForDateRange(dayStart, dayEnd)
            dailyTimes.add(screenTime)
        }

        return dailyTimes
    }

    /**
     * Gets the success rate as a percentage (0-100).
     */
    fun getSuccessRatePercentage(): Int {
        return (_weeklyStats.value?.successRate?.times(100) ?: 0f).toInt()
    }

    /**
     * Gets the success rate color based on performance.
     *
     * @return Color code: 0 = red (<40%), 1 = yellow (40-70%), 2 = green (>70%)
     */
    fun getSuccessRateColor(): Int {
        val rate = _weeklyStats.value?.successRate ?: 0f
        return when {
            rate >= 0.7f -> 2 // Green
            rate >= 0.4f -> 1 // Yellow
            else -> 0 // Red
        }
    }

    /**
     * Formats screen time in milliseconds to a readable string.
     *
     * @param ms Time in milliseconds
     * @return Formatted string like "2h 30m" or "45m"
     */
    fun formatScreenTime(ms: Long): String {
        val minutes = ms / 1000 / 60
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
 * Weekly statistics data class.
 */
data class WeeklyStats(
    val totalAgreements: Int,
    val completedAgreements: Int,
    val violatedAgreements: Int,
    val successRate: Float, // 0.0 - 1.0
    val dailyScreenTime: List<Long>, // 7 days in milliseconds
    val topApps: List<AppUsageInfo>
)
