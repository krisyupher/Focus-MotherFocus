package com.focusmother.android.data.api

import com.focusmother.android.data.api.models.ClaudeMessageRequest
import com.focusmother.android.data.api.models.ClaudeMessageResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Retrofit service interface for Claude AI API.
 *
 * This service provides methods to interact with Anthropic's Claude API for generating
 * AI responses to user messages. The API uses a conversational format with message history.
 *
 * Authentication: Requires an API key passed via "x-api-key" header
 * Rate Limits: Subject to Anthropic's rate limiting policies
 * Documentation: https://docs.anthropic.com/claude/reference/messages
 *
 * Example usage:
 * ```kotlin
 * val service = ClaudeApiService.create()
 * val request = ClaudeMessageRequest(
 *     messages = listOf(ClaudeMessage(role = "user", content = "Hello, Claude!"))
 * )
 * val response = service.sendMessage(apiKey = "your-api-key", request = request)
 * println(response.getTextContent())
 * ```
 */
interface ClaudeApiService {

    /**
     * Sends a message to Claude and receives a response.
     *
     * This is the primary endpoint for conversational AI interactions. It supports
     * multi-turn conversations by including previous messages in the request.
     *
     * @param apiKey Your Anthropic API key (passed as "x-api-key" header)
     * @param request The message request containing conversation history and parameters
     * @return Claude's response including generated text and usage statistics
     * @throws retrofit2.HttpException if the API returns an error response
     * @throws java.io.IOException if network communication fails
     */
    @POST("v1/messages")
    @Headers("anthropic-version: 2023-06-01")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Body request: ClaudeMessageRequest
    ): ClaudeMessageResponse

    companion object {
        private const val BASE_URL = "https://api.anthropic.com/"

        /**
         * Creates a configured instance of ClaudeApiService.
         *
         * The service is configured with:
         * - 30 second connection timeout
         * - 60 second read timeout (Claude can take time to generate responses)
         * - 30 second write timeout
         * - Gson for JSON serialization/deserialization
         *
         * @return Ready-to-use ClaudeApiService instance
         */
        fun create(): ClaudeApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ClaudeApiService::class.java)
        }
    }
}
