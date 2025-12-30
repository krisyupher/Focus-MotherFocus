"""Use case for checking application running status and triggering alerts."""
from src.core.entities.monitoring_session import MonitoringSession
from src.application.interfaces.process_detector import IProcessDetector
from src.application.interfaces.alert_notifier import IAlertNotifier


class CheckApplicationsUseCase:
    """
    Use case for checking if monitored applications are running and managing alerts.

    This is the core application monitoring logic that runs periodically.
    It checks each monitored application's running status and triggers/clears
    alerts accordingly.
    """

    def __init__(
        self,
        session: MonitoringSession,
        process_detector: IProcessDetector,
        alert_notifier: IAlertNotifier
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The monitoring session aggregate root
            process_detector: Service for detecting running processes
            alert_notifier: Service for sending/clearing alerts
        """
        self._session = session
        self._process_detector = process_detector
        self._alert_notifier = alert_notifier

    def execute(self) -> None:
        """
        Check all monitored applications and manage alerts.

        Alert Logic:
        - If application is running AND not currently alerting -> start alert
        - If application is NOT running AND currently alerting -> clear alert

        This ensures alerts are active only while applications are running.
        """
        if not self._session.is_active:
            return

        for application in self._session.get_all_applications():
            is_running = self._process_detector.is_process_running(
                application.process_name
            )

            # Trigger alert when application starts running
            if is_running and not application.is_alerting:
                application.mark_as_alerting()
                self._alert_notifier.notify(str(application.process_name))

            # Clear alert when application stops running
            elif not is_running and application.is_alerting:
                application.clear_alert()
                self._alert_notifier.clear(str(application.process_name))
