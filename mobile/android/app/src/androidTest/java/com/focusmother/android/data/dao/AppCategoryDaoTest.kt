package com.focusmother.android.data.dao

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.entity.AppCategoryMapping
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppCategoryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: FocusMotherDatabase
    private lateinit var appCategoryDao: AppCategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FocusMotherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appCategoryDao = database.appCategoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insert_systemMapping_savesSuccessfully() = runTest {
        val mapping = AppCategoryMapping.systemMapping(
            packageName = "com.instagram.android",
            category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA
        )
        appCategoryDao.insert(mapping)
        val retrieved = appCategoryDao.getByPackage("com.instagram.android")
        assertNotNull("Mapping should be found", retrieved)
        assertEquals("Category should match", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, retrieved?.category)
    }

    @Test
    fun insert_replaceExisting_updatesMapping() = runTest {
        val original = AppCategoryMapping.systemMapping(
            packageName = "com.test.app",
            category = AppCategoryMapping.CATEGORY_GAMES
        )
        appCategoryDao.insert(original)
        
        val updated = AppCategoryMapping.userMapping(
            packageName = "com.test.app",
            category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA,
            isBlocked = true
        )
        appCategoryDao.insert(updated)
        
        val retrieved = appCategoryDao.getByPackage("com.test.app")
        assertEquals("Category should be updated", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, retrieved?.category)
        assertTrue("Should be blocked", retrieved?.isBlocked == true)
        assertEquals("Added by should be USER", AppCategoryMapping.ADDED_BY_USER, retrieved?.addedBy)
    }

    @Test
    fun insertIfNotExists_preservesExisting() = runTest {
        val original = AppCategoryMapping.userMapping(
            packageName = "com.test.app",
            category = AppCategoryMapping.CATEGORY_GAMES,
            isBlocked = true
        )
        appCategoryDao.insert(original)
        
        val system = AppCategoryMapping.systemMapping(
            packageName = "com.test.app",
            category = AppCategoryMapping.CATEGORY_SOCIAL_MEDIA
        )
        appCategoryDao.insertIfNotExists(system)
        
        val retrieved = appCategoryDao.getByPackage("com.test.app")
        assertEquals("Should preserve user category", AppCategoryMapping.CATEGORY_GAMES, retrieved?.category)
        assertTrue("Should preserve blocked status", retrieved?.isBlocked == true)
    }

    @Test
    fun getByCategory_filtersByCategory() = runTest {
        appCategoryDao.insert(
            AppCategoryMapping.systemMapping("com.instagram.android", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA),
            AppCategoryMapping.systemMapping("com.facebook.android", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA),
            AppCategoryMapping.systemMapping("com.game.app", AppCategoryMapping.CATEGORY_GAMES)
        )
        
        val socialMedia = appCategoryDao.getByCategory(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA)
        assertEquals("Should have 2 social media apps", 2, socialMedia.size)
    }

    @Test
    fun getBlocked_returnsOnlyBlocked() = runTest {
        appCategoryDao.insert(
            AppCategoryMapping.systemMapping("com.app1.android", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, isBlocked = true),
            AppCategoryMapping.systemMapping("com.app2.android", AppCategoryMapping.CATEGORY_GAMES, isBlocked = false),
            AppCategoryMapping.systemMapping("com.app3.android", AppCategoryMapping.CATEGORY_ADULT_CONTENT, isBlocked = true)
        )
        
        val blocked = appCategoryDao.getBlocked()
        assertEquals("Should have 2 blocked apps", 2, blocked.size)
    }

    @Test
    fun updateBlockedStatus_modifiesExisting() = runTest {
        val mapping = AppCategoryMapping.systemMapping("com.test.app", AppCategoryMapping.CATEGORY_GAMES)
        appCategoryDao.insert(mapping)
        
        appCategoryDao.updateBlockedStatus("com.test.app", true)
        val updated = appCategoryDao.getByPackage("com.test.app")
        
        assertTrue("Should be blocked", updated?.isBlocked == true)
    }

    @Test
    fun updateCustomThreshold_setsThreshold() = runTest {
        val mapping = AppCategoryMapping.systemMapping("com.test.app", AppCategoryMapping.CATEGORY_GAMES)
        appCategoryDao.insert(mapping)
        
        val threshold = 15 * 60 * 1000L
        appCategoryDao.updateCustomThreshold("com.test.app", threshold)
        val updated = appCategoryDao.getByPackage("com.test.app")
        
        assertEquals("Threshold should match", threshold, updated?.customThreshold)
    }

    @Test
    fun delete_removesMapping() = runTest {
        val mapping = AppCategoryMapping.systemMapping("com.test.app", AppCategoryMapping.CATEGORY_GAMES)
        appCategoryDao.insert(mapping)
        
        appCategoryDao.delete("com.test.app")
        val retrieved = appCategoryDao.getByPackage("com.test.app")
        
        assertNull("Mapping should be deleted", retrieved)
    }

    @Test
    fun deleteAll_removesAllMappings() = runTest {
        appCategoryDao.insert(
            AppCategoryMapping.systemMapping("com.app1.android", AppCategoryMapping.CATEGORY_SOCIAL_MEDIA),
            AppCategoryMapping.systemMapping("com.app2.android", AppCategoryMapping.CATEGORY_GAMES)
        )
        appCategoryDao.deleteAll()
        val all = appCategoryDao.getAll()
        assertTrue("Should be empty", all.isEmpty())
    }
}
