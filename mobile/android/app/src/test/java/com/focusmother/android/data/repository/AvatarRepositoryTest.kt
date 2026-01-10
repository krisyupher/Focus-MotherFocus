package com.focusmother.android.data.repository

import com.focusmother.android.data.api.ReadyPlayerMeApiService
import com.focusmother.android.data.api.models.AvatarCreationResponse
import com.focusmother.android.data.api.models.AvatarStatusResponse
import com.focusmother.android.data.dao.AvatarDao
import com.focusmother.android.data.entity.AvatarConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * Unit tests for AvatarRepository.
 *
 * Tests avatar management operations including:
 * - Creating avatars from photos
 * - Polling for avatar completion status
 * - Saving and retrieving avatar configurations
 * - Downloading GLB files
 * - Error handling for network failures
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class AvatarRepositoryTest {

    @Mock
    private lateinit var mockAvatarDao: AvatarDao

    @Mock
    private lateinit var mockApiService: ReadyPlayerMeApiService

    private lateinit var repository: AvatarRepository
    private lateinit var testPhotoFile: File

    @Before
    fun setup() {
        // Create a real temporary photo file for testing
        testPhotoFile = createTempFile("test_photo", ".jpg")
        testPhotoFile.writeText("fake image data")

        repository = AvatarRepository(mockAvatarDao, mockApiService)
    }

    @After
    fun tearDown() {
        // Clean up temporary file
        if (::testPhotoFile.isInitialized && testPhotoFile.exists()) {
            testPhotoFile.delete()
        }
    }

    @Test
    fun `createAvatarFromPhoto success flow`() = runTest {
        // Given
        val creationResponse = AvatarCreationResponse(
            id = "avatar-123",
            status = "processing",
            url = null
        )

        `when`(mockApiService.createAvatar(any(), any())).thenReturn(creationResponse)

        // When
        val result = repository.createAvatarFromPhoto(testPhotoFile)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("avatar-123", result.getOrNull())
        verify(mockApiService).createAvatar(any(), any())
    }

    @Test
    fun `createAvatarFromPhoto handles invalid file`() = runTest {
        // Given
        val nonExistentFile = File("/non/existent/file.jpg")

        // When
        val result = repository.createAvatarFromPhoto(nonExistentFile)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("does not exist") == true)
        verify(mockApiService, never()).createAvatar(any(), any())
    }

    @Test
    fun `createAvatarFromPhoto handles API creation error`() = runTest {
        // Given
        val errorResponse = Response.error<AvatarCreationResponse>(
            400,
            "Bad request".toResponseBody()
        )
        `when`(mockApiService.createAvatar(any(), any()))
            .thenThrow(HttpException(errorResponse))

        // When
        val result = repository.createAvatarFromPhoto(testPhotoFile)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `createAvatarFromPhoto handles network error`() = runTest {
        // Given
        `when`(mockApiService.createAvatar(any(), any()))
            .thenThrow(IOException("Network error"))

        // When
        val result = repository.createAvatarFromPhoto(testPhotoFile)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `pollAvatarStatus returns completed status`() = runTest {
        // Given
        val completedResponse = AvatarStatusResponse(
            id = "avatar-123",
            status = "completed",
            glbUrl = "https://example.com/avatar.glb",
            thumbnailUrl = "https://example.com/thumb.png"
        )

        `when`(mockApiService.getAvatarStatus("avatar-123"))
            .thenReturn(completedResponse)

        // When
        val result = repository.pollAvatarStatus("avatar-123", maxAttempts = 3, delayMs = 100)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("completed", result.getOrNull()?.status)
        assertEquals("https://example.com/avatar.glb", result.getOrNull()?.glbUrl)
    }

    @Test
    fun `pollAvatarStatus handles failed status from API`() = runTest {
        // Given
        val failedResponse = AvatarStatusResponse(
            id = "avatar-123",
            status = "failed",
            glbUrl = null,
            thumbnailUrl = null
        )

        `when`(mockApiService.getAvatarStatus("avatar-123"))
            .thenReturn(failedResponse)

        // When
        val result = repository.pollAvatarStatus("avatar-123", maxAttempts = 3, delayMs = 100)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("failed", result.getOrNull()?.status)
    }

    @Test
    fun `pollAvatarStatus handles timeout with processing status`() = runTest {
        // Given - Always return processing status
        val processingResponse = AvatarStatusResponse(
            id = "avatar-123",
            status = "processing",
            glbUrl = null,
            thumbnailUrl = null
        )

        `when`(mockApiService.getAvatarStatus("avatar-123"))
            .thenReturn(processingResponse)

        // When - Only allow 2 attempts with short delay
        val result = repository.pollAvatarStatus("avatar-123", maxAttempts = 2, delayMs = 50)

        // Then - Should timeout and return failure
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("timeout") == true ||
                  result.exceptionOrNull()?.message?.contains("max attempts") == true)
    }

    @Test
    fun `getAvatarConfig retrieves from database`() = runTest {
        // Given
        val avatarConfig = AvatarConfig.create(
            avatarUrl = "https://example.com/avatar",
            localGlbPath = "/path/to/avatar.glb",
            thumbnailPath = "/path/to/thumb.png",
            readyPlayerMeId = "avatar-123"
        )

        `when`(mockAvatarDao.getAvatar()).thenReturn(avatarConfig)

        // When
        val result = repository.getAvatarConfig()

        // Then
        assertNotNull(result)
        assertEquals("avatar-123", result?.readyPlayerMeId)
        verify(mockAvatarDao).getAvatar()
    }

    @Test
    fun `saveAvatarConfig stores in database`() = runTest {
        // Given
        val avatarConfig = AvatarConfig.create(
            avatarUrl = "https://example.com/avatar",
            localGlbPath = "/path/to/avatar.glb",
            thumbnailPath = "/path/to/thumb.png",
            readyPlayerMeId = "avatar-123"
        )

        // When
        repository.saveAvatarConfig(avatarConfig)

        // Then
        verify(mockAvatarDao).insert(avatarConfig)
    }

    @Test
    fun `hasAvatar returns true when avatar exists`() = runTest {
        // Given
        `when`(mockAvatarDao.hasAvatar()).thenReturn(true)

        // When
        val hasAvatar = repository.hasAvatar()

        // Then
        assertTrue(hasAvatar)
    }

    @Test
    fun `hasAvatar returns false when no avatar exists`() = runTest {
        // Given
        `when`(mockAvatarDao.hasAvatar()).thenReturn(false)

        // When
        val hasAvatar = repository.hasAvatar()

        // Then
        assertFalse(hasAvatar)
    }

    @Test
    fun `deleteAvatar removes from database`() = runTest {
        // When
        repository.deleteAvatar()

        // Then
        verify(mockAvatarDao).deleteAll()
    }
}
