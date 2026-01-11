package com.focusmother.android.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a time agreement between the user and the AI avatar.
 *
 * An agreement can be for a specific app (e.g., Instagram) or general phone usage.
 * It tracks the agreed-upon duration, current status, and violation/completion timestamps.
 *
 * PERFORMANCE: Indexes added for frequently queried fields:
 * - status: Used to query active agreements in MonitoringService
 * - expiresAt: Used to check if agreements have expired
 * - createdAt: Used for date range queries in analytics
 *
 * @property id Unique identifier for the agreement
 * @property appPackageName Package name of the app (null for general phone usage)
 * @property appName Display name of the app or "Phone Usage" for general agreements
 * @property appCategory Category like "SOCIAL_MEDIA", "GAMES", "ADULT_CONTENT", etc.
 * @property agreedDuration Agreed time limit in milliseconds
 * @property createdAt Timestamp when agreement was created
 * @property expiresAt Timestamp when agreement expires (createdAt + agreedDuration)
 * @property status Current status: "ACTIVE", "COMPLETED", or "VIOLATED"
 * @property violatedAt Timestamp when agreement was violated (null if not violated)
 * @property completedAt Timestamp when agreement was completed successfully (null if not completed)
 * @property conversationId Foreign key to the conversation that created this agreement
 */
@Entity(
    tableName = "agreements",
    indices = [
        Index(value = ["status"]),
        Index(value = ["expiresAt"]),
        Index(value = ["createdAt"])
    ]
)
data class Agreement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val appPackageName: String?,
    
    val appName: String,
    
    val appCategory: String,
    
    val agreedDuration: Long,
    
    val createdAt: Long,
    
    val expiresAt: Long,
    
    val status: String,
    
    val violatedAt: Long? = null,
    
    val completedAt: Long? = null,
    
    val conversationId: Long
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_VIOLATED = "VIOLATED"
        
        /**
         * Creates a new agreement with automatic expiry calculation.
         */
        fun create(
            appPackageName: String?,
            appName: String,
            appCategory: String,
            agreedDurationMs: Long,
            conversationId: Long
        ): Agreement {
            val now = System.currentTimeMillis()
            return Agreement(
                appPackageName = appPackageName,
                appName = appName,
                appCategory = appCategory,
                agreedDuration = agreedDurationMs,
                createdAt = now,
                expiresAt = now + agreedDurationMs,
                status = STATUS_ACTIVE,
                conversationId = conversationId
            )
        }
    }
}
