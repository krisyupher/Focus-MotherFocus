package com.focusmother.android.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data class holding agreement statistics.
 *
 * @property total Total number of agreements
 * @property completed Number of successfully completed agreements
 * @property violated Number of violated agreements
 * @property successRate Success rate percentage (0-100)
 */
data class AgreementStats(
    val total: Int,
    val completed: Int,
    val violated: Int
) {
    val successRate: Int
        get() = if (total == 0) 0 else ((completed.toFloat() / total) * 100).toInt()
}

/**
 * Card component displaying agreement statistics.
 *
 * Shows:
 * - Success rate as large percentage (color-coded)
 * - Breakdown of completed vs violated
 * - Total agreements count
 *
 * Color coding:
 * - Green (>70%): Excellent performance
 * - Yellow (40-70%): Moderate performance
 * - Red (<40%): Needs improvement
 */
@Composable
fun AgreementStatsCard(stats: AgreementStats) {
    val successRateColor = getSuccessRateColor(stats.successRate)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        border = BorderStroke(2.dp, successRateColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF7C0EDA)
                )
                Text(
                    "Agreement Success Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }

            // Success rate percentage
            Text(
                "${stats.successRate}%",
                style = MaterialTheme.typography.displayLarge,
                color = successRateColor,
                fontWeight = FontWeight.Bold
            )

            // Success message
            Text(
                getSuccessMessage(stats.successRate),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0B0B0)
            )

            HorizontalDivider(color = Color(0xFF3A3A5A))

            // Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Completed
                StatItem(
                    icon = Icons.Default.CheckCircle,
                    value = stats.completed,
                    label = "Completed",
                    color = Color(0xFF00FF00)
                )

                // Violated
                StatItem(
                    icon = Icons.Default.Cancel,
                    value = stats.violated,
                    label = "Violated",
                    color = Color(0xFFFF6B00)
                )

                // Total
                StatItem(
                    icon = Icons.Default.Timer,
                    value = stats.total,
                    label = "Total",
                    color = Color(0xFF7C0EDA)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0B0B0)
        )
    }
}

/**
 * Returns color based on success rate.
 *
 * - Green (>70%): Excellent
 * - Yellow (40-70%): Moderate
 * - Red (<40%): Poor
 */
private fun getSuccessRateColor(successRate: Int): Color {
    return when {
        successRate >= 70 -> Color(0xFF00FF00) // Green
        successRate >= 40 -> Color(0xFFFFD700) // Yellow
        else -> Color(0xFFFF6B00) // Orange/Red
    }
}

/**
 * Returns motivational message based on success rate.
 */
private fun getSuccessMessage(successRate: Int): String {
    return when {
        successRate >= 90 -> "Exceptional discipline, warrior!"
        successRate >= 70 -> "You are maintaining strong focus!"
        successRate >= 50 -> "Your discipline is improving!"
        successRate >= 30 -> "Keep working on your agreements"
        else -> "Let's work together to build better habits"
    }
}
