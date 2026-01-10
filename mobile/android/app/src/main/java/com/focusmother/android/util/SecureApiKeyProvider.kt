package com.focusmother.android.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure API key provider using Android KeyStore system.
 *
 * This class provides secure storage for API keys using Android's KeyStore system,
 * which stores cryptographic keys in a hardware-backed security module when available.
 * The API key is encrypted using AES-256-GCM encryption before being stored in
 * SharedPreferences.
 *
 * Security features:
 * - Hardware-backed encryption (when available on device)
 * - AES-256-GCM authenticated encryption
 * - Keys never stored in plain text
 * - Keys isolated from other apps
 *
 * Usage:
 * ```kotlin
 * val provider = SecureApiKeyProvider(context)
 *
 * // Save API key
 * provider.saveApiKey("sk-ant-1234567890abcdef")
 *
 * // Retrieve API key
 * val apiKey = provider.getApiKey()
 *
 * // Check if key exists
 * if (provider.hasApiKey()) {
 *     // Use the key
 * }
 *
 * // Clear API key
 * provider.clearApiKey()
 * ```
 *
 * Note: Requires Android API 23 (Marshmallow) or higher for full functionality.
 *
 * @param context Android application context
 */
class SecureApiKeyProvider(private val context: Context) {

    private val keystore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Saves API key encrypted using Android KeyStore.
     *
     * The API key is encrypted using AES-256-GCM and stored in SharedPreferences.
     * The encryption key is stored in Android KeyStore, which provides hardware-backed
     * security on supported devices.
     *
     * @param apiKey The API key to store securely
     * @throws IllegalStateException if encryption fails
     */
    fun saveApiKey(apiKey: String) {
        try {
            // Generate or retrieve encryption key
            val secretKey = getOrCreateSecretKey()

            // Encrypt API key
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            // Save encrypted data and IV
            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to save API key securely", e)
        }
    }

    /**
     * Retrieves and decrypts API key from Android KeyStore.
     *
     * Decrypts the stored API key using the encryption key from Android KeyStore.
     *
     * @return The decrypted API key, or null if no key is stored or decryption fails
     */
    fun getApiKey(): String? {
        val encryptedKeyStr = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val ivStr = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivStr, Base64.DEFAULT)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val encryptedBytes = Base64.decode(encryptedKeyStr, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks if API key exists in secure storage.
     *
     * @return true if an API key is stored, false otherwise
     */
    fun hasApiKey(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_API_KEY)
    }

    /**
     * Clears stored API key from secure storage.
     *
     * Removes the encrypted API key and initialization vector from SharedPreferences.
     * The encryption key remains in KeyStore for potential reuse.
     */
    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .remove(KEY_IV)
            .apply()
    }

    /**
     * Retrieves existing encryption key or creates a new one.
     *
     * @return The secret key for encryption/decryption
     */
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keystore.containsAlias(KEY_ALIAS)) {
            val entry = keystore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            entry.secretKey
        } else {
            createSecretKey()
        }
    }

    /**
     * Creates a new AES-256 key in Android KeyStore.
     *
     * The key is stored in hardware-backed KeyStore when available, providing
     * enhanced security against key extraction.
     *
     * @return The newly created secret key
     * @throws IllegalStateException if key creation fails
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "secure_api_prefs"
        private const val KEY_ALIAS = "claude_api_key_alias"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_api_key"
        private const val KEY_IV = "encryption_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
