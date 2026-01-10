package com.focusmother.android.data.dao

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.entity.AvatarConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: FocusMotherDatabase
    private lateinit var avatarDao: AvatarDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusMotherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        avatarDao = database.avatarDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_avatarConfig_savesSuccessfully() = runTest {
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/avatar.glb",
            thumbnailPath = "/data/avatar_thumb.png",
            readyPlayerMeId = "rpm-123"
        )
        avatarDao.insert(avatar)
        val retrieved = avatarDao.getAvatar()
        assertNotNull("Avatar should be found", retrieved)
        assertEquals("URL should match", "https://models.readyplayer.me/123.glb", retrieved?.avatarUrl)
    }

    @Test
    fun insert_secondAvatar_replacesFirst() = runTest {
        val first = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/first.glb",
            localGlbPath = "/data/first.glb",
            thumbnailPath = "/data/first_thumb.png",
            readyPlayerMeId = "rpm-first"
        )
        avatarDao.insert(first)
        
        val second = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/second.glb",
            localGlbPath = "/data/second.glb",
            thumbnailPath = "/data/second_thumb.png",
            readyPlayerMeId = "rpm-second"
        )
        avatarDao.insert(second)
        
        val retrieved = avatarDao.getAvatar()
        assertEquals("Should have second avatar", "rpm-second", retrieved?.readyPlayerMeId)
    }

    @Test
    fun getAvatar_noAvatarSet_returnsNull() = runTest {
        val avatar = avatarDao.getAvatar()
        assertNull("Should return null when no avatar", avatar)
    }

    @Test
    fun hasAvatar_noAvatar_returnsFalse() = runTest {
        val hasAvatar = avatarDao.hasAvatar()
        assertFalse("Should return false when no avatar", hasAvatar)
    }

    @Test
    fun hasAvatar_withAvatar_returnsTrue() = runTest {
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/avatar.glb",
            thumbnailPath = "/data/avatar_thumb.png",
            readyPlayerMeId = "rpm-123"
        )
        avatarDao.insert(avatar)
        val hasAvatar = avatarDao.hasAvatar()
        assertTrue("Should return true when avatar exists", hasAvatar)
    }

    @Test
    fun updatePaths_modifiesExisting() = runTest {
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/old.glb",
            thumbnailPath = "/data/old_thumb.png",
            readyPlayerMeId = "rpm-123"
        )
        avatarDao.insert(avatar)
        
        val newTime = System.currentTimeMillis()
        avatarDao.updatePaths("/data/new.glb", "/data/new_thumb.png", newTime)
        
        val updated = avatarDao.getAvatar()
        assertEquals("GLB path should be updated", "/data/new.glb", updated?.localGlbPath)
        assertEquals("Thumbnail should be updated", "/data/new_thumb.png", updated?.thumbnailPath)
        assertEquals("Last modified should be updated", newTime, updated?.lastModified)
    }

    @Test
    fun updateCustomization_modifiesData() = runTest {
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/avatar.glb",
            thumbnailPath = "/data/avatar_thumb.png",
            readyPlayerMeId = "rpm-123",
            customizationData = "{}"
        )
        avatarDao.insert(avatar)
        
        val newCustomization = """{"hairColor": "blue", "skinTone": "light"}"""
        val newTime = System.currentTimeMillis()
        avatarDao.updateCustomization(newCustomization, newTime)
        
        val updated = avatarDao.getAvatar()
        assertEquals("Customization should be updated", newCustomization, updated?.customizationData)
        assertEquals("Last modified should be updated", newTime, updated?.lastModified)
    }

    @Test
    fun deleteAll_removesAvatar() = runTest {
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/avatar.glb",
            thumbnailPath = "/data/avatar_thumb.png",
            readyPlayerMeId = "rpm-123"
        )
        avatarDao.insert(avatar)
        
        avatarDao.deleteAll()
        val retrieved = avatarDao.getAvatar()
        
        assertNull("Avatar should be deleted", retrieved)
        assertFalse("hasAvatar should return false", avatarDao.hasAvatar())
    }

    @Test
    fun insert_withComplexCustomizationData_preservesJson() = runTest {
        val complexJson = """
            {
                "hair": {"style": "long", "color": "#FF5733"},
                "skin": {"tone": "medium", "texture": "smooth"},
                "clothing": ["shirt", "pants", "shoes"]
            }
        """.trimIndent()
        
        val avatar = AvatarConfig.create(
            avatarUrl = "https://models.readyplayer.me/123.glb",
            localGlbPath = "/data/avatar.glb",
            thumbnailPath = "/data/avatar_thumb.png",
            readyPlayerMeId = "rpm-123",
            customizationData = complexJson
        )
        avatarDao.insert(avatar)
        
        val retrieved = avatarDao.getAvatar()
        assertEquals("JSON should be preserved", complexJson, retrieved?.customizationData)
    }
}
