"""Unit tests for JsonConfigRepository"""
import pytest
import os
import json
import tempfile
from src.infrastructure.persistence.json_config_repository import JsonConfigRepository
from src.core.entities.monitoring_session import MonitoringSession
from src.core.value_objects.url import URL


class TestJsonConfigRepository:
    """Test suite for JsonConfigRepository"""

    @pytest.fixture
    def temp_config_file(self):
        """Create a temporary config file for testing"""
        fd, path = tempfile.mkstemp(suffix='.json')
        os.close(fd)
        yield path
        # Cleanup
        if os.path.exists(path):
            os.remove(path)

    def test_save_empty_session(self, temp_config_file):
        """Test saving an empty session"""
        repository = JsonConfigRepository(temp_config_file)
        session = MonitoringSession()

        result = repository.save_session(session)

        assert result is True
        assert os.path.exists(temp_config_file)

        # Verify file contents
        with open(temp_config_file, 'r') as f:
            data = json.load(f)
        assert data["websites"] == []
        assert data["monitoring_interval"] == 10

    def test_save_session_with_websites(self, temp_config_file):
        """Test saving a session with websites"""
        repository = JsonConfigRepository(temp_config_file)
        session = MonitoringSession(monitoring_interval=30)
        session.add_website(URL("google.com"))
        session.add_website(URL("yahoo.com"))

        result = repository.save_session(session)

        assert result is True

        # Verify file contents
        with open(temp_config_file, 'r') as f:
            data = json.load(f)
        assert len(data["websites"]) == 2
        assert "https://google.com" in data["websites"]
        assert "https://yahoo.com" in data["websites"]
        assert data["monitoring_interval"] == 30

    def test_load_nonexistent_file(self, temp_config_file):
        """Test loading when config file doesn't exist"""
        # Remove the temp file
        os.remove(temp_config_file)

        repository = JsonConfigRepository(temp_config_file)
        session = repository.load_session()

        assert isinstance(session, MonitoringSession)
        assert session.get_website_count() == 0
        assert session.monitoring_interval == 10

    def test_load_session_with_websites(self, temp_config_file):
        """Test loading a session with websites"""
        # Create config file
        config_data = {
            "websites": ["https://google.com", "https://yahoo.com"],
            "monitoring_interval": 30
        }
        with open(temp_config_file, 'w') as f:
            json.dump(config_data, f)

        repository = JsonConfigRepository(temp_config_file)
        session = repository.load_session()

        assert session.get_website_count() == 2
        assert session.has_website(URL("google.com"))
        assert session.has_website(URL("yahoo.com"))
        assert session.monitoring_interval == 30

    def test_load_session_with_invalid_url(self, temp_config_file):
        """Test loading session with invalid URL skips it"""
        config_data = {
            "websites": ["https://google.com", "", "https://yahoo.com"],
            "monitoring_interval": 10
        }
        with open(temp_config_file, 'w') as f:
            json.dump(config_data, f)

        repository = JsonConfigRepository(temp_config_file)
        session = repository.load_session()

        # Should only load valid URLs
        assert session.get_website_count() == 2

    def test_round_trip_save_load(self, temp_config_file):
        """Test saving and loading produces same data"""
        repository = JsonConfigRepository(temp_config_file)

        # Create and save session
        original_session = MonitoringSession(monitoring_interval=20)
        original_session.add_website(URL("google.com"))
        original_session.add_website(URL("yahoo.com"))
        repository.save_session(original_session)

        # Load session
        loaded_session = repository.load_session()

        assert loaded_session.get_website_count() == original_session.get_website_count()
        assert loaded_session.monitoring_interval == original_session.monitoring_interval
        assert loaded_session.has_website(URL("google.com"))
        assert loaded_session.has_website(URL("yahoo.com"))
