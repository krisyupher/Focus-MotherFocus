"""Use case for stopping monitoring - Unified version for MonitoringSessionV2"""
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.interfaces.config_repository import IConfigRepository
from src.application.interfaces.monitoring_scheduler import IMonitoringScheduler
from src.application.interfaces.alert_notifier import IAlertNotifier


class StopMonitoringV2UseCase:
    """
    Use case for stopping the unified monitoring session.

    Works with MonitoringSessionV2 and unified targets.
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        config_repository: IConfigRepository,
        scheduler: IMonitoringScheduler,
        alert_notifier: IAlertNotifier
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The unified monitoring session
            config_repository: Repository for persisting configuration
            scheduler: Scheduler for periodic monitoring checks
            alert_notifier: Service for clearing alerts
        """
        self.session = session
        self.config_repository = config_repository
        self.scheduler = scheduler
        self.alert_notifier = alert_notifier

    def execute(self) -> None:
        """
        Stop monitoring all targets.

        Stops the scheduler, clears all alerts, and updates session state.

        Raises:
            ValueError: If session is not currently active
        """
        # Stop the scheduler first
        self.scheduler.stop()

        # Clear all active alerts
        for target in self.session.get_alerting_targets():
            self.alert_notifier.clear(target.name)

        # Stop the session (validates state and clears alert flags)
        self.session.stop()

        # Persist the updated session state
        self.config_repository.save_session(self.session)
