package com.focusmother.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single message in a conversation with the AI avatar.
 *
 * Stores both user and assistant messages for conversation context and history tracking.
 * Tracks token usage for cost management and includes model information for debugging.
 *
 * @property id Unique identifier for the message
 * @property conversationId Foreign key linking messages to a conversation thread
 * @property role Message role: "user" or "assistant"
 * @property content The actual message text
 * @property timestamp When the message was created
 * @property tokenCount Number of tokens used (for cost tracking and API management)
 * @property modelUsed The Claude model used to generate the response
 */
@Entity(tableName = "conversation_messages")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val conversationId: Long,
    
    val role: String,
    
    val content: String,
    
    val timestamp: Long,
    
    val tokenCount: Int = 0,
    
    val modelUsed: String = "claude-3-5-sonnet-20241022"
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        
        /**
         * Creates a user message.
         */
        fun userMessage(conversationId: Long, content: String): ConversationMessage {
            return ConversationMessage(
                conversationId = conversationId,
                role = ROLE_USER,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        }
        
        /**
         * Creates an assistant message.
         */
        fun assistantMessage(
            conversationId: Long,
            content: String,
            tokenCount: Int = 0,
            modelUsed: String = "claude-3-5-sonnet-20241022"
        ): ConversationMessage {
            return ConversationMessage(
                conversationId = conversationId,
                role = ROLE_ASSISTANT,
                content = content,
                timestamp = System.currentTimeMillis(),
                tokenCount = tokenCount,
                modelUsed = modelUsed
            )
        }
    }
}
