"""Unit tests for RemoveWebsite use case"""
import pytest
from src.application.use_cases.remove_website import RemoveWebsiteUseCase
from src.core.value_objects.url import URL


class TestRemoveWebsiteUseCase:
    """Test suite for RemoveWebsite use case"""

    def test_remove_website_success(self, session_with_websites, mock_config_repository, mock_alert_notifier):
        """Test successfully removing a website"""
        use_case = RemoveWebsiteUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            alert_notifier=mock_alert_notifier
        )

        website = use_case.execute("google.com")

        assert website.url == URL("https://google.com")
        assert session_with_websites.get_website_count() == 1
        mock_alert_notifier.clear_alerts.assert_called_once()
        mock_config_repository.save_session.assert_called_once()

    def test_remove_nonexistent_website_raises_error(self, session_with_websites, mock_config_repository, mock_alert_notifier):
        """Test that removing nonexistent website raises error"""
        use_case = RemoveWebsiteUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            alert_notifier=mock_alert_notifier
        )

        with pytest.raises(ValueError, match="is not being monitored"):
            use_case.execute("example.com")

    def test_remove_website_clears_alerts(self, session_with_websites, mock_config_repository, mock_alert_notifier):
        """Test that removing website clears its alerts"""
        use_case = RemoveWebsiteUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute("google.com")

        mock_alert_notifier.clear_alerts.assert_called_once_with(URL("https://google.com"))

    def test_remove_website_persists_config(self, session_with_websites, mock_config_repository, mock_alert_notifier):
        """Test that removing website persists configuration"""
        use_case = RemoveWebsiteUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute("google.com")

        mock_config_repository.save_session.assert_called_once_with(session_with_websites)
