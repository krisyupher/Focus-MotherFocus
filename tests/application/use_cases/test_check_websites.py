"""Unit tests for CheckWebsites use case"""
import pytest
from src.application.use_cases.check_websites import CheckWebsitesUseCase
from src.core.value_objects.url import URL


class TestCheckWebsitesUseCase:
    """Test suite for CheckWebsites use case"""

    def test_check_websites_when_online(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test checking websites when they are online"""
        session_with_websites.start()
        mock_http_checker.check_website.return_value = True

        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # Should check both websites
        assert mock_http_checker.check_website.call_count == 2

        # Should send alerts for both
        assert mock_alert_notifier.send_alert.call_count == 2

        # Websites should be marked online
        for website in session_with_websites.get_all_websites():
            assert website.is_online is True

    def test_check_websites_when_offline(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test checking websites when they are offline"""
        session_with_websites.start()
        mock_http_checker.check_website.return_value = False

        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # Should check both websites
        assert mock_http_checker.check_website.call_count == 2

        # Should not send alerts
        assert mock_alert_notifier.send_alert.call_count == 0

        # Websites should be marked offline
        for website in session_with_websites.get_all_websites():
            assert website.is_online is False

    def test_check_websites_mixed_status(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test checking websites with mixed online/offline status"""
        session_with_websites.start()

        # First website online, second offline
        def check_side_effect(url):
            return str(url) == "https://google.com"

        mock_http_checker.check_website.side_effect = check_side_effect

        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # Should send alert for online website only
        assert mock_alert_notifier.send_alert.call_count == 1
        mock_alert_notifier.send_alert.assert_called_with(URL("https://google.com"))

    def test_check_websites_going_offline_clears_alerts(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test that websites going offline clear their alerts"""
        session_with_websites.start()

        # First check: website is online
        mock_http_checker.check_website.return_value = True

        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # Second check: website goes offline
        mock_http_checker.check_website.return_value = False
        use_case.execute()

        # Should clear alerts for offline websites
        assert mock_alert_notifier.clear_alerts.call_count == 2  # Both websites went offline

    def test_check_websites_when_inactive(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test that checking inactive session does nothing"""
        # Don't start the session
        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # Should not check or alert
        assert mock_http_checker.check_website.call_count == 0
        assert mock_alert_notifier.send_alert.call_count == 0

    def test_check_websites_updates_check_time(self, session_with_websites, mock_http_checker, mock_alert_notifier):
        """Test that checking websites updates their last check time"""
        session_with_websites.start()
        mock_http_checker.check_website.return_value = True

        use_case = CheckWebsitesUseCase(
            session=session_with_websites,
            http_checker=mock_http_checker,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        # All websites should have a check time
        for website in session_with_websites.get_all_websites():
            assert website.last_check_time is not None
