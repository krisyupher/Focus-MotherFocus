package com.focusmother.android.data.api.models

/**
 * Data models for Ready Player Me API integration.
 *
 * These models represent the request and response structures for Ready Player Me avatar creation.
 * API Documentation: https://docs.readyplayer.me/
 */

/**
 * Response when uploading a photo to create an avatar.
 *
 * @param id Unique identifier for the avatar creation job
 * @param status Current status: "processing", "completed", or "failed"
 * @param url URL to the avatar model (available when status is "completed")
 */
data class AvatarCreationResponse(
    val id: String,
    val status: String,  // "processing", "completed", "failed"
    val url: String?      // Available when status is "completed"
)

/**
 * Response when polling for avatar creation status.
 *
 * @param id Avatar identifier
 * @param status Current processing status
 * @param glbUrl URL to the GLB (3D model) file when completed
 * @param thumbnailUrl URL to avatar thumbnail image when completed
 */
data class AvatarStatusResponse(
    val id: String,
    val status: String,
    val glbUrl: String?,
    val thumbnailUrl: String?
)

/**
 * Configuration for a completed avatar.
 *
 * Stores the essential information needed to display and use the avatar.
 *
 * @param avatarId Unique identifier for this avatar
 * @param modelUrl URL to the 3D model file (GLB format)
 * @param thumbnailUrl URL to the thumbnail image
 * @param createdAt Timestamp when avatar was created
 */
data class AvatarConfig(
    val avatarId: String,
    val modelUrl: String,
    val thumbnailUrl: String,
    val createdAt: Long = System.currentTimeMillis()
)
