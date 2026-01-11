package com.focusmother.android.domain

import com.focusmother.android.data.entity.Agreement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AgreementEnforcer.
 *
 * Tests violation detection, expiration handling, and enforcement logic.
 */
class AgreementEnforcerTest {

    private lateinit var enforcer: AgreementEnforcer

    @Before
    fun setup() {
        enforcer = AgreementEnforcer()
    }

    // ============================================================================
    // No Violations Tests
    // ============================================================================

    @Test
    fun `checkViolations should return NONE when no active agreements`() {
        // Arrange
        val agreements = emptyList<Agreement>()
        val currentApp = "com.instagram.android"
        val currentTime = System.currentTimeMillis()

        // Act
        val result = enforcer.checkViolations(agreements, currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
        assertNull(result.violatedAgreement)
        assertNull(result.expiredAgreement)
    }

    @Test
    fun `checkViolations should return NONE when current app is null`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentTime = System.currentTimeMillis()

        // Act
        val result = enforcer.checkViolations(listOf(agreement), null, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    @Test
    fun `checkViolations should return NONE when using different app than agreement`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.example.other"
        val currentTime = System.currentTimeMillis()

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    @Test
    fun `checkViolations should return NONE when agreement not expired yet`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.createdAt + (5 * 60 * 1000L) // 5 minutes in

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    // ============================================================================
    // Violation Detection Tests
    // ============================================================================

    @Test
    fun `checkViolations should detect violation when agreement expired and app still in use`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.expiresAt + 1000L // 1 second after expiry

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
        assertNotNull(result.violatedAgreement)
        assertEquals(agreement.id, result.violatedAgreement?.id)
    }

    @Test
    fun `checkViolations should detect violation at exact expiry time`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.expiresAt

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
        assertNotNull(result.violatedAgreement)
    }

    @Test
    fun `checkViolations should detect violation for general phone usage agreement`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = null, // General phone usage
            durationMs = 30 * 60 * 1000L
        )
        val currentApp = "com.anything.android"
        val currentTime = agreement.expiresAt + 5000L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
        assertNotNull(result.violatedAgreement)
    }

    // ============================================================================
    // Completion Detection Tests
    // ============================================================================

    @Test
    fun `checkViolations should detect completion when agreement expired but app closed`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.other.app" // Different app - user switched away
        val currentTime = agreement.expiresAt + 1000L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.COMPLETION, result.action)
        assertNotNull(result.expiredAgreement)
        assertEquals(agreement.id, result.expiredAgreement?.id)
    }

    @Test
    fun `checkViolations should detect completion when user is on home screen`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = null // Home screen or no foreground app
        val currentTime = agreement.expiresAt + 1000L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.COMPLETION, result.action)
        assertNotNull(result.expiredAgreement)
    }

    // ============================================================================
    // Multiple Agreements Tests
    // ============================================================================

    @Test
    fun `checkViolations should prioritize first violated agreement`() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val agreement1 = createActiveAgreement(
            id = 1L,
            appPackage = "com.instagram.android",
            durationMs = 5 * 60 * 1000L,
            createdAt = currentTime - 10 * 60 * 1000L // Created 10 min ago
        )
        val agreement2 = createActiveAgreement(
            id = 2L,
            appPackage = "com.instagram.android",
            durationMs = 3 * 60 * 1000L,
            createdAt = currentTime - 8 * 60 * 1000L // Created 8 min ago
        )
        val currentApp = "com.instagram.android"

        // Act
        val result = enforcer.checkViolations(
            listOf(agreement1, agreement2),
            currentApp,
            currentTime
        )

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
        // Should detect the first violated agreement in the list
        assertNotNull(result.violatedAgreement)
    }

    @Test
    fun `checkViolations should handle mix of expired and active agreements`() {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val expiredAgreement = createActiveAgreement(
            id = 1L,
            appPackage = "com.facebook.android",
            durationMs = 5 * 60 * 1000L,
            createdAt = currentTime - 10 * 60 * 1000L // Expired
        )
        val activeAgreement = createActiveAgreement(
            id = 2L,
            appPackage = "com.instagram.android",
            durationMs = 20 * 60 * 1000L,
            createdAt = currentTime - 5 * 60 * 1000L // Still active
        )
        val currentApp = "com.instagram.android"

        // Act
        val result = enforcer.checkViolations(
            listOf(expiredAgreement, activeAgreement),
            currentApp,
            currentTime
        )

        // Assert
        // Should only care about the current app's agreement, which is still active
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    // ============================================================================
    // Edge Cases Tests
    // ============================================================================

    @Test
    fun `checkViolations should handle very short duration agreements`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 1000L // 1 second
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.expiresAt + 100L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
    }

    @Test
    fun `checkViolations should handle very long duration agreements`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 24 * 60 * 60 * 1000L // 24 hours
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.createdAt + (12 * 60 * 60 * 1000L) // 12 hours in

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    @Test
    fun `checkViolations should handle agreement just before expiry`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.instagram.android"
        val currentTime = agreement.expiresAt - 1L // 1ms before expiry

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    @Test
    fun `checkViolations should handle past time (clock skew)`() {
        // Arrange
        val futureTime = System.currentTimeMillis() + 1000000L
        val agreement = createActiveAgreement(
            appPackage = "com.instagram.android",
            durationMs = 10 * 60 * 1000L,
            createdAt = futureTime
        )
        val currentApp = "com.instagram.android"
        val currentTime = System.currentTimeMillis() // Before agreement created

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        // Agreement not yet started - should be NONE
        assertEquals(ViolationResult.Action.NONE, result.action)
    }

    // ============================================================================
    // General Phone Usage Tests
    // ============================================================================

    @Test
    fun `checkViolations should detect violation for general usage regardless of app`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = null, // General phone usage
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = "com.random.app"
        val currentTime = agreement.expiresAt + 1000L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.VIOLATION, result.action)
        assertNotNull(result.violatedAgreement)
    }

    @Test
    fun `checkViolations should detect completion for general usage when on home`() {
        // Arrange
        val agreement = createActiveAgreement(
            appPackage = null, // General phone usage
            durationMs = 10 * 60 * 1000L
        )
        val currentApp = null // Home screen
        val currentTime = agreement.expiresAt + 1000L

        // Act
        val result = enforcer.checkViolations(listOf(agreement), currentApp, currentTime)

        // Assert
        assertEquals(ViolationResult.Action.COMPLETION, result.action)
        assertNotNull(result.expiredAgreement)
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createActiveAgreement(
        id: Long = 1L,
        appPackage: String?,
        durationMs: Long,
        createdAt: Long = System.currentTimeMillis()
    ): Agreement {
        return Agreement(
            id = id,
            appPackageName = appPackage,
            appName = if (appPackage == null) "Phone Usage" else "Test App",
            appCategory = "TEST",
            agreedDuration = durationMs,
            createdAt = createdAt,
            expiresAt = createdAt + durationMs,
            status = Agreement.STATUS_ACTIVE,
            conversationId = 1L
        )
    }
}
