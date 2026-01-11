package com.focusmother.android.domain

import com.focusmother.android.data.dao.AppCategoryDao
import com.focusmother.android.data.entity.AppCategoryMapping

/**
 * Manages app categorization and threshold configuration.
 *
 * This class provides a high-level API for:
 * - Seeding the database with predefined app categories
 * - Categorizing apps based on their package names
 * - Managing custom thresholds for individual apps
 * - Handling app blocking rules
 * - Supporting user overrides of system categorizations
 *
 * The manager uses a two-tier approach:
 * 1. Database-first: Check if app has a mapping in the database
 * 2. Fallback to seed data: If not in database, check seed data
 * 3. Default to UNKNOWN: If app not found anywhere
 *
 * User categorizations always override system categorizations via the
 * database REPLACE strategy.
 *
 * @property appCategoryDao Data access object for app category operations
 */
class CategoryManager(
    private val appCategoryDao: AppCategoryDao
) {

    /**
     * Seeds the database with all predefined app categories.
     *
     * This method should be called once on first app launch to populate
     * the database with system-defined categorizations. It uses
     * insertIfNotExists to preserve any existing user customizations.
     *
     * Seeds the following categories:
     * - 60+ social media apps
     * - 100+ games
     * - 40+ entertainment apps
     * - 15+ browsers
     * - 30+ productivity apps
     * - 20+ communication apps
     * - 10+ adult content placeholders
     *
     * Total: 300+ apps
     */
    suspend fun seedDatabase() {
        // Seed social media apps
        AppCategorySeedData.SOCIAL_MEDIA.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed games
        AppCategorySeedData.GAMES.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_GAMES
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed entertainment apps
        AppCategorySeedData.ENTERTAINMENT.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_ENTERTAINMENT
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed browsers
        AppCategorySeedData.BROWSER.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = "BROWSER"
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed productivity apps
        AppCategorySeedData.PRODUCTIVITY.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_PRODUCTIVITY
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed communication apps
        AppCategorySeedData.COMMUNICATION.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_COMMUNICATION
            )
            appCategoryDao.insertIfNotExists(mapping)
        }

        // Seed adult content placeholders
        AppCategorySeedData.ADULT_CONTENT.forEach { packageName ->
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = AppCategoryMapping.CATEGORY_ADULT_CONTENT
            )
            appCategoryDao.insertIfNotExists(mapping)
        }
    }

    /**
     * Categorizes an app based on its package name.
     *
     * The categorization follows this priority order:
     * 1. Database lookup (includes both system and user categorizations)
     * 2. Fallback to seed data (for apps not yet in database)
     * 3. Return UNKNOWN if app is not found
     *
     * @param packageName Android package name (e.g., "com.instagram.android")
     * @return Category string (e.g., "SOCIAL_MEDIA", "GAMES", "UNKNOWN")
     */
    suspend fun categorizeApp(packageName: String): String {
        // First check database (includes user overrides)
        val dbMapping = appCategoryDao.getByPackage(packageName)
        if (dbMapping != null) {
            return dbMapping.category
        }

        // Fallback to seed data
        return when {
            packageName in AppCategorySeedData.SOCIAL_MEDIA ->
                AppCategoryMapping.CATEGORY_SOCIAL_MEDIA

            packageName in AppCategorySeedData.GAMES ->
                AppCategoryMapping.CATEGORY_GAMES

            packageName in AppCategorySeedData.ENTERTAINMENT ->
                AppCategoryMapping.CATEGORY_ENTERTAINMENT

            packageName in AppCategorySeedData.BROWSER ->
                "BROWSER"

            packageName in AppCategorySeedData.PRODUCTIVITY ->
                AppCategoryMapping.CATEGORY_PRODUCTIVITY

            packageName in AppCategorySeedData.COMMUNICATION ->
                AppCategoryMapping.CATEGORY_COMMUNICATION

            packageName in AppCategorySeedData.ADULT_CONTENT ->
                AppCategoryMapping.CATEGORY_ADULT_CONTENT

            else -> CATEGORY_UNKNOWN
        }
    }

    /**
     * Gets the usage threshold for a specific app.
     *
     * Returns the threshold in this priority order:
     * 1. App-specific custom threshold (if set)
     * 2. Category default threshold
     * 3. UNKNOWN category default (60 minutes)
     *
     * @param packageName Android package name
     * @return Threshold in milliseconds
     */
    suspend fun getThreshold(packageName: String): Long {
        // Check if app has custom threshold in database
        val dbMapping = appCategoryDao.getByPackage(packageName)
        if (dbMapping?.customThreshold != null) {
            return dbMapping.customThreshold
        }

        // Get category and return its default threshold
        val category = categorizeApp(packageName)
        return AppCategorySeedData.CATEGORY_THRESHOLDS[category]
            ?: AppCategorySeedData.CATEGORY_THRESHOLDS[CATEGORY_UNKNOWN]
            ?: 60 * 60 * 1000L // Fallback to 60 minutes
    }

    /**
     * Allows user to categorize or recategorize an app.
     *
     * This creates a USER mapping that overrides any existing SYSTEM mapping.
     * The database REPLACE strategy ensures the new categorization takes precedence.
     *
     * @param packageName Android package name
     * @param category New category for the app
     */
    suspend fun userCategorize(packageName: String, category: String) {
        val mapping = AppCategoryMapping.userMapping(
            packageName = packageName,
            category = category
        )
        appCategoryDao.insert(mapping) // REPLACE strategy
    }

    /**
     * Sets a custom usage threshold for a specific app.
     *
     * This allows per-app time limits that override category defaults.
     * If the app doesn't exist in the database, it creates a new entry.
     *
     * @param packageName Android package name
     * @param threshold Custom threshold in milliseconds (or null to remove)
     */
    suspend fun setCustomThreshold(packageName: String, threshold: Long?) {
        val existing = appCategoryDao.getByPackage(packageName)

        if (existing != null) {
            // Update existing mapping
            appCategoryDao.updateCustomThreshold(packageName, threshold)
        } else {
            // Create new mapping with custom threshold
            val category = categorizeApp(packageName)
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = category,
                customThreshold = threshold
            )
            appCategoryDao.insert(mapping)
        }
    }

    /**
     * Gets the full category mapping for an app.
     *
     * @param packageName Android package name
     * @return AppCategoryMapping if found in database, null otherwise
     */
    suspend fun getMapping(packageName: String): AppCategoryMapping? {
        return appCategoryDao.getByPackage(packageName)
    }

    /**
     * Checks if an app is blocked.
     *
     * @param packageName Android package name
     * @return true if app is blocked, false otherwise
     */
    suspend fun isBlocked(packageName: String): Boolean {
        val mapping = appCategoryDao.getByPackage(packageName)
        return mapping?.isBlocked ?: false
    }

    /**
     * Sets the blocked status for an app.
     *
     * Blocked apps are completely prevented from being used, regardless
     * of time thresholds. If the app doesn't exist in the database,
     * it creates a new entry.
     *
     * @param packageName Android package name
     * @param blocked true to block the app, false to unblock
     */
    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        val existing = appCategoryDao.getByPackage(packageName)

        if (existing != null) {
            // Update existing mapping
            appCategoryDao.updateBlockedStatus(packageName, blocked)
        } else {
            // Create new mapping with blocked status
            val category = categorizeApp(packageName)
            val mapping = AppCategoryMapping.systemMapping(
                packageName = packageName,
                category = category,
                isBlocked = blocked
            )
            appCategoryDao.insert(mapping)
        }
    }

    companion object {
        /**
         * Category constant for unknown/uncategorized apps.
         */
        const val CATEGORY_UNKNOWN = "UNKNOWN"
    }
}
