"""
Tests for track agreements use case.
"""
import pytest
import time
from datetime import datetime
from unittest.mock import Mock

from src.application.use_cases.track_agreements import TrackAgreementsUseCase
from src.core.entities.agreement import Agreement
from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent


@pytest.fixture
def tracker():
    """Create agreement tracker."""
    return TrackAgreementsUseCase(
        grace_period_seconds=30.0,
        warning_before_seconds=60.0
    )


@pytest.fixture
def test_agreement():
    """Create test agreement."""
    return Agreement.create(
        event_type="endless_scrolling",
        url="https://reddit.com",
        process_name=None,
        agreed_duration_minutes=0.05,  # 3 seconds for testing
        user_response="test",
        counselor_message="test"
    )


class TestTrackAgreementsUseCase:
    """Test agreement tracking logic."""

    def test_add_agreement(self, tracker, test_agreement):
        """Agreement can be added to tracking."""
        tracker.add_agreement(test_agreement)

        active = tracker.get_active_agreements()
        assert len(active) == 1
        assert active[0].id == test_agreement.id

    def test_remove_agreement(self, tracker, test_agreement):
        """Agreement can be removed from tracking."""
        tracker.add_agreement(test_agreement)
        tracker.remove_agreement(test_agreement.id)

        active = tracker.get_active_agreements()
        assert len(active) == 0

    def test_get_active_agreements_excludes_expired(self, tracker, test_agreement):
        """Expired agreements not included in active list."""
        tracker.add_agreement(test_agreement)

        # Wait for expiration
        time.sleep(4)

        active = tracker.get_active_agreements()
        assert len(active) == 0  # Expired, so not active

    def test_get_expired_agreements(self, tracker, test_agreement):
        """Expired agreements can be retrieved."""
        tracker.add_agreement(test_agreement)

        # Wait for expiration
        time.sleep(4)

        expired = tracker.get_expired_agreements()
        assert len(expired) == 1
        assert expired[0].id == test_agreement.id

    def test_check_compliance_calls_warning(self, tracker):
        """Warning callback called when approaching expiration."""
        agreement = Agreement.create(
            event_type="test",
            url="https://test.com",
            process_name=None,
            agreed_duration_minutes=1.0,  # 1 minute
            user_response="test",
            counselor_message="test"
        )

        tracker.add_agreement(agreement)
        tracker.warning_before_seconds = 120.0  # Warn within 2 minutes

        warning_callback = Mock()

        tracker.check_compliance(
            current_event=None,
            on_warning=warning_callback
        )

        # Should trigger warning since 1 minute < 2 minutes threshold
        assert warning_callback.call_count == 1

    def test_check_compliance_calls_expired(self, tracker, test_agreement):
        """Expired callback called when agreement expires."""
        tracker.add_agreement(test_agreement)

        # Wait for expiration
        time.sleep(4)

        expired_callback = Mock()

        tracker.check_compliance(
            current_event=None,
            on_expired=expired_callback
        )

        assert expired_callback.call_count == 1

    def test_check_compliance_detects_violation(self, tracker):
        """Violation detected when user continues activity after expiration."""
        agreement = Agreement.create(
            event_type="endless_scrolling",
            url="https://reddit.com",
            process_name=None,
            agreed_duration_minutes=0.05,  # 3 seconds
            user_response="test",
            counselor_message="test"
        )

        tracker.add_agreement(agreement)

        # Wait for expiration
        time.sleep(4)

        # User still scrolling Reddit!
        current_event = BehavioralEvent(
            event_type="endless_scrolling",
            severity="medium",
            url="https://reddit.com",
            process_name=None,
            duration_seconds=10.0,
            detected_at=datetime.now(),
            metadata={}
        )

        violation_callback = Mock()

        tracker.check_compliance(
            current_event=current_event,
            on_violation=violation_callback
        )

        # Should detect violation
        assert violation_callback.call_count == 1
        assert agreement.is_violated is True

    def test_get_agreement_status(self, tracker, test_agreement):
        """Agreement status can be retrieved."""
        tracker.add_agreement(test_agreement)

        status = tracker.get_agreement_status(test_agreement.id)

        assert status is not None
        assert status['id'] == test_agreement.id
        assert status['status'] == 'active'
        assert 'time_remaining_minutes' in status

    def test_get_summary(self, tracker, test_agreement):
        """Summary provides statistics."""
        tracker.add_agreement(test_agreement)

        summary = tracker.get_summary()

        assert summary['total_tracked'] == 1
        assert summary['active'] == 1
        assert len(summary['agreements']) == 1

    def test_cleanup_inactive(self, tracker, test_agreement):
        """Inactive agreements can be cleaned up."""
        tracker.add_agreement(test_agreement)

        # Deactivate
        test_agreement.deactivate()

        removed = tracker.cleanup_inactive()

        assert removed == 1
        assert len(tracker._active_agreements) == 0
