package com.focusmother.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for ResponseParser.
 *
 * Tests the extraction of time durations from natural language user messages.
 * Covers various patterns including: minutes, hours, seconds, colloquial phrases,
 * edge cases, and invalid inputs.
 */
class ResponseParserTest {

    private val parser = ResponseParser()

    // ============================================================================
    // Minute Patterns
    // ============================================================================

    @Test
    fun `parseDuration should extract minutes from 'X minutes' pattern`() {
        val result = parser.parseDuration("Can I have 5 minutes please?")
        assertEquals(5 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract minutes from 'X min' pattern`() {
        val result = parser.parseDuration("Just 10 min")
        assertEquals(10 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract minutes from 'X mins' pattern`() {
        val result = parser.parseDuration("Give me 15 mins")
        assertEquals(15 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract minutes from 'X more minutes' pattern`() {
        val result = parser.parseDuration("5 more minutes")
        assertEquals(5 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle single minute`() {
        val result = parser.parseDuration("1 minute")
        assertEquals(1 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle large minute values`() {
        val result = parser.parseDuration("120 minutes")
        assertEquals(120 * 60 * 1000L, result)
    }

    // ============================================================================
    // Hour Patterns
    // ============================================================================

    @Test
    fun `parseDuration should extract hours from 'X hours' pattern`() {
        val result = parser.parseDuration("I need 2 hours")
        assertEquals(2 * 60 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract hours from 'X hour' pattern`() {
        val result = parser.parseDuration("Just 1 hour")
        assertEquals(1 * 60 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract hours from 'X hr' pattern`() {
        val result = parser.parseDuration("Give me 3 hr")
        assertEquals(3 * 60 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract hours from 'X hrs' pattern`() {
        val result = parser.parseDuration("Need 4 hrs")
        assertEquals(4 * 60 * 60 * 1000L, result)
    }

    // ============================================================================
    // Second Patterns
    // ============================================================================

    @Test
    fun `parseDuration should extract seconds from 'X seconds' pattern`() {
        val result = parser.parseDuration("Just 30 seconds")
        assertEquals(30 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract seconds from 'X sec' pattern`() {
        val result = parser.parseDuration("Give me 45 sec")
        assertEquals(45 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract seconds from 'X secs' pattern`() {
        val result = parser.parseDuration("Wait 60 secs")
        assertEquals(60 * 1000L, result)
    }

    // ============================================================================
    // Colloquial Phrases
    // ============================================================================

    @Test
    fun `parseDuration should recognize 'half hour' as 30 minutes`() {
        val result = parser.parseDuration("Can I get half an hour?")
        assertEquals(30 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'half hour' without 'an'`() {
        val result = parser.parseDuration("half hour please")
        assertEquals(30 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'a bit' as 2 minutes default`() {
        val result = parser.parseDuration("just a bit longer")
        assertEquals(2 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'a little' as 2 minutes default`() {
        val result = parser.parseDuration("a little more time")
        assertEquals(2 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'quick' as 1 minute default`() {
        val result = parser.parseDuration("just quick")
        assertEquals(1 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'couple minutes' as 2 minutes`() {
        val result = parser.parseDuration("a couple minutes")
        assertEquals(2 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should recognize 'few minutes' as 3 minutes`() {
        val result = parser.parseDuration("a few minutes")
        assertEquals(3 * 60 * 1000L, result)
    }

    // ============================================================================
    // Edge Cases
    // ============================================================================

    @Test
    fun `parseDuration should handle zero minutes`() {
        val result = parser.parseDuration("0 minutes")
        // Should return null for zero duration as it's not a valid agreement
        assertNull(result)
    }

    @Test
    fun `parseDuration should handle decimal minutes by rounding down`() {
        val result = parser.parseDuration("5.5 minutes")
        // Should extract 5 minutes (rounding down or taking integer part)
        assertEquals(5 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle case insensitivity`() {
        val result = parser.parseDuration("10 MINUTES")
        assertEquals(10 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle mixed case`() {
        val result = parser.parseDuration("15 MiNuTeS")
        assertEquals(15 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should extract first time mention when multiple present`() {
        val result = parser.parseDuration("Can I have 5 minutes or maybe 10 minutes?")
        // Should extract the first mention
        assertEquals(5 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle whitespace variations`() {
        val result = parser.parseDuration("   10   minutes   ")
        assertEquals(10 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle no spaces between number and unit`() {
        val result = parser.parseDuration("5min")
        assertEquals(5 * 60 * 1000L, result)
    }

    // ============================================================================
    // Invalid Inputs
    // ============================================================================

    @Test
    fun `parseDuration should return null when no time found`() {
        val result = parser.parseDuration("I don't know")
        assertNull(result)
    }

    @Test
    fun `parseDuration should return null for empty string`() {
        val result = parser.parseDuration("")
        assertNull(result)
    }

    @Test
    fun `parseDuration should return null for whitespace only`() {
        val result = parser.parseDuration("   ")
        assertNull(result)
    }

    @Test
    fun `parseDuration should return null when only text without time`() {
        val result = parser.parseDuration("I need more time")
        assertNull(result)
    }

    @Test
    fun `parseDuration should return null for negative numbers`() {
        val result = parser.parseDuration("-5 minutes")
        // Should not extract negative durations
        assertNull(result)
    }

    @Test
    fun `parseDuration should return null for rejection phrases`() {
        val result = parser.parseDuration("No, I'll stop now")
        assertNull(result)
    }

    // ============================================================================
    // Complex Sentences
    // ============================================================================

    @Test
    fun `parseDuration should extract time from complex sentence`() {
        val result = parser.parseDuration(
            "Well, I understand your concern, but could I possibly have 15 more minutes? I'm almost done."
        )
        assertEquals(15 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle time at start of sentence`() {
        val result = parser.parseDuration("10 minutes would be great")
        assertEquals(10 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle time at end of sentence`() {
        val result = parser.parseDuration("Can I please have 20 minutes")
        assertEquals(20 * 60 * 1000L, result)
    }

    // ============================================================================
    // Boundary Values
    // ============================================================================

    @Test
    fun `parseDuration should handle very small duration - 1 second`() {
        val result = parser.parseDuration("1 second")
        assertEquals(1000L, result)
    }

    @Test
    fun `parseDuration should handle very large duration - 24 hours`() {
        val result = parser.parseDuration("24 hours")
        assertEquals(24 * 60 * 60 * 1000L, result)
    }

    // ============================================================================
    // Real User Messages
    // ============================================================================

    @Test
    fun `parseDuration should handle realistic user plea`() {
        val result = parser.parseDuration("Please mom, just 5 more minutes!")
        assertEquals(5 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle negotiation message`() {
        val result = parser.parseDuration("What about 10 minutes instead?")
        assertEquals(10 * 60 * 1000L, result)
    }

    @Test
    fun `parseDuration should handle bargaining message`() {
        val result = parser.parseDuration("Okay okay, 3 minutes then")
        assertEquals(3 * 60 * 1000L, result)
    }
}
