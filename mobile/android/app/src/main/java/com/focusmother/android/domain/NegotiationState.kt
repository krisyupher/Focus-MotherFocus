package com.focusmother.android.domain

/**
 * Represents the current state of negotiation between user and AI avatar.
 *
 * The negotiation follows a state machine pattern with the following transitions:
 *
 * Initial -> ProposedTime (user proposes a time)
 * Initial -> Rejected (user explicitly declines)
 * ProposedTime -> AgreementReached (user accepts)
 * ProposedTime -> Negotiating (user counter-offers)
 * ProposedTime -> Rejected (user declines)
 * Negotiating -> AgreementReached (agreement reached or max rounds)
 * Negotiating -> Negotiating (continued negotiation)
 * Negotiating -> Rejected (user gives up)
 */
sealed class NegotiationState {

    /**
     * Initial state - no negotiation started yet.
     * Waiting for user to propose a time or reject.
     */
    object Initial : NegotiationState()

    /**
     * User has proposed a time duration.
     * Waiting for user to confirm or counter-offer.
     *
     * @property durationMs Proposed duration in milliseconds
     */
    data class ProposedTime(val durationMs: Long) : NegotiationState()

    /**
     * Active negotiation - user and avatar are exchanging offers.
     * Multiple rounds of back-and-forth.
     *
     * @property durationMs Current proposed duration in milliseconds
     */
    data class Negotiating(val durationMs: Long) : NegotiationState()

    /**
     * Agreement has been reached.
     * Ready to create the time agreement.
     *
     * @property agreedDurationMs Final agreed duration in milliseconds
     */
    data class AgreementReached(val agreedDurationMs: Long) : NegotiationState()

    /**
     * User rejected the negotiation and will stop using the app/phone.
     * No agreement will be created.
     */
    object Rejected : NegotiationState()
}
