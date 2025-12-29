"""Unit tests for MonitoringSession entity"""
import pytest
from datetime import datetime
from src.core.entities.monitoring_session import MonitoringSession
from src.core.entities.website import Website
from src.core.value_objects.url import URL


class TestMonitoringSession:
    """Test suite for MonitoringSession entity"""

    def test_create_session(self):
        """Test creating a monitoring session"""
        session = MonitoringSession()
        assert session.monitoring_interval == 10
        assert session.is_active is False
        assert len(session.websites) == 0

    def test_create_session_with_interval(self):
        """Test creating session with custom interval"""
        session = MonitoringSession(monitoring_interval=30)
        assert session.monitoring_interval == 30

    def test_add_website(self):
        """Test adding a website to session"""
        session = MonitoringSession()
        url = URL("google.com")
        website = session.add_website(url)
        assert isinstance(website, Website)
        assert website.url == url
        assert session.get_website_count() == 1

    def test_add_duplicate_website_raises_error(self):
        """Test that adding duplicate website raises error"""
        session = MonitoringSession()
        url = URL("google.com")
        session.add_website(url)
        with pytest.raises(ValueError, match="already being monitored"):
            session.add_website(url)

    def test_remove_website(self):
        """Test removing a website from session"""
        session = MonitoringSession()
        url = URL("google.com")
        session.add_website(url)
        removed = session.remove_website(url)
        assert removed.url == url
        assert session.get_website_count() == 0

    def test_remove_nonexistent_website_raises_error(self):
        """Test that removing nonexistent website raises error"""
        session = MonitoringSession()
        url = URL("google.com")
        with pytest.raises(ValueError, match="is not being monitored"):
            session.remove_website(url)

    def test_get_website(self):
        """Test getting a website by URL"""
        session = MonitoringSession()
        url = URL("google.com")
        session.add_website(url)
        website = session.get_website(url)
        assert website is not None
        assert website.url == url

    def test_get_nonexistent_website(self):
        """Test getting nonexistent website returns None"""
        session = MonitoringSession()
        url = URL("google.com")
        website = session.get_website(url)
        assert website is None

    def test_has_website(self):
        """Test checking if website exists"""
        session = MonitoringSession()
        url = URL("google.com")
        assert session.has_website(url) is False
        session.add_website(url)
        assert session.has_website(url) is True

    def test_get_all_websites(self):
        """Test getting all websites"""
        session = MonitoringSession()
        url1 = URL("google.com")
        url2 = URL("yahoo.com")
        session.add_website(url1)
        session.add_website(url2)
        websites = session.get_all_websites()
        assert len(websites) == 2

    def test_get_online_websites(self):
        """Test getting only online websites"""
        session = MonitoringSession()
        url1 = URL("google.com")
        url2 = URL("yahoo.com")
        session.add_website(url1)
        session.add_website(url2)
        session.get_website(url1).mark_online()
        online = session.get_online_websites()
        assert len(online) == 1
        assert online[0].url == url1

    def test_get_offline_websites(self):
        """Test getting only offline websites"""
        session = MonitoringSession()
        url1 = URL("google.com")
        url2 = URL("yahoo.com")
        session.add_website(url1)
        session.add_website(url2)
        session.get_website(url1).mark_online()
        offline = session.get_offline_websites()
        assert len(offline) == 1
        assert offline[0].url == url2

    def test_update_website_status(self):
        """Test updating website status through session"""
        session = MonitoringSession()
        url = URL("google.com")
        session.add_website(url)
        status_changed = session.update_website_status(url, True)
        assert status_changed is True
        assert session.get_website(url).is_online is True

    def test_update_nonexistent_website_status_raises_error(self):
        """Test updating status of nonexistent website raises error"""
        session = MonitoringSession()
        url = URL("google.com")
        with pytest.raises(ValueError, match="is not being monitored"):
            session.update_website_status(url, True)

    def test_start_session(self):
        """Test starting a monitoring session"""
        session = MonitoringSession()
        session.add_website(URL("google.com"))
        session.start()
        assert session.is_active is True
        assert session.started_at is not None

    def test_start_already_active_session_raises_error(self):
        """Test starting already active session raises error"""
        session = MonitoringSession()
        session.add_website(URL("google.com"))
        session.start()
        with pytest.raises(ValueError, match="already active"):
            session.start()

    def test_start_empty_session_raises_error(self):
        """Test starting session with no websites raises error"""
        session = MonitoringSession()
        with pytest.raises(ValueError, match="no websites"):
            session.start()

    def test_stop_session(self):
        """Test stopping a monitoring session"""
        session = MonitoringSession()
        session.add_website(URL("google.com"))
        session.start()
        session.stop()
        assert session.is_active is False
        assert session.stopped_at is not None

    def test_stop_inactive_session_raises_error(self):
        """Test stopping inactive session raises error"""
        session = MonitoringSession()
        with pytest.raises(ValueError, match="not active"):
            session.stop()

    def test_set_monitoring_interval(self):
        """Test setting monitoring interval"""
        session = MonitoringSession()
        session.set_monitoring_interval(30)
        assert session.monitoring_interval == 30

    def test_set_invalid_interval_raises_error(self):
        """Test setting invalid interval raises error"""
        session = MonitoringSession()
        with pytest.raises(ValueError, match="greater than 0"):
            session.set_monitoring_interval(0)
        with pytest.raises(ValueError, match="greater than 0"):
            session.set_monitoring_interval(-1)

    def test_clear_all_websites(self):
        """Test clearing all websites"""
        session = MonitoringSession()
        session.add_website(URL("google.com"))
        session.add_website(URL("yahoo.com"))
        session.clear_all_websites()
        assert session.get_website_count() == 0

    def test_clear_websites_while_active_raises_error(self):
        """Test clearing websites while monitoring is active raises error"""
        session = MonitoringSession()
        session.add_website(URL("google.com"))
        session.start()
        with pytest.raises(ValueError, match="while monitoring is active"):
            session.clear_all_websites()

    def test_start_resets_website_statuses(self):
        """Test that starting session resets all website statuses"""
        session = MonitoringSession()
        url = URL("google.com")
        session.add_website(url)
        session.get_website(url).mark_online()
        session.start()
        # After stopping and starting, status should be reset
        session.stop()
        session.start()
        assert session.get_website(url).is_online is False
