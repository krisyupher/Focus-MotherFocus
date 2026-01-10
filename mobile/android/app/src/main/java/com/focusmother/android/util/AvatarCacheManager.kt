package com.focusmother.android.util

import android.content.Context
import java.io.File

/**
 * Manages local file caching for avatar GLB files and thumbnails.
 *
 * Provides utilities for:
 * - Creating and accessing the avatar cache directory
 * - Generating file paths for avatar models and thumbnails
 * - Checking cache existence
 * - Clearing cached files
 *
 * The cache directory is located at: `{filesDir}/avatars/`
 * - GLB files are stored as: `{avatarId}.glb`
 * - Thumbnails are stored as: `{avatarId}_thumbnail.png`
 *
 * Example usage:
 * ```kotlin
 * val cacheManager = AvatarCacheManager
 * val avatarFile = cacheManager.getAvatarFile(context, "avatar-123")
 * if (cacheManager.cacheExists(context, "avatar-123")) {
 *     // Load from cache
 * }
 * ```
 */
object AvatarCacheManager {

    private const val CACHE_DIRECTORY_NAME = "avatars"
    private const val GLB_EXTENSION = ".glb"
    private const val THUMBNAIL_EXTENSION = "_thumbnail.png"

    /**
     * Gets the avatar cache directory.
     *
     * Creates the directory if it doesn't exist. The directory is located
     * in the app's internal storage at `{filesDir}/avatars/`.
     *
     * @param context Android context for accessing file system
     * @return The cache directory File object
     */
    fun getCacheDir(context: Context): File {
        val cacheDir = context.filesDir.resolve(CACHE_DIRECTORY_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Gets the file path for a specific avatar's GLB model.
     *
     * @param context Android context for accessing file system
     * @param avatarId Unique identifier for the avatar
     * @return File object representing the GLB file path
     */
    fun getAvatarFile(context: Context, avatarId: String): File {
        return getCacheDir(context).resolve("$avatarId$GLB_EXTENSION")
    }

    /**
     * Gets the file path for a specific avatar's thumbnail image.
     *
     * @param context Android context for accessing file system
     * @param avatarId Unique identifier for the avatar
     * @return File object representing the thumbnail file path
     */
    fun getThumbnailFile(context: Context, avatarId: String): File {
        return getCacheDir(context).resolve("$avatarId$THUMBNAIL_EXTENSION")
    }

    /**
     * Checks if the GLB file for a given avatar exists in cache.
     *
     * @param context Android context for accessing file system
     * @param avatarId Unique identifier for the avatar
     * @return true if the avatar file exists, false otherwise
     */
    fun cacheExists(context: Context, avatarId: String): Boolean {
        return getAvatarFile(context, avatarId).exists()
    }

    /**
     * Deletes all files in the avatar cache directory.
     *
     * This removes all cached GLB files and thumbnails but preserves
     * the cache directory itself. Useful for clearing storage or
     * resetting avatar data.
     *
     * @param context Android context for accessing file system
     */
    fun clearCache(context: Context) {
        val cacheDir = getCacheDir(context)
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    /**
     * Gets the size of all cached avatar files in bytes.
     *
     * @param context Android context for accessing file system
     * @return Total size of cached files in bytes
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = getCacheDir(context)
        return cacheDir.listFiles()?.sumOf { file ->
            if (file.isFile) file.length() else 0L
        } ?: 0L
    }
}
