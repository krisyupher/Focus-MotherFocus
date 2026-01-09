"""Stop Monitoring Use Case"""
from dataclasses import dataclass
from ...core.entities.monitoring_session import MonitoringSession
from ..interfaces.config_repository import IConfigRepository
from ..interfaces.monitoring_scheduler import IMonitoringScheduler
from ..interfaces.alert_notifier import IAlertNotifier


@dataclass
class StopMonitoringUseCase:
    """
    Use case for stopping the monitoring session.

    This encapsulates the business logic for stopping monitoring,
    including scheduler cleanup and alert clearing.

    Attributes:
        session: The monitoring session to stop
        config_repository: Repository for persisting configuration
        scheduler: Scheduler for periodic checks
        alert_notifier: Notifier for clearing alerts
    """
    session: MonitoringSession
    config_repository: IConfigRepository
    scheduler: IMonitoringScheduler
    alert_notifier: IAlertNotifier

    def execute(self) -> None:
        """
        Stop the monitoring session.

        Raises:
            ValueError: If session is not active
        """
        # Validate session can be stopped
        if not self.session.is_active:
            raise ValueError("Monitoring session is not active")

        # Stop the scheduler first
        self.scheduler.stop()

        # Clear all alerts
        self.alert_notifier.clear_all_alerts()

        # Stop the session
        self.session.stop()

        # Persist the inactive state
        self.config_repository.save_session(self.session)
