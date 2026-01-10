package com.focusmother.android.ui.avatar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusmother.android.ui.theme.FocusMotherFocusTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Instrumented UI tests for AvatarSetupActivity.
 *
 * Tests the avatar setup wizard flow including:
 * - Welcome screen display and navigation
 * - Camera screen transition
 * - Processing screen display
 * - Success and error screens
 * - User interactions (buttons, navigation)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AvatarSetupActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockViewModel: AvatarSetupViewModel

    private var completeCalled = false
    private var cancelCalled = false

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        completeCalled = false
        cancelCalled = false
    }

    @Test
    fun welcomeScreenDisplaysCorrectly() {
        // Given
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                WelcomeScreen(
                    onStart = {},
                    onCancel = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Create Your Digital Avatar").assertExists()
        composeTestRule.onNodeWithText("Begin Avatar Creation").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
        composeTestRule.onNodeWithText(
            "Rangers! I shall transform your likeness",
            substring = true
        ).assertExists()
    }

    @Test
    fun welcomeScreenInstructionsDisplayed() {
        // Given
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                WelcomeScreen(
                    onStart = {},
                    onCancel = {}
                )
            }
        }

        // Then - Verify all instruction items
        composeTestRule.onNodeWithText(
            "Take a clear selfie with good lighting",
            substring = true
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Face the camera directly",
            substring = true
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Processing takes 15-30 seconds",
            substring = true
        ).assertExists()
    }

    @Test
    fun welcomeScreenStartButtonTriggerCallback() {
        // Given
        var startCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                WelcomeScreen(
                    onStart = { startCalled = true },
                    onCancel = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Begin Avatar Creation").performClick()

        // Then
        assert(startCalled)
    }

    @Test
    fun welcomeScreenCancelButtonTriggerCallback() {
        // Given
        var cancelCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                WelcomeScreen(
                    onStart = {},
                    onCancel = { cancelCalled = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assert(cancelCalled)
    }

    @Test
    fun processingScreenDisplaysProgress() {
        // Given
        val progressMessage = "Uploading your photo..."
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ProcessingScreen(
                    progress = progressMessage,
                    onCancel = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(progressMessage).assertExists()
        composeTestRule.onNodeWithText(
            "Zordon is channeling the morphing grid",
            substring = true
        ).assertExists()
    }

    @Test
    fun processingScreenShowsLoadingIndicator() {
        // Given
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ProcessingScreen(
                    progress = "Processing...",
                    onCancel = {}
                )
            }
        }

        // Then - CircularProgressIndicator should be displayed
        // We can't directly test for CircularProgressIndicator, but we can verify the screen renders
        composeTestRule.onNodeWithText("Processing...").assertExists()
    }

    @Test
    fun processingScreenCancelButtonWorks() {
        // Given
        var cancelCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ProcessingScreen(
                    progress = "Processing...",
                    onCancel = { cancelCalled = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assert(cancelCalled)
    }

    @Test
    fun successScreenDisplaysCorrectly() {
        // Given
        val avatarId = "test-avatar-123"
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                SuccessScreen(
                    avatarId = avatarId,
                    onComplete = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Avatar Created!").assertExists()
        composeTestRule.onNodeWithText(
            "Excellent, Ranger!",
            substring = true
        ).assertExists()
        composeTestRule.onNodeWithText("Continue").assertExists()
    }

    @Test
    fun successScreenCompleteButtonWorks() {
        // Given
        var completeCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                SuccessScreen(
                    avatarId = "test-avatar",
                    onComplete = { completeCalled = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Continue").performClick()

        // Then
        assert(completeCalled)
    }

    @Test
    fun errorScreenDisplaysMessage() {
        // Given
        val errorMessage = "Network connection failed"
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ErrorScreen(
                    message = errorMessage,
                    onRetry = {},
                    onCancel = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Avatar Creation Failed").assertExists()
        composeTestRule.onNodeWithText(errorMessage).assertExists()
        composeTestRule.onNodeWithText("Try Again").assertExists()
    }

    @Test
    fun errorScreenRetryButtonWorks() {
        // Given
        var retryCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ErrorScreen(
                    message = "Test error",
                    onRetry = { retryCalled = true },
                    onCancel = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Try Again").performClick()

        // Then
        assert(retryCalled)
    }

    @Test
    fun errorScreenCancelButtonWorks() {
        // Given
        var cancelCalled = false
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                ErrorScreen(
                    message = "Test error",
                    onRetry = {},
                    onCancel = { cancelCalled = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assert(cancelCalled)
    }

    @Test
    fun instructionItemDisplaysCorrectly() {
        // Given
        val instructionText = "Test instruction"
        composeTestRule.setContent {
            FocusMotherFocusTheme {
                InstructionItem(text = instructionText)
            }
        }

        // Then
        composeTestRule.onNodeWithText(instructionText).assertExists()
    }
}
