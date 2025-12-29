"""Unit tests for AddWebsite use case"""
import pytest
from src.application.use_cases.add_website import AddWebsiteUseCase
from src.core.value_objects.url import URL


class TestAddWebsiteUseCase:
    """Test suite for AddWebsite use case"""

    def test_add_website_success(self, empty_session, mock_config_repository):
        """Test successfully adding a website"""
        use_case = AddWebsiteUseCase(
            session=empty_session,
            config_repository=mock_config_repository
        )

        website = use_case.execute("google.com")

        assert website.url == URL("https://google.com")
        assert empty_session.get_website_count() == 1
        mock_config_repository.save_session.assert_called_once()

    def test_add_website_with_protocol(self, empty_session, mock_config_repository):
        """Test adding website that already has protocol"""
        use_case = AddWebsiteUseCase(
            session=empty_session,
            config_repository=mock_config_repository
        )

        website = use_case.execute("https://google.com")

        assert website.url == URL("https://google.com")
        assert empty_session.get_website_count() == 1

    def test_add_duplicate_website_raises_error(self, session_with_websites, mock_config_repository):
        """Test that adding duplicate website raises error"""
        use_case = AddWebsiteUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository
        )

        with pytest.raises(ValueError, match="already being monitored"):
            use_case.execute("google.com")

    def test_add_invalid_url_raises_error(self, empty_session, mock_config_repository):
        """Test that adding invalid URL raises error"""
        use_case = AddWebsiteUseCase(
            session=empty_session,
            config_repository=mock_config_repository
        )

        with pytest.raises(ValueError):
            use_case.execute("")

    def test_add_website_persists_config(self, empty_session, mock_config_repository):
        """Test that adding website persists configuration"""
        use_case = AddWebsiteUseCase(
            session=empty_session,
            config_repository=mock_config_repository
        )

        use_case.execute("google.com")

        mock_config_repository.save_session.assert_called_once_with(empty_session)
