"""Pytest configuration and shared fixtures"""
import pytest
from unittest.mock import Mock
from src.core.entities.monitoring_session import MonitoringSession
from src.core.value_objects.url import URL
from src.application.interfaces.http_checker import IHttpChecker
from src.application.interfaces.alert_notifier import IAlertNotifier
from src.application.interfaces.config_repository import IConfigRepository
from src.application.interfaces.monitoring_scheduler import IMonitoringScheduler


@pytest.fixture
def mock_http_checker():
    """Mock HTTP checker for testing"""
    return Mock(spec=IHttpChecker)


@pytest.fixture
def mock_alert_notifier():
    """Mock alert notifier for testing"""
    return Mock(spec=IAlertNotifier)


@pytest.fixture
def mock_config_repository():
    """Mock config repository for testing"""
    mock = Mock(spec=IConfigRepository)
    mock.save_session.return_value = True
    return mock


@pytest.fixture
def mock_scheduler():
    """Mock scheduler for testing"""
    return Mock(spec=IMonitoringScheduler)


@pytest.fixture
def empty_session():
    """Empty monitoring session for testing"""
    return MonitoringSession()


@pytest.fixture
def session_with_websites():
    """Monitoring session with some websites for testing"""
    session = MonitoringSession()
    session.add_website(URL("google.com"))
    session.add_website(URL("yahoo.com"))
    return session
