package com.focusmother.android.domain

import com.focusmother.android.data.repository.AgreementRepository
import java.util.regex.Pattern

/**
 * Manages the negotiation state machine for time agreements.
 *
 * Handles the conversation flow between user and AI avatar, tracking the negotiation
 * state and determining when to create agreements. Supports up to 3 rounds of negotiation
 * before forcing an agreement.
 *
 * State transitions:
 * - Initial: Waiting for user to propose time or reject
 * - ProposedTime: User proposed a time, waiting for confirmation
 * - Negotiating: Back-and-forth negotiation in progress
 * - AgreementReached: Final agreement ready to be created
 * - Rejected: User declined and will stop
 *
 * @property responseParser Parser for extracting time from user messages
 * @property agreementRepository Repository for creating agreements
 */
class NegotiationManager(
    private val responseParser: ResponseParser,
    private val agreementRepository: AgreementRepository
) {

    /**
     * Current negotiation state.
     */
    var currentState: NegotiationState = NegotiationState.Initial
        private set

    /**
     * Current negotiation round (1-based).
     * Max of 3 rounds before forcing agreement.
     */
    var negotiationRound: Int = 0
        private set

    companion object {
        private const val MAX_NEGOTIATION_ROUNDS = 3

        // Patterns for detecting affirmative responses
        private val AFFIRMATIVE_PATTERN = Pattern.compile(
            """(?:^|\s)(okay|ok|yes|sure|fine|deal|alright|agreed?|accept)(?:\s|$)""",
            Pattern.CASE_INSENSITIVE
        )

        // Patterns for detecting rejections
        private val REJECTION_PATTERN = Pattern.compile(
            """(?:no|fine|okay|alright).*(?:stop|quit|done|close)""",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Processes a user message and transitions to the next negotiation state.
     *
     * The state machine logic:
     * 1. Check for explicit rejection first
     * 2. Try to extract time duration from message
     * 3. Transition based on current state and extracted information
     *
     * @param message User's message
     * @return New negotiation state after processing
     */
    suspend fun processMessage(message: String): NegotiationState {
        negotiationRound++

        // Check for explicit rejection
        if (isRejection(message)) {
            currentState = NegotiationState.Rejected
            return currentState
        }

        // Try to extract time duration
        val extractedDuration = responseParser.parseDuration(message)

        currentState = when (currentState) {
            is NegotiationState.Initial -> {
                handleInitialState(extractedDuration)
            }
            is NegotiationState.ProposedTime -> {
                handleProposedTimeState(extractedDuration, message)
            }
            is NegotiationState.Negotiating -> {
                handleNegotiatingState(extractedDuration, message)
            }
            is NegotiationState.AgreementReached,
            is NegotiationState.Rejected -> {
                // Terminal states - no further transitions
                currentState
            }
        }

        return currentState
    }

    /**
     * Handles state transitions from Initial state.
     */
    private fun handleInitialState(extractedDuration: Long?): NegotiationState {
        return if (extractedDuration != null) {
            NegotiationState.ProposedTime(extractedDuration)
        } else {
            NegotiationState.Initial
        }
    }

    /**
     * Handles state transitions from ProposedTime state.
     */
    private fun handleProposedTimeState(
        extractedDuration: Long?,
        message: String
    ): NegotiationState {
        val currentDuration = (currentState as NegotiationState.ProposedTime).durationMs

        return when {
            // User proposes a different time - enter negotiation
            extractedDuration != null && extractedDuration != currentDuration -> {
                NegotiationState.Negotiating(extractedDuration)
            }
            // User accepts with affirmative response
            isAffirmative(message) -> {
                NegotiationState.AgreementReached(currentDuration)
            }
            // User proposes same time again - treat as acceptance
            extractedDuration == currentDuration -> {
                NegotiationState.AgreementReached(currentDuration)
            }
            // Unclear response - stay in proposed state
            else -> {
                currentState
            }
        }
    }

    /**
     * Handles state transitions from Negotiating state.
     */
    private fun handleNegotiatingState(
        extractedDuration: Long?,
        message: String
    ): NegotiationState {
        val currentDuration = (currentState as NegotiationState.Negotiating).durationMs

        return when {
            // Max rounds reached - force agreement with current duration
            hasReachedMaxRounds() -> {
                NegotiationState.AgreementReached(currentDuration)
            }
            // User proposes new time - continue negotiating
            extractedDuration != null -> {
                // If max rounds reached after this, force agreement
                if (negotiationRound >= MAX_NEGOTIATION_ROUNDS) {
                    NegotiationState.AgreementReached(extractedDuration)
                } else {
                    NegotiationState.Negotiating(extractedDuration)
                }
            }
            // User accepts with affirmative response
            isAffirmative(message) -> {
                NegotiationState.AgreementReached(currentDuration)
            }
            // Unclear response - stay in negotiating state
            else -> {
                currentState
            }
        }
    }

    /**
     * Checks if a message contains an affirmative response.
     *
     * Detects: okay, yes, sure, fine, deal, alright, agreed, accept
     *
     * @param message User's message
     * @return True if affirmative, false otherwise
     */
    fun isAffirmative(message: String): Boolean {
        return AFFIRMATIVE_PATTERN.matcher(message.lowercase()).find()
    }

    /**
     * Checks if a message contains a rejection.
     *
     * Detects patterns like: "no I'll stop", "fine I quit", "okay I'm done"
     *
     * @param message User's message
     * @return True if rejection, false otherwise
     */
    fun isRejection(message: String): Boolean {
        return REJECTION_PATTERN.matcher(message.lowercase()).find()
    }

    /**
     * Creates a time agreement with the specified parameters.
     *
     * @param durationMinutes Agreement duration in minutes
     * @param appPackage App package name (null for general phone usage)
     * @param appName Display name of the app
     * @param category App category
     * @param conversationId ID of the conversation creating this agreement
     * @return ID of the created agreement
     */
    suspend fun createAgreement(
        durationMinutes: Int,
        appPackage: String?,
        appName: String,
        category: String,
        conversationId: Long
    ): Long {
        val durationMs = durationMinutes * 60 * 1000L
        return agreementRepository.createAgreement(
            appPackage = appPackage,
            appName = appName,
            category = category,
            durationMs = durationMs,
            conversationId = conversationId
        )
    }

    /**
     * Gets the current proposed duration from the current state.
     *
     * @return Duration in milliseconds, or null if no duration proposed
     */
    fun getCurrentProposedDuration(): Long? {
        return when (currentState) {
            is NegotiationState.ProposedTime -> (currentState as NegotiationState.ProposedTime).durationMs
            is NegotiationState.Negotiating -> (currentState as NegotiationState.Negotiating).durationMs
            is NegotiationState.AgreementReached -> (currentState as NegotiationState.AgreementReached).agreedDurationMs
            else -> null
        }
    }

    /**
     * Checks if the maximum number of negotiation rounds has been reached.
     *
     * @return True if at or above max rounds, false otherwise
     */
    fun hasReachedMaxRounds(): Boolean {
        return negotiationRound >= MAX_NEGOTIATION_ROUNDS
    }

    /**
     * Resets the negotiation manager to initial state.
     * Use this when starting a new negotiation session.
     */
    fun reset() {
        currentState = NegotiationState.Initial
        negotiationRound = 0
    }
}
