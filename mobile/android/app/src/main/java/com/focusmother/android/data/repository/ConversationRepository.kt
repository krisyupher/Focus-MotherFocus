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
            // Get API key
            val apiKey = apiKeyProvider.getApiKey()
                ?: return Result.failure(IllegalStateException("API key not found"))

            // Get conversation history (last 10 messages to keep context manageable)
            val history = conversationDao.getConversation(conversationId)
                .takeLast(10)

            // Build system prompt with current context
            val systemPrompt = promptBuilder.buildSystemPrompt(context)

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

            // Save user message to database
            val userMessage = ConversationMessage.userMessage(
                conversationId = conversationId,
                content = message
            )
            conversationDao.insert(userMessage)

            // Save AI response to database
            val assistantMessage = ConversationMessage.assistantMessage(
                conversationId = conversationId,
                content = aiResponse,
                tokenCount = response.usage.output_tokens,
                modelUsed = response.model
            )
            conversationDao.insert(assistantMessage)

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
     * Clears all conversation history from database.
     *
     * Used for testing and privacy purposes.
     */
    suspend fun clearHistory() {
        conversationDao.deleteAll()
    }
}
