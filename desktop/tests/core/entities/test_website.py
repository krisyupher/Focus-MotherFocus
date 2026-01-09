"""Unit tests for Website entity"""
import pytest
from datetime import datetime
from src.core.entities.website import Website
from src.core.value_objects.url import URL


class TestWebsite:
    """Test suite for Website entity"""

    def test_create_website(self):
        """Test creating a website"""
        url = URL("google.com")
        website = Website(url=url)
        assert website.url == url
        assert website.is_online is False
        assert website.last_check_time is None
        assert website.added_at is not None

    def test_mark_online(self):
        """Test marking website as online"""
        website = Website(url=URL("google.com"))
        check_time = datetime.now()
        website.mark_online(check_time)
        assert website.is_online is True
        assert website.last_check_time == check_time

    def test_mark_offline(self):
        """Test marking website as offline"""
        website = Website(url=URL("google.com"))
        check_time = datetime.now()
        website.mark_offline(check_time)
        assert website.is_online is False
        assert website.last_check_time == check_time

    def test_update_status_online(self):
        """Test updating status to online"""
        website = Website(url=URL("google.com"))
        check_time = datetime.now()
        status_changed = website.update_status(True, check_time)
        assert status_changed is True
        assert website.is_online is True
        assert website.last_check_time == check_time

    def test_update_status_offline(self):
        """Test updating status to offline"""
        website = Website(url=URL("google.com"))
        website.mark_online()
        check_time = datetime.now()
        status_changed = website.update_status(False, check_time)
        assert status_changed is True
        assert website.is_online is False
        assert website.last_check_time == check_time

    def test_update_status_no_change(self):
        """Test updating status when it doesn't change"""
        website = Website(url=URL("google.com"))
        status_changed = website.update_status(False)
        assert status_changed is False
        assert website.is_online is False

    def test_get_url_string(self):
        """Test getting URL as string"""
        website = Website(url=URL("google.com"))
        assert website.get_url_string() == "https://google.com"

    def test_website_equality(self):
        """Test that websites with same URL are equal"""
        url = URL("google.com")
        website1 = Website(url=url)
        website2 = Website(url=url)
        assert website1 == website2

    def test_website_inequality(self):
        """Test that websites with different URLs are not equal"""
        website1 = Website(url=URL("google.com"))
        website2 = Website(url=URL("yahoo.com"))
        assert website1 != website2

    def test_website_hash(self):
        """Test that websites can be hashed"""
        url = URL("google.com")
        website1 = Website(url=url)
        website2 = Website(url=url)
        website_set = {website1, website2}
        assert len(website_set) == 1

    def test_website_str(self):
        """Test string representation"""
        website = Website(url=URL("google.com"))
        assert "google.com" in str(website)
        assert "OFFLINE" in str(website)

    def test_website_str_online(self):
        """Test string representation when online"""
        website = Website(url=URL("google.com"))
        website.mark_online()
        assert "google.com" in str(website)
        assert "ONLINE" in str(website)
