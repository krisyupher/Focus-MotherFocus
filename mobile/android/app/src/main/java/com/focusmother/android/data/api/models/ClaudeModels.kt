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
 * PERFORMANCE: Supports prompt caching to reduce API costs.
 * - System prompts can be cached for repeated conversations
 * - Reduces input tokens charged by ~90% for cached content
 * - Cache persists for 5 minutes
 *
 * @param model The Claude model to use (default: claude-3-5-sonnet-20241022)
 * @param max_tokens Maximum tokens to generate in response
 * @param messages List of conversation messages
 * @param system Optional system message to set context (can include cache_control)
 * @param temperature Controls randomness (0.0 = deterministic, 1.0 = creative)
 * @param stream Whether to stream the response (not supported in this implementation)
 */
data class ClaudeMessageRequest(
    val model: String = "claude-3-5-sonnet-20241022",
    val max_tokens: Int = 1024,
    val messages: List<ClaudeMessage>,
    val system: Any? = null,  // Can be String or List<SystemBlock> for caching
    val temperature: Double = 0.7,
    val stream: Boolean = false
)

/**
 * System block with cache control for prompt caching.
 *
 * PERFORMANCE: Use this to cache system prompts and reduce API costs.
 *
 * @param type Block type (always "text")
 * @param text The system prompt text
 * @param cache_control Cache control directive (optional)
 */
data class SystemBlock(
    val type: String = "text",
    val text: String,
    val cache_control: CacheControl? = null
)

/**
 * Cache control directive for prompt caching.
 *
 * @param type Cache type (always "ephemeral" for 5-minute cache)
 */
data class CacheControl(
    val type: String = "ephemeral"
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
 * PERFORMANCE: Includes prompt caching metrics.
 *
 * @param input_tokens Number of tokens in the input
 * @param output_tokens Number of tokens generated in the output
 * @param cache_creation_input_tokens Tokens used to create cache (only on first request)
 * @param cache_read_input_tokens Tokens read from cache (significantly cheaper)
 */
data class ClaudeUsage(
    val input_tokens: Int,
    val output_tokens: Int,
    val cache_creation_input_tokens: Int? = null,
    val cache_read_input_tokens: Int? = null
)

/**
 * Helper extension to extract text content from Claude response.
 *
 * @return The text from the first text content block, or empty string if none found
 */
fun ClaudeMessageResponse.getTextContent(): String {
    return content.firstOrNull { it.type == "text" }?.text ?: ""
}
