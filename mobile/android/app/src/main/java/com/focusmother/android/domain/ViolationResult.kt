package com.focusmother.android.domain

import com.focusmother.android.data.entity.Agreement

/**
 * Result of checking for agreement violations.
 *
 * Indicates the action to take based on the current state of active agreements:
 * - NONE: No action needed (agreement still active or no relevant agreements)
 * - VIOLATION: User violated an agreement (continued using app after expiry)
 * - COMPLETION: User successfully honored an agreement (stopped using app before/at expiry)
 *
 * @property action The action to take
 * @property violatedAgreement The agreement that was violated (if action is VIOLATION)
 * @property expiredAgreement The agreement that was successfully completed (if action is COMPLETION)
 */
data class ViolationResult(
    val action: Action,
    val violatedAgreement: Agreement? = null,
    val expiredAgreement: Agreement? = null
) {
    /**
     * Actions that can be taken based on violation check.
     */
    enum class Action {
        /**
         * No action needed - agreement still active or no relevant agreements.
         */
        NONE,

        /**
         * User violated an agreement - continued using app/phone after time expired.
         * The violatedAgreement property contains the violated agreement.
         */
        VIOLATION,

        /**
         * User successfully completed an agreement - stopped using app/phone on time.
         * The expiredAgreement property contains the completed agreement.
         */
        COMPLETION
    }

    companion object {
        /**
         * Creates a result indicating no action is needed.
         */
        fun none(): ViolationResult {
            return ViolationResult(Action.NONE)
        }

        /**
         * Creates a result indicating a violation occurred.
         *
         * @param agreement The violated agreement
         */
        fun violation(agreement: Agreement): ViolationResult {
            return ViolationResult(Action.VIOLATION, violatedAgreement = agreement)
        }

        /**
         * Creates a result indicating an agreement was completed successfully.
         *
         * @param agreement The completed agreement
         */
        fun completion(agreement: Agreement): ViolationResult {
            return ViolationResult(Action.COMPLETION, expiredAgreement = agreement)
        }
    }
}
