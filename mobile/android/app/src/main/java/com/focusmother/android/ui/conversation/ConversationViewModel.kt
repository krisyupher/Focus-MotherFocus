package com.focusmother.android.ui.conversation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusmother.android.data.entity.ConversationMessage
import com.focusmother.android.data.repository.ConversationRepository
import com.focusmother.android.domain.ConversationContext
import kotlinx.coroutines.launch

/**
 * ViewModel for managing conversation state and interactions.
 *
 * Handles:
 * - Message sending and receiving
 * - Conversation state transitions
 * - Error handling and retry logic
 * - Loading conversation history
 *
 * @property repository Repository for conversation operations
 * @property conversationId ID of the current conversation
 * @property context Current usage context for building prompts
 */
class ConversationViewModel(
    private val repository: ConversationRepository,
    private val conversationId: Long,
    private val context: ConversationContext
) : ViewModel() {

    private val _messages = MutableLiveData<List<ConversationMessage>>(emptyList())
    val messages: LiveData<List<ConversationMessage>> = _messages

    private val _conversationState = MutableLiveData<ConversationState>(ConversationState.Idle)
    val conversationState: LiveData<ConversationState> = _conversationState

    private var lastMessage: String? = null

    init {
        loadHistory()
    }

    /**
     * Loads conversation history from repository.
     */
    fun loadHistory() {
        viewModelScope.launch {
            try {
                val history = repository.getConversationHistory(conversationId)
                _messages.value = history
            } catch (e: Exception) {
                // Silently fail for history loading - not critical
                _messages.value = emptyList()
            }
        }
    }

    /**
     * Sends a user message to Claude AI.
     *
     * State transitions:
     * - Idle -> WaitingForResponse
     * - WaitingForResponse -> ShowingResponse (on success)
     * - WaitingForResponse -> Error (on failure)
     *
     * @param message User's message
     */
    fun sendMessage(message: String) {
        // Validate message
        if (message.isBlank()) {
            return
        }

        viewModelScope.launch {
            try {
                // Store for retry
                lastMessage = message

                // Add user message to UI immediately (optimistic update)
                val userMessage = ConversationMessage.userMessage(
                    conversationId = conversationId,
                    content = message
                )
                _messages.value = _messages.value.orEmpty() + userMessage

                // Update state to waiting
                _conversationState.value = ConversationState.WaitingForResponse

                // Send message to repository (it will save user message to database)
                val result = repository.sendMessage(
                    conversationId = conversationId,
                    message = message,
                    context = context
                )

                // Handle result
                result.fold(
                    onSuccess = { response ->
                        // Reload conversation history to show both user and AI messages from database
                        loadHistory()

                        // Update state to showing response
                        _conversationState.value = ConversationState.ShowingResponse(response)
                    },
                    onFailure = { error ->
                        // Update state to error
                        val errorMessage = when (error) {
                            is java.io.IOException -> "Network error. Please check your connection and try again."
                            is retrofit2.HttpException -> when (error.code()) {
                                401 -> "Authentication error. Please check your API key."
                                429 -> "Rate limit exceeded. Please wait a moment and try again."
                                else -> "API error: ${error.message()}"
                            }
                            else -> "An error occurred: ${error.message ?: "Unknown error"}"
                        }
                        _conversationState.value = ConversationState.Error(errorMessage)
                    }
                )
            } catch (e: Exception) {
                _conversationState.value = ConversationState.Error(
                    "An unexpected error occurred: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    /**
     * Selects a quick reply option.
     *
     * Quick replies are pre-defined responses like "I agree", "5 more minutes", etc.
     *
     * @param reply The quick reply text
     */
    fun selectQuickReply(reply: String) {
        sendMessage(reply)
    }

    /**
     * Retries sending the last message after an error.
     */
    fun retry() {
        lastMessage?.let { message ->
            sendMessage(message)
        }
    }

    /**
     * Resets conversation state to Idle.
     *
     * Used after showing a response to prepare for next message.
     */
    fun resetToIdle() {
        _conversationState.value = ConversationState.Idle
    }
}

/**
 * Represents the current state of the conversation UI.
 */
sealed class ConversationState {
    /**
     * Idle state - waiting for user input.
     */
    data object Idle : ConversationState()

    /**
     * User is actively typing (optional - can be used by UI).
     */
    data object Typing : ConversationState()

    /**
     * Waiting for Claude AI response.
     */
    data object WaitingForResponse : ConversationState()

    /**
     * Showing Claude's response.
     *
     * @property message The AI's response message
     */
    data class ShowingResponse(val message: String) : ConversationState()

    /**
     * Error occurred during message sending.
     *
     * @property message Error message to display
     */
    data class Error(val message: String) : ConversationState()
}
