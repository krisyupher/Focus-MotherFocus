package com.focusmother.android.domain

import com.focusmother.android.data.entity.Agreement

/**
 * Enforces time agreements by detecting violations and completions.
 *
 * This class implements the business logic for determining whether a user has:
 * - Violated an agreement (continued using app/phone after time expired)
 * - Completed an agreement (stopped using app/phone before/at expiry)
 * - No action needed (agreement still active)
 *
 * The enforcer checks all active agreements against the current app usage
 * and time to determine the appropriate action.
 */
class AgreementEnforcer {

    /**
     * Checks for violations or completions of active agreements.
     *
     * Logic:
     * 1. For each active agreement, check if it has expired
     * 2. If expired and user still on the app -> VIOLATION
     * 3. If expired and user not on the app -> COMPLETION
     * 4. If not expired -> NONE
     *
     * For general phone usage agreements (appPackageName = null):
     * - Applies to any app usage
     * - VIOLATION if any app is in use after expiry
     * - COMPLETION if no app is in use (home screen) after expiry
     *
     * @param agreements List of active agreements to check
     * @param currentApp Package name of currently active app (null if on home screen)
     * @param currentTime Current timestamp in milliseconds
     * @return ViolationResult indicating the action to take
     */
    fun checkViolations(
        agreements: List<Agreement>,
        currentApp: String?,
        currentTime: Long
    ): ViolationResult {
        // No agreements - no action needed
        if (agreements.isEmpty()) {
            return ViolationResult.none()
        }

        // Check each agreement for violations or completions
        for (agreement in agreements) {
            val result = checkSingleAgreement(agreement, currentApp, currentTime)
            if (result.action != ViolationResult.Action.NONE) {
                return result
            }
        }

        // No violations or completions found
        return ViolationResult.none()
    }

    /**
     * Checks a single agreement for violation or completion.
     *
     * @param agreement The agreement to check
     * @param currentApp Current foreground app package name (null if home screen)
     * @param currentTime Current timestamp
     * @return ViolationResult for this specific agreement
     */
    private fun checkSingleAgreement(
        agreement: Agreement,
        currentApp: String?,
        currentTime: Long
    ): ViolationResult {
        // Check if agreement has expired
        if (currentTime < agreement.expiresAt) {
            // Agreement still active - no action
            return ViolationResult.none()
        }

        // Agreement has expired - determine if violation or completion
        return when {
            // General phone usage agreement (applies to any app)
            agreement.appPackageName == null -> {
                handleGeneralUsageAgreement(agreement, currentApp)
            }
            // Specific app agreement
            else -> {
                handleAppSpecificAgreement(agreement, currentApp)
            }
        }
    }

    /**
     * Handles enforcement for general phone usage agreements.
     *
     * @param agreement The general usage agreement
     * @param currentApp Current app (null if home screen)
     * @return ViolationResult
     */
    private fun handleGeneralUsageAgreement(
        agreement: Agreement,
        currentApp: String?
    ): ViolationResult {
        return if (currentApp != null) {
            // User is still on any app after expiry - violation
            ViolationResult.violation(agreement)
        } else {
            // User is on home screen after expiry - completion
            ViolationResult.completion(agreement)
        }
    }

    /**
     * Handles enforcement for app-specific agreements.
     *
     * @param agreement The app-specific agreement
     * @param currentApp Current app package name
     * @return ViolationResult
     */
    private fun handleAppSpecificAgreement(
        agreement: Agreement,
        currentApp: String?
    ): ViolationResult {
        return if (currentApp == agreement.appPackageName) {
            // User is still on the specific app after expiry - violation
            ViolationResult.violation(agreement)
        } else if (currentApp != null) {
            // User switched to a different app - completion
            ViolationResult.completion(agreement)
        } else {
            // User went to home screen - completion
            ViolationResult.completion(agreement)
        }
    }

    /**
     * Gets the time remaining for an agreement.
     *
     * @param agreement The agreement to check
     * @param currentTime Current timestamp
     * @return Time remaining in milliseconds (negative if expired)
     */
    fun getTimeRemaining(agreement: Agreement, currentTime: Long): Long {
        return agreement.expiresAt - currentTime
    }

    /**
     * Checks if an agreement has expired.
     *
     * @param agreement The agreement to check
     * @param currentTime Current timestamp
     * @return True if expired, false otherwise
     */
    fun isExpired(agreement: Agreement, currentTime: Long): Boolean {
        return currentTime >= agreement.expiresAt
    }

    /**
     * Gets the progress percentage of an agreement (0-100).
     *
     * @param agreement The agreement to check
     * @param currentTime Current timestamp
     * @return Progress percentage (0 = just started, 100 = expired)
     */
    fun getProgressPercentage(agreement: Agreement, currentTime: Long): Int {
        val elapsed = currentTime - agreement.createdAt
        val total = agreement.agreedDuration
        val percentage = (elapsed.toDouble() / total.toDouble() * 100).toInt()
        return percentage.coerceIn(0, 100)
    }
}
