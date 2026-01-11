package com.focusmother.android.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for CountdownTimer utility functions.
 *
 * Tests time formatting and color determination logic.
 */
class CountdownTimerTest {

    // ============================================================================
    // Time Formatting Tests
    // ============================================================================

    @Test
    fun `formatTimeRemaining should format minutes and seconds correctly`() {
        // 5 minutes 30 seconds
        val result = formatTimeRemaining(5 * 60 * 1000L + 30 * 1000L)
        assertEquals("5m 30s", result)
    }

    @Test
    fun `formatTimeRemaining should format exact minutes`() {
        // Exactly 10 minutes
        val result = formatTimeRemaining(10 * 60 * 1000L)
        assertEquals("10m 0s", result)
    }

    @Test
    fun `formatTimeRemaining should format less than one minute`() {
        // 45 seconds
        val result = formatTimeRemaining(45 * 1000L)
        assertEquals("0m 45s", result)
    }

    @Test
    fun `formatTimeRemaining should format zero time`() {
        val result = formatTimeRemaining(0L)
        assertEquals("0m 0s", result)
    }

    @Test
    fun `formatTimeRemaining should format large durations`() {
        // 59 minutes 59 seconds
        val result = formatTimeRemaining(59 * 60 * 1000L + 59 * 1000L)
        assertEquals("59m 59s", result)
    }

    @Test
    fun `formatTimeRemaining should handle hours by showing total minutes`() {
        // 2 hours = 120 minutes
        val result = formatTimeRemaining(2 * 60 * 60 * 1000L)
        assertEquals("120m 0s", result)
    }

    @Test
    fun `formatTimeRemaining should handle milliseconds by rounding down`() {
        // 5 minutes 30.999 seconds -> should show 5m 30s
        val result = formatTimeRemaining(5 * 60 * 1000L + 30 * 1000L + 999L)
        assertEquals("5m 30s", result)
    }

    @Test
    fun `formatTimeRemaining should handle negative time as zero`() {
        val result = formatTimeRemaining(-5000L)
        assertEquals("0m 0s", result)
    }

    // ============================================================================
    // Color Determination Tests
    // ============================================================================

    @Test
    fun `getTimerColor should return green for more than 5 minutes`() {
        // 6 minutes remaining
        val color = getTimerColorType(6 * 60 * 1000L)
        assertEquals(TimerColor.GREEN, color)
    }

    @Test
    fun `getTimerColor should return green for exactly 5 minutes`() {
        val color = getTimerColorType(5 * 60 * 1000L)
        assertEquals(TimerColor.GREEN, color)
    }

    @Test
    fun `getTimerColor should return yellow for 4 minutes`() {
        val color = getTimerColorType(4 * 60 * 1000L)
        assertEquals(TimerColor.YELLOW, color)
    }

    @Test
    fun `getTimerColor should return yellow for exactly 2 minutes`() {
        val color = getTimerColorType(2 * 60 * 1000L)
        assertEquals(TimerColor.YELLOW, color)
    }

    @Test
    fun `getTimerColor should return red for less than 2 minutes`() {
        val color = getTimerColorType(1 * 60 * 1000L + 59 * 1000L)
        assertEquals(TimerColor.RED, color)
    }

    @Test
    fun `getTimerColor should return red for exactly 1 minute`() {
        val color = getTimerColorType(1 * 60 * 1000L)
        assertEquals(TimerColor.RED, color)
    }

    @Test
    fun `getTimerColor should return red for zero time`() {
        val color = getTimerColorType(0L)
        assertEquals(TimerColor.RED, color)
    }

    @Test
    fun `getTimerColor should return red for negative time`() {
        val color = getTimerColorType(-5000L)
        assertEquals(TimerColor.RED, color)
    }

    @Test
    fun `getTimerColor should return green for very long durations`() {
        // 2 hours
        val color = getTimerColorType(2 * 60 * 60 * 1000L)
        assertEquals(TimerColor.GREEN, color)
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun `formatTimeRemaining should handle one second`() {
        val result = formatTimeRemaining(1000L)
        assertEquals("0m 1s", result)
    }

    @Test
    fun `formatTimeRemaining should handle 999 milliseconds`() {
        val result = formatTimeRemaining(999L)
        assertEquals("0m 0s", result)
    }

    @Test
    fun `color boundaries should be precise at 5 minute mark`() {
        // Just above 5 minutes
        assertEquals(TimerColor.GREEN, getTimerColorType(5 * 60 * 1000L + 1L))
        // Just below 5 minutes
        assertEquals(TimerColor.YELLOW, getTimerColorType(5 * 60 * 1000L - 1L))
    }

    @Test
    fun `color boundaries should be precise at 2 minute mark`() {
        // Just above 2 minutes
        assertEquals(TimerColor.YELLOW, getTimerColorType(2 * 60 * 1000L + 1L))
        // Just below 2 minutes
        assertEquals(TimerColor.RED, getTimerColorType(2 * 60 * 1000L - 1L))
    }
}

/**
 * Timer color types for testing.
 */
enum class TimerColor {
    GREEN, YELLOW, RED
}

/**
 * Formats time remaining in milliseconds to "Xm Ys" format.
 */
fun formatTimeRemaining(remainingMs: Long): String {
    val remaining = if (remainingMs < 0) 0L else remainingMs
    val minutes = (remaining / 1000 / 60).toInt()
    val seconds = ((remaining / 1000) % 60).toInt()
    return "${minutes}m ${seconds}s"
}

/**
 * Determines timer color based on time remaining.
 */
fun getTimerColorType(remainingMs: Long): TimerColor {
    return when {
        remainingMs >= 5 * 60 * 1000L -> TimerColor.GREEN
        remainingMs >= 2 * 60 * 1000L -> TimerColor.YELLOW
        else -> TimerColor.RED
    }
}
