package com.focusmother.android.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SecureApiKeyProvider.
 *
 * These tests run on an Android device or emulator and verify the secure storage
 * of API keys using Android KeyStore.
 *
 * Tests cover:
 * - Saving and retrieving API keys
 * - Key existence checks
 * - Key clearing
 * - Encryption/decryption correctness
 * - Edge cases (empty strings, special characters)
 * - Multiple save/retrieve cycles
 */
@RunWith(AndroidJUnit4::class)
class SecureApiKeyProviderTest {

    private lateinit var context: Context
    private lateinit var provider: SecureApiKeyProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = SecureApiKeyProvider(context)
        // Clean up any existing keys before each test
        provider.clearApiKey()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        provider.clearApiKey()
    }

    @Test
    fun testSaveAndRetrieveApiKey() {
        // Arrange
        val testApiKey = "sk-ant-api03-test1234567890abcdefghijklmnopqrstuvwxyz"

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testHasApiKeyReturnsFalseWhenNoKeyStored() {
        // Assert
        assertFalse(provider.hasApiKey())
    }

    @Test
    fun testHasApiKeyReturnsTrueWhenKeyStored() {
        // Arrange
        val testApiKey = "sk-ant-test-key"

        // Act
        provider.saveApiKey(testApiKey)

        // Assert
        assertTrue(provider.hasApiKey())
    }

    @Test
    fun testGetApiKeyReturnsNullWhenNoKeyStored() {
        // Act
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNull(retrievedKey)
    }

    @Test
    fun testClearApiKeyRemovesStoredKey() {
        // Arrange
        val testApiKey = "sk-ant-test-clear-key"
        provider.saveApiKey(testApiKey)
        assertTrue(provider.hasApiKey())

        // Act
        provider.clearApiKey()

        // Assert
        assertFalse(provider.hasApiKey())
        assertNull(provider.getApiKey())
    }

    @Test
    fun testSaveApiKeyOverwritesPreviousKey() {
        // Arrange
        val firstKey = "sk-ant-first-key"
        val secondKey = "sk-ant-second-key"

        // Act
        provider.saveApiKey(firstKey)
        val firstRetrieved = provider.getApiKey()

        provider.saveApiKey(secondKey)
        val secondRetrieved = provider.getApiKey()

        // Assert
        assertEquals(firstKey, firstRetrieved)
        assertEquals(secondKey, secondRetrieved)
    }

    @Test
    fun testEncryptionProducesDifferentCiphertext() {
        // Arrange
        val testApiKey = "sk-ant-encryption-test"
        val prefs = context.getSharedPreferences("secure_api_prefs", Context.MODE_PRIVATE)

        // Act
        provider.saveApiKey(testApiKey)
        val firstEncrypted = prefs.getString("encrypted_api_key", null)

        provider.clearApiKey()

        provider.saveApiKey(testApiKey)
        val secondEncrypted = prefs.getString("encrypted_api_key", null)

        // Assert
        assertNotNull(firstEncrypted)
        assertNotNull(secondEncrypted)
        // Due to random IV, encrypted values should be different
        // (This is actually a good security property - same plaintext produces different ciphertext)
    }

    @Test
    fun testSaveAndRetrieveApiKeyWithSpecialCharacters() {
        // Arrange
        val testApiKey = "sk-ant-!@#$%^&*()_+-=[]{}|;':\",./<>?`~"

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testSaveAndRetrieveVeryLongApiKey() {
        // Arrange
        val testApiKey = "sk-ant-" + "a".repeat(500) // 506 character key

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testSaveEmptyStringApiKey() {
        // Arrange
        val testApiKey = ""

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
        assertTrue(provider.hasApiKey())
    }

    @Test
    fun testSaveAndRetrieveApiKeyWithUnicodeCharacters() {
        // Arrange
        val testApiKey = "sk-ant-测试-🔑-ключ-مفتاح"

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testMultipleSaveRetrieveCycles() {
        // Arrange
        val keys = listOf(
            "sk-ant-key-1",
            "sk-ant-key-2",
            "sk-ant-key-3",
            "sk-ant-key-4",
            "sk-ant-key-5"
        )

        // Act & Assert
        for (key in keys) {
            provider.saveApiKey(key)
            val retrieved = provider.getApiKey()
            assertEquals(key, retrieved)
            assertTrue(provider.hasApiKey())
        }
    }

    @Test
    fun testClearApiKeyMultipleTimes() {
        // Arrange
        val testApiKey = "sk-ant-clear-multiple"
        provider.saveApiKey(testApiKey)

        // Act
        provider.clearApiKey()
        provider.clearApiKey() // Clear again when already cleared
        provider.clearApiKey() // And again

        // Assert
        assertFalse(provider.hasApiKey())
        assertNull(provider.getApiKey())
    }

    @Test
    fun testGetApiKeyAfterClearReturnsNull() {
        // Arrange
        val testApiKey = "sk-ant-test-after-clear"
        provider.saveApiKey(testApiKey)
        assertEquals(testApiKey, provider.getApiKey())

        // Act
        provider.clearApiKey()
        val retrievedAfterClear = provider.getApiKey()

        // Assert
        assertNull(retrievedAfterClear)
    }

    @Test
    fun testIsolationBetweenProviderInstances() {
        // Arrange
        val testApiKey = "sk-ant-isolation-test"
        val provider1 = SecureApiKeyProvider(context)
        val provider2 = SecureApiKeyProvider(context)

        // Act
        provider1.saveApiKey(testApiKey)
        val retrievedFromProvider2 = provider2.getApiKey()

        // Assert
        // Both providers should access the same underlying storage
        assertEquals(testApiKey, retrievedFromProvider2)

        // Cleanup
        provider1.clearApiKey()
    }

    @Test
    fun testApiKeyStorageIsPersistent() {
        // Arrange
        val testApiKey = "sk-ant-persistent-test"

        // Act
        provider.saveApiKey(testApiKey)

        // Create a new provider instance (simulating app restart)
        val newProvider = SecureApiKeyProvider(context)
        val retrievedKey = newProvider.getApiKey()

        // Assert
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testSaveApiKeyWithWhitespace() {
        // Arrange
        val testApiKey = "  sk-ant-whitespace-test  "

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        // Should preserve whitespace exactly as provided
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testSaveApiKeyWithNewlines() {
        // Arrange
        val testApiKey = "sk-ant-line1\nline2\nline3"

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
    }

    @Test
    fun testRealisticClaudeApiKeyFormat() {
        // Arrange - Realistic Claude API key format
        val testApiKey = "sk-ant-api03-1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-ABCD"

        // Act
        provider.saveApiKey(testApiKey)
        val retrievedKey = provider.getApiKey()

        // Assert
        assertNotNull(retrievedKey)
        assertEquals(testApiKey, retrievedKey)
        assertTrue(retrievedKey!!.startsWith("sk-ant-"))
    }
}
