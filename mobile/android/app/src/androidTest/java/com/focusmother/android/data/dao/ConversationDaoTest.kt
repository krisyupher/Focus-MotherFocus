package com.focusmother.android.data.dao

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.entity.ConversationMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: FocusMotherDatabase
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusMotherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        conversationDao = database.conversationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_userMessage_savesSuccessfully() = runTest {
        val message = ConversationMessage.userMessage(
            conversationId = 1L,
            content = "I need help with Instagram"
        )
        conversationDao.insert(message)
        val messages = conversationDao.getConversation(1L)
        assertEquals("Should have 1 message", 1, messages.size)
        assertEquals("Role should be user", ConversationMessage.ROLE_USER, messages[0].role)
    }

    @Test
    fun insert_assistantMessage_includesTokenCount() = runTest {
        val message = ConversationMessage.assistantMessage(
            conversationId = 1L,
            content = "Let me help you create an agreement",
            tokenCount = 250
        )
        conversationDao.insert(message)
        val messages = conversationDao.getConversation(1L)
        assertEquals("Should have 1 message", 1, messages.size)
        assertEquals("Token count should match", 250, messages[0].tokenCount)
    }

    @Test
    fun getConversation_returnsInChronologicalOrder() = runTest {
        val msg1 = ConversationMessage.userMessage(1L, "First message")
        Thread.sleep(10)
        val msg2 = ConversationMessage.assistantMessage(1L, "Second message")
        Thread.sleep(10)
        val msg3 = ConversationMessage.userMessage(1L, "Third message")
        
        conversationDao.insert(msg1, msg2, msg3)
        val messages = conversationDao.getConversation(1L)
        
        assertEquals("Should have 3 messages", 3, messages.size)
        assertTrue("First should be oldest", messages[0].timestamp < messages[1].timestamp)
        assertTrue("Second should be middle", messages[1].timestamp < messages[2].timestamp)
    }

    @Test
    fun getRecent_limitsResults() = runTest {
        for (i in 1..10) {
            val msg = ConversationMessage.userMessage(i.toLong(), "Message $i")
            conversationDao.insert(msg)
            Thread.sleep(5)
        }
        val recent = conversationDao.getRecent(5)
        assertEquals("Should limit to 5 messages", 5, recent.size)
    }

    @Test
    fun deleteOlderThan_removesOldMessages() = runTest {
        val old = ConversationMessage.userMessage(1L, "Old message")
            .copy(timestamp = System.currentTimeMillis() - 100000)
        conversationDao.insert(old)
        
        Thread.sleep(100)
        val cutoff = System.currentTimeMillis()
        Thread.sleep(100)
        
        val recent = ConversationMessage.userMessage(1L, "Recent message")
        conversationDao.insert(recent)
        
        conversationDao.deleteOlderThan(cutoff)
        val remaining = conversationDao.getAll()
        
        assertEquals("Should have 1 message", 1, remaining.size)
        assertEquals("Should keep recent message", "Recent message", remaining[0].content)
    }

    @Test
    fun getTotalTokenUsage_sumsAllTokens() = runTest {
        conversationDao.insert(
            ConversationMessage.assistantMessage(1L, "Message 1", tokenCount = 100),
            ConversationMessage.assistantMessage(1L, "Message 2", tokenCount = 150),
            ConversationMessage.assistantMessage(2L, "Message 3", tokenCount = 75)
        )
        val total = conversationDao.getTotalTokenUsage()
        assertEquals("Total should be 325", 325, total)
    }

    @Test
    fun deleteAll_removesAllMessages() = runTest {
        conversationDao.insert(
            ConversationMessage.userMessage(1L, "Message 1"),
            ConversationMessage.userMessage(2L, "Message 2")
        )
        conversationDao.deleteAll()
        val messages = conversationDao.getAll()
        assertTrue("Should be empty", messages.isEmpty())
    }
}
