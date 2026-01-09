"""Unit tests for StartMonitoring use case"""
import pytest
from src.application.use_cases.start_monitoring import StartMonitoringUseCase


class TestStartMonitoringUseCase:
    """Test suite for StartMonitoring use case"""

    def test_start_monitoring_success(self, session_with_websites, mock_config_repository, mock_scheduler):
        """Test successfully starting monitoring"""
        use_case = StartMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler
        )

        def dummy_callback():
            pass

        use_case.execute(dummy_callback)

        assert session_with_websites.is_active is True
        mock_config_repository.save_session.assert_called_once()
        mock_scheduler.start.assert_called_once()

    def test_start_monitoring_with_callback(self, session_with_websites, mock_config_repository, mock_scheduler):
        """Test that scheduler is started with correct parameters"""
        use_case = StartMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler
        )

        def callback():
            pass

        use_case.execute(callback)

        mock_scheduler.start.assert_called_once_with(
            interval=session_with_websites.monitoring_interval,
            callback=callback
        )

    def test_start_already_active_session_raises_error(self, session_with_websites, mock_config_repository, mock_scheduler):
        """Test that starting already active session raises error"""
        session_with_websites.start()
        use_case = StartMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler
        )

        with pytest.raises(ValueError, match="already active"):
            use_case.execute(lambda: None)

    def test_start_empty_session_raises_error(self, empty_session, mock_config_repository, mock_scheduler):
        """Test that starting empty session raises error"""
        use_case = StartMonitoringUseCase(
            session=empty_session,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler
        )

        with pytest.raises(ValueError, match="no websites"):
            use_case.execute(lambda: None)

    def test_start_monitoring_persists_config(self, session_with_websites, mock_config_repository, mock_scheduler):
        """Test that starting monitoring persists configuration"""
        use_case = StartMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler
        )

        use_case.execute(lambda: None)

        mock_config_repository.save_session.assert_called_once_with(session_with_websites)
