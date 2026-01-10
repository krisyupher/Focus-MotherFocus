package com.focusmother.android.util

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

/**
 * Unit tests for AvatarCacheManager.
 *
 * Tests file caching operations for avatar GLB files including:
 * - Cache directory creation and retrieval
 * - Avatar file path generation
 * - Cache existence checks
 * - Cache clearing operations
 */
@RunWith(MockitoJUnitRunner::class)
class AvatarCacheManagerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var tempFilesDir: File

    @Before
    fun setup() {
        // Create a real temporary directory for testing
        tempFilesDir = createTempDir("test-files")

        // Setup mock context to return our temp directory
        `when`(mockContext.filesDir).thenReturn(tempFilesDir)
    }

    @Test
    fun `getCacheDir returns correct directory path`() {
        // When
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)

        // Then
        assertNotNull(cacheDir)
        assertEquals("avatars", cacheDir.name)
        assertTrue(cacheDir.parentFile == tempFilesDir)
    }

    @Test
    fun `getCacheDir creates directory if it does not exist`() {
        // Given - directory doesn't exist yet
        val cachePath = File(tempFilesDir, "avatars")
        assertFalse(cachePath.exists())

        // When
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)

        // Then
        assertTrue(cacheDir.exists())
        assertTrue(cacheDir.isDirectory)
    }

    @Test
    fun `getAvatarFile returns correct file path for given avatar ID`() {
        // Given
        val avatarId = "test-avatar-123"

        // When
        val avatarFile = AvatarCacheManager.getAvatarFile(mockContext, avatarId)

        // Then
        assertNotNull(avatarFile)
        assertEquals("$avatarId.glb", avatarFile.name)
        assertTrue(avatarFile.parentFile?.name == "avatars")
    }

    @Test
    fun `getAvatarFile uses glb extension`() {
        // Given
        val avatarId = "avatar-with-extension"

        // When
        val avatarFile = AvatarCacheManager.getAvatarFile(mockContext, avatarId)

        // Then
        assertTrue(avatarFile.name.endsWith(".glb"))
        assertEquals("$avatarId.glb", avatarFile.name)
    }

    @Test
    fun `getThumbnailFile returns correct path for avatar thumbnail`() {
        // Given
        val avatarId = "test-avatar"

        // When
        val thumbnailFile = AvatarCacheManager.getThumbnailFile(mockContext, avatarId)

        // Then
        assertNotNull(thumbnailFile)
        assertEquals("${avatarId}_thumbnail.png", thumbnailFile.name)
        assertTrue(thumbnailFile.parentFile?.name == "avatars")
    }

    @Test
    fun `cacheExists returns true when avatar file exists`() {
        // Given
        val avatarId = "existing-avatar"
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)
        val avatarFile = File(cacheDir, "$avatarId.glb")
        avatarFile.createNewFile() // Create the file

        // When
        val exists = AvatarCacheManager.cacheExists(mockContext, avatarId)

        // Then
        assertTrue(exists)
    }

    @Test
    fun `cacheExists returns false when avatar file does not exist`() {
        // Given
        val avatarId = "missing-avatar"
        // Don't create the file

        // When
        val exists = AvatarCacheManager.cacheExists(mockContext, avatarId)

        // Then
        assertFalse(exists)
    }

    @Test
    fun `clearCache deletes all files in cache directory`() {
        // Given
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)
        val file1 = File(cacheDir, "avatar1.glb")
        val file2 = File(cacheDir, "avatar2.glb")
        val file3 = File(cacheDir, "avatar3_thumbnail.png")
        file1.createNewFile()
        file2.createNewFile()
        file3.createNewFile()

        assertTrue(file1.exists())
        assertTrue(file2.exists())
        assertTrue(file3.exists())

        // When
        AvatarCacheManager.clearCache(mockContext)

        // Then
        assertFalse(file1.exists())
        assertFalse(file2.exists())
        assertFalse(file3.exists())
    }

    @Test
    fun `clearCache handles empty directory`() {
        // Given
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)
        assertTrue(cacheDir.listFiles()?.isEmpty() == true)

        // When/Then - should not throw exception
        assertDoesNotThrow {
            AvatarCacheManager.clearCache(mockContext)
        }
    }

    @Test
    fun `getCacheSize returns total size of cached files`() {
        // Given
        val cacheDir = AvatarCacheManager.getCacheDir(mockContext)
        val file1 = File(cacheDir, "avatar1.glb")
        val file2 = File(cacheDir, "avatar2.glb")

        file1.writeText("test content 1") // 14 bytes
        file2.writeText("test content 2") // 14 bytes

        // When
        val cacheSize = AvatarCacheManager.getCacheSize(mockContext)

        // Then
        assertEquals(28L, cacheSize)
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }
}
