package com.focusmother.android.data.repository

import com.focusmother.android.data.dao.AgreementDao
import com.focusmother.android.data.entity.Agreement
import com.focusmother.android.data.entity.AppCategoryMapping
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AgreementRepository.
 *
 * Tests CRUD operations and state transitions for agreements.
 * Uses MockK for mocking the AgreementDao dependency.
 */
class AgreementRepositoryTest {

    private lateinit var agreementDao: AgreementDao
    private lateinit var repository: AgreementRepository

    @Before
    fun setup() {
        agreementDao = mockk()
        repository = AgreementRepository(agreementDao)
    }

    // ============================================================================
    // createAgreement Tests
    // ============================================================================

    @Test
    fun `createAgreement should create agreement with app package`() = runTest {
        // Arrange
        val appPackage = "com.instagram.android"
        val appName = "Instagram"
        val category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA
        val durationMs = 10 * 60 * 1000L // 10 minutes
        val conversationId = 1L
        val expectedId = 42L

        coEvery { agreementDao.insert(any()) } returns expectedId

        // Act
        val result = repository.createAgreement(
            appPackage = appPackage,
            appName = appName,
            category = category,
            durationMs = durationMs,
            conversationId = conversationId
        )

        // Assert
        assertEquals(expectedId, result)
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                agreement.appPackageName == appPackage &&
                agreement.appName == appName &&
                agreement.appCategory == category &&
                agreement.agreedDuration == durationMs &&
                agreement.conversationId == conversationId &&
                agreement.status == Agreement.STATUS_ACTIVE
            })
        }
    }

    @Test
    fun `createAgreement should create agreement without app package for general usage`() = runTest {
        // Arrange
        val appName = "Phone Usage"
        val category = "GENERAL"
        val durationMs = 30 * 60 * 1000L // 30 minutes
        val conversationId = 2L
        val expectedId = 99L

        coEvery { agreementDao.insert(any()) } returns expectedId

        // Act
        val result = repository.createAgreement(
            appPackage = null,
            appName = appName,
            category = category,
            durationMs = durationMs,
            conversationId = conversationId
        )

        // Assert
        assertEquals(expectedId, result)
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                agreement.appPackageName == null &&
                agreement.appName == appName &&
                agreement.appCategory == category &&
                agreement.agreedDuration == durationMs &&
                agreement.status == Agreement.STATUS_ACTIVE
            })
        }
    }

    @Test
    fun `createAgreement should set correct expiry time`() = runTest {
        // Arrange
        val durationMs = 5 * 60 * 1000L // 5 minutes
        val beforeCreation = System.currentTimeMillis()

        coEvery { agreementDao.insert(any()) } returns 1L

        // Act
        repository.createAgreement(
            appPackage = "com.example.app",
            appName = "Test App",
            category = "TEST",
            durationMs = durationMs,
            conversationId = 1L
        )

        val afterCreation = System.currentTimeMillis()

        // Assert
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                // Expiry should be createdAt + duration
                val expectedExpiry = agreement.createdAt + durationMs
                agreement.expiresAt == expectedExpiry &&
                // Creation time should be within test execution window
                agreement.createdAt >= beforeCreation &&
                agreement.createdAt <= afterCreation
            })
        }
    }

    // ============================================================================
    // getActiveAgreements Tests
    // ============================================================================

    @Test
    fun `getActiveAgreements should return list of active agreements`() = runTest {
        // Arrange
        val activeAgreements = listOf(
            createSampleAgreement(id = 1L, status = Agreement.STATUS_ACTIVE),
            createSampleAgreement(id = 2L, status = Agreement.STATUS_ACTIVE)
        )

        coEvery { agreementDao.getByStatus(Agreement.STATUS_ACTIVE) } returns activeAgreements

        // Act
        val result = repository.getActiveAgreements()

        // Assert
        assertEquals(2, result.size)
        assertEquals(activeAgreements, result)
        coVerify(exactly = 1) { agreementDao.getByStatus(Agreement.STATUS_ACTIVE) }
    }

    @Test
    fun `getActiveAgreements should return empty list when no active agreements`() = runTest {
        // Arrange
        coEvery { agreementDao.getByStatus(Agreement.STATUS_ACTIVE) } returns emptyList()

        // Act
        val result = repository.getActiveAgreements()

        // Assert
        assertEquals(0, result.size)
        coVerify(exactly = 1) { agreementDao.getByStatus(Agreement.STATUS_ACTIVE) }
    }

    // ============================================================================
    // getAgreementById Tests
    // ============================================================================

    @Test
    fun `getAgreementById should return agreement when found`() = runTest {
        // Arrange
        val agreementId = 5L
        val agreement = createSampleAgreement(id = agreementId)

        coEvery { agreementDao.getById(agreementId) } returns agreement

        // Act
        val result = repository.getAgreementById(agreementId)

        // Assert
        assertNotNull(result)
        assertEquals(agreementId, result?.id)
        coVerify(exactly = 1) { agreementDao.getById(agreementId) }
    }

    @Test
    fun `getAgreementById should return null when not found`() = runTest {
        // Arrange
        val agreementId = 999L

        coEvery { agreementDao.getById(agreementId) } returns null

        // Act
        val result = repository.getAgreementById(agreementId)

        // Assert
        assertNull(result)
        coVerify(exactly = 1) { agreementDao.getById(agreementId) }
    }

    // ============================================================================
    // completeAgreement Tests
    // ============================================================================

    @Test
    fun `completeAgreement should mark agreement as completed with timestamp`() = runTest {
        // Arrange
        val agreementId = 10L
        val beforeCompletion = System.currentTimeMillis()

        coEvery { agreementDao.updateStatus(any(), any(), any()) } returns Unit

        // Act
        repository.completeAgreement(agreementId)

        val afterCompletion = System.currentTimeMillis()

        // Assert
        coVerify(exactly = 1) {
            agreementDao.updateStatus(
                id = agreementId,
                status = Agreement.STATUS_COMPLETED,
                time = match { timestamp ->
                    timestamp >= beforeCompletion && timestamp <= afterCompletion
                }
            )
        }
    }

    @Test
    fun `completeAgreement should handle multiple agreements`() = runTest {
        // Arrange
        val agreementIds = listOf(1L, 2L, 3L)

        coEvery { agreementDao.updateStatus(any(), any(), any()) } returns Unit

        // Act
        agreementIds.forEach { id ->
            repository.completeAgreement(id)
        }

        // Assert
        agreementIds.forEach { id ->
            coVerify(exactly = 1) {
                agreementDao.updateStatus(
                    id = id,
                    status = Agreement.STATUS_COMPLETED,
                    time = any()
                )
            }
        }
    }

    // ============================================================================
    // violateAgreement Tests
    // ============================================================================

    @Test
    fun `violateAgreement should mark agreement as violated with timestamp`() = runTest {
        // Arrange
        val agreementId = 15L
        val beforeViolation = System.currentTimeMillis()

        coEvery { agreementDao.markViolated(any(), any()) } returns Unit

        // Act
        repository.violateAgreement(agreementId)

        val afterViolation = System.currentTimeMillis()

        // Assert
        coVerify(exactly = 1) {
            agreementDao.markViolated(
                id = agreementId,
                time = match { timestamp ->
                    timestamp >= beforeViolation && timestamp <= afterViolation
                }
            )
        }
    }

    @Test
    fun `violateAgreement should handle multiple violations`() = runTest {
        // Arrange
        val agreementIds = listOf(5L, 10L, 15L)

        coEvery { agreementDao.markViolated(any(), any()) } returns Unit

        // Act
        agreementIds.forEach { id ->
            repository.violateAgreement(id)
        }

        // Assert
        agreementIds.forEach { id ->
            coVerify(exactly = 1) {
                agreementDao.markViolated(
                    id = id,
                    time = any()
                )
            }
        }
    }

    // ============================================================================
    // Edge Cases and Error Scenarios
    // ============================================================================

    @Test
    fun `createAgreement should handle zero duration`() = runTest {
        // Arrange
        val durationMs = 0L

        coEvery { agreementDao.insert(any()) } returns 1L

        // Act
        repository.createAgreement(
            appPackage = "com.example.app",
            appName = "Test",
            category = "TEST",
            durationMs = durationMs,
            conversationId = 1L
        )

        // Assert
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                agreement.agreedDuration == 0L &&
                agreement.expiresAt == agreement.createdAt
            })
        }
    }

    @Test
    fun `createAgreement should handle very large duration`() = runTest {
        // Arrange
        val durationMs = 24 * 60 * 60 * 1000L // 24 hours

        coEvery { agreementDao.insert(any()) } returns 1L

        // Act
        repository.createAgreement(
            appPackage = "com.example.app",
            appName = "Test",
            category = "TEST",
            durationMs = durationMs,
            conversationId = 1L
        )

        // Assert
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                agreement.agreedDuration == durationMs
            })
        }
    }

    @Test
    fun `createAgreement should handle special characters in app name`() = runTest {
        // Arrange
        val appName = "Test App: Special @#$%"

        coEvery { agreementDao.insert(any()) } returns 1L

        // Act
        repository.createAgreement(
            appPackage = "com.example.app",
            appName = appName,
            category = "TEST",
            durationMs = 1000L,
            conversationId = 1L
        )

        // Assert
        coVerify(exactly = 1) {
            agreementDao.insert(match { agreement ->
                agreement.appName == appName
            })
        }
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createSampleAgreement(
        id: Long,
        status: String = Agreement.STATUS_ACTIVE,
        appPackage: String? = "com.example.app",
        appName: String = "Test App",
        category: String = "TEST",
        durationMs: Long = 10 * 60 * 1000L
    ): Agreement {
        val now = System.currentTimeMillis()
        return Agreement(
            id = id,
            appPackageName = appPackage,
            appName = appName,
            appCategory = category,
            agreedDuration = durationMs,
            createdAt = now,
            expiresAt = now + durationMs,
            status = status,
            conversationId = 1L
        )
    }
}
