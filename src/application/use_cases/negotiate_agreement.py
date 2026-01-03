"""
Negotiate Agreement Use Case.

Handles multi-turn negotiation dialogue with user to reach
mutually acceptable time limits and behavioral agreements.
"""
import re
from typing import Optional, Tuple

from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent
from src.core.entities.agreement import Agreement


class NegotiateAgreementUseCase:
    """
    Use case for negotiating agreements with users.

    Implements counselor dialogue logic:
    1. Present the detected behavior
    2. Ask user how much longer they need
    3. Evaluate user's request
    4. Counter-offer if needed (max 3 rounds)
    5. Reach agreement or impose limit
    """

    def __init__(self, max_negotiation_rounds: int = 3):
        """
        Initialize negotiation use case.

        Args:
            max_negotiation_rounds: Maximum dialogue turns before imposing limit
        """
        self.max_negotiation_rounds = max_negotiation_rounds
        self.current_round = 0

    def start_negotiation(
        self,
        event: BehavioralEvent,
        recommendation: dict
    ) -> Tuple[str, bool]:
        """
        Start negotiation dialogue.

        Args:
            event: Behavioral event that triggered negotiation
            recommendation: Intervention recommendation

        Returns:
            Tuple of (counselor_message, requires_user_response)
        """
        self.current_round = 0

        # Construct opening message based on event type
        if event.event_type == "endless_scrolling":
            duration_minutes = int(event.duration_seconds / 60)
            message = (
                f"I noticed you've been scrolling {event.url or 'this site'} "
                f"for {duration_minutes} minutes.\n\n"
                f"How much longer do you need?"
            )
        elif event.event_type == "distraction_site":
            category = event.metadata.get('category', 'distraction')
            message = (
                f"You're on {category}: {event.url}\n\n"
                f"Is this for work or leisure? How long do you need?"
            )
        elif event.event_type == "adult_content":
            message = (
                "I've detected inappropriate content.\n\n"
                "This needs to stop immediately. Do you understand?"
            )
        else:
            message = (
                f"I need to discuss your focus on {event.url or 'this activity'}.\n\n"
                f"How much longer do you need?"
            )

        return message, True

    def process_user_response(
        self,
        user_response: str,
        event: BehavioralEvent,
        original_message: str
    ) -> Tuple[Optional[str], bool, Optional[Agreement]]:
        """
        Process user's response and decide next step.

        Args:
            user_response: What user said
            event: Original behavioral event
            original_message: Original counselor message

        Returns:
            Tuple of (counselor_message, requires_more_negotiation, agreement_if_reached)
        """
        self.current_round += 1

        # Parse user's requested time
        requested_minutes = self._parse_time_from_response(user_response)

        # Special handling for adult content (no negotiation)
        if event.event_type == "adult_content":
            agreement = Agreement.create(
                event_type=event.event_type,
                url=event.url,
                process_name=event.process_name,
                agreed_duration_minutes=0.0,  # Immediate stop
                user_response=user_response,
                counselor_message="You must stop viewing inappropriate content immediately."
            )
            return None, False, agreement

        # No time specified - ask again
        if requested_minutes is None:
            if self.current_round >= self.max_negotiation_rounds:
                # Impose default limit
                default_limit = self._get_default_limit(event)
                agreement = Agreement.create(
                    event_type=event.event_type,
                    url=event.url,
                    process_name=event.process_name,
                    agreed_duration_minutes=default_limit,
                    user_response=user_response,
                    counselor_message=f"Since we can't agree, I'm setting a {default_limit}-minute limit."
                )
                return None, False, agreement
            else:
                message = "Please tell me how many minutes you need. For example: '10 minutes' or '15 min'."
                return message, True, None

        # Evaluate requested time
        is_reasonable, counter_offer = self._evaluate_time_request(
            requested_minutes,
            event
        )

        if is_reasonable:
            # Accept request
            agreement = Agreement.create(
                event_type=event.event_type,
                url=event.url,
                process_name=event.process_name,
                agreed_duration_minutes=requested_minutes,
                user_response=user_response,
                counselor_message=f"Okay, I'll check back in {requested_minutes} minutes. Stay focused!"
            )
            return None, False, agreement

        else:
            # Counter-offer
            if self.current_round >= self.max_negotiation_rounds:
                # Impose limit after too many rounds
                agreement = Agreement.create(
                    event_type=event.event_type,
                    url=event.url,
                    process_name=event.process_name,
                    agreed_duration_minutes=counter_offer,
                    user_response=user_response,
                    counselor_message=f"We've negotiated enough. {counter_offer} minutes, final offer."
                )
                return None, False, agreement
            else:
                message = (
                    f"{requested_minutes} minutes seems excessive.\n\n"
                    f"How about {counter_offer} minutes instead?"
                )
                return message, True, None

    def _parse_time_from_response(self, response: str) -> Optional[float]:
        """
        Extract time duration from user response.

        Examples:
        - "10 minutes" -> 10.0
        - "5 min" -> 5.0
        - "half an hour" -> 30.0
        - "just 2 more minutes" -> 2.0

        Args:
            response: User's text response

        Returns:
            Minutes as float, or None if not found
        """
        response_lower = response.lower()

        # Pattern 1: "X minutes" or "X min"
        pattern1 = r'(\d+\.?\d*)\s*(?:minutes?|mins?|m\b)'
        match = re.search(pattern1, response_lower)
        if match:
            return float(match.group(1))

        # Pattern 2: "X hours"
        pattern2 = r'(\d+\.?\d*)\s*(?:hours?|hrs?|h\b)'
        match = re.search(pattern2, response_lower)
        if match:
            return float(match.group(1)) * 60.0

        # Pattern 3: Common phrases
        if 'half hour' in response_lower or '30 min' in response_lower:
            return 30.0
        if 'quarter hour' in response_lower or '15 min' in response_lower:
            return 15.0
        if 'hour' in response_lower and 'half' not in response_lower:
            return 60.0

        # Pattern 4: Just a number (assume minutes)
        pattern4 = r'\b(\d+)\b'
        match = re.search(pattern4, response_lower)
        if match:
            num = int(match.group(1))
            if 1 <= num <= 180:  # Reasonable range (1-180 minutes)
                return float(num)

        return None

    def _evaluate_time_request(
        self,
        requested_minutes: float,
        event: BehavioralEvent
    ) -> Tuple[bool, float]:
        """
        Evaluate if requested time is reasonable.

        Args:
            requested_minutes: Time user requested
            event: Original behavioral event

        Returns:
            Tuple of (is_reasonable, counter_offer_minutes)
        """
        # Define limits by event type
        if event.event_type == "endless_scrolling":
            max_reasonable = 15.0
            counter_offer = min(requested_minutes * 0.6, 10.0)

        elif event.event_type == "distraction_site":
            severity = event.severity
            if severity == "high":
                max_reasonable = 5.0
                counter_offer = 5.0
            elif severity == "medium":
                max_reasonable = 10.0
                counter_offer = min(requested_minutes * 0.7, 10.0)
            else:
                max_reasonable = 20.0
                counter_offer = min(requested_minutes * 0.8, 15.0)

        elif event.event_type == "adult_content":
            max_reasonable = 0.0
            counter_offer = 0.0

        else:
            max_reasonable = 15.0
            counter_offer = min(requested_minutes * 0.7, 10.0)

        is_reasonable = requested_minutes <= max_reasonable
        return is_reasonable, counter_offer

    def _get_default_limit(self, event: BehavioralEvent) -> float:
        """
        Get default time limit when negotiation fails.

        Args:
            event: Behavioral event

        Returns:
            Default limit in minutes
        """
        if event.event_type == "endless_scrolling":
            return 10.0
        elif event.event_type == "distraction_site":
            return 5.0 if event.severity == "high" else 10.0
        elif event.event_type == "adult_content":
            return 0.0
        else:
            return 10.0

    def reset(self) -> None:
        """Reset negotiation state for new dialogue."""
        self.current_round = 0
