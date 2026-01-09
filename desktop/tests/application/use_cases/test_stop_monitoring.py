"""Unit tests for StopMonitoring use case"""
import pytest
from src.application.use_cases.stop_monitoring import StopMonitoringUseCase


class TestStopMonitoringUseCase:
    """Test suite for StopMonitoring use case"""

    def test_stop_monitoring_success(self, session_with_websites, mock_config_repository, mock_scheduler, mock_alert_notifier):
        """Test successfully stopping monitoring"""
        session_with_websites.start()
        use_case = StopMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        assert session_with_websites.is_active is False
        mock_scheduler.stop.assert_called_once()
        mock_alert_notifier.clear_all_alerts.assert_called_once()
        mock_config_repository.save_session.assert_called_once()

    def test_stop_inactive_session_raises_error(self, session_with_websites, mock_config_repository, mock_scheduler, mock_alert_notifier):
        """Test that stopping inactive session raises error"""
        use_case = StopMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler,
            alert_notifier=mock_alert_notifier
        )

        with pytest.raises(ValueError, match="not active"):
            use_case.execute()

    def test_stop_monitoring_stops_scheduler_first(self, session_with_websites, mock_config_repository, mock_scheduler, mock_alert_notifier):
        """Test that scheduler is stopped before session"""
        session_with_websites.start()
        use_case = StopMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler,
            alert_notifier=mock_alert_notifier
        )

        # Track call order
        call_order = []
        mock_scheduler.stop.side_effect = lambda: call_order.append('scheduler')
        mock_alert_notifier.clear_all_alerts.side_effect = lambda: call_order.append('alerts')

        use_case.execute()

        # Scheduler should be stopped before alerts cleared
        assert call_order == ['scheduler', 'alerts']

    def test_stop_monitoring_persists_config(self, session_with_websites, mock_config_repository, mock_scheduler, mock_alert_notifier):
        """Test that stopping monitoring persists configuration"""
        session_with_websites.start()
        use_case = StopMonitoringUseCase(
            session=session_with_websites,
            config_repository=mock_config_repository,
            scheduler=mock_scheduler,
            alert_notifier=mock_alert_notifier
        )

        use_case.execute()

        mock_config_repository.save_session.assert_called_once_with(session_with_websites)
