"""
Tests for enforce agreement use case.
"""
import pytest
import time
from unittest.mock import Mock

from src.application.use_cases.enforce_agreement import EnforceAgreementUseCase
from src.core.entities.agreement import Agreement


@pytest.fixture
def mock_browser_controller():
    """Create mock browser controller."""
    controller = Mock()
    controller.close_tab_by_url.return_value = True
    return controller


@pytest.fixture
def enforcement_use_case(mock_browser_controller):
    """Create enforcement use case."""
    return EnforceAgreementUseCase(
        browser_controller=mock_browser_controller,
        grace_period_seconds=2.0  # Short grace period for testing
    )


@pytest.fixture
def expired_agreement():
    """Create expired agreement."""
    agreement = Agreement.create(
        event_type="endless_scrolling",
        url="https://reddit.com",
        process_name=None,
        agreed_duration_minutes=0.01,  # Very short
        user_response="test",
        counselor_message="test"
    )

    # Wait for expiration
    time.sleep(1)

    return agreement


class TestEnforceAgreementUseCase:
    """Test enforcement logic."""

    def test_enforce_not_executed_if_not_expired(self, enforcement_use_case):
        """Enforcement not executed if agreement not expired."""
        agreement = Agreement.create(
            event_type="test",
            url="https://test.com",
            process_name=None,
            agreed_duration_minutes=10.0,  # Not expired
            user_response="test",
            counselor_message="test"
        )

        result = enforcement_use_case.enforce(agreement)

        assert result is False

    def test_enforce_starts_grace_period(self, enforcement_use_case, expired_agreement):
        """First enforcement call starts grace period."""
        warning_callback = Mock()

        result = enforcement_use_case.enforce(
            expired_agreement,
            on_warning=warning_callback
        )

        # Should not enforce yet, just warn
        assert result is False
        assert warning_callback.call_count == 1

    def test_enforce_waits_for_grace_period(self, enforcement_use_case, expired_agreement):
        """Enforcement waits for grace period to expire."""
        # Start grace period
        enforcement_use_case.enforce(expired_agreement)

        # Try to enforce immediately (grace period not over)
        result = enforcement_use_case.enforce(expired_agreement)

        assert result is False

    def test_enforce_executes_after_grace_period(self, enforcement_use_case, expired_agreement, mock_browser_controller):
        """Enforcement executes after grace period."""
        # Start grace period
        enforcement_use_case.enforce(expired_agreement)

        # Wait for grace period
        time.sleep(2.5)

        enforced_callback = Mock()

        result = enforcement_use_case.enforce(
            expired_agreement,
            on_enforced=enforced_callback
        )

        # Should enforce now
        assert result is True
        assert enforced_callback.call_count == 1
        mock_browser_controller.close_tab_by_url.assert_called_once_with(expired_agreement.url)

    def test_enforce_force_skips_grace_period(self, enforcement_use_case, expired_agreement, mock_browser_controller):
        """Force enforcement skips grace period."""
        enforced_callback = Mock()

        result = enforcement_use_case.enforce(
            expired_agreement,
            force=True,
            on_enforced=enforced_callback
        )

        # Should enforce immediately
        assert result is True
        assert enforced_callback.call_count == 1
        mock_browser_controller.close_tab_by_url.assert_called_once()

    def test_send_warning(self, enforcement_use_case):
        """Warning can be sent."""
        agreement = Agreement.create(
            event_type="test",
            url="https://test.com",
            process_name=None,
            agreed_duration_minutes=10.0,
            user_response="test",
            counselor_message="test"
        )

        warning_callback = Mock()

        enforcement_use_case.send_warning(
            agreement,
            seconds_remaining=60.0,
            on_warning=warning_callback
        )

        assert warning_callback.call_count == 1

    def test_get_grace_period_remaining(self, enforcement_use_case, expired_agreement):
        """Grace period remaining can be retrieved."""
        # Start grace period
        enforcement_use_case.enforce(expired_agreement)

        remaining = enforcement_use_case.get_grace_period_remaining(expired_agreement.id)

        assert remaining is not None
        assert 0 < remaining <= 2.0

    def test_cancel_grace_period(self, enforcement_use_case, expired_agreement):
        """Grace period can be cancelled."""
        # Start grace period
        enforcement_use_case.enforce(expired_agreement)

        enforcement_use_case.cancel_grace_period(expired_agreement.id)

        remaining = enforcement_use_case.get_grace_period_remaining(expired_agreement.id)
        assert remaining is None

    def test_enforcement_deactivates_agreement(self, enforcement_use_case, expired_agreement):
        """Enforcement deactivates the agreement."""
        assert expired_agreement.is_active is True

        enforcement_use_case.enforce(expired_agreement, force=True)

        assert expired_agreement.is_active is False
