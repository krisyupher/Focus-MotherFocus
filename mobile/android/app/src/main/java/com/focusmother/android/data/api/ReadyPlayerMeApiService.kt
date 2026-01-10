package com.focusmother.android.data.api

import com.focusmother.android.data.api.models.AvatarCreationResponse
import com.focusmother.android.data.api.models.AvatarStatusResponse
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Retrofit service interface for Ready Player Me API.
 *
 * This service provides methods to create 3D avatars from photos using Ready Player Me's
 * AI-powered avatar generation. The avatar creation is asynchronous - you upload a photo,
 * receive an avatar ID, then poll for completion status.
 *
 * Authentication: Optional - can be used with or without auth token
 * Processing Time: Avatar creation typically takes 15-30 seconds
 * Documentation: https://docs.readyplayer.me/
 *
 * Example usage:
 * ```kotlin
 * val service = ReadyPlayerMeApiService.create()
 *
 * // Step 1: Upload photo
 * val photoPart = MultipartBody.Part.createFormData(
 *     "photo",
 *     "selfie.jpg",
 *     photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
 * )
 * val creationResponse = service.createAvatar(photoPart)
 *
 * // Step 2: Poll for completion
 * var status = service.getAvatarStatus(creationResponse.id)
 * while (status.status == "processing") {
 *     delay(2000)
 *     status = service.getAvatarStatus(creationResponse.id)
 * }
 *
 * // Step 3: Use avatar URLs
 * println("Avatar model: ${status.glbUrl}")
 * println("Thumbnail: ${status.thumbnailUrl}")
 * ```
 */
interface ReadyPlayerMeApiService {

    /**
     * Creates a new avatar from a photo.
     *
     * Uploads a selfie photo and initiates the avatar creation process. The response
     * contains an avatar ID that can be used to poll for completion status.
     *
     * @param photo Multipart photo file (JPEG or PNG recommended)
     * @param token Optional authorization token for authenticated requests
     * @return Creation response with avatar ID and initial status
     * @throws retrofit2.HttpException if the API returns an error response
     * @throws java.io.IOException if network communication fails
     */
    @Multipart
    @POST("api/users/me/avatars")
    suspend fun createAvatar(
        @Part photo: MultipartBody.Part,
        @Header("Authorization") token: String? = null
    ): AvatarCreationResponse

    /**
     * Gets the current status of an avatar creation job.
     *
     * Poll this endpoint to check if avatar processing is complete. When status is "completed",
     * the glbUrl and thumbnailUrl fields will be populated.
     *
     * @param avatarId The avatar ID received from createAvatar()
     * @return Status response with processing state and URLs (when complete)
     * @throws retrofit2.HttpException if the API returns an error response
     * @throws java.io.IOException if network communication fails
     */
    @GET("api/avatars/{avatarId}")
    suspend fun getAvatarStatus(
        @Path("avatarId") avatarId: String
    ): AvatarStatusResponse

    companion object {
        private const val BASE_URL = "https://api.readyplayer.me/"

        /**
         * Creates a configured instance of ReadyPlayerMeApiService.
         *
         * The service is configured with:
         * - 30 second connection timeout
         * - 120 second read timeout (avatar creation can take 15-30 seconds)
         * - 60 second write timeout (photo uploads can be slow)
         * - Gson for JSON serialization/deserialization
         *
         * @return Ready-to-use ReadyPlayerMeApiService instance
         */
        fun create(): ReadyPlayerMeApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // Avatar creation can take time
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ReadyPlayerMeApiService::class.java)
        }
    }
}
