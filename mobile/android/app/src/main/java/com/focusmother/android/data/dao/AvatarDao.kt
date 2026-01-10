package com.focusmother.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusmother.android.data.entity.AvatarConfig

/**
 * Data Access Object for AvatarConfig operations.
 *
 * Manages the 3D avatar configuration. Only one avatar can be active at a time.
 * The avatar entity uses a fixed ID of 1 to ensure singleton behavior.
 */
@Dao
interface AvatarDao {
    
    /**
     * Retrieves the current avatar configuration.
     *
     * @return AvatarConfig if set, null if no avatar configured
     */
    @Query("SELECT * FROM avatar_config WHERE id = 1")
    suspend fun getAvatar(): AvatarConfig?
    
    /**
     * Inserts or replaces the avatar configuration.
     *
     * Uses REPLACE strategy to ensure only one avatar is stored.
     *
     * @param config Avatar configuration to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: AvatarConfig)
    
    /**
     * Updates the avatar's local file paths.
     *
     * @param glbPath New GLB file path
     * @param thumbnailPath New thumbnail path
     */
    @Query("UPDATE avatar_config SET localGlbPath = :glbPath, thumbnailPath = :thumbnailPath, lastModified = :lastModified WHERE id = 1")
    suspend fun updatePaths(glbPath: String, thumbnailPath: String, lastModified: Long)
    
    /**
     * Updates the avatar's customization data.
     *
     * @param customizationData New JSON customization data
     * @param lastModified Timestamp of the update
     */
    @Query("UPDATE avatar_config SET customizationData = :customizationData, lastModified = :lastModified WHERE id = 1")
    suspend fun updateCustomization(customizationData: String, lastModified: Long)
    
    /**
     * Checks if an avatar is configured.
     *
     * @return true if avatar exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM avatar_config WHERE id = 1)")
    suspend fun hasAvatar(): Boolean
    
    /**
     * Deletes the current avatar configuration.
     *
     * Used when user wants to reset or change their avatar.
     */
    @Query("DELETE FROM avatar_config")
    suspend fun deleteAll()
}
