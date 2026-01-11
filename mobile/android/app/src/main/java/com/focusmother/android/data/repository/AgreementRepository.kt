package com.focusmother.android.data.repository

import com.focusmother.android.data.dao.AgreementDao
import com.focusmother.android.data.entity.Agreement

/**
 * Repository for managing time agreements.
 *
 * Provides high-level CRUD operations for agreements and handles business logic
 * related to agreement lifecycle (creation, completion, violation).
 *
 * This repository abstracts the data access layer and provides a clean API
 * for the domain and UI layers to interact with agreement data.
 *
 * @property agreementDao Data access object for agreement database operations
 */
class AgreementRepository(
    private val agreementDao: AgreementDao
) {

    /**
     * Creates a new time agreement.
     *
     * The agreement is created with ACTIVE status and the expiry time is automatically
     * calculated based on the current time plus the agreed duration.
     *
     * @param appPackage Package name of the app (null for general phone usage)
     * @param appName Display name of the app or "Phone Usage" for general agreements
     * @param category App category (e.g., SOCIAL_MEDIA, GAMES, ADULT_CONTENT)
     * @param durationMs Agreed duration in milliseconds
     * @param conversationId ID of the conversation that created this agreement
     * @return The ID of the newly created agreement
     */
    suspend fun createAgreement(
        appPackage: String?,
        appName: String,
        category: String,
        durationMs: Long,
        conversationId: Long
    ): Long {
        val agreement = Agreement.create(
            appPackageName = appPackage,
            appName = appName,
            appCategory = category,
            agreedDurationMs = durationMs,
            conversationId = conversationId
        )
        return agreementDao.insert(agreement)
    }

    /**
     * Retrieves all active agreements.
     *
     * Active agreements are those with status = ACTIVE and have not yet expired,
     * been completed, or violated.
     *
     * @return List of active agreements ordered by creation time (newest first)
     */
    suspend fun getActiveAgreements(): List<Agreement> {
        return agreementDao.getByStatus(Agreement.STATUS_ACTIVE)
    }

    /**
     * Retrieves a specific agreement by ID.
     *
     * @param id Agreement ID
     * @return Agreement if found, null otherwise
     */
    suspend fun getAgreementById(id: Long): Agreement? {
        return agreementDao.getById(id)
    }

    /**
     * Marks an agreement as completed.
     *
     * This is called when the user successfully honors the agreement and the
     * time limit expires without violation. Sets status to COMPLETED and records
     * the completion timestamp.
     *
     * @param id Agreement ID to complete
     */
    suspend fun completeAgreement(id: Long) {
        val now = System.currentTimeMillis()
        agreementDao.updateStatus(
            id = id,
            status = Agreement.STATUS_COMPLETED,
            time = now
        )
    }

    /**
     * Marks an agreement as violated.
     *
     * This is called when the user breaks the agreement by continuing to use
     * the app or phone beyond the agreed time limit. Sets status to VIOLATED
     * and records the violation timestamp.
     *
     * @param id Agreement ID to mark as violated
     */
    suspend fun violateAgreement(id: Long) {
        val now = System.currentTimeMillis()
        agreementDao.markViolated(
            id = id,
            time = now
        )
    }

    /**
     * Retrieves active agreements for a specific app.
     *
     * Useful for checking if there are any existing agreements before creating
     * a new one for the same app.
     *
     * @param packageName App package name
     * @return List of active agreements for the specified app
     */
    suspend fun getActiveAgreementsForApp(packageName: String): List<Agreement> {
        return agreementDao.getActiveAgreementsForApp(packageName)
    }

    /**
     * Retrieves all agreements (for testing and analytics purposes).
     *
     * @return All agreements ordered by creation time (newest first)
     */
    suspend fun getAllAgreements(): List<Agreement> {
        return agreementDao.getAll()
    }

    /**
     * Retrieves recent agreements up to a specified limit.
     *
     * Useful for showing agreement history in the UI.
     *
     * @param limit Maximum number of agreements to retrieve
     * @return Recent agreements ordered by creation time (newest first)
     */
    suspend fun getRecentAgreements(limit: Int = 10): List<Agreement> {
        return agreementDao.getRecent(limit)
    }

    /**
     * Retrieves agreements within a specific date range.
     *
     * Useful for analytics and reporting (e.g., weekly stats).
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return List of agreements created within the specified range
     */
    suspend fun getAgreementsByDateRange(startTime: Long, endTime: Long): List<Agreement> {
        return agreementDao.getAll().filter { agreement ->
            agreement.createdAt in startTime..endTime
        }
    }
}
