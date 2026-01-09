"""
Tests for Agreement entity.
"""
import pytest
from datetime import datetime, timedelta

from src.core.entities.agreement import Agreement


class TestAgreement:
    """Test Agreement entity."""

    def test_create_agreement(self):
        """Test creating an agreement."""
        agreement = Agreement.create(
            event_type="endless_scrolling",
            url="https://reddit.com",
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="10 minutes",
            counselor_message="Okay, I'll check back in 10 minutes"
        )

        assert agreement.event_type == "endless_scrolling"
        assert agreement.url == "https://reddit.com"
        assert agreement.agreed_duration_minutes == 10.0
        assert agreement.is_active is True
        assert agreement.is_violated is False
        assert agreement.id is not None

    def test_is_expired_false_when_new(self):
        """New agreements are not expired."""
        agreement = Agreement.create(
            event_type="distraction_site",
            url="https://youtube.com",
            process_name=None,
            agreed_duration_minutes=15.0,
            user_response="15 minutes",
            counselor_message="15 minutes limit"
        )

        assert agreement.is_expired() is False

    def test_is_expired_true_when_time_passed(self):
        """Agreement expires when time passes."""
        agreement = Agreement.create(
            event_type="distraction_site",
            url="https://youtube.com",
            process_name=None,
            agreed_duration_minutes=0.01,  # Very short duration
            user_response="test",
            counselor_message="test"
        )

        # Wait for expiration
        import time
        time.sleep(1)

        assert agreement.is_expired() is True

    def test_time_remaining_minutes(self):
        """Time remaining is calculated correctly."""
        agreement = Agreement.create(
            event_type="test",
            url=None,
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="test",
            counselor_message="test"
        )

        remaining = agreement.time_remaining_minutes()
        assert 9.9 < remaining <= 10.0  # Should be close to 10 minutes

    def test_time_remaining_zero_when_expired(self):
        """Time remaining is zero when expired."""
        agreement = Agreement.create(
            event_type="test",
            url=None,
            process_name=None,
            agreed_duration_minutes=0.01,
            user_response="test",
            counselor_message="test"
        )

        import time
        time.sleep(1)

        assert agreement.time_remaining_minutes() == 0.0

    def test_mark_violated(self):
        """Agreement can be marked as violated."""
        agreement = Agreement.create(
            event_type="test",
            url=None,
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="test",
            counselor_message="test"
        )

        assert agreement.is_violated is False
        assert agreement.violation_count == 0

        agreement.mark_violated()

        assert agreement.is_violated is True
        assert agreement.violation_count == 1

    def test_deactivate(self):
        """Agreement can be deactivated."""
        agreement = Agreement.create(
            event_type="test",
            url=None,
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="test",
            counselor_message="test"
        )

        assert agreement.is_active is True

        agreement.deactivate()

        assert agreement.is_active is False

    def test_extend(self):
        """Agreement time can be extended."""
        agreement = Agreement.create(
            event_type="test",
            url=None,
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="test",
            counselor_message="test"
        )

        original_expires_at = agreement.expires_at

        agreement.extend(5.0)

        # Should be extended by 5 minutes
        delta = agreement.expires_at - original_expires_at
        assert 4.99 < delta.total_seconds() / 60 < 5.01

    def test_string_representation(self):
        """Agreement string representation is informative."""
        agreement = Agreement.create(
            event_type="endless_scrolling",
            url="https://reddit.com",
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="10 minutes",
            counselor_message="Test"
        )

        str_repr = str(agreement)
        assert "endless_scrolling" in str_repr
        assert "10" in str_repr
        assert "ACTIVE" in str_repr
