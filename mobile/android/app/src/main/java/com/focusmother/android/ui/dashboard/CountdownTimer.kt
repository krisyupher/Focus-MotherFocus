package com.focusmother.android.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Reusable countdown timer Compose component.
 *
 * Displays time remaining in "Xm Ys" format with dynamic color coding:
 * - Green: More than 5 minutes remaining
 * - Yellow: 2-5 minutes remaining
 * - Red: Less than 2 minutes remaining
 *
 * The timer updates every second automatically using LaunchedEffect.
 *
 * @param expiresAt Timestamp when the agreement expires (milliseconds)
 * @param modifier Optional modifier for styling
 * @param label Optional label to show above the timer
 */
@Composable
fun CountdownTimer(
    expiresAt: Long,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    // Track current time to trigger recomposition
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Update current time every second
    LaunchedEffect(expiresAt) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L) // Update every second
        }
    }

    // Calculate time remaining
    val remainingMs = expiresAt - currentTime
    val formattedTime = formatTime(remainingMs)
    val timerColor = getTimerColor(remainingMs)

    Column(modifier = modifier) {
        // Optional label
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Timer display
        Text(
            text = formattedTime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = timerColor
        )
    }
}

/**
 * Formats time remaining in milliseconds to "Xm Ys" format.
 *
 * Examples:
 * - 5 minutes 30 seconds -> "5m 30s"
 * - 0 minutes 45 seconds -> "0m 45s"
 * - Negative time -> "0m 0s" (expired)
 *
 * @param remainingMs Time remaining in milliseconds
 * @return Formatted time string
 */
fun formatTime(remainingMs: Long): String {
    val remaining = if (remainingMs < 0) 0L else remainingMs
    val minutes = (remaining / 1000 / 60).toInt()
    val seconds = ((remaining / 1000) % 60).toInt()
    return "${minutes}m ${seconds}s"
}

/**
 * Determines timer color based on time remaining.
 *
 * Color thresholds:
 * - Green: >= 5 minutes
 * - Yellow: >= 2 minutes and < 5 minutes
 * - Red: < 2 minutes
 *
 * @param remainingMs Time remaining in milliseconds
 * @return Color for the timer text
 */
@Composable
fun getTimerColor(remainingMs: Long): Color {
    return when {
        remainingMs >= 5 * 60 * 1000L -> Color(0xFF4CAF50) // Green
        remainingMs >= 2 * 60 * 1000L -> Color(0xFFFFC107) // Yellow/Amber
        else -> Color(0xFFF44336) // Red
    }
}
