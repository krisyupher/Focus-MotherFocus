package com.focusmother.android.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BlocklistEncryption utility.
 *
 * Tests encryption/decryption of adult content package names for privacy.
 * Uses Robolectric to provide Android context for KeyStore operations.
 *
 * These tests verify:
 * - Encrypt/decrypt roundtrip preserves data
 * - Empty list handling
 * - Invalid data returns empty list (fail-safe)
 * - Large list handling
 * - Special characters in package names
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BlocklistEncryptionTest {

    @Before
    fun setup() {
        // Robolectric provides Android KeyStore environment
        // Each test starts with clean state
    }

    @Test
    fun `encrypt and decrypt roundtrip preserves data`() {
        // Arrange
        val originalList = listOf(
            "com.example.adult1",
            "com.example.adult2",
            "com.example.adult3"
        )

        // Act
        val encrypted = BlocklistEncryption.encrypt(originalList)
        val decrypted = BlocklistEncryption.decrypt(encrypted)

        // Assert
        assertEquals("Decrypted list should match original", originalList, decrypted)
        assertTrue("Encrypted data should not be empty", encrypted.isNotEmpty())
    }

    @Test
    fun `encrypt empty list returns valid encrypted data`() {
        // Arrange
        val emptyList = emptyList<String>()

        // Act
        val encrypted = BlocklistEncryption.encrypt(emptyList)
        val decrypted = BlocklistEncryption.decrypt(encrypted)

        // Assert
        assertTrue("Encrypted data should not be empty", encrypted.isNotEmpty())
        assertEquals("Decrypted list should be empty", emptyList, decrypted)
    }

    @Test
    fun `encrypt single package name works correctly`() {
        // Arrange
        val singleItem = listOf("com.example.adult")

        // Act
        val encrypted = BlocklistEncryption.encrypt(singleItem)
        val decrypted = BlocklistEncryption.decrypt(encrypted)

        // Assert
        assertEquals("Decrypted list should match original", singleItem, decrypted)
    }

    @Test
    fun `decrypt invalid data returns empty list`() {
        // Arrange
        val invalidData = ByteArray(16) { 0xFF.toByte() } // Random invalid encrypted data

        // Act
        val decrypted = BlocklistEncryption.decrypt(invalidData)

        // Assert
        assertTrue("Decrypting invalid data should return empty list (fail-safe)", decrypted.isEmpty())
    }

    @Test
    fun `decrypt empty byte array returns empty list`() {
        // Arrange
        val emptyData = ByteArray(0)

        // Act
        val decrypted = BlocklistEncryption.decrypt(emptyData)

        // Assert
        assertTrue("Decrypting empty data should return empty list", decrypted.isEmpty())
    }

    @Test
    fun `encrypt produces different output for different inputs`() {
        // Arrange
        val list1 = listOf("com.example.adult1")
        val list2 = listOf("com.example.adult2")

        // Act
        val encrypted1 = BlocklistEncryption.encrypt(list1)
        val encrypted2 = BlocklistEncryption.encrypt(list2)

        // Assert
        assertFalse("Different inputs should produce different encrypted outputs",
            encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun `encrypt large list of package names`() {
        // Arrange
        val largeList = (1..100).map { "com.example.adult$it" }

        // Act
        val encrypted = BlocklistEncryption.encrypt(largeList)
        val decrypted = BlocklistEncryption.decrypt(encrypted)

        // Assert
        assertEquals("Large list should encrypt and decrypt correctly", largeList, decrypted)
    }

    @Test
    fun `encrypt package names with special characters`() {
        // Arrange
        val specialList = listOf(
            "com.example.adult-test",
            "com.example.adult_underscore",
            "com.example.adult.multiple.dots"
        )

        // Act
        val encrypted = BlocklistEncryption.encrypt(specialList)
        val decrypted = BlocklistEncryption.decrypt(encrypted)

        // Assert
        assertEquals("Special characters should be preserved", specialList, decrypted)
    }

    @Test
    fun `getOrCreateKey returns same key on multiple calls`() {
        // Act
        val key1 = BlocklistEncryption.getOrCreateKey()
        val key2 = BlocklistEncryption.getOrCreateKey()

        // Assert
        assertNotNull("First key should not be null", key1)
        assertNotNull("Second key should not be null", key2)
        assertEquals("Keys should be the same", key1, key2)
    }

    @Test
    fun `decrypt handles data with invalid IV length gracefully`() {
        // Arrange - Create data with negative IV length
        val badData = ByteArray(8)
        badData[0] = 0xFF.toByte() // Set first byte to create invalid IV length

        // Act
        val decrypted = BlocklistEncryption.decrypt(badData)

        // Assert
        assertTrue("Should return empty list for invalid IV length", decrypted.isEmpty())
    }
}
