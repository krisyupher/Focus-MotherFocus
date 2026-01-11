package com.focusmother.android.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles encryption and decryption of the adult content blocklist.
 *
 * Uses Android KeyStore for secure, device-specific key storage. The encryption key
 * is generated once and stored securely in the Android KeyStore, making it:
 * - Device-specific (cannot be extracted or used on other devices)
 * - Secure (protected by Android's security infrastructure)
 * - Persistent (survives app updates and reinstalls)
 *
 * Encryption algorithm: AES/GCM/NoPadding (256-bit key)
 * - AES: Advanced Encryption Standard (industry standard)
 * - GCM: Galois/Counter Mode (provides authentication + encryption)
 * - NoPadding: GCM doesn't require padding
 *
 * The encrypted data format is:
 * [IV_LENGTH (4 bytes)][IV (variable)][CIPHERTEXT (variable)]
 *
 * Security considerations:
 * - IV (Initialization Vector) is randomly generated for each encryption
 * - GCM provides authenticated encryption (detects tampering)
 * - Key is never exposed outside Android KeyStore
 * - Suitable for storing sensitive package names
 *
 * @see android.security.keystore.KeyGenParameterSpec
 * @see javax.crypto.Cipher
 */
object BlocklistEncryption {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "FocusMotherBlocklistKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    /**
     * Encrypts a list of package names using AES-GCM.
     *
     * Generates a random IV for each encryption operation, ensuring that
     * encrypting the same plaintext twice produces different ciphertexts.
     * The list is serialized as newline-separated package names.
     *
     * @param packageNames List of package names to encrypt
     * @return Encrypted byte array containing [IV_LENGTH][IV][CIPHERTEXT]
     * @throws SecurityException if encryption fails
     */
    fun encrypt(packageNames: List<String>): ByteArray {
        return try {
            // Serialize list as newline-separated string
            val data = packageNames.joinToString("\n")

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val key = getOrCreateKey()

            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Package IV length + IV + ciphertext
            ByteBuffer.allocate(4 + iv.size + ciphertext.size)
                .putInt(iv.size)
                .put(iv)
                .put(ciphertext)
                .array()
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypts encrypted data and returns list of package names.
     *
     * Extracts the IV from the encrypted data and uses it to decrypt the ciphertext.
     * GCM mode automatically verifies the authentication tag, detecting any tampering.
     * Returns an empty list if decryption fails (fail-safe behavior).
     *
     * @param encryptedData Encrypted byte array containing [IV_LENGTH][IV][CIPHERTEXT]
     * @return List of decrypted package names, or empty list if decryption fails
     */
    fun decrypt(encryptedData: ByteArray): List<String> {
        // Handle empty data gracefully
        if (encryptedData.isEmpty()) {
            return emptyList()
        }

        return try {
            val buffer = ByteBuffer.wrap(encryptedData)

            // Extract IV
            val ivLength = buffer.int
            if (ivLength < 0 || ivLength > encryptedData.size - 4) {
                // Fail-safe: return empty list instead of throwing
                return emptyList()
            }

            val iv = ByteArray(ivLength)
            buffer.get(iv)

            // Extract ciphertext
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val key = getOrCreateKey()
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
            val plaintext = cipher.doFinal(ciphertext)

            val decryptedString = String(plaintext, Charsets.UTF_8)

            // Deserialize newline-separated string back to list
            if (decryptedString.isEmpty()) {
                emptyList()
            } else {
                decryptedString.split("\n").filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            // SECURITY: Silent failure - never log decryption errors as they may expose:
            // - Encrypted blocklist data in exception message
            // - File paths revealing blocklist location
            // - Package names if partial decryption occurred
            // Fail-safe behavior: return empty list
            emptyList()
        }
    }

    /**
     * Retrieves existing encryption key or generates a new one.
     *
     * Checks Android KeyStore for existing key. If not found, generates
     * a new 256-bit AES key with the following properties:
     * - Purpose: Encryption and Decryption
     * - Algorithm: AES
     * - Block mode: GCM (Galois/Counter Mode)
     * - Padding: None (GCM doesn't require padding)
     * - User authentication: Not required
     * - Randomized encryption: Enabled (different IV each time)
     *
     * @return SecretKey for encryption/decryption operations
     * @throws SecurityException if key generation fails
     */
    fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            // Check if key already exists
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null)
                if (entry is KeyStore.SecretKeyEntry) {
                    return entry.secretKey
                }
            }

            // Generate new key
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            throw SecurityException("Failed to get or create encryption key", e)
        }
    }

    /**
     * Deletes the encryption key from Android KeyStore.
     *
     * This is primarily for testing purposes. In production, deleting the key
     * would make any encrypted data unrecoverable.
     *
     * @throws SecurityException if key deletion fails
     */
    @Suppress("unused")
    fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            throw SecurityException("Failed to delete encryption key", e)
        }
    }
}
