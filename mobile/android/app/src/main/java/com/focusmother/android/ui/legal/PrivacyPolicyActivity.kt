package com.focusmother.android.ui.legal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focusmother.android.ui.theme.FocusMotherFocusTheme

/**
 * Activity for displaying the Privacy Policy legal document.
 *
 * This activity can be launched in two modes:
 * 1. **View-only mode** (from Settings): Shows the privacy policy with just a back button
 * 2. **Onboarding mode**: Shows an "I Accept" button that returns a result to the calling activity
 *
 * To launch in view-only mode:
 * ```kotlin
 * val intent = Intent(context, PrivacyPolicyActivity::class.java)
 * startActivity(intent)
 * ```
 *
 * To launch in onboarding mode:
 * ```kotlin
 * val intent = PrivacyPolicyActivity.createIntent(context, showAcceptButton = true)
 * startActivityForResult(intent, REQUEST_CODE)
 * ```
 *
 * Result codes:
 * - RESULT_OK: User accepted the privacy policy
 * - RESULT_CANCELED: User declined or navigated back
 */
class PrivacyPolicyActivity : ComponentActivity() {

    private var showAcceptButton: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if we should show the accept button (for onboarding flow)
        showAcceptButton = intent.getBooleanExtra(EXTRA_SHOW_ACCEPT_BUTTON, false)

        setContent {
            FocusMotherFocusTheme {
                LegalDocumentScreen(
                    title = "Privacy Policy",
                    htmlFileName = "privacy_policy.html",
                    showAcceptButton = showAcceptButton,
                    onBackClick = { handleBackNavigation() },
                    onAcceptClick = if (showAcceptButton) {
                        { handleAccept() }
                    } else {
                        null
                    }
                )
            }
        }
    }

    /**
     * Handles back navigation based on the current mode.
     * In onboarding mode, returns RESULT_CANCELED.
     * In view-only mode, simply finishes the activity.
     */
    private fun handleBackNavigation() {
        if (showAcceptButton) {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    /**
     * Handles the "I Accept" button click in onboarding mode.
     * Sets RESULT_OK and finishes the activity.
     */
    private fun handleAccept() {
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        private const val EXTRA_SHOW_ACCEPT_BUTTON = "show_accept_button"

        /**
         * Creates an intent to launch PrivacyPolicyActivity.
         *
         * @param context The context to use for creating the intent
         * @param showAcceptButton Whether to show the "I Accept" button (for onboarding)
         * @return Intent configured to launch PrivacyPolicyActivity
         */
        fun createIntent(context: Context, showAcceptButton: Boolean = false): Intent {
            return Intent(context, PrivacyPolicyActivity::class.java).apply {
                putExtra(EXTRA_SHOW_ACCEPT_BUTTON, showAcceptButton)
            }
        }
    }
}
