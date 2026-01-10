package com.focusmother.android.data.api.models

/**
 * Data models for Claude API integration.
 *
 * These models represent the request and response structures for the Claude Messages API.
 * API Documentation: https://docs.anthropic.com/claude/reference/messages
 */

/**
 * Request payload for Claude Messages API.
 *
 * @param model The Claude model to use (default: claude-3-5-sonnet-20241022)
 * @param max_tokens Maximum tokens to generate in response
 * @param messages List of conversation messages
 * @param system Optional system message to set context
 * @param temperature Controls randomness (0.0 = deterministic, 1.0 = creative)
 * @param stream Whether to stream the response (not supported in this implementation)
 */
data class ClaudeMessageRequest(
    val model: String = "claude-3-5-sonnet-20241022",
    val max_tokens: Int = 1024,
    val messages: List<ClaudeMessage>,
    val system: String? = null,
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

/**
 * Individual message in a Claude conversation.
 *
 * @param role Either "user" or "assistant"
 * @param content The message content
 */
data class ClaudeMessage(
    val role: String,  // "user" or "assistant"
    val content: String
)

/**
 * Response from Claude Messages API.
 *
 * @param id Unique identifier for this response
 * @param type Response type (always "message")
 * @param role Role of the responder (always "assistant")
 * @param content List of content blocks in the response
 * @param model The model that generated the response
 * @param stop_reason Why the model stopped generating ("end_turn", "max_tokens", etc.)
 * @param usage Token usage statistics
 */
data class ClaudeMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    val stop_reason: String?,
    val usage: ClaudeUsage
)

/**
 * Content block in a Claude response.
 *
 * @param type Content type (usually "text")
 * @param text The actual text content
 */
data class ClaudeContent(
    val type: String,
    val text: String
)

/**
 * Token usage statistics from Claude API.
 *
 * @param input_tokens Number of tokens in the input
 * @param output_tokens Number of tokens generated in the output
 */
data class ClaudeUsage(
    val input_tokens: Int,
    val output_tokens: Int
)

/**
 * Helper extension to extract text content from Claude response.
 *
 * @return The text from the first text content block, or empty string if none found
 */
fun ClaudeMessageResponse.getTextContent(): String {
    return content.firstOrNull { it.type == "text" }?.text ?: ""
}
