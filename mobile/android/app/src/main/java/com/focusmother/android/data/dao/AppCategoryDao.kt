package com.focusmother.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusmother.android.data.entity.AppCategoryMapping

/**
 * Data Access Object for AppCategoryMapping operations.
 *
 * Manages app categorization, blocking rules, and custom thresholds.
 * Supports both system-defined and user-defined categorizations.
 */
@Dao
interface AppCategoryDao {
    
    /**
     * Retrieves the category mapping for a specific package.
     *
     * @param pkg Package name to lookup
     * @return AppCategoryMapping if found, null otherwise
     */
    @Query("SELECT * FROM app_categories WHERE packageName = :pkg")
    suspend fun getByPackage(pkg: String): AppCategoryMapping?
    
    /**
     * Inserts or replaces a category mapping.
     *
     * Uses REPLACE strategy to allow updates of existing mappings.
     *
     * @param mapping Category mapping to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: AppCategoryMapping)
    
    /**
     * Inserts a mapping only if it doesn't already exist.
     *
     * Uses IGNORE strategy to preserve existing user customizations.
     *
     * @param mapping Category mapping to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(mapping: AppCategoryMapping)
    
    /**
     * Retrieves all apps in a specific category.
     *
     * @param category Category to filter by
     * @return List of app mappings in the category
     */
    @Query("SELECT * FROM app_categories WHERE category = :category")
    suspend fun getByCategory(category: String): List<AppCategoryMapping>
    
    /**
     * Retrieves all blocked apps.
     *
     * @return List of app mappings where isBlocked = true
     */
    @Query("SELECT * FROM app_categories WHERE isBlocked = 1")
    suspend fun getBlocked(): List<AppCategoryMapping>
    
    /**
     * Retrieves all category mappings (for admin and testing).
     *
     * @return All app category mappings
     */
    @Query("SELECT * FROM app_categories")
    suspend fun getAll(): List<AppCategoryMapping>
    
    /**
     * Updates the blocked status of an app.
     *
     * @param packageName Package name to update
     * @param isBlocked New blocked status
     */
    @Query("UPDATE app_categories SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockedStatus(packageName: String, isBlocked: Boolean)
    
    /**
     * Updates the custom threshold for an app.
     *
     * @param packageName Package name to update
     * @param threshold New custom threshold in milliseconds (null to remove)
     */
    @Query("UPDATE app_categories SET customThreshold = :threshold WHERE packageName = :packageName")
    suspend fun updateCustomThreshold(packageName: String, threshold: Long?)
    
    /**
     * Deletes a specific category mapping.
     *
     * @param packageName Package name to delete
     */
    @Query("DELETE FROM app_categories WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
    
    /**
     * Deletes all category mappings (for testing purposes).
     */
    @Query("DELETE FROM app_categories")
    suspend fun deleteAll()
}
