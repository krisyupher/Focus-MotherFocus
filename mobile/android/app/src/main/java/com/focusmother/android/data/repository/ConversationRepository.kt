package com.focusmother.android.data.repository

import com.focusmother.android.data.api.ClaudeApiService
import com.focusmother.android.data.api.models.ClaudeMessage
import com.focusmother.android.data.api.models.ClaudeMessageRequest
import com.focusmother.android.data.api.models.getTextContent
import com.focusmother.android.data.dao.ConversationDao
import com.focusmother.android.data.entity.ConversationMessage
import com.focusmother.android.domain.ConversationContext
import com.focusmother.android.domain.PromptBuilder
import com.focusmother.android.util.SecureApiKeyProvider
import java.util.concurrent.atomic.AtomicLong

/**
 * Repository for managing conversations with Claude AI.
 *
 * Handles:
 * - Sending messages to Claude API
 * - Saving messages to local database
 * - Retrieving conversation history
 * - Building system prompts with context
 * - Error handling for network and API failures
 *
 * @property conversationDao Database access for message persistence
 * @property claudeApiService Retrofit service for Claude API
 * @property apiKeyProvider Secure API key retrieval
 * @property promptBuilder Builds system prompts with Zordon personality
 */
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val claudeApiService: ClaudeApiService,
    private val apiKeyProvider: SecureApiKeyProvider,
    private val promptBuilder: PromptBuilder
) {

    // SECURITY: Rate limiting to prevent API abuse and cost exploitation
    private val lastRequestTime = AtomicLong(0)

    companion object {
        // SECURITY: Rate limiting constants
        // Minimum interval between API requests (1 second)
        // Prevents rapid-fire requests that could:
        // - Exhaust user's API quota
        // - Generate unexpected costs ($100+ if abused)
        // - Trigger Anthropic's rate limits
        private const val MIN_REQUEST_INTERVAL_MS = 1000L

        // Maximum requests per hour (additional safety layer)
        private const val MAX_REQUESTS_PER_HOUR = 100

        // Track recent request timestamps for hourly limit
        private val recentRequests = mutableListOf<Long>()

        // PERFORMANCE: Conversation history pruning constants
        // Keep messages for 30 days (30 * 24 * 60 * 60 * 1000)
        private const val MESSAGE_RETENTION_PERIOD_MS = 30L * 24 * 60 * 60 * 1000

        // Auto-prune after every 10 messages sent (to avoid excessive DB operations)
        private const val AUTO_PRUNE_INTERVAL = 10

        // Track message count for auto-pruning
        private var messagesSinceLastPrune = 0
    }

    /**
     * Sends a message to Claude AI and returns the response.
     *
     * Process:
     * 1. Retrieve conversation history from database
     * 2. Build system prompt with current context
     * 3. Construct API request with history and new message
     * 4. Send to Claude API
     * 5. Save both user message and AI response to database
     * 6. Return AI response
     *
     * @param conversationId ID of the conversation thread
     * @param message User's message
     * @param context Current usage context (screen time, app, etc.)
     * @return Result containing AI response or error
     */
    suspend fun sendMessage(
        conversationId: Long,
        message: String,
        context: ConversationContext
    ): Result<String> {
        return try {
            // SECURITY: Rate limiting check
            val now = System.currentTimeMillis()
            val timeSinceLastRequest = now - lastRequestTime.get()

            // Check minimum interval between requests
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                val waitTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest
                return Result.failure(
                    RateLimitException(
                        "Request throttled. Please wait ${waitTime}ms before sending another message."
                    )
                )
            }

            // Check hourly request limit
            synchronized(recentRequests) {
                // Remove requests older than 1 hour
                val oneHourAgo = now - (60 * 60 * 1000)
                recentRequests.removeAll { it < oneHourAgo }

                // Check if at limit
                if (recentRequests.size >= MAX_REQUESTS_PER_HOUR) {
                    return Result.failure(
                        RateLimitException(
                            "Hourly request limit reached ($MAX_REQUESTS_PER_HOUR requests/hour). Please try again later."
                        )
                    )
                }

                // Record this request
                recentRequests.add(now)
            }

            // Update last request time
            lastRequestTime.set(now)

            // Get API key
            val apiKey = apiKeyProvider.getApiKey()
                ?: return Result.failure(IllegalStateException("API key not found"))

            // Save user message to database FIRST (before API call)
            // This ensures the message appears immediately in the UI
            val userMessage = ConversationMessage.userMessage(
                conversationId = conversationId,
                content = message
            )
            conversationDao.insert(userMessage)

            // Get conversation history (last 10 messages to keep context manageable)
            val history = conversationDao.getConversation(conversationId)
                .takeLast(10)

            // Build system prompt with current context
            // PERFORMANCE: Use prompt caching to reduce API costs
            val systemPromptText = promptBuilder.buildSystemPrompt(context)
            val systemPrompt = listOf(
                com.focusmother.android.data.api.models.SystemBlock(
                    text = systemPromptText,
                    cache_control = com.focusmother.android.data.api.models.CacheControl()
                )
            )

            // Convert history to Claude message format
            val historyMessages = history.map { msg ->
                ClaudeMessage(
                    role = msg.role,
                    content = msg.content
                )
            }

            // Add new user message
            val allMessages = historyMessages + ClaudeMessage(
                role = ConversationMessage.ROLE_USER,
                content = message
            )

            // Build API request
            val request = ClaudeMessageRequest(
                model = "claude-3-5-sonnet-20241022",
                max_tokens = 300, // Short responses
                messages = allMessages,
                system = systemPrompt,
                temperature = 0.7, // Balanced creativity
                stream = false
            )

            // Call Claude API
            val response = claudeApiService.sendMessage(apiKey, request)

            // Extract text from response
            val aiResponse = response.getTextContent()

            // Save AI response to database
            val assistantMessage = ConversationMessage.assistantMessage(
                conversationId = conversationId,
                content = aiResponse,
                tokenCount = response.usage.output_tokens,
                modelUsed = response.model
            )
            conversationDao.insert(assistantMessage)

            // PERFORMANCE: Auto-prune old messages periodically
            messagesSinceLastPrune++
            if (messagesSinceLastPrune >= AUTO_PRUNE_INTERVAL) {
                pruneOldMessages()
                messagesSinceLastPrune = 0
            }

            // Return success
            Result.success(aiResponse)
        } catch (e: Exception) {
            // Return failure with exception
            Result.failure(e)
        }
    }

    /**
     * Retrieves conversation history from database.
     *
     * @param conversationId ID of the conversation
     * @return List of messages ordered chronologically
     */
    suspend fun getConversationHistory(conversationId: Long): List<ConversationMessage> {
        return conversationDao.getConversation(conversationId)
    }

    /**
     * Prunes old conversation messages to prevent database bloat.
     *
     * PERFORMANCE: Automatically deletes messages older than MESSAGE_RETENTION_PERIOD_MS (30 days).
     * This prevents the database from growing indefinitely and improves query performance.
     *
     * PRIVACY: Also serves as automatic data deletion for privacy compliance.
     *
     * @return Number of messages deleted
     */
    suspend fun pruneOldMessages(): Int {
        val cutoffTime = System.currentTimeMillis() - MESSAGE_RETENTION_PERIOD_MS
        return conversationDao.deleteOlderThan(cutoffTime)
    }

    /**
     * Manually triggers conversation history pruning.
     *
     * Useful for settings UI where users can clean up old data.
     *
     * @param daysToKeep Number of days of history to retain (default: 30)
     * @return Number of messages deleted
     */
    suspend fun pruneMessages(daysToKeep: Int = 30): Int {
        val retentionPeriod = daysToKeep.toLong() * 24 * 60 * 60 * 1000
        val cutoffTime = System.currentTimeMillis() - retentionPeriod
        return conversationDao.deleteOlderThan(cutoffTime)
    }

    /**
     * Gets total token usage across all messages.
     *
     * Useful for cost tracking and API usage monitoring.
     *
     * @return Total tokens used, or 0 if no messages
     */
    suspend fun getTotalTokenUsage(): Int {
        return conversationDao.getTotalTokenUsage() ?: 0
    }

    /**
     * Clears all conversation history from database.
     *
     * PRIVACY: Used for complete data deletion when user requests it.
     */
    suspend fun clearHistory() {
        conversationDao.deleteAll()
    }
}
