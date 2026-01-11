package com.focusmother.android.domain

import com.focusmother.android.data.entity.AppCategoryMapping
import com.focusmother.android.data.repository.AgreementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NegotiationManager.
 *
 * Tests the state machine for negotiation flow and agreement creation.
 * Covers all negotiation states and transitions.
 */
class NegotiationManagerTest {

    private lateinit var responseParser: ResponseParser
    private lateinit var agreementRepository: AgreementRepository
    private lateinit var negotiationManager: NegotiationManager

    @Before
    fun setup() {
        responseParser = mockk()
        agreementRepository = mockk()
        negotiationManager = NegotiationManager(responseParser, agreementRepository)
    }

    // ============================================================================
    // State Initialization Tests
    // ============================================================================

    @Test
    fun `initial state should be Initial`() {
        // Assert
        assertEquals(NegotiationState.Initial, negotiationManager.currentState)
        assertEquals(0, negotiationManager.negotiationRound)
    }

    // ============================================================================
    // processMessage - Initial State Tests
    // ============================================================================

    @Test
    fun `processMessage should transition to ProposedTime when time is extracted from initial state`() = runTest {
        // Arrange
        val message = "Can I have 5 more minutes?"
        val extractedDuration = 5 * 60 * 1000L

        every { responseParser.parseDuration(message) } returns extractedDuration

        // Act
        val result = negotiationManager.processMessage(message)

        // Assert
        assertEquals(NegotiationState.ProposedTime(extractedDuration), result)
        assertEquals(NegotiationState.ProposedTime(extractedDuration), negotiationManager.currentState)
        assertEquals(1, negotiationManager.negotiationRound)
    }

    @Test
    fun `processMessage should stay in Initial state when no time extracted`() = runTest {
        // Arrange
        val message = "I don't know"

        every { responseParser.parseDuration(message) } returns null

        // Act
        val result = negotiationManager.processMessage(message)

        // Assert
        assertEquals(NegotiationState.Initial, result)
        assertEquals(NegotiationState.Initial, negotiationManager.currentState)
        assertEquals(1, negotiationManager.negotiationRound)
    }

    @Test
    fun `processMessage should transition to Rejected when user explicitly rejects`() = runTest {
        // Arrange
        val rejectMessages = listOf(
            "No, I'll stop now",
            "okay fine",
            "I'll quit",
            "alright I'm done"
        )

        rejectMessages.forEach { message ->
            // Arrange
            negotiationManager.reset()
            every { responseParser.parseDuration(message) } returns null

            // Act
            val result = negotiationManager.processMessage(message)

            // Assert
            assertEquals(NegotiationState.Rejected, result)
            assertEquals(NegotiationState.Rejected, negotiationManager.currentState)
        }
    }

    // ============================================================================
    // processMessage - ProposedTime State Tests
    // ============================================================================

    @Test
    fun `processMessage should transition from ProposedTime to Negotiating when user counters`() = runTest {
        // Arrange
        negotiationManager.processMessage("5 minutes")
        every { responseParser.parseDuration("How about 10 minutes instead?") } returns 10 * 60 * 1000L

        // Act
        val result = negotiationManager.processMessage("How about 10 minutes instead?")

        // Assert
        assertEquals(NegotiationState.Negotiating(10 * 60 * 1000L), result)
        assertEquals(2, negotiationManager.negotiationRound)
    }

    @Test
    fun `processMessage should accept initial proposal with affirmative response`() = runTest {
        // Arrange
        val proposedDuration = 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")
        every { responseParser.parseDuration("okay") } returns null

        // Simulate ProposedTime state
        negotiationManager = NegotiationManager(responseParser, agreementRepository)
        every { responseParser.parseDuration("5 minutes") } returns proposedDuration
        negotiationManager.processMessage("5 minutes")

        // Act
        every { responseParser.parseDuration("okay") } returns null
        val result = negotiationManager.processMessage("okay")

        // Assert
        assertEquals(NegotiationState.AgreementReached(proposedDuration), result)
    }

    // ============================================================================
    // processMessage - Negotiating State Tests
    // ============================================================================

    @Test
    fun `processMessage should continue negotiating when new time is proposed`() = runTest {
        // Arrange
        negotiationManager = NegotiationManager(responseParser, agreementRepository)
        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        every { responseParser.parseDuration("10 minutes") } returns 10 * 60 * 1000L
        negotiationManager.processMessage("10 minutes")

        // Now in Negotiating state, propose another time
        every { responseParser.parseDuration("8 minutes") } returns 8 * 60 * 1000L

        // Act
        val result = negotiationManager.processMessage("8 minutes")

        // Assert
        assertEquals(NegotiationState.Negotiating(8 * 60 * 1000L), result)
        assertEquals(3, negotiationManager.negotiationRound)
    }

    @Test
    fun `processMessage should reach agreement when user accepts in negotiating state`() = runTest {
        // Arrange
        negotiationManager = NegotiationManager(responseParser, agreementRepository)
        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        val negotiatedDuration = 10 * 60 * 1000L
        every { responseParser.parseDuration("10 minutes") } returns negotiatedDuration
        negotiationManager.processMessage("10 minutes")

        // Act
        every { responseParser.parseDuration("okay deal") } returns null
        val result = negotiationManager.processMessage("okay deal")

        // Assert
        assertEquals(NegotiationState.AgreementReached(negotiatedDuration), result)
    }

    @Test
    fun `processMessage should force agreement after max negotiation rounds`() = runTest {
        // Arrange
        negotiationManager = NegotiationManager(responseParser, agreementRepository)

        // Round 1
        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        // Round 2
        every { responseParser.parseDuration("10 minutes") } returns 10 * 60 * 1000L
        negotiationManager.processMessage("10 minutes")

        // Round 3 (max)
        val finalDuration = 8 * 60 * 1000L
        every { responseParser.parseDuration("8 minutes") } returns finalDuration
        val result = negotiationManager.processMessage("8 minutes")

        // Assert
        assertEquals(NegotiationState.AgreementReached(finalDuration), result)
        assertEquals(3, negotiationManager.negotiationRound)
    }

    // ============================================================================
    // Affirmative Response Detection Tests
    // ============================================================================

    @Test
    fun `isAffirmative should detect positive responses`() {
        // Arrange
        val affirmativeMessages = listOf(
            "okay",
            "yes",
            "sure",
            "fine",
            "deal",
            "alright",
            "agreed",
            "accept",
            "OK"
        )

        // Act & Assert
        affirmativeMessages.forEach { message ->
            assertTrue("'$message' should be affirmative",
                negotiationManager.isAffirmative(message))
        }
    }

    @Test
    fun `isAffirmative should reject negative or unclear responses`() {
        // Arrange
        val nonAffirmativeMessages = listOf(
            "no",
            "maybe",
            "I don't know",
            "10 minutes",
            ""
        )

        // Act & Assert
        nonAffirmativeMessages.forEach { message ->
            assertFalse("'$message' should not be affirmative",
                negotiationManager.isAffirmative(message))
        }
    }

    // ============================================================================
    // Rejection Detection Tests
    // ============================================================================

    @Test
    fun `isRejection should detect rejection phrases`() {
        // Arrange
        val rejectionMessages = listOf(
            "no I'll stop",
            "fine I quit",
            "okay I'm done",
            "alright I'll close it"
        )

        // Act & Assert
        rejectionMessages.forEach { message ->
            assertTrue("'$message' should be rejection",
                negotiationManager.isRejection(message))
        }
    }

    @Test
    fun `isRejection should not trigger on non-rejection messages`() {
        // Arrange
        val nonRejectionMessages = listOf(
            "5 minutes please",
            "okay deal",
            "yes",
            "maybe later"
        )

        // Act & Assert
        nonRejectionMessages.forEach { message ->
            assertFalse("'$message' should not be rejection",
                negotiationManager.isRejection(message))
        }
    }

    // ============================================================================
    // createAgreement Tests
    // ============================================================================

    @Test
    fun `createAgreement should create agreement with correct parameters`() = runTest {
        // Arrange
        val durationMinutes = 10
        val appPackage = "com.instagram.android"
        val appName = "Instagram"
        val category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA
        val conversationId = 1L
        val expectedAgreementId = 42L

        coEvery {
            agreementRepository.createAgreement(
                appPackage = appPackage,
                appName = appName,
                category = category,
                durationMs = durationMinutes * 60 * 1000L,
                conversationId = conversationId
            )
        } returns expectedAgreementId

        // Act
        val result = negotiationManager.createAgreement(
            durationMinutes = durationMinutes,
            appPackage = appPackage,
            appName = appName,
            category = category,
            conversationId = conversationId
        )

        // Assert
        assertEquals(expectedAgreementId, result)
        coVerify(exactly = 1) {
            agreementRepository.createAgreement(
                appPackage = appPackage,
                appName = appName,
                category = category,
                durationMs = durationMinutes * 60 * 1000L,
                conversationId = conversationId
            )
        }
    }

    @Test
    fun `createAgreement should handle general phone usage agreement`() = runTest {
        // Arrange
        val durationMinutes = 30
        val conversationId = 2L
        val expectedAgreementId = 99L

        coEvery {
            agreementRepository.createAgreement(
                appPackage = null,
                appName = "Phone Usage",
                category = "GENERAL",
                durationMs = durationMinutes * 60 * 1000L,
                conversationId = conversationId
            )
        } returns expectedAgreementId

        // Act
        val result = negotiationManager.createAgreement(
            durationMinutes = durationMinutes,
            appPackage = null,
            appName = "Phone Usage",
            category = "GENERAL",
            conversationId = conversationId
        )

        // Assert
        assertEquals(expectedAgreementId, result)
    }

    // ============================================================================
    // Reset Tests
    // ============================================================================

    @Test
    fun `reset should return to initial state`() = runTest {
        // Arrange
        every { responseParser.parseDuration(any()) } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")
        negotiationManager.processMessage("10 minutes")

        // Act
        negotiationManager.reset()

        // Assert
        assertEquals(NegotiationState.Initial, negotiationManager.currentState)
        assertEquals(0, negotiationManager.negotiationRound)
    }

    // ============================================================================
    // Edge Cases Tests
    // ============================================================================

    @Test
    fun `processMessage should handle empty messages`() = runTest {
        // Arrange
        every { responseParser.parseDuration("") } returns null

        // Act
        val result = negotiationManager.processMessage("")

        // Assert
        assertEquals(NegotiationState.Initial, result)
    }

    @Test
    fun `processMessage should handle very long messages`() = runTest {
        // Arrange
        val longMessage = "I really need more time because I'm in the middle of something important " +
                "and I promise I'll stop after 15 minutes I really mean it this time please mom"
        every { responseParser.parseDuration(longMessage) } returns 15 * 60 * 1000L

        // Act
        val result = negotiationManager.processMessage(longMessage)

        // Assert
        assertEquals(NegotiationState.ProposedTime(15 * 60 * 1000L), result)
    }

    @Test
    fun `getCurrentProposedDuration should return null in initial state`() {
        // Assert
        assertNull(negotiationManager.getCurrentProposedDuration())
    }

    @Test
    fun `getCurrentProposedDuration should return duration in ProposedTime state`() = runTest {
        // Arrange
        val duration = 5 * 60 * 1000L
        every { responseParser.parseDuration("5 minutes") } returns duration
        negotiationManager.processMessage("5 minutes")

        // Assert
        assertEquals(duration, negotiationManager.getCurrentProposedDuration())
    }

    @Test
    fun `getCurrentProposedDuration should return latest duration in Negotiating state`() = runTest {
        // Arrange
        negotiationManager = NegotiationManager(responseParser, agreementRepository)
        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        val latestDuration = 10 * 60 * 1000L
        every { responseParser.parseDuration("10 minutes") } returns latestDuration
        negotiationManager.processMessage("10 minutes")

        // Assert
        assertEquals(latestDuration, negotiationManager.getCurrentProposedDuration())
    }

    @Test
    fun `hasReachedMaxRounds should return true after 3 rounds`() = runTest {
        // Arrange
        negotiationManager = NegotiationManager(responseParser, agreementRepository)

        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        every { responseParser.parseDuration("10 minutes") } returns 10 * 60 * 1000L
        negotiationManager.processMessage("10 minutes")

        every { responseParser.parseDuration("8 minutes") } returns 8 * 60 * 1000L
        negotiationManager.processMessage("8 minutes")

        // Assert
        assertTrue(negotiationManager.hasReachedMaxRounds())
    }

    @Test
    fun `hasReachedMaxRounds should return false before 3 rounds`() = runTest {
        // Arrange
        every { responseParser.parseDuration("5 minutes") } returns 5 * 60 * 1000L
        negotiationManager.processMessage("5 minutes")

        // Assert
        assertFalse(negotiationManager.hasReachedMaxRounds())
    }
}
