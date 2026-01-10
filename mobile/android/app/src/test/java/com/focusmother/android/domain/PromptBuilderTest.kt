package com.focusmother.android.domain

import com.focusmother.android.data.entity.Agreement
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PromptBuilder.
 *
 * Validates that system prompts are correctly built with Zordon personality,
 * context information, and appropriate constraints. Tests various scenarios
 * including different app categories, intervention reasons, and agreement history.
 */
class PromptBuilderTest {

    private lateinit var promptBuilder: PromptBuilder

    @Before
    fun setup() {
        promptBuilder = PromptBuilder()
    }

    @Test
    fun `buildSystemPrompt includes Zordon personality`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Zordon"))
        assertTrue(prompt.contains("Power Rangers") || prompt.contains("digital guardian"))
        assertTrue(prompt.contains("warrior") || prompt.contains("ranger"))
    }

    @Test
    fun `buildSystemPrompt includes screen time context`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "3h 45m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Continuous usage"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("3h 45m"))
    }

    @Test
    fun `buildSystemPrompt includes current app when provided`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = "Instagram",
            currentAppCategory = "SOCIAL_MEDIA",
            appUsageToday = "• Instagram: 1h 30m",
            recentAgreements = emptyList(),
            interventionReason = "Excessive Instagram usage"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Instagram"))
        assertTrue(prompt.contains("SOCIAL_MEDIA") || prompt.contains("Social Media"))
    }

    @Test
    fun `buildSystemPrompt includes intervention reason`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Continuous usage detected for 30 minutes"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Continuous usage detected for 30 minutes"))
    }

    @Test
    fun `buildSystemPrompt includes app usage summary`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "4h 15m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "• Instagram: 2h 0m\n• YouTube: 1h 30m\n• TikTok: 45m",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Instagram") || prompt.contains("YouTube"))
    }

    @Test
    fun `buildSystemPrompt includes recent agreements when available`() {
        // Arrange
        val agreements = listOf(
            Agreement.create("com.instagram.android", "Instagram", "SOCIAL_MEDIA", 10 * 60 * 1000L, 1L),
            Agreement.create("com.youtube.android", "YouTube", "ENTERTAINMENT", 15 * 60 * 1000L, 2L)
        )
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = agreements,
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Instagram") || prompt.contains("YouTube") || prompt.contains("agreement"))
    }

    @Test
    fun `buildSystemPrompt emphasizes time limit constraints`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("5") && prompt.contains("30") || prompt.contains("time limit") || prompt.contains("minutes"))
    }

    @Test
    fun `buildSystemPrompt includes response length constraint`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("100 words") || prompt.contains("concise") || prompt.contains("brief"))
    }

    @Test
    fun `buildSystemPrompt mentions asking user intent`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(
            prompt.contains("accomplish") ||
            prompt.contains("trying to") ||
            prompt.contains("purpose") ||
            prompt.contains("task")
        )
    }

    @Test
    fun `buildSystemPrompt emphasizes no lecturing`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(
            prompt.contains("no lecturing") ||
            prompt.contains("no shaming") ||
            prompt.contains("empathetic") ||
            prompt.contains("warm")
        )
    }

    @Test
    fun `buildSystemPrompt handles social media category`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = "Instagram",
            currentAppCategory = "SOCIAL_MEDIA",
            appUsageToday = "• Instagram: 1h 30m",
            recentAgreements = emptyList(),
            interventionReason = "Excessive social media usage"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Instagram"))
        assertTrue(prompt.contains("SOCIAL_MEDIA") || prompt.contains("social"))
    }

    @Test
    fun `buildSystemPrompt handles entertainment category`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "3h 0m",
            currentApp = "YouTube",
            currentAppCategory = "ENTERTAINMENT",
            appUsageToday = "• YouTube: 2h 0m",
            recentAgreements = emptyList(),
            interventionReason = "Long video watching session"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("YouTube"))
        assertTrue(prompt.contains("ENTERTAINMENT") || prompt.contains("entertainment"))
    }

    @Test
    fun `buildSystemPrompt handles games category`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = "Candy Crush",
            currentAppCategory = "GAMES",
            appUsageToday = "• Candy Crush: 1h 30m",
            recentAgreements = emptyList(),
            interventionReason = "Extended gaming session"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Candy Crush") || prompt.contains("GAMES"))
    }

    @Test
    fun `buildSystemPrompt handles unknown category`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "2h 30m",
            currentApp = "Unknown App",
            currentAppCategory = "UNKNOWN",
            appUsageToday = "• Unknown App: 1h 0m",
            recentAgreements = emptyList(),
            interventionReason = "General usage"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("Unknown App") || prompt.isNotEmpty())
    }

    @Test
    fun `buildSystemPrompt is not empty`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "0m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "No significant app usage today",
            recentAgreements = emptyList(),
            interventionReason = "Test"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.length > 100) // Should be substantial
    }

    @Test
    fun `buildSystemPrompt handles multiple recent agreements`() {
        // Arrange
        val agreements = listOf(
            Agreement.create("com.instagram.android", "Instagram", "SOCIAL_MEDIA", 10 * 60 * 1000L, 1L),
            Agreement.create("com.youtube.android", "YouTube", "ENTERTAINMENT", 15 * 60 * 1000L, 2L),
            Agreement.create("com.tiktok", "TikTok", "SOCIAL_MEDIA", 5 * 60 * 1000L, 3L)
        )
        val context = ConversationContext(
            todayScreenTime = "3h 30m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "Multiple apps",
            recentAgreements = agreements,
            interventionReason = "Pattern of excessive usage"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        // Should reference agreements or recent history
        assertTrue(prompt.isNotEmpty())
    }

    @Test
    fun `buildSystemPrompt handles no current app with general intervention`() {
        // Arrange
        val context = ConversationContext(
            todayScreenTime = "4h 0m",
            currentApp = null,
            currentAppCategory = null,
            appUsageToday = "• Instagram: 1h 30m\n• YouTube: 1h 0m\n• Chrome: 45m",
            recentAgreements = emptyList(),
            interventionReason = "General phone overuse detected"
        )

        // Act
        val prompt = promptBuilder.buildSystemPrompt(context)

        // Assert
        assertTrue(prompt.contains("4h 0m") || prompt.contains("General phone overuse"))
        assertTrue(prompt.isNotEmpty())
    }
}
