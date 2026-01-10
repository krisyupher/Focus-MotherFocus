package com.focusmother.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.focusmother.android.data.entity.Agreement

/**
 * Data Access Object for Agreement operations.
 *
 * Provides suspend functions for accessing and modifying agreements in the database.
 * All operations are coroutine-compatible for non-blocking database access.
 */
@Dao
interface AgreementDao {
    
    /**
     * Retrieves all agreements with a specific status.
     *
     * @param status Agreement status (ACTIVE, COMPLETED, or VIOLATED)
     * @return List of agreements ordered by creation time (newest first)
     */
    @Query("SELECT * FROM agreements WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getByStatus(status: String): List<Agreement>
    
    /**
     * Retrieves all agreements created after a specific timestamp.
     *
     * @param since Timestamp (milliseconds) to filter from
     * @return List of agreements ordered by creation time (newest first)
     */
    @Query("SELECT * FROM agreements WHERE createdAt > :since ORDER BY createdAt DESC")
    suspend fun getSince(since: Long): List<Agreement>
    
    /**
     * Retrieves all agreements (for testing and admin purposes).
     *
     * @return All agreements ordered by creation time (newest first)
     */
    @Query("SELECT * FROM agreements ORDER BY createdAt DESC")
    suspend fun getAll(): List<Agreement>

    /**
     * Retrieves the most recent agreements up to a specified limit.
     *
     * @param limit Maximum number of agreements to retrieve
     * @return List of recent agreements ordered by creation time (newest first)
     */
    @Query("SELECT * FROM agreements ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<Agreement>
    
    /**
     * Inserts a new agreement.
     *
     * @param agreement The agreement to insert
     * @return The ID of the inserted agreement
     */
    @Insert
    suspend fun insert(agreement: Agreement): Long
    
    /**
     * Updates an agreement's status and completion time.
     *
     * @param id Agreement ID to update
     * @param status New status value
     * @param time Completion timestamp
     */
    @Query("UPDATE agreements SET status = :status, completedAt = :time WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, time: Long)
    
    /**
     * Marks an agreement as violated.
     *
     * @param id Agreement ID to mark as violated
     * @param time Timestamp when violation occurred
     */
    @Query("UPDATE agreements SET status = 'VIOLATED', violatedAt = :time WHERE id = :id")
    suspend fun markViolated(id: Long, time: Long)
    
    /**
     * Retrieves a specific agreement by ID.
     *
     * @param id Agreement ID
     * @return Agreement if found, null otherwise
     */
    @Query("SELECT * FROM agreements WHERE id = :id")
    suspend fun getById(id: Long): Agreement?
    
    /**
     * Retrieves active agreements for a specific app.
     *
     * @param packageName App package name
     * @return List of active agreements for the app
     */
    @Query("SELECT * FROM agreements WHERE appPackageName = :packageName AND status = 'ACTIVE' ORDER BY createdAt DESC")
    suspend fun getActiveAgreementsForApp(packageName: String): List<Agreement>
    
    /**
     * Deletes all agreements (for testing purposes).
     */
    @Query("DELETE FROM agreements")
    suspend fun deleteAll()
}
