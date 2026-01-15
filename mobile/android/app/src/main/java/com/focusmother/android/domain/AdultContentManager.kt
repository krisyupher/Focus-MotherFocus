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
        list.contains(packageName.lowercase().trim())
    }

    /**
     * Loads the current blocklist. Public for testing.
     */
    suspend fun loadBlocklist(): List<String> {
        return getBlocklist().toList()
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
                val list = decryptedList.map { it.lowercase().trim() }.toSet()
                cachedBlocklist = list
                list
            } catch (e: Exception) {
                e.printStackTrace()
                emptySet()
            }
        } else {
            // Seed initial blocklist if it doesn't exist
            val initialList = AppCategorySeedData.ADULT_CONTENT.map { it.lowercase().trim() }.toSet()
            saveBlocklist(initialList)
            initialList
        }
    }

    /**
     * Encrypt and save a new blocklist to disk
     */
    suspend fun saveBlocklist(packages: Set<String>) = withContext(Dispatchers.IO) {
        try {
            val list = packages.map { it.lowercase().trim() }
            val encryptedData = BlocklistEncryption.encrypt(list)
            blocklistFile.writeBytes(encryptedData)
            cachedBlocklist = list.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a direct message for adult content detection.
     * Zordon speaks directly about preserving vital energy.
     * Randomly selects from multiple motivational messages.
     */
    fun getConversationPrompt(): String {
        return CONVERSATION_PROMPTS.random()
    }

    /**
     * Direct message for adult content detection - warns about vital energy.
     * Randomly selects from multiple short messages.
     */
    fun getInterventionMessage(): String {
        return INTERVENTION_MESSAGES.random()
    }

    companion object {
        /**
         * Very strict limit for adult content - only 1 minute allowed
         * After this, the app will be closed and Zordon will intervene
         */
        const val SUGGESTED_LIMIT_MS = 1 * 60 * 1000L // 1 minute - strict limit

        /**
         * Grace period before forcefully closing the app (gives user time to react)
         */
        const val GRACE_PERIOD_MS = 10 * 1000L // 10 seconds warning before close

        /**
         * Short intervention messages displayed when blocking adult content.
         * Direct, powerful reminders from Zordon about preserving vital energy.
         */
        val INTERVENTION_MESSAGES = listOf(
            "Don't waste your vital source.",
            "Protect your energy, Ranger.",
            "Your power is sacred. Guard it.",
            "This path weakens you. Turn back.",
            "Preserve your life force.",
            "Your strength comes from discipline.",
            "Channel your energy wisely.",
            "A true warrior guards their essence.",
            "Your vitality is your greatest weapon.",
            "Rise above. You're better than this.",
            "Reclaim your power, Ranger.",
            "Your future self will thank you.",
            "Break the chain. Choose strength.",
            "Real power comes from self-control.",
            "You're stronger than this temptation."
        )

        /**
         * Full conversation prompts for Zordon when discussing adult content intervention.
         * Motivational and focused on redirecting energy toward positive goals.
         */
        val CONVERSATION_PROMPTS = listOf(
            "Ranger! Don't waste your vital source. This path drains your power and focus. Your energy is meant for greater things - return to your mission!",
            "I sense a disturbance in your focus, Ranger. Your vital energy is precious - it fuels your dreams, your ambitions, your true potential. Let's redirect it toward something meaningful.",
            "Ranger, your life force is your greatest asset. Every moment spent here weakens your shield against life's real challenges. Stand tall and walk away.",
            "The path you're on leads nowhere good. Your energy, your drive, your passion - these are meant for building, creating, connecting. Not this. Rise up!",
            "I've watched many Rangers fall to this trap. But you? You're different. You have the strength to choose better. Your vital source powers everything you want to achieve.",
            "This content is designed to drain you - your time, your energy, your motivation. You're worth more than that. Your mission awaits, Ranger.",
            "Every great warrior knows: true strength comes from what you resist, not what you give in to. Your vital energy is calling you to something greater.",
            "Ranger, I'm not here to judge - I'm here to remind you of your power. You have dreams to chase, goals to crush, a life to build. Don't let this steal your fire.",
            "Your ancestors didn't survive countless challenges so you could drain your life force here. Honor your potential. Your vital energy deserves better.",
            "The strongest Rangers aren't those who never face temptation - they're the ones who choose to walk away. Your vital source thanks you for protecting it.",
            "Think about who you want to become. Does that person waste their precious energy here? No. That person channels their power into greatness. Be that person now.",
            "Your brain is lying to you right now. This won't satisfy you - it'll leave you emptier. Your vital source knows the truth. Trust it and step away.",
            "Ranger, real connection, real achievement, real satisfaction - none of it comes from this path. Your energy is meant for the real world. Go claim it.",
            "Every time you choose to walk away, you get stronger. Your vital force grows. Your willpower sharpens. This is how Rangers are forged.",
            "I believe in you, Ranger. Not because you're perfect, but because you have the courage to keep trying. Protect your vital source. Your mission needs you at full power."
        )
    }
}
