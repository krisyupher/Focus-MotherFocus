package com.focusmother.android.domain

import java.util.regex.Pattern

/**
 * Parses natural language user messages to extract time durations.
 *
 * Handles various patterns including:
 * - Explicit time mentions: "5 minutes", "2 hours", "30 seconds"
 * - Abbreviated forms: "10 min", "3 hrs", "45 sec"
 * - Colloquial phrases: "half hour", "a bit", "quick", "couple minutes"
 * - Complex sentences with time embedded
 *
 * Returns duration in milliseconds or null if no valid time found.
 */
class ResponseParser {

    companion object {
        // Regex patterns for time extraction
        private val MINUTE_PATTERN = Pattern.compile(
            """(\d+(?:\.\d+)?)\s*(?:more\s+)?(?:minutes?|mins?)""",
            Pattern.CASE_INSENSITIVE
        )

        private val HOUR_PATTERN = Pattern.compile(
            """(\d+(?:\.\d+)?)\s*(?:more\s+)?(?:hours?|hrs?)""",
            Pattern.CASE_INSENSITIVE
        )

        private val SECOND_PATTERN = Pattern.compile(
            """(\d+(?:\.\d+)?)\s*(?:more\s+)?(?:seconds?|secs?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Colloquial phrases
        private val HALF_HOUR_PATTERN = Pattern.compile(
            """half\s+(?:an?\s+)?hour""",
            Pattern.CASE_INSENSITIVE
        )

        private val BIT_PATTERN = Pattern.compile(
            """(?:a\s+)?bit(?:\s+longer)?""",
            Pattern.CASE_INSENSITIVE
        )

        private val LITTLE_PATTERN = Pattern.compile(
            """(?:a\s+)?little(?:\s+more)?(?:\s+time)?""",
            Pattern.CASE_INSENSITIVE
        )

        private val QUICK_PATTERN = Pattern.compile(
            """quick(?:ly)?""",
            Pattern.CASE_INSENSITIVE
        )

        private val COUPLE_PATTERN = Pattern.compile(
            """(?:a\s+)?couple(?:\s+of)?\s+(?:minutes?|mins?)""",
            Pattern.CASE_INSENSITIVE
        )

        private val FEW_PATTERN = Pattern.compile(
            """(?:a\s+)?few\s+(?:minutes?|mins?)""",
            Pattern.CASE_INSENSITIVE
        )

        // Time unit conversions to milliseconds
        private const val SECOND_MS = 1000L
        private const val MINUTE_MS = 60 * SECOND_MS
        private const val HOUR_MS = 60 * MINUTE_MS
    }

    /**
     * Parses a user message to extract time duration.
     *
     * Attempts to match various time patterns in order of specificity:
     * 1. Explicit hours, minutes, seconds
     * 2. Colloquial phrases (half hour, couple minutes, etc.)
     * 3. Vague terms (a bit, quick)
     *
     * @param message The user's natural language message
     * @return Duration in milliseconds, or null if no valid time found
     */
    fun parseDuration(message: String): Long? {
        if (message.isBlank()) {
            return null
        }

        val trimmedMessage = message.trim()

        // Try to extract explicit time mentions first (most specific)

        // Check for hours
        val hourMatcher = HOUR_PATTERN.matcher(trimmedMessage)
        if (hourMatcher.find()) {
            val hours = hourMatcher.group(1)?.toDoubleOrNull()
            if (hours != null && hours > 0) {
                return (hours * HOUR_MS).toLong()
            }
        }

        // Check for minutes
        val minuteMatcher = MINUTE_PATTERN.matcher(trimmedMessage)
        if (minuteMatcher.find()) {
            val minutes = minuteMatcher.group(1)?.toDoubleOrNull()
            if (minutes != null && minutes > 0) {
                return (minutes * MINUTE_MS).toLong()
            }
        }

        // Check for seconds
        val secondMatcher = SECOND_PATTERN.matcher(trimmedMessage)
        if (secondMatcher.find()) {
            val seconds = secondMatcher.group(1)?.toDoubleOrNull()
            if (seconds != null && seconds > 0) {
                return (seconds * SECOND_MS).toLong()
            }
        }

        // Check for colloquial phrases (less specific)

        // "half hour" or "half an hour" -> 30 minutes
        if (HALF_HOUR_PATTERN.matcher(trimmedMessage).find()) {
            return 30 * MINUTE_MS
        }

        // "couple minutes" -> 2 minutes
        if (COUPLE_PATTERN.matcher(trimmedMessage).find()) {
            return 2 * MINUTE_MS
        }

        // "few minutes" -> 3 minutes
        if (FEW_PATTERN.matcher(trimmedMessage).find()) {
            return 3 * MINUTE_MS
        }

        // "a bit" or "a little" -> 2 minutes default
        if (BIT_PATTERN.matcher(trimmedMessage).find() ||
            LITTLE_PATTERN.matcher(trimmedMessage).find()) {
            return 2 * MINUTE_MS
        }

        // "quick" -> 1 minute default
        if (QUICK_PATTERN.matcher(trimmedMessage).find()) {
            return 1 * MINUTE_MS
        }

        // No valid time found
        return null
    }
}
