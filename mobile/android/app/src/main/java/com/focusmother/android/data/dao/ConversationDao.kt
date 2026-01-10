package com.focusmother.android.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.focusmother.android.data.entity.ConversationMessage

/**
 * Data Access Object for ConversationMessage operations.
 *
 * Manages conversation history and message storage for AI interactions.
 * Supports context retrieval and conversation pruning.
 */
@Dao
interface ConversationDao {
    
    /**
     * Retrieves all messages for a specific conversation.
     *
     * @param convId Conversation ID to retrieve
     * @return List of messages ordered chronologically (oldest first)
     */
    @Query("SELECT * FROM conversation_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getConversation(convId: Long): List<ConversationMessage>
    
    /**
     * Retrieves the most recent messages across all conversations.
     *
     * @param limit Maximum number of messages to return
     * @return List of recent messages ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ConversationMessage>
    
    /**
     * Retrieves all messages (for testing and debugging).
     *
     * @return All messages ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC")
    suspend fun getAll(): List<ConversationMessage>
    
    /**
     * Inserts one or more conversation messages.
     *
     * @param messages Messages to insert
     */
    @Insert
    suspend fun insert(vararg messages: ConversationMessage)
    
    /**
     * Deletes messages older than a specific timestamp.
     *
     * Used for conversation history cleanup and privacy management.
     *
     * @param cutoffTime Messages older than this timestamp will be deleted
     * @return Number of messages deleted
     */
    @Query("DELETE FROM conversation_messages WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
    
    /**
     * Counts total messages in a conversation.
     *
     * @param convId Conversation ID
     * @return Number of messages in the conversation
     */
    @Query("SELECT COUNT(*) FROM conversation_messages WHERE conversationId = :convId")
    suspend fun getMessageCount(convId: Long): Int
    
    /**
     * Retrieves total token usage across all messages.
     *
     * Useful for cost tracking and API usage monitoring.
     *
     * @return Sum of all token counts
     */
    @Query("SELECT SUM(tokenCount) FROM conversation_messages")
    suspend fun getTotalTokenUsage(): Int?
    
    /**
     * Deletes all messages (for testing purposes).
     */
    @Query("DELETE FROM conversation_messages")
    suspend fun deleteAll()
}
