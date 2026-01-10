package com.focusmother.android.ui.conversation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.focusmother.android.data.entity.ConversationMessage
import com.focusmother.android.data.repository.ConversationRepository
import com.focusmother.android.domain.ConversationContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for ConversationViewModel.
 *
 * Tests state management, message sending, error handling,
 * and conversation flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ConversationViewModel
    private lateinit var mockRepository: ConversationRepository

    private val testConversationId = 1L
    private val testContext = ConversationContext(
        todayScreenTime = "2h 30m",
        currentApp = "Instagram",
        currentAppCategory = "SOCIAL_MEDIA",
        appUsageToday = "• Instagram: 1h 30m",
        recentAgreements = emptyList(),
        interventionReason = "Test"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()

        viewModel = ConversationViewModel(
            repository = mockRepository,
            conversationId = testConversationId,
            context = testContext
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        // Assert
        assertTrue(viewModel.conversationState.value is ConversationState.Idle)
    }

    @Test
    fun `loadHistory retrieves messages from repository`() = runTest {
        // Arrange
        val messages = listOf(
            ConversationMessage.userMessage(testConversationId, "Hello"),
            ConversationMessage.assistantMessage(testConversationId, "Hi there")
        )
        coEvery { mockRepository.getConversationHistory(testConversationId) } returns messages

        // Act
        viewModel.loadHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(2, viewModel.messages.value?.size)
        assertEquals("Hello", viewModel.messages.value?.get(0)?.content)
        assertEquals("Hi there", viewModel.messages.value?.get(1)?.content)
    }

    @Test
    fun `sendMessage transitions to WaitingForResponse state`() = runTest(testDispatcher) {
        // Arrange
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()
        coEvery { mockRepository.sendMessage(any(), any(), any()) } coAnswers {
            // Never completes - simulate long-running operation
            kotlinx.coroutines.delay(Long.MAX_VALUE)
            Result.success("Response")
        }

        // Advance past init/loadHistory
        testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.sendMessage("Test message")
        // Advance time to start the coroutine and execute up to the suspend point
        testDispatcher.scheduler.advanceTimeBy(1L)
        testDispatcher.scheduler.runCurrent()

        // Assert - state should be WaitingForResponse before API call completes
        assertTrue(viewModel.conversationState.value is ConversationState.WaitingForResponse)
    }

    @Test
    fun `sendMessage successfully shows response`() = runTest {
        // Arrange
        val response = "Young warrior, what brings you here?"
        coEvery { mockRepository.sendMessage(testConversationId, "Test", testContext) } returns Result.success(response)
        coEvery { mockRepository.getConversationHistory(testConversationId) } returns emptyList()

        // Act
        viewModel.sendMessage("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.conversationState.value
        assertTrue(state is ConversationState.ShowingResponse)
        assertEquals(response, (state as ConversationState.ShowingResponse).message)
    }

    @Test
    fun `sendMessage calls repository with correct parameters`() = runTest {
        // Arrange
        val userMessage = "I need 5 more minutes"
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.success("Agreed")
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.sendMessage(userMessage)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify {
            mockRepository.sendMessage(
                conversationId = testConversationId,
                message = userMessage,
                context = testContext
            )
        }
    }

    @Test
    fun `sendMessage reloads history after response`() = runTest {
        // Arrange
        val initialMessages = emptyList<ConversationMessage>()
        val updatedMessages = listOf(
            ConversationMessage.userMessage(testConversationId, "Test"),
            ConversationMessage.assistantMessage(testConversationId, "Response")
        )

        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.success("Response")
        coEvery { mockRepository.getConversationHistory(testConversationId) } returnsMany listOf(
            initialMessages,
            updatedMessages
        )

        // Act
        viewModel.loadHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendMessage("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(2, viewModel.messages.value?.size)
    }

    @Test
    fun `sendMessage handles network error`() = runTest {
        // Arrange
        val error = IOException("Network error")
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.failure(error)
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.sendMessage("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.conversationState.value
        assertTrue(state is ConversationState.Error)
        assertTrue((state as ConversationState.Error).message.contains("Network") || state.message.contains("error"))
    }

    @Test
    fun `sendMessage handles API error`() = runTest {
        // Arrange
        val error = Exception("API error")
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.failure(error)
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.sendMessage("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.conversationState.value
        assertTrue(state is ConversationState.Error)
    }

    @Test
    fun `sendMessage does not call repository with empty message`() = runTest {
        // Act
        viewModel.sendMessage("")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `sendMessage does not call repository with blank message`() = runTest {
        // Act
        viewModel.sendMessage("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `selectQuickReply sends predefined message`() = runTest {
        // Arrange
        val quickReply = "I agree"
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.success("Great!")
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.selectQuickReply(quickReply)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify {
            mockRepository.sendMessage(
                conversationId = testConversationId,
                message = quickReply,
                context = testContext
            )
        }
    }

    @Test
    fun `retry after error resends last message`() = runTest {
        // Arrange
        val message = "Test message"
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returnsMany listOf(
            Result.failure(IOException("Network error")),
            Result.success("Success")
        )
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act - first attempt fails
        viewModel.sendMessage(message)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        assertTrue(viewModel.conversationState.value is ConversationState.Error)

        // Act - retry
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - should succeed
        assertTrue(viewModel.conversationState.value is ConversationState.ShowingResponse)
    }

    @Test
    fun `retry does nothing if no previous message`() = runTest {
        // Act
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { mockRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `multiple sendMessage calls are handled sequentially`() = runTest {
        // Arrange
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.success("Response")
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.sendMessage("Message 1")
        viewModel.sendMessage("Message 2")
        viewModel.sendMessage("Message 3")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - all messages should be sent
        coVerify(exactly = 3) { mockRepository.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `conversation state returns to Idle after showing response timeout`() = runTest {
        // Arrange
        coEvery { mockRepository.sendMessage(any(), any(), any()) } returns Result.success("Response")
        coEvery { mockRepository.getConversationHistory(any()) } returns emptyList()

        // Act
        viewModel.sendMessage("Test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify showing response
        assertTrue(viewModel.conversationState.value is ConversationState.ShowingResponse)

        // Act - reset to idle
        viewModel.resetToIdle()

        // Assert
        assertTrue(viewModel.conversationState.value is ConversationState.Idle)
    }
}
