package com.focusmother.android.ui.legal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for LegalDocumentScreen composable.
 */
@RunWith(AndroidJUnit4::class)
class LegalDocumentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var backClickCount = 0
    private var acceptClickCount = 0

    @Before
    fun setup() {
        backClickCount = 0
        acceptClickCount = 0
    }

    @Test
    fun legalDocumentScreen_displaysTitle() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = false,
                    onBackClick = {},
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun legalDocumentScreen_displaysDifferentTitle() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Terms of Service",
                    htmlFileName = "terms_of_service.html",
                    showAcceptButton = false,
                    onBackClick = {},
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithText("Terms of Service").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun legalDocumentScreen_backButtonTriggersCallback() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = false,
                    onBackClick = { backClickCount++ },
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Back button callback not triggered", 1, backClickCount)
    }

    @Test
    fun legalDocumentScreen_withoutAcceptButton_doesNotShowButton() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = false,
                    onBackClick = {},
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithText("I Accept").assertDoesNotExist()
    }

    @Test
    fun legalDocumentScreen_withAcceptButton_showsButton() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = true,
                    onBackClick = {},
                    onAcceptClick = { acceptClickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithText("I Accept").assertExists()
    }

    @Test
    fun legalDocumentScreen_showsLoadingIndicator() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = false,
                    onBackClick = {},
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithText("Loading document...").assertExists()
    }

    @Test
    fun legalDocumentScreen_multipleBackClicks_triggerMultipleCallbacks() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = false,
                    onBackClick = { backClickCount++ },
                    onAcceptClick = null
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Expected 2 back clicks", 2, backClickCount)
    }

    @Test
    fun legalDocumentScreen_acceptButtonExistsInOnboardingMode() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Terms of Service",
                    htmlFileName = "terms_of_service.html",
                    showAcceptButton = true,
                    onBackClick = { backClickCount++ },
                    onAcceptClick = { acceptClickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Terms of Service").assertExists()
        composeTestRule.onNodeWithText("I Accept").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun legalDocumentScreen_bothCallbacksAreIndependent() {
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = true,
                    onBackClick = { backClickCount++ },
                    onAcceptClick = { acceptClickCount++ }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Back button callback not triggered", 1, backClickCount)
        assertEquals("Accept callback should not be triggered by back button", 0, acceptClickCount)
    }
}
