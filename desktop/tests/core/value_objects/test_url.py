"""Unit tests for URL value object"""
import pytest
from src.core.value_objects.url import URL


class TestURL:
    """Test suite for URL value object"""

    def test_create_url_with_https(self):
        """Test creating URL that already has https://"""
        url = URL("https://google.com")
        assert url.value == "https://google.com"

    def test_create_url_with_http(self):
        """Test creating URL that already has http://"""
        url = URL("http://google.com")
        assert url.value == "http://google.com"

    def test_create_url_without_protocol(self):
        """Test creating URL without protocol - should add https://"""
        url = URL("google.com")
        assert url.value == "https://google.com"

    def test_create_url_with_whitespace(self):
        """Test creating URL with surrounding whitespace"""
        url = URL("  google.com  ")
        assert url.value == "https://google.com"

    def test_create_url_empty_raises_error(self):
        """Test that empty URL raises ValueError"""
        with pytest.raises(ValueError, match="URL cannot be empty"):
            URL("")

    def test_create_url_whitespace_only_raises_error(self):
        """Test that whitespace-only URL raises ValueError"""
        with pytest.raises(ValueError, match="URL cannot be empty"):
            URL("   ")

    def test_create_url_with_spaces_raises_error(self):
        """Test that URL with spaces raises ValueError"""
        with pytest.raises(ValueError, match="Invalid URL format"):
            URL("google .com")

    def test_create_url_protocol_only_raises_error(self):
        """Test that protocol-only URL raises ValueError"""
        with pytest.raises(ValueError, match="Invalid URL format"):
            URL("https://")

    def test_from_string_factory(self):
        """Test factory method for creating URL"""
        url = URL.from_string("google.com")
        assert url.value == "https://google.com"

    def test_url_equality(self):
        """Test that URLs with same value are equal"""
        url1 = URL("google.com")
        url2 = URL("https://google.com")
        assert url1 == url2

    def test_url_inequality(self):
        """Test that URLs with different values are not equal"""
        url1 = URL("google.com")
        url2 = URL("yahoo.com")
        assert url1 != url2

    def test_url_hash(self):
        """Test that URLs can be hashed for use in sets/dicts"""
        url1 = URL("google.com")
        url2 = URL("https://google.com")
        url_set = {url1, url2}
        assert len(url_set) == 1  # Should be same URL

    def test_url_str(self):
        """Test string representation"""
        url = URL("google.com")
        assert str(url) == "https://google.com"

    def test_url_repr(self):
        """Test developer representation"""
        url = URL("google.com")
        assert repr(url) == "URL('https://google.com')"

    def test_url_immutable(self):
        """Test that URL is immutable"""
        url = URL("google.com")
        with pytest.raises(AttributeError):
            url.value = "yahoo.com"
