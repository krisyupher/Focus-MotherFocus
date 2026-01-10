package com.focusmother.android.domain

import android.content.Context
import com.focusmother.android.data.dao.AgreementDao
import com.focusmother.android.data.entity.Agreement
import com.focusmother.android.monitor.AppUsageInfo
import com.focusmother.android.monitor.UsageMonitor
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ContextBuilder.
 *
 * Tests context gathering from multiple sources including UsageMonitor,
 * CategoryManager, and AgreementDao. Validates proper formatting and
 * edge case handling.
 */
class ContextBuilderTest {

    private lateinit var contextBuilder: ContextBuilder
    private lateinit var mockContext: Context
    private lateinit var mockUsageMonitor: UsageMonitor
    private lateinit var mockCategoryManager: CategoryManager
    private lateinit var mockAgreementDao: AgreementDao

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockUsageMonitor = mockk()
        mockCategoryManager = mockk()
        mockAgreementDao = mockk()

        contextBuilder = ContextBuilder(
            usageMonitor = mockUsageMonitor,
            categoryManager = mockCategoryManager,
            agreementDao = mockAgreementDao
        )
    }

    @Test
    fun `buildContext returns valid context with all data`() = runTest {
        // Arrange
        val screenTime = 2 * 60 * 60 * 1000L + 45 * 60 * 1000L // 2h 45m
        val topApps = listOf(
            AppUsageInfo("com.instagram.android", "Instagram", 0L, 60 * 60 * 1000L),
            AppUsageInfo("com.youtube.android", "YouTube", 0L, 30 * 60 * 1000L)
        )
        val agreements = listOf(
            Agreement.create(
                appPackageName = "com.instagram.android",
                appName = "Instagram",
                appCategory = "SOCIAL_MEDIA",
                agreedDurationMs = 10 * 60 * 1000L,
                conversationId = 1L
            )
        )

        coEvery { mockUsageMonitor.getTodayScreenTime() } returns screenTime
        coEvery { mockUsageMonitor.getTopApps(5) } returns topApps
        coEvery { mockCategoryManager.categorizeApp("com.instagram.android") } returns "SOCIAL_MEDIA"
        coEvery { mockAgreementDao.getRecent(5) } returns agreements

        // Act
        val result = contextBuilder.buildContext(
            currentApp = "com.instagram.android",
            interventionReason = "Continuous usage detected"
        )

        // Assert
        assertEquals("2h 45m", result.todayScreenTime)
        assertEquals("Instagram", result.currentApp)
        assertEquals("SOCIAL_MEDIA", result.currentAppCategory)
        assertEquals("Continuous usage detected", result.interventionReason)
        assertEquals(1, result.recentAgreements.size)
        assertNotNull(result.appUsageToday)
    }

    @Test
    fun `buildContext formats screen time correctly for hours and minutes`() = runTest {
        // Arrange
        val screenTime = 3 * 60 * 60 * 1000L + 15 * 60 * 1000L // 3h 15m
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns screenTime
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals("3h 15m", result.todayScreenTime)
    }

    @Test
    fun `buildContext formats screen time correctly for minutes only`() = runTest {
        // Arrange
        val screenTime = 45 * 60 * 1000L // 45m
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns screenTime
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals("45m", result.todayScreenTime)
    }

    @Test
    fun `buildContext formats screen time correctly for less than 1 minute`() = runTest {
        // Arrange
        val screenTime = 30 * 1000L // 30 seconds
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns screenTime
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals("<1m", result.todayScreenTime)
    }

    @Test
    fun `buildContext handles null current app`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "General usage"
        )

        // Assert
        assertNull(result.currentApp)
        assertNull(result.currentAppCategory)
    }

    @Test
    fun `buildContext resolves app name from package name`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns listOf(
            AppUsageInfo("com.facebook.katana", "Facebook", 0L, 1000L)
        )
        coEvery { mockCategoryManager.categorizeApp("com.facebook.katana") } returns "SOCIAL_MEDIA"
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = "com.facebook.katana",
            interventionReason = "Test"
        )

        // Assert
        assertEquals("Facebook", result.currentApp)
    }

    @Test
    fun `buildContext uses package name when app name not found`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockCategoryManager.categorizeApp("com.unknown.app") } returns "UNKNOWN"
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = "com.unknown.app",
            interventionReason = "Test"
        )

        // Assert
        assertEquals("com.unknown.app", result.currentApp)
    }

    @Test
    fun `buildContext builds app usage summary correctly`() = runTest {
        // Arrange
        val topApps = listOf(
            AppUsageInfo("com.instagram.android", "Instagram", 0L, 60 * 60 * 1000L),
            AppUsageInfo("com.youtube.android", "YouTube", 0L, 45 * 60 * 1000L),
            AppUsageInfo("com.tiktok", "TikTok", 0L, 30 * 60 * 1000L)
        )
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns topApps
        coEvery { mockCategoryManager.categorizeApp(any()) } returns "SOCIAL_MEDIA"
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assert(result.appUsageToday.contains("Instagram"))
        assert(result.appUsageToday.contains("1h 0m"))
        assert(result.appUsageToday.contains("YouTube"))
        assert(result.appUsageToday.contains("45m"))
    }

    @Test
    fun `buildContext handles empty top apps list`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals("No significant app usage today", result.appUsageToday)
    }

    @Test
    fun `buildContext retrieves recent agreements correctly`() = runTest {
        // Arrange
        val agreements = listOf(
            Agreement.create("com.instagram.android", "Instagram", "SOCIAL_MEDIA", 10 * 60 * 1000L, 1L),
            Agreement.create("com.youtube.android", "YouTube", "ENTERTAINMENT", 15 * 60 * 1000L, 2L),
            Agreement.create("com.tiktok", "TikTok", "SOCIAL_MEDIA", 5 * 60 * 1000L, 3L)
        )
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns agreements

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals(3, result.recentAgreements.size)
        assertEquals("Instagram", result.recentAgreements[0].appName)
        assertEquals("YouTube", result.recentAgreements[1].appName)
        assertEquals("TikTok", result.recentAgreements[2].appName)
    }

    @Test
    fun `buildContext handles empty agreements list`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = null,
            interventionReason = "Test"
        )

        // Assert
        assertEquals(0, result.recentAgreements.size)
    }

    @Test
    fun `buildContext categorizes current app correctly`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns listOf(
            AppUsageInfo("com.instagram.android", "Instagram", 0L, 1000L)
        )
        coEvery { mockCategoryManager.categorizeApp("com.instagram.android") } returns "SOCIAL_MEDIA"
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = "com.instagram.android",
            interventionReason = "Test"
        )

        // Assert
        assertEquals("SOCIAL_MEDIA", result.currentAppCategory)
    }

    @Test
    fun `buildContext handles unknown app category`() = runTest {
        // Arrange
        coEvery { mockUsageMonitor.getTodayScreenTime() } returns 1000L
        coEvery { mockUsageMonitor.getTopApps(5) } returns emptyList()
        coEvery { mockCategoryManager.categorizeApp("com.unknown.app") } returns "UNKNOWN"
        coEvery { mockAgreementDao.getRecent(5) } returns emptyList()

        // Act
        val result = contextBuilder.buildContext(
            currentApp = "com.unknown.app",
            interventionReason = "Test"
        )

        // Assert
        assertEquals("UNKNOWN", result.currentAppCategory)
    }
}
