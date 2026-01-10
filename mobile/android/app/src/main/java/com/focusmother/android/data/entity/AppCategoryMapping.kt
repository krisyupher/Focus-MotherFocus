package com.focusmother.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Maps app package names to categories and tracks blocking/threshold configuration.
 *
 * This entity allows the system to categorize apps (e.g., social media, games, adult content)
 * and apply custom rules per app. Supports both system-defined and user-defined categorizations.
 *
 * @property packageName Android package name (e.g., "com.instagram.android")
 * @property category App category: "SOCIAL_MEDIA", "GAMES", "ADULT_CONTENT", "PRODUCTIVITY", etc.
 * @property isBlocked Whether the app is completely blocked
 * @property customThreshold Custom time threshold for this specific app (milliseconds, null = use default)
 * @property addedBy Who added this mapping: "SYSTEM" or "USER"
 */
@Entity(tableName = "app_categories")
data class AppCategoryMapping(
    @PrimaryKey
    val packageName: String,
    
    val category: String,
    
    val isBlocked: Boolean = false,
    
    val customThreshold: Long? = null,
    
    val addedBy: String = "SYSTEM"
) {
    companion object {
        // Category constants
        const val CATEGORY_SOCIAL_MEDIA = "SOCIAL_MEDIA"
        const val CATEGORY_GAMES = "GAMES"
        const val CATEGORY_ADULT_CONTENT = "ADULT_CONTENT"
        const val CATEGORY_ENTERTAINMENT = "ENTERTAINMENT"
        const val CATEGORY_PRODUCTIVITY = "PRODUCTIVITY"
        const val CATEGORY_COMMUNICATION = "COMMUNICATION"
        const val CATEGORY_SHOPPING = "SHOPPING"
        const val CATEGORY_NEWS = "NEWS"
        const val CATEGORY_OTHER = "OTHER"
        
        // Added by constants
        const val ADDED_BY_SYSTEM = "SYSTEM"
        const val ADDED_BY_USER = "USER"
        
        /**
         * Creates a system-defined category mapping.
         */
        fun systemMapping(
            packageName: String,
            category: String,
            isBlocked: Boolean = false,
            customThreshold: Long? = null
        ): AppCategoryMapping {
            return AppCategoryMapping(
                packageName = packageName,
                category = category,
                isBlocked = isBlocked,
                customThreshold = customThreshold,
                addedBy = ADDED_BY_SYSTEM
            )
        }
        
        /**
         * Creates a user-defined category mapping.
         */
        fun userMapping(
            packageName: String,
            category: String,
            isBlocked: Boolean = false,
            customThreshold: Long? = null
        ): AppCategoryMapping {
            return AppCategoryMapping(
                packageName = packageName,
                category = category,
                isBlocked = isBlocked,
                customThreshold = customThreshold,
                addedBy = ADDED_BY_USER
            )
        }
    }
}
