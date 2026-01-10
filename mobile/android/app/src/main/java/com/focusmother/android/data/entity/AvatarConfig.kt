package com.focusmother.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the current 3D avatar configuration for the AI assistant.
 *
 * The avatar is created via Ready Player Me API and stored locally as a GLB file.
 * This entity tracks the avatar URL, local file paths, and customization metadata.
 *
 * @property id Primary key (always 1, since only one avatar is active)
 * @property avatarUrl Ready Player Me URL for the avatar
 * @property localGlbPath Local file system path to the downloaded GLB file
 * @property thumbnailPath Local path to the avatar thumbnail/preview image
 * @property createdAt Timestamp when avatar was first created
 * @property lastModified Timestamp when avatar was last updated
 * @property readyPlayerMeId Unique identifier from Ready Player Me API
 * @property customizationData JSON metadata containing avatar customization details
 */
@Entity(tableName = "avatar_config")
data class AvatarConfig(
    @PrimaryKey
    val id: Int = 1,
    
    val avatarUrl: String,
    
    val localGlbPath: String,
    
    val thumbnailPath: String,
    
    val createdAt: Long,
    
    val lastModified: Long,
    
    val readyPlayerMeId: String,
    
    val customizationData: String
) {
    companion object {
        /**
         * Creates a new avatar configuration.
         */
        fun create(
            avatarUrl: String,
            localGlbPath: String,
            thumbnailPath: String,
            readyPlayerMeId: String,
            customizationData: String = "{}"
        ): AvatarConfig {
            val now = System.currentTimeMillis()
            return AvatarConfig(
                avatarUrl = avatarUrl,
                localGlbPath = localGlbPath,
                thumbnailPath = thumbnailPath,
                createdAt = now,
                lastModified = now,
                readyPlayerMeId = readyPlayerMeId,
                customizationData = customizationData
            )
        }
    }
}
