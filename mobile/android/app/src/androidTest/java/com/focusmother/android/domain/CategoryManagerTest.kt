package com.focusmother.android.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.data.database.FocusMotherDatabase
import com.focusmother.android.data.entity.AppCategoryMapping
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for CategoryManager.
 *
 * Tests the app categorization system including:
 * - Database seeding with comprehensive app catalog
 * - App categorization (system-defined and user-defined)
 * - Custom thresholds
 * - Blocking functionality
 * - User overrides
 */
@RunWith(AndroidJUnit4::class)
class CategoryManagerTest {

    private lateinit var database: FocusMotherDatabase
    private lateinit var categoryManager: CategoryManager
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = FocusMotherDatabase.getInMemoryDatabase(context)
        categoryManager = CategoryManager(database.appCategoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test that seedDatabase creates all expected mappings.
     */
    @Test
    fun seedDatabase_createsAllMappings() = runTest {
        // Act
        categoryManager.seedDatabase()

        // Assert
        val allMappings = database.appCategoryDao().getAll()

        // Verify we have at least 300 apps seeded
        assertTrue("Expected at least 300 apps, got ${allMappings.size}",
            allMappings.size >= 300)

        // Verify some known apps exist
        val facebook = database.appCategoryDao().getByPackage("com.facebook.katana")
        assertNotNull("Facebook should be seeded", facebook)
        assertEquals(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, facebook?.category)

        val instagram = database.appCategoryDao().getByPackage("com.instagram.android")
        assertNotNull("Instagram should be seeded", instagram)
        assertEquals(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, instagram?.category)

        val youtube = database.appCategoryDao().getByPackage("com.google.android.youtube")
        assertNotNull("YouTube should be seeded", youtube)
        assertEquals(AppCategoryMapping.CATEGORY_ENTERTAINMENT, youtube?.category)

        val chrome = database.appCategoryDao().getByPackage("com.android.chrome")
        assertNotNull("Chrome should be seeded", chrome)
        assertEquals("BROWSER", chrome?.category)

        // Verify all mappings are marked as system-added
        assertTrue("All seed data should be SYSTEM",
            allMappings.all { it.addedBy == AppCategoryMapping.ADDED_BY_SYSTEM })
    }

    /**
     * Test that categorizeApp returns correct category for known apps.
     */
    @Test
    fun categorizeApp_knownSocialMediaApp_returnsCorrectCategory() = runTest {
        // Arrange
        categoryManager.seedDatabase()

        // Act
        val category = categoryManager.categorizeApp("com.tiktok.android")

        // Assert
        assertEquals(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA, category)
    }

    /**
     * Test that categorizeApp returns UNKNOWN for unknown apps.
     */
    @Test
    fun categorizeApp_unknownApp_returnsUnknown() = runTest {
        // Arrange
        categoryManager.seedDatabase()

        // Act
        val category = categoryManager.categorizeApp("com.example.randomapp.unknown")

        // Assert
        assertEquals(CategoryManager.CATEGORY_UNKNOWN, category)
    }

    /**
     * Test that user categorization overrides system categorization.
     */
    @Test
    fun userCategorize_existingApp_overridesSystemCategory() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.facebook.katana"

        // Verify initial category
        assertEquals(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA,
            categoryManager.categorizeApp(packageName))

        // Act - User recategorizes Facebook as productivity (unlikely but tests override)
        categoryManager.userCategorize(packageName, AppCategoryMapping.CATEGORY_PRODUCTIVITY)

        // Assert
        val newCategory = categoryManager.categorizeApp(packageName)
        assertEquals(AppCategoryMapping.CATEGORY_PRODUCTIVITY, newCategory)

        // Verify it's marked as user-added
        val mapping = database.appCategoryDao().getByPackage(packageName)
        assertEquals(AppCategoryMapping.ADDED_BY_USER, mapping?.addedBy)
    }

    /**
     * Test that custom threshold overrides category default threshold.
     */
    @Test
    fun getThreshold_customThreshold_returnsCustomValue() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.instagram.android"
        val customThreshold = 10 * 60 * 1000L // 10 minutes

        // Get default threshold
        val defaultThreshold = categoryManager.getThreshold(packageName)
        assertEquals(AppCategorySeedData.CATEGORY_THRESHOLDS[AppCategoryMapping.CATEGORY_SOCIAL_MEDIA],
            defaultThreshold)

        // Act - Set custom threshold
        categoryManager.setCustomThreshold(packageName, customThreshold)

        // Assert
        val newThreshold = categoryManager.getThreshold(packageName)
        assertEquals(customThreshold, newThreshold)
    }

    /**
     * Test that getThreshold returns category default when no custom threshold is set.
     */
    @Test
    fun getThreshold_noCustomThreshold_returnsCategoryDefault() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.king.candycrushsaga"

        // Act
        val threshold = categoryManager.getThreshold(packageName)

        // Assert - Should return GAMES category default
        assertEquals(AppCategorySeedData.CATEGORY_THRESHOLDS[AppCategoryMapping.CATEGORY_GAMES],
            threshold)
    }

    /**
     * Test that isBlocked correctly identifies blocked apps.
     */
    @Test
    fun isBlocked_appNotBlocked_returnsFalse() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.netflix.mediaclient"

        // Act
        val blocked = categoryManager.isBlocked(packageName)

        // Assert
        assertFalse("Netflix should not be blocked by default", blocked)
    }

    /**
     * Test that setBlocked persists block status.
     */
    @Test
    fun setBlocked_blockApp_persistsBlockStatus() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.twitter.android"

        // Verify initially not blocked
        assertFalse(categoryManager.isBlocked(packageName))

        // Act - Block Twitter
        categoryManager.setBlocked(packageName, true)

        // Assert
        assertTrue("Twitter should be blocked", categoryManager.isBlocked(packageName))

        // Verify in database
        val mapping = database.appCategoryDao().getByPackage(packageName)
        assertTrue("Database should reflect blocked status", mapping?.isBlocked == true)
    }

    /**
     * Test that setBlocked can unblock an app.
     */
    @Test
    fun setBlocked_unblockApp_persistsUnblockedStatus() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val packageName = "com.snapchat.android"
        categoryManager.setBlocked(packageName, true)
        assertTrue(categoryManager.isBlocked(packageName))

        // Act - Unblock Snapchat
        categoryManager.setBlocked(packageName, false)

        // Assert
        assertFalse("Snapchat should be unblocked", categoryManager.isBlocked(packageName))
    }

    /**
     * Test that seeding doesn't overwrite user customizations.
     */
    @Test
    fun seedDatabase_withExistingUserCustomization_preservesUserChanges() = runTest {
        // Arrange - Seed initial data
        categoryManager.seedDatabase()

        // User customizes Instagram
        val packageName = "com.instagram.android"
        categoryManager.userCategorize(packageName, AppCategoryMapping.CATEGORY_PRODUCTIVITY)
        categoryManager.setCustomThreshold(packageName, 5 * 60 * 1000L)
        categoryManager.setBlocked(packageName, true)

        // Act - Seed again (simulating app restart)
        categoryManager.seedDatabase()

        // Assert - User customizations should be preserved
        assertEquals(AppCategoryMapping.CATEGORY_PRODUCTIVITY,
            categoryManager.categorizeApp(packageName))
        assertEquals(5 * 60 * 1000L, categoryManager.getThreshold(packageName))
        assertTrue(categoryManager.isBlocked(packageName))

        val mapping = database.appCategoryDao().getByPackage(packageName)
        assertEquals(AppCategoryMapping.ADDED_BY_USER, mapping?.addedBy)
    }

    /**
     * Test that unknown apps get the UNKNOWN category default threshold.
     */
    @Test
    fun getThreshold_unknownApp_returnsUnknownCategoryDefault() = runTest {
        // Arrange
        categoryManager.seedDatabase()
        val unknownPackage = "com.example.totally.unknown.app"

        // Act
        val threshold = categoryManager.getThreshold(unknownPackage)

        // Assert
        assertEquals(AppCategorySeedData.CATEGORY_THRESHOLDS[CategoryManager.CATEGORY_UNKNOWN],
            threshold)
    }

    /**
     * Test that each major category has apps seeded.
     */
    @Test
    fun seedDatabase_allMajorCategories_haveApps() = runTest {
        // Arrange & Act
        categoryManager.seedDatabase()

        // Assert - Check each category has apps
        val socialMedia = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_SOCIAL_MEDIA)
        assertTrue("Social media should have at least 60 apps", socialMedia.size >= 60)

        val games = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_GAMES)
        assertTrue("Games should have at least 100 apps", games.size >= 100)

        val entertainment = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_ENTERTAINMENT)
        assertTrue("Entertainment should have at least 40 apps", entertainment.size >= 40)

        val browsers = database.appCategoryDao().getByCategory("BROWSER")
        assertTrue("Browsers should have at least 15 apps", browsers.size >= 15)

        val productivity = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_PRODUCTIVITY)
        assertTrue("Productivity should have at least 30 apps", productivity.size >= 30)

        val communication = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_COMMUNICATION)
        assertTrue("Communication should have at least 20 apps", communication.size >= 20)

        val adultContent = database.appCategoryDao().getByCategory(AppCategoryMapping.CATEGORY_ADULT_CONTENT)
        assertTrue("Adult content should have at least 10 placeholder apps", adultContent.size >= 10)
    }
}
