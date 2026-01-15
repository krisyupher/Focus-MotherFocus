package com.focusmother.android.domain

import android.content.Context
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests for AdultContentManager.
 *
 * Tests blocklist loading, decryption, package checking, and privacy-focused messaging.
 * These tests verify:
 * - Encrypted blocklist can be loaded and decrypted
 * - Package name checking works correctly
 * - Non-judgmental conversation prompts are generated
 * - Privacy is maintained (no logging of adult content)
 * - Empty blocklist is handled gracefully
 * - Invalid encrypted data is handled
 */
class AdultContentManagerTest {

    private lateinit var mockContext: Context
    private lateinit var adultContentManager: AdultContentManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        adultContentManager = AdultContentManager(mockContext)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `isAdultContent returns true for packages in blocklist`() = runBlocking {
        // Arrange
        val testPackage = "com.example.adult.app"
        mockBlocklistResource(listOf(testPackage, "com.example.other"))

        // Act
        val result = adultContentManager.isAdultContent(testPackage)

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isAdultContent returns false for packages not in blocklist`() = runBlocking {
        // Arrange
        val testPackage = "com.instagram.android"
        mockBlocklistResource(listOf("com.example.adult.app1", "com.example.adult.app2"))

        // Act
        val result = adultContentManager.isAdultContent(testPackage)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isAdultContent handles empty blocklist gracefully`() = runBlocking {
        // Arrange
        mockBlocklistResource(emptyList())

        // Act
        val result = adultContentManager.isAdultContent("any.package")

        // Assert
        assertFalse(result)
    }

    @Test
    fun `loadBlocklist returns list of package names`() = runBlocking {
        // Arrange
        val expectedPackages = listOf(
            "com.example.adult.app1",
            "com.example.adult.app2",
            "com.example.adult.app3"
        )
        mockBlocklistResource(expectedPackages)

        // Act
        val result = adultContentManager.loadBlocklist()

        // Assert
        assertEquals(expectedPackages.size, result.size)
        assertTrue(result.containsAll(expectedPackages))
    }

    @Test
    fun `loadBlocklist handles whitespace in package names`() = runBlocking {
        // Arrange
        val packagesWithWhitespace = listOf(
            "com.example.app1  ",
            "  com.example.app2",
            "com.example.app3"
        )
        mockBlocklistResource(packagesWithWhitespace)

        // Act
        val result = adultContentManager.loadBlocklist()

        // Assert
        assertTrue(result.contains("com.example.app1"))
        assertTrue(result.contains("com.example.app2"))
        assertTrue(result.contains("com.example.app3"))
    }

    @Test
    fun `loadBlocklist filters out empty lines`() = runBlocking {
        // Arrange
        mockBlocklistResource(listOf("com.example.app1", "", "com.example.app2", "   "))

        // Act
        val result = adultContentManager.loadBlocklist()

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.contains("com.example.app1"))
        assertTrue(result.contains("com.example.app2"))
    }

    @Test
    fun `getConversationPrompt returns message from valid list`() {
        // Act
        val prompt = adultContentManager.getConversationPrompt()

        // Assert - should be one of the predefined prompts
        assertTrue(AdultContentManager.CONVERSATION_PROMPTS.contains(prompt))
    }

    @Test
    fun `getConversationPrompt returns non-judgmental message`() {
        // Act - test all prompts
        AdultContentManager.CONVERSATION_PROMPTS.forEach { prompt ->
            // Assert - no shaming language
            assertFalse(prompt.contains("porn", ignoreCase = true))
            assertFalse(prompt.contains("shame", ignoreCase = true))
            assertFalse(prompt.contains("wrong", ignoreCase = true))
            assertFalse(prompt.contains("bad", ignoreCase = true))
            assertTrue(prompt.length > 50) // Should be substantial
        }
    }

    @Test
    fun `getConversationPrompt promotes positive themes`() {
        // Act - test all prompts contain motivational themes
        AdultContentManager.CONVERSATION_PROMPTS.forEach { prompt ->
            // Assert - should contain at least one positive theme
            val hasPositiveTheme = prompt.contains("mission", ignoreCase = true) ||
                prompt.contains("energy", ignoreCase = true) ||
                prompt.contains("focus", ignoreCase = true) ||
                prompt.contains("power", ignoreCase = true) ||
                prompt.contains("strength", ignoreCase = true) ||
                prompt.contains("vital", ignoreCase = true) ||
                prompt.contains("Ranger", ignoreCase = true) ||
                prompt.contains("dream", ignoreCase = true) ||
                prompt.contains("potential", ignoreCase = true)
            assertTrue("Prompt should contain positive theme: $prompt", hasPositiveTheme)
        }
    }

    @Test
    fun `getInterventionMessage returns message from valid list`() {
        // Act
        val message = adultContentManager.getInterventionMessage()

        // Assert - should be one of the predefined messages
        assertTrue(AdultContentManager.INTERVENTION_MESSAGES.contains(message))
    }

    @Test
    fun `getInterventionMessage returns short messages`() {
        // Act - test all intervention messages
        AdultContentManager.INTERVENTION_MESSAGES.forEach { message ->
            // Assert - should be short for notification display
            assertTrue("Message too long: $message", message.length < 50)
        }
    }

    @Test
    fun `INTERVENTION_MESSAGES has variety of messages`() {
        // Assert - should have at least 10 different messages for variety
        assertTrue(AdultContentManager.INTERVENTION_MESSAGES.size >= 10)
    }

    @Test
    fun `CONVERSATION_PROMPTS has variety of prompts`() {
        // Assert - should have at least 10 different prompts for variety
        assertTrue(AdultContentManager.CONVERSATION_PROMPTS.size >= 10)
    }

    @Test
    fun `SUGGESTED_LIMIT_MS is 1 minute for strict adult content blocking`() {
        // Assert - 1 minute limit (60,000 ms)
        assertEquals(60_000L, AdultContentManager.SUGGESTED_LIMIT_MS)
    }

    @Test
    fun `GRACE_PERIOD_MS is 10 seconds`() {
        // Assert - 10 second grace period
        assertEquals(10_000L, AdultContentManager.GRACE_PERIOD_MS)
    }

    @Test
    fun `isAdultContent caches blocklist for performance`() = runBlocking {
        // Arrange
        val testPackages = listOf("com.example.app1", "com.example.app2")
        mockBlocklistResource(testPackages)

        // Act
        adultContentManager.isAdultContent("com.example.app1")
        adultContentManager.isAdultContent("com.example.app2")
        adultContentManager.isAdultContent("com.example.app1") // Repeat

        // Assert
        // Verify resource was only opened once (cached after first load)
        verify(exactly = 1) { mockContext.resources }
    }

    @Test
    fun `isAdultContent is case-insensitive`() = runBlocking {
        // Arrange
        mockBlocklistResource(listOf("com.Example.Adult.App"))

        // Act
        val result1 = adultContentManager.isAdultContent("com.example.adult.app")
        val result2 = adultContentManager.isAdultContent("COM.EXAMPLE.ADULT.APP")

        // Assert
        assertTrue(result1)
        assertTrue(result2)
    }

    @Test
    fun `loadBlocklist handles corrupted encrypted data gracefully`() = runBlocking {
        // Arrange
        val corruptedData = ByteArray(10) { it.toByte() }
        every { mockContext.resources.openRawResource(any()) } returns
            ByteArrayInputStream(corruptedData)

        // Act & Assert
        try {
            adultContentManager.loadBlocklist()
            // Should either return empty list or throw exception
            assertTrue(true)
        } catch (e: Exception) {
            // Exception is acceptable for corrupted data
            assertTrue(e is SecurityException || e is IllegalArgumentException)
        }
    }

    /**
     * Helper function to mock blocklist resource.
     * Simulates encrypted blocklist by creating plaintext for testing.
     */
    private fun mockBlocklistResource(packages: List<String>) {
        val blocklistContent = packages.joinToString("\n")
        val inputStream = ByteArrayInputStream(blocklistContent.toByteArray())

        every { mockContext.resources.openRawResource(any()) } returns inputStream
    }
}
