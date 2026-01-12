package com.focusmother.android.ui.legal

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TermsOfServiceActivity.
 *
 * Tests the Terms of Service activity functionality including:
 * - Activity launches successfully
 * - Document content loads from assets
 * - Back button navigation works
 * - Accept button appears in onboarding mode
 * - Activity result codes are correct (RESULT_OK, RESULT_CANCELED)
 * - HTML content is displayed properly
 *
 * These tests run on an Android device or emulator.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class TermsOfServiceActivityTest {

    @Test
    fun activityLaunches_inViewOnlyMode_successfully() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            // Verify activity is in resumed state
            it.onActivity { activity ->
                assert(activity != null)
                assert(!activity.isFinishing)
            }
        }
    }

    @Test
    fun activityLaunches_inOnboardingMode_successfully() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = true
        )

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            it.onActivity { activity ->
                assert(activity != null)
                assert(!activity.isFinishing)
            }
        }
    }

    @Test
    fun titleIsDisplayed_correctly() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            // Wait for content to load
            Thread.sleep(1000)

            // Verify title is displayed
            onView(withText("Terms of Service"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun htmlContent_loadsFromAssets() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            // Wait for WebView to load content
            Thread.sleep(2000)

            // Verify WebView exists (content is loaded)
            // Note: WebView content is hard to test with Espresso
            // We verify the activity doesn't crash and stays open
            it.onActivity { activity ->
                assert(!activity.isFinishing)
            }
        }
    }

    @Test
    fun backButton_finishesActivity() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // When
        scenario.use {
            // Click back button
            onView(withContentDescription("Back")).perform(click())

            // Then
            Thread.sleep(500)
            it.onActivity { activity ->
                assert(activity.isFinishing || activity.isDestroyed)
            }
        }
    }

    @Test
    fun acceptButton_notShown_inViewOnlyMode() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = false
        )

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            Thread.sleep(1000)

            // Verify accept button does not exist
            onView(withText("I Accept"))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun acceptButton_shown_inOnboardingMode() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = true
        )

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then
        scenario.use {
            Thread.sleep(1000)

            // Verify accept button exists
            onView(withText("I Accept"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun acceptButton_click_returnsResultOk() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = true
        )
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // When
        scenario.use {
            // Wait for content to load
            Thread.sleep(2000)

            // Click accept button
            onView(withText("I Accept")).perform(click())

            // Then
            Thread.sleep(500)
            it.onActivity { activity ->
                // Activity should be finishing
                assert(activity.isFinishing || activity.isDestroyed)
            }

            // Verify result code
            assert(scenario.result.resultCode == Activity.RESULT_OK) {
                "Expected RESULT_OK but got ${scenario.result.resultCode}"
            }
        }
    }

    @Test
    fun backButton_inOnboardingMode_returnsResultCanceled() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = true
        )
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // When
        scenario.use {
            Thread.sleep(1000)

            // Click back button
            onView(withContentDescription("Back")).perform(click())

            // Then
            Thread.sleep(500)

            // Verify result code
            assert(scenario.result.resultCode == Activity.RESULT_CANCELED) {
                "Expected RESULT_CANCELED but got ${scenario.result.resultCode}"
            }
        }
    }

    @Test
    fun createIntent_withShowAcceptFalse_launchesCorrectly() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = false
        )

        // Then
        assert(intent.component?.className == TermsOfServiceActivity::class.java.name)
        assert(intent.getBooleanExtra("show_accept_button", true) == false)
    }

    @Test
    fun createIntent_withShowAcceptTrue_launchesCorrectly() {
        // Given
        val intent = TermsOfServiceActivity.createIntent(
            ApplicationProvider.getApplicationContext(),
            showAcceptButton = true
        )

        // Then
        assert(intent.component?.className == TermsOfServiceActivity::class.java.name)
        assert(intent.getBooleanExtra("show_accept_button", false) == true)
    }

    @Test
    fun activity_doesNotCrash_withMissingExtras() {
        // Given - Intent without extras (edge case)
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)
        // Don't add any extras

        // When
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // Then - Should not crash
        scenario.use {
            Thread.sleep(1000)
            it.onActivity { activity ->
                assert(!activity.isFinishing)
            }
        }
    }

    @Test
    fun activity_handlesConfigurationChange_gracefully() {
        // Given
        val intent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)
        val scenario = ActivityScenario.launch<TermsOfServiceActivity>(intent)

        // When - Rotate device (simulate configuration change)
        scenario.use {
            Thread.sleep(1000)

            it.recreate()
            Thread.sleep(1000)

            // Then - Activity should still be alive
            it.onActivity { activity ->
                assert(!activity.isFinishing)
            }
        }
    }

    @Test
    fun termsOfService_differentFromPrivacyPolicy() {
        // This test ensures we're loading the correct document
        // Given
        val tosIntent = Intent(ApplicationProvider.getApplicationContext(), TermsOfServiceActivity::class.java)
        val ppIntent = Intent(ApplicationProvider.getApplicationContext(), PrivacyPolicyActivity::class.java)

        // When
        val tosScenario = ActivityScenario.launch<TermsOfServiceActivity>(tosIntent)
        tosScenario.use {
            Thread.sleep(1000)
            onView(withText("Terms of Service")).check(matches(isDisplayed()))
        }

        val ppScenario = ActivityScenario.launch<PrivacyPolicyActivity>(ppIntent)
        ppScenario.use {
            Thread.sleep(1000)
            onView(withText("Privacy Policy")).check(matches(isDisplayed()))
        }

        // Then - Activities should display different titles
        // (This verifies we're not accidentally loading the same content)
    }
}
