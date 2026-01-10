package com.focusmother.android.ui.avatar

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.focusmother.android.data.api.models.AvatarStatusResponse
import com.focusmother.android.data.entity.AvatarConfig
import com.focusmother.android.data.repository.AvatarRepository
import com.focusmother.android.util.AvatarCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

/**
 * Unit tests for AvatarSetupViewModel.
 *
 * Tests the avatar setup flow state management including:
 * - Step navigation (welcome, camera, processing, success, error)
 * - Photo capture and avatar creation
 * - Error handling and recovery
 * - Progress updates during processing
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AvatarSetupViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: AvatarRepository

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPhotoFile: File

    @Mock
    private lateinit var mockGlbFile: File

    @Mock
    private lateinit var mockThumbnailFile: File

    private lateinit var viewModel: AvatarSetupViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        viewModel = AvatarSetupViewModel(mockRepository, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Welcome`() {
        // Then
        assertTrue(viewModel.setupState.value is SetupState.Welcome)
    }

    @Test
    fun `nextStep from Welcome transitions to CameraCapture`() {
        // Given
        assertEquals(SetupState.Welcome, viewModel.setupState.value)

        // When
        viewModel.nextStep()

        // Then
        assertTrue(viewModel.setupState.value is SetupState.CameraCapture)
    }

    @Test
    fun `previousStep from CameraCapture transitions to Welcome`() {
        // Given
        viewModel.nextStep() // Move to CameraCapture
        assertTrue(viewModel.setupState.value is SetupState.CameraCapture)

        // When
        viewModel.previousStep()

        // Then
        assertEquals(SetupState.Welcome, viewModel.setupState.value)
    }

    @Test
    fun `previousStep from Welcome does nothing`() {
        // Given
        assertEquals(SetupState.Welcome, viewModel.setupState.value)

        // When
        viewModel.previousStep()

        // Then
        assertEquals(SetupState.Welcome, viewModel.setupState.value)
    }

    @Test
    fun `onPhotoTaken triggers avatar creation with success`() = runTest {
        // Given
        val avatarId = "test-avatar-123"
        val glbUrl = "https://example.com/avatar.glb"
        val thumbnailUrl = "https://example.com/thumb.png"

        `when`(mockPhotoFile.exists()).thenReturn(true)
        `when`(mockRepository.createAvatarFromPhoto(mockPhotoFile))
            .thenReturn(Result.success(avatarId))

        val statusResponse = AvatarStatusResponse(
            id = avatarId,
            status = "completed",
            glbUrl = glbUrl,
            thumbnailUrl = thumbnailUrl
        )
        `when`(mockRepository.pollAvatarStatus(avatarId, 30, 2000))
            .thenReturn(Result.success(statusResponse))

        // Mock AvatarCacheManager calls
        `when`(mockContext.filesDir).thenReturn(mock(File::class.java))

        // When
        viewModel.onPhotoTaken(mockPhotoFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.setupState.value
        assertTrue(finalState is SetupState.Success)
        assertEquals(avatarId, (finalState as SetupState.Success).avatarId)

        verify(mockRepository).createAvatarFromPhoto(mockPhotoFile)
    }

    @Test
    fun `onPhotoTaken handles creation failure`() = runTest {
        // Given
        val errorMessage = "Network error"
        `when`(mockPhotoFile.exists()).thenReturn(true)
        `when`(mockRepository.createAvatarFromPhoto(mockPhotoFile))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // When
        viewModel.onPhotoTaken(mockPhotoFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.setupState.value
        assertTrue(finalState is SetupState.Error)
        assertTrue((finalState as SetupState.Error).message.contains(errorMessage))

        verify(mockRepository).createAvatarFromPhoto(mockPhotoFile)
    }

    @Test
    fun `onPhotoTaken transitions through Processing state`() = runTest {
        // Given
        val avatarId = "test-avatar"
        `when`(mockPhotoFile.exists()).thenReturn(true)
        `when`(mockRepository.createAvatarFromPhoto(mockPhotoFile))
            .thenReturn(Result.success(avatarId))

        val statusResponse = AvatarStatusResponse(
            id = avatarId,
            status = "completed",
            glbUrl = "https://example.com/avatar.glb",
            thumbnailUrl = "https://example.com/thumb.png"
        )
        `when`(mockRepository.pollAvatarStatus(avatarId, 30, 2000))
            .thenReturn(Result.success(statusResponse))

        `when`(mockContext.filesDir).thenReturn(mock(File::class.java))

        val stateHistory = mutableListOf<SetupState>()

        // Collect states
        viewModel.setupState.observeForever { state ->
            stateHistory.add(state)
        }

        // When
        viewModel.onPhotoTaken(mockPhotoFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(stateHistory.any { it is SetupState.Processing })
    }

    @Test
    fun `retry from Error state clears error and returns to Welcome`() {
        // Given
        viewModel.setStateForTesting(SetupState.Error("Test error"))

        // When
        viewModel.retry()

        // Then
        assertEquals(SetupState.Welcome, viewModel.setupState.value)
    }

    @Test
    fun `onPhotoTaken updates progress messages during processing`() = runTest {
        // Given
        val avatarId = "progress-avatar"
        `when`(mockPhotoFile.exists()).thenReturn(true)
        `when`(mockRepository.createAvatarFromPhoto(mockPhotoFile))
            .thenReturn(Result.success(avatarId))

        // Simulate processing with multiple status checks
        val processingResponse = AvatarStatusResponse(
            id = avatarId,
            status = "processing",
            glbUrl = null,
            thumbnailUrl = null
        )
        val completedResponse = AvatarStatusResponse(
            id = avatarId,
            status = "completed",
            glbUrl = "https://example.com/avatar.glb",
            thumbnailUrl = "https://example.com/thumb.png"
        )

        `when`(mockRepository.pollAvatarStatus(avatarId, 30, 2000))
            .thenReturn(Result.success(completedResponse))

        `when`(mockContext.filesDir).thenReturn(mock(File::class.java))

        val progressMessages = mutableListOf<String>()
        viewModel.setupState.observeForever { state ->
            if (state is SetupState.Processing) {
                progressMessages.add(state.progress)
            }
        }

        // When
        viewModel.onPhotoTaken(mockPhotoFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(progressMessages.isNotEmpty())
        assertTrue(progressMessages.any { it.contains("Uploading") || it.contains("Creating") })
    }

    @Test
    fun `onPhotoTaken handles polling timeout`() = runTest {
        // Given
        val avatarId = "timeout-avatar"
        `when`(mockPhotoFile.exists()).thenReturn(true)
        `when`(mockRepository.createAvatarFromPhoto(mockPhotoFile))
            .thenReturn(Result.success(avatarId))

        `when`(mockRepository.pollAvatarStatus(avatarId, 30, 2000))
            .thenReturn(Result.failure(IllegalStateException("Avatar processing timeout")))

        // When
        viewModel.onPhotoTaken(mockPhotoFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val finalState = viewModel.setupState.value
        assertTrue(finalState is SetupState.Error)
        assertTrue((finalState as SetupState.Error).message.contains("timeout"))
    }

    @Test
    fun `cancel during processing transitions to Error state`() {
        // Given
        viewModel.setStateForTesting(SetupState.Processing("Processing..."))

        // When
        viewModel.cancel()

        // Then
        val finalState = viewModel.setupState.value
        assertTrue(finalState is SetupState.Error)
        assertTrue((finalState as SetupState.Error).message.contains("cancelled"))
    }

    @Test
    fun `isLoading is true during Processing state`() {
        // Given
        viewModel.setStateForTesting(SetupState.Processing("Processing..."))

        // When
        val isLoading = viewModel.isLoading.value

        // Then
        assertTrue(isLoading == true)
    }

    @Test
    fun `isLoading is false during Welcome state`() {
        // Given
        viewModel.setStateForTesting(SetupState.Welcome)

        // When
        val isLoading = viewModel.isLoading.value

        // Then
        assertFalse(isLoading == true)
    }

    @Test
    fun `isLoading is false during Error state`() {
        // Given
        viewModel.setStateForTesting(SetupState.Error("Error occurred"))

        // When
        val isLoading = viewModel.isLoading.value

        // Then
        assertFalse(isLoading == true)
    }
}
