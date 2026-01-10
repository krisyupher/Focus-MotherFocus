package com.focusmother.android.data.repository

import com.focusmother.android.data.api.ClaudeApiService
import com.focusmother.android.data.api.models.ClaudeContent
import com.focusmother.android.data.api.models.ClaudeMessage
import com.focusmother.android.data.api.models.ClaudeMessageResponse
import com.focusmother.android.data.api.models.ClaudeUsage
import com.focusmother.android.data.dao.ConversationDao
import com.focusmother.android.data.entity.ConversationMessage
import com.focusmother.android.domain.ConversationContext
import com.focusmother.android.domain.PromptBuilder
import com.focusmother.android.util.SecureApiKeyProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

/**
 * Unit tests for ConversationRepository.
 *
 * Tests conversation management including:
 * - Sending messages to Claude API
 * - Saving messages to database
 * - Retrieving conversation history
 * - Error handling (network, API, auth)
 * - System prompt construction
 */
class ConversationRepositoryTest {

    private lateinit var repository: ConversationRepository
    private lateinit var mockConversationDao: ConversationDao
    private lateinit var mockClaudeApiService: ClaudeApiService
    private lateinit var mockApiKeyProvider: SecureApiKeyProvider
    private lateinit var mockPromptBuilder: PromptBuilder

    private val testApiKey = "test-api-key-12345"
    private val testConversationId = 1L

    @Before
    fun setup() {
        mockConversationDao = mockk(relaxed = true)
        mockClaudeApiService = mockk()
        mockApiKeyProvider = mockk()
        mockPromptBuilder = mockk()

        every { mockApiKeyProvider.getApiKey() } returns testApiKey

        repository = ConversationRepository(
            conversationDao = mockConversationDao,
            claudeApiService = mockClaudeApiService,
            apiKeyProvider = mockApiKeyProvider,
            promptBuilder = mockPromptBuilder
        )
    }

    @Test
    fun `sendMessage successfully returns AI response`() = runTest {
        // Arrange
        val userMessage = "I need to check Instagram for work"
        val context = createTestContext()
        val systemPrompt = "You are Zordon..."
        val aiResponse = "Young warrior, what task brings you to Instagram?"

        every { mockPromptBuilder.buildSystemPrompt(context) } returns systemPrompt

        val mockResponse = ClaudeMessageResponse(
            id = "msg_123",
            type = "message",
            role = "assistant",
            content = listOf(ClaudeContent("text", aiResponse)),
            model = "claude-3-5-sonnet-20241022",
            stop_reason = "end_turn",
            usage = ClaudeUsage(input_tokens = 100, output_tokens = 50)
        )

        coEvery {
            mockClaudeApiService.sendMessage(any(), any())
        } returns mockResponse

        coEvery { mockConversationDao.getConversation(testConversationId) } returns emptyList()

        // Act
        val result = repository.sendMessage(
            conversationId = testConversationId,
            message = userMessage,
            context = context
        )

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(aiResponse, result.getOrNull())
    }

    @Test
    fun `sendMessage saves user message to database`() = runTest {
        // Arrange
        val userMessage = "Test message"
        val context = createTestContext()
        val messageSlots = mutableListOf<ConversationMessage>()

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockConversationDao.insert(capture(messageSlots)) } returns Unit

        // Act
        repository.sendMessage(testConversationId, userMessage, context)

        // Assert
        coVerify(exactly = 2) { mockConversationDao.insert(any()) }
        assertEquals(2, messageSlots.size)

        // First message should be the user message
        val userMsg = messageSlots[0]
        assertEquals(ConversationMessage.ROLE_USER, userMsg.role)
        assertEquals(userMessage, userMsg.content)
        assertEquals(testConversationId, userMsg.conversationId)

        // Second message should be the assistant message
        val assistantMsg = messageSlots[1]
        assertEquals(ConversationMessage.ROLE_ASSISTANT, assistantMsg.role)
        assertEquals(testConversationId, assistantMsg.conversationId)
    }

    @Test
    fun `sendMessage saves assistant response to database`() = runTest {
        // Arrange
        val userMessage = "Test message"
        val context = createTestContext()
        val aiResponse = "Test response"
        val messageSlot = mutableListOf<ConversationMessage>()

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse(aiResponse)
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockConversationDao.insert(capture(messageSlot)) } returns Unit

        // Act
        repository.sendMessage(testConversationId, userMessage, context)

        // Assert
        coVerify(exactly = 2) { mockConversationDao.insert(any()) }
        assertTrue(messageSlot.size == 2)

        val assistantMessage = messageSlot[1]
        assertEquals(ConversationMessage.ROLE_ASSISTANT, assistantMessage.role)
        assertEquals(aiResponse, assistantMessage.content)
        assertEquals(testConversationId, assistantMessage.conversationId)
    }

    @Test
    fun `sendMessage includes conversation history in API request`() = runTest {
        // Arrange
        val existingMessages = listOf(
            ConversationMessage.userMessage(testConversationId, "Previous message 1"),
            ConversationMessage.assistantMessage(testConversationId, "Previous response 1"),
            ConversationMessage.userMessage(testConversationId, "Previous message 2"),
            ConversationMessage.assistantMessage(testConversationId, "Previous response 2")
        )
        val newMessage = "New message"
        val context = createTestContext()

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(testConversationId) } returns existingMessages
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()

        // Act
        repository.sendMessage(testConversationId, newMessage, context)

        // Assert
        coVerify {
            mockClaudeApiService.sendMessage(
                apiKey = testApiKey,
                request = match { request ->
                    // Should include last 10 messages plus the new one
                    request.messages.size == 5 // 4 history + 1 new
                }
            )
        }
    }

    @Test
    fun `sendMessage limits conversation history to last 10 messages`() = runTest {
        // Arrange
        val manyMessages = (1..15).flatMap { i ->
            listOf(
                ConversationMessage.userMessage(testConversationId, "User $i"),
                ConversationMessage.assistantMessage(testConversationId, "Assistant $i")
            )
        }
        val context = createTestContext()

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(testConversationId) } returns manyMessages
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()

        // Act
        repository.sendMessage(testConversationId, "New message", context)

        // Assert
        coVerify {
            mockClaudeApiService.sendMessage(
                apiKey = testApiKey,
                request = match { request ->
                    // Should only include last 10 messages + new one = 11 total
                    request.messages.size == 11
                }
            )
        }
    }

    @Test
    fun `sendMessage handles network error`() = runTest {
        // Arrange
        val context = createTestContext()
        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } throws IOException("Network error")

        // Act
        val result = repository.sendMessage(testConversationId, "Test", context)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `sendMessage handles API error`() = runTest {
        // Arrange
        val context = createTestContext()
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 429 // Rate limit
        every { httpException.message() } returns "Rate limit exceeded"

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } throws httpException

        // Act
        val result = repository.sendMessage(testConversationId, "Test", context)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `sendMessage handles authentication error`() = runTest {
        // Arrange
        val context = createTestContext()
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 401 // Unauthorized
        every { httpException.message() } returns "Invalid API key"

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } throws httpException

        // Act
        val result = repository.sendMessage(testConversationId, "Test", context)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `sendMessage uses correct Claude model`() = runTest {
        // Arrange
        val context = createTestContext()
        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()

        // Act
        repository.sendMessage(testConversationId, "Test", context)

        // Assert
        coVerify {
            mockClaudeApiService.sendMessage(
                apiKey = testApiKey,
                request = match { request ->
                    request.model == "claude-3-5-sonnet-20241022"
                }
            )
        }
    }

    @Test
    fun `sendMessage uses correct temperature and max tokens`() = runTest {
        // Arrange
        val context = createTestContext()
        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()

        // Act
        repository.sendMessage(testConversationId, "Test", context)

        // Assert
        coVerify {
            mockClaudeApiService.sendMessage(
                apiKey = testApiKey,
                request = match { request ->
                    request.temperature == 0.7 && request.max_tokens == 300
                }
            )
        }
    }

    @Test
    fun `getConversationHistory retrieves messages from database`() = runTest {
        // Arrange
        val messages = listOf(
            ConversationMessage.userMessage(testConversationId, "Message 1"),
            ConversationMessage.assistantMessage(testConversationId, "Response 1"),
            ConversationMessage.userMessage(testConversationId, "Message 2")
        )
        coEvery { mockConversationDao.getConversation(testConversationId) } returns messages

        // Act
        val result = repository.getConversationHistory(testConversationId)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Message 1", result[0].content)
        assertEquals("Response 1", result[1].content)
        assertEquals("Message 2", result[2].content)
    }

    @Test
    fun `clearHistory deletes all messages`() = runTest {
        // Arrange
        coEvery { mockConversationDao.deleteAll() } returns Unit

        // Act
        repository.clearHistory()

        // Assert
        coVerify { mockConversationDao.deleteAll() }
    }

    @Test
    fun `sendMessage handles empty AI response`() = runTest {
        // Arrange
        val context = createTestContext()
        val emptyResponse = ClaudeMessageResponse(
            id = "msg_123",
            type = "message",
            role = "assistant",
            content = emptyList(),
            model = "claude-3-5-sonnet-20241022",
            stop_reason = "end_turn",
            usage = ClaudeUsage(0, 0)
        )

        every { mockPromptBuilder.buildSystemPrompt(any()) } returns "System prompt"
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns emptyResponse

        // Act
        val result = repository.sendMessage(testConversationId, "Test", context)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull()) // Should handle gracefully with empty string
    }

    @Test
    fun `sendMessage includes system prompt in API request`() = runTest {
        // Arrange
        val context = createTestContext()
        val systemPrompt = "You are Zordon, the wise digital guardian..."

        every { mockPromptBuilder.buildSystemPrompt(context) } returns systemPrompt
        coEvery { mockConversationDao.getConversation(any()) } returns emptyList()
        coEvery { mockClaudeApiService.sendMessage(any(), any()) } returns createMockResponse()

        // Act
        repository.sendMessage(testConversationId, "Test", context)

        // Assert
        coVerify {
            mockClaudeApiService.sendMessage(
                apiKey = testApiKey,
                request = match { request ->
                    request.system == systemPrompt
                }
            )
        }
    }

    // Helper functions

    private fun createTestContext() = ConversationContext(
        todayScreenTime = "2h 30m",
        currentApp = "Instagram",
        currentAppCategory = "SOCIAL_MEDIA",
        appUsageToday = "• Instagram: 1h 30m",
        recentAgreements = emptyList(),
        interventionReason = "Test intervention"
    )

    private fun createMockResponse(aiResponse: String = "Test response") = ClaudeMessageResponse(
        id = "msg_123",
        type = "message",
        role = "assistant",
        content = listOf(ClaudeContent("text", aiResponse)),
        model = "claude-3-5-sonnet-20241022",
        stop_reason = "end_turn",
        usage = ClaudeUsage(input_tokens = 100, output_tokens = 50)
    )
}
