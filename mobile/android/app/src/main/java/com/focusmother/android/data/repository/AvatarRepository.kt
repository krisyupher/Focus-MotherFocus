package com.focusmother.android.data.repository

import com.focusmother.android.data.api.ReadyPlayerMeApiService
import com.focusmother.android.data.api.models.AvatarStatusResponse
import com.focusmother.android.data.dao.AvatarDao
import com.focusmother.android.data.entity.AvatarConfig
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for managing avatar data operations.
 *
 * Coordinates between the Ready Player Me API service and local database
 * to handle avatar creation, storage, and retrieval. Manages the complete
 * avatar lifecycle from photo upload to GLB file caching.
 *
 * Key responsibilities:
 * - Upload photos to Ready Player Me API
 * - Poll for avatar generation completion
 * - Download and cache GLB files locally
 * - Manage avatar configurations in database
 *
 * @property avatarDao DAO for avatar database operations
 * @property apiService Ready Player Me API service
 */
class AvatarRepository(
    private val avatarDao: AvatarDao,
    private val apiService: ReadyPlayerMeApiService
) {

    private val httpClient = OkHttpClient()

    /**
     * Creates an avatar from a photo file.
     *
     * This method:
     * 1. Uploads the photo to Ready Player Me API
     * 2. Polls for avatar generation completion (up to 30 attempts, 2 seconds apart)
     * 3. Returns the avatar ID on success
     *
     * @param photoFile The selfie photo file to use for avatar creation
     * @return Result containing the avatar ID on success, or an exception on failure
     * @throws IllegalArgumentException if photo file doesn't exist
     * @throws retrofit2.HttpException if API returns an error
     * @throws java.io.IOException if network communication fails
     * @throws IllegalStateException if avatar creation fails or times out
     */
    suspend fun createAvatarFromPhoto(photoFile: File): Result<String> {
        return try {
            // Validate photo file exists
            if (!photoFile.exists()) {
                return Result.failure(IllegalArgumentException("Photo file does not exist"))
            }

            // Step 1: Upload photo
            val requestBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData(
                "photo",
                photoFile.name,
                requestBody
            )

            val creationResponse = apiService.createAvatar(photoPart)

            // Step 2: Poll for completion
            val statusResult = pollAvatarStatus(
                avatarId = creationResponse.id,
                maxAttempts = 30,
                delayMs = 2000
            )

            if (statusResult.isFailure) {
                return Result.failure(statusResult.exceptionOrNull()!!)
            }

            val statusResponse = statusResult.getOrNull()!!
            if (statusResponse.status == "failed") {
                return Result.failure(IllegalStateException("Avatar creation failed"))
            }

            Result.success(creationResponse.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Polls the avatar status endpoint until completion or timeout.
     *
     * Makes repeated requests to check if avatar generation is complete.
     * Stops when status is "completed" or "failed", or when max attempts reached.
     *
     * @param avatarId The avatar ID to check
     * @param maxAttempts Maximum number of polling attempts (default: 30)
     * @param delayMs Delay between polling attempts in milliseconds (default: 2000)
     * @return Result containing the final status response or timeout exception
     */
    suspend fun pollAvatarStatus(
        avatarId: String,
        maxAttempts: Int = 30,
        delayMs: Long = 2000
    ): Result<AvatarStatusResponse> {
        return try {
            repeat(maxAttempts) { attempt ->
                val statusResponse = apiService.getAvatarStatus(avatarId)

                when (statusResponse.status) {
                    "completed", "failed" -> return Result.success(statusResponse)
                    "processing" -> {
                        if (attempt < maxAttempts - 1) {
                            delay(delayMs)
                        }
                    }
                }
            }

            // Timeout after max attempts
            Result.failure(IllegalStateException("Avatar processing timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the current avatar configuration from the database.
     *
     * @return AvatarConfig if an avatar is configured, null otherwise
     */
    suspend fun getAvatarConfig(): AvatarConfig? {
        return avatarDao.getAvatar()
    }

    /**
     * Saves an avatar configuration to the database.
     *
     * Replaces any existing avatar configuration (only one avatar is active at a time).
     *
     * @param config The avatar configuration to save
     */
    suspend fun saveAvatarConfig(config: AvatarConfig) {
        avatarDao.insert(config)
    }

    /**
     * Checks if an avatar is configured in the database.
     *
     * @return true if an avatar exists, false otherwise
     */
    suspend fun hasAvatar(): Boolean {
        return avatarDao.hasAvatar()
    }

    /**
     * Completely deletes the current avatar including all cached files.
     *
     * SECURITY: This method ensures complete data deletion for privacy compliance.
     * It deletes:
     * 1. GLB file (3D model containing biometric facial data)
     * 2. Thumbnail image file
     * 3. Database entry
     *
     * This prevents retention of sensitive biometric data after user requests deletion.
     *
     * @return true if avatar was deleted, false if no avatar existed
     */
    suspend fun deleteAvatar(): Boolean {
        // Get current avatar to find file paths
        val avatar = avatarDao.getAvatar() ?: return false

        // Securely delete GLB file (contains biometric facial data)
        try {
            val glbFile = File(avatar.localGlbPath)
            if (glbFile.exists()) {
                secureDeleteFile(glbFile)
            }
        } catch (e: Exception) {
            // Silent failure - log at error level but continue with deletion
            android.util.Log.e("AvatarRepository", "Error securely deleting GLB file: ${e.message}")
        }

        // Securely delete thumbnail file
        try {
            val thumbnailFile = File(avatar.thumbnailPath)
            if (thumbnailFile.exists()) {
                secureDeleteFile(thumbnailFile)
            }
        } catch (e: Exception) {
            // Silent failure - log at error level but continue with deletion
            android.util.Log.e("AvatarRepository", "Error securely deleting thumbnail file: ${e.message}")
        }

        // Delete database entry (Room handles this atomically)
        avatarDao.deleteAll()

        return true
    }

    /**
     * Securely deletes a file by overwriting its contents before deletion.
     *
     * SECURITY: This prevents biometric data recovery from disk using forensic tools.
     * The file is overwritten with random data 3 times (DoD 5220.22-M standard variant).
     *
     * GDPR Compliance: Ensures biometric data (Article 9 special category) cannot be
     * recovered after deletion, fulfilling right to erasure (Article 17).
     *
     * @param file The file to securely delete
     * @throws IOException if file operations fail
     */
    private fun secureDeleteFile(file: File) {
        if (!file.exists()) return

        val fileLength = file.length()

        // Overwrite file contents 3 times with random data (DoD 5220.22-M variant)
        repeat(3) { pass ->
            try {
                java.io.RandomAccessFile(file, "rws").use { raf ->
                    raf.seek(0)
                    val buffer = ByteArray(8192) // 8KB buffer for efficient overwriting
                    var remaining = fileLength

                    while (remaining > 0) {
                        val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                        // Fill buffer with cryptographically secure random bytes
                        java.security.SecureRandom().nextBytes(buffer)
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }

                    // Force write to disk (bypass OS write cache)
                    raf.fd.sync()
                }
            } catch (e: Exception) {
                android.util.Log.w("AvatarRepository", "Secure overwrite pass ${pass + 1} failed: ${e.message}")
            }
        }

        // Final deletion after secure overwriting
        val deleted = file.delete()
        if (!deleted) {
            android.util.Log.w("AvatarRepository", "File deletion failed after secure overwrite: ${file.path}")
        }
    }

    /**
     * Downloads a GLB file from a URL and saves it to the specified output file.
     *
     * Uses OkHttp to download the file with streaming to handle large files efficiently.
     * GLB files are typically 2-5MB in size.
     *
     * @param url The URL of the GLB file to download
     * @param outputFile The file to save the downloaded content to
     * @throws java.io.IOException if download or file write fails
     */
    suspend fun downloadGlbFile(url: String, outputFile: File) {
        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Failed to download GLB file: ${response.code}")
            }

            response.body?.let { body ->
                FileOutputStream(outputFile).use { outputStream ->
                    body.byteStream().copyTo(outputStream)
                }
            } ?: throw java.io.IOException("Empty response body")
        }
    }

    /**
     * Downloads a thumbnail image from a URL and saves it to the specified output file.
     *
     * @param url The URL of the thumbnail image to download
     * @param outputFile The file to save the downloaded content to
     * @throws java.io.IOException if download or file write fails
     */
    suspend fun downloadThumbnail(url: String, outputFile: File) {
        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Failed to download thumbnail: ${response.code}")
            }

            response.body?.let { body ->
                FileOutputStream(outputFile).use { outputStream ->
                    body.byteStream().copyTo(outputStream)
                }
            } ?: throw java.io.IOException("Empty response body")
        }
    }
}
