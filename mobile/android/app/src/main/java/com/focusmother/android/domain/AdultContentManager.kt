package com.focusmother.android.domain

import android.content.Context
import androidx.annotation.WorkerThread
import com.focusmother.android.util.BlocklistEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * AdultContentManager - Manages the blocklist for adult content and handles detection
 */
class AdultContentManager(private val context: Context) {

    private val blocklistFile = File(context.filesDir, "adult_blocklist.dat")
    
    private var cachedBlocklist: Set<String>? = null

    /**
     * Check if a package name is in the adult content blocklist
     */
    suspend fun isAdultContent(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val list = getBlocklist()
        list.contains(packageName)
    }

    /**
     * Get the current blocklist, loading from disk if necessary
     */
    @WorkerThread
    private suspend fun getBlocklist(): Set<String> {
        cachedBlocklist?.let { return it }

        return if (blocklistFile.exists()) {
            try {
                val encryptedData = blocklistFile.readBytes()
                val decryptedList = BlocklistEncryption.decrypt(encryptedData)
                val list = decryptedList.toSet()
                cachedBlocklist = list
                list
            } catch (e: Exception) {
                // SECURITY: Silent failure - never log blocklist errors
                // Auto-recovery: delete corrupted file and reseed from defaults
                blocklistFile.delete()
                val initialList = AppCategorySeedData.ADULT_CONTENT.toSet()
                saveBlocklist(initialList)
                initialList
            }
        } else {
            // Seed initial blocklist if it doesn't exist
            // Using ADULT_CONTENT from AppCategorySeedData
            val initialList = AppCategorySeedData.ADULT_CONTENT.toSet()
            saveBlocklist(initialList)
            initialList
        }
    }

    /**
     * Encrypt and save a new blocklist to disk
     */
    suspend fun saveBlocklist(packages: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val encryptedData = BlocklistEncryption.encrypt(packages.toList())
            blocklistFile.writeBytes(encryptedData)
            cachedBlocklist = packages
        } catch (e: Exception) {
            // SECURITY: Silent failure - never log encryption errors for blocklist
            // Graceful degradation: cache will remain in memory for this session
        }
    }

    /**
     * Non-judgmental message for adult content detection
     */
    fun getInterventionMessage(): String {
        return "Rangers! I sense a disturbance in your focus. I've observed you're heading towards content that might not align with your goals for today. Let's return to your mission and stay on the path of discipline!"
    }

    companion object {
        const val SUGGESTED_LIMIT_MS = 5 * 60 * 1000L // 5 minutes
    }
}
