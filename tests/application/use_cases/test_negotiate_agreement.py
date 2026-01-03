"""
Tests for negotiate agreement use case.
"""
import pytest
from datetime import datetime

from src.application.use_cases.negotiate_agreement import NegotiateAgreementUseCase
from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent


@pytest.fixture
def negotiation_use_case():
    """Create negotiation use case."""
    return NegotiateAgreementUseCase(max_negotiation_rounds=3)


@pytest.fixture
def scrolling_event():
    """Create endless scrolling event."""
    return BehavioralEvent(
        event_type="endless_scrolling",
        severity="medium",
        url="https://reddit.com",
        process_name=None,
        duration_seconds=180.0,  # 3 minutes
        detected_at=datetime.now(),
        metadata={'site_type': 'infinite_scroll'}
    )


@pytest.fixture
def adult_content_event():
    """Create adult content event."""
    return BehavioralEvent(
        event_type="adult_content",
        severity="high",
        url="https://bad-site.com",
        process_name=None,
        duration_seconds=0.0,
        detected_at=datetime.now(),
        metadata={'matched_pattern': 'xxx'}
    )


class TestNegotiateAgreementUseCase:
    """Test negotiation dialogue logic."""

    def test_start_negotiation_scrolling(self, negotiation_use_case, scrolling_event):
        """Starting negotiation for scrolling generates appropriate message."""
        message, requires_response = negotiation_use_case.start_negotiation(
            scrolling_event,
            {'type': 'negotiate'}
        )

        assert requires_response is True
        assert "scrolling" in message.lower()
        assert "reddit" in message.lower()
        assert "how" in message.lower() or "longer" in message.lower()

    def test_start_negotiation_adult_content(self, negotiation_use_case, adult_content_event):
        """Starting negotiation for adult content is immediate."""
        message, requires_response = negotiation_use_case.start_negotiation(
            adult_content_event,
            {'type': 'block'}
        )

        assert requires_response is True
        assert "inappropriate" in message.lower() or "stop" in message.lower()

    def test_parse_time_from_response_minutes(self, negotiation_use_case):
        """Parse time from various minute formats."""
        assert negotiation_use_case._parse_time_from_response("10 minutes") == 10.0
        assert negotiation_use_case._parse_time_from_response("5 min") == 5.0
        assert negotiation_use_case._parse_time_from_response("just 2 more minutes") == 2.0
        assert negotiation_use_case._parse_time_from_response("15m") == 15.0

    def test_parse_time_from_response_hours(self, negotiation_use_case):
        """Parse time from hour formats."""
        assert negotiation_use_case._parse_time_from_response("1 hour") == 60.0
        assert negotiation_use_case._parse_time_from_response("2 hours") == 120.0
        assert negotiation_use_case._parse_time_from_response("half hour") == 30.0

    def test_parse_time_from_response_just_number(self, negotiation_use_case):
        """Parse time from just a number."""
        assert negotiation_use_case._parse_time_from_response("10") == 10.0
        assert negotiation_use_case._parse_time_from_response("5") == 5.0

    def test_parse_time_from_response_no_time(self, negotiation_use_case):
        """Returns None when no time found."""
        assert negotiation_use_case._parse_time_from_response("okay") is None
        assert negotiation_use_case._parse_time_from_response("yes") is None
        assert negotiation_use_case._parse_time_from_response("fine") is None

    def test_reasonable_time_accepted(self, negotiation_use_case, scrolling_event):
        """Reasonable time request is accepted."""
        negotiation_use_case.start_negotiation(scrolling_event, {'type': 'negotiate'})

        next_msg, continue_neg, agreement = negotiation_use_case.process_user_response(
            user_response="10 minutes",
            event=scrolling_event,
            original_message="test"
        )

        assert next_msg is None
        assert continue_neg is False
        assert agreement is not None
        assert agreement.agreed_duration_minutes == 10.0

    def test_excessive_time_countered(self, negotiation_use_case, scrolling_event):
        """Excessive time request gets counter-offer."""
        negotiation_use_case.start_negotiation(scrolling_event, {'type': 'negotiate'})

        next_msg, continue_neg, agreement = negotiation_use_case.process_user_response(
            user_response="60 minutes",  # Too much
            event=scrolling_event,
            original_message="test"
        )

        assert next_msg is not None
        assert continue_neg is True
        assert agreement is None
        assert "excessive" in next_msg.lower() or "how about" in next_msg.lower()

    def test_no_time_specified_asks_again(self, negotiation_use_case, scrolling_event):
        """When no time specified, asks again."""
        negotiation_use_case.start_negotiation(scrolling_event, {'type': 'negotiate'})

        next_msg, continue_neg, agreement = negotiation_use_case.process_user_response(
            user_response="okay",  # No time
            event=scrolling_event,
            original_message="test"
        )

        assert next_msg is not None
        assert continue_neg is True
        assert agreement is None
        assert "minutes" in next_msg.lower()

    def test_max_rounds_imposes_limit(self, negotiation_use_case, scrolling_event):
        """After max rounds, limit is imposed."""
        negotiation_use_case.start_negotiation(scrolling_event, {'type': 'negotiate'})

        # Round 1
        msg1, cont1, agr1 = negotiation_use_case.process_user_response(
            "okay", scrolling_event, "test"
        )
        assert cont1 is True

        # Round 2
        msg2, cont2, agr2 = negotiation_use_case.process_user_response(
            "fine", scrolling_event, "test"
        )
        assert cont2 is True

        # Round 3 - Should impose limit
        msg3, cont3, agr3 = negotiation_use_case.process_user_response(
            "whatever", scrolling_event, "test"
        )
        assert cont3 is False
        assert agr3 is not None
        assert agr3.agreed_duration_minutes > 0

    def test_adult_content_no_negotiation(self, negotiation_use_case, adult_content_event):
        """Adult content doesn't allow negotiation."""
        negotiation_use_case.start_negotiation(adult_content_event, {'type': 'block'})

        next_msg, continue_neg, agreement = negotiation_use_case.process_user_response(
            user_response="10 minutes",
            event=adult_content_event,
            original_message="test"
        )

        assert next_msg is None
        assert continue_neg is False
        assert agreement is not None
        assert agreement.agreed_duration_minutes == 0.0  # Immediate stop

    def test_reset_clears_state(self, negotiation_use_case, scrolling_event):
        """Reset clears negotiation state."""
        negotiation_use_case.start_negotiation(scrolling_event, {'type': 'negotiate'})
        negotiation_use_case.process_user_response("okay", scrolling_event, "test")

        assert negotiation_use_case.current_round > 0

        negotiation_use_case.reset()

        assert negotiation_use_case.current_round == 0
