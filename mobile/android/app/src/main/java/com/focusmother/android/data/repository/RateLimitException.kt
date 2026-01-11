package com.focusmother.android.data.repository

/**
 * Exception thrown when API rate limiting is triggered.
 *
 * SECURITY: Rate limiting prevents:
 * - API quota exhaustion
 * - Unexpected API costs (could reach $100+ if abused)
 * - Service denial from Anthropic rate limits
 * - Malicious abuse by compromised apps
 *
 * @property message Detailed rate limit message for user
 */
class RateLimitException(message: String) : Exception(message)
