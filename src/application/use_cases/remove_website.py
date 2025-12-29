"""Remove Website Use Case"""
from dataclasses import dataclass
from ...core.entities.monitoring_session import MonitoringSession
from ...core.entities.website import Website
from ...core.value_objects.url import URL
from ..interfaces.config_repository import IConfigRepository
from ..interfaces.alert_notifier import IAlertNotifier


@dataclass
class RemoveWebsiteUseCase:
    """
    Use case for removing a website from the monitoring session.

    This encapsulates the business logic for removing a website,
    including clearing alerts, persistence, and state management.

    Attributes:
        session: The monitoring session to remove from
        config_repository: Repository for persisting configuration
        alert_notifier: Notifier for clearing alerts
    """
    session: MonitoringSession
    config_repository: IConfigRepository
    alert_notifier: IAlertNotifier

    def execute(self, url_string: str) -> Website:
        """
        Remove a website from the monitoring session.

        Args:
            url_string: URL string to remove

        Returns:
            The Website entity that was removed

        Raises:
            ValueError: If website does not exist
        """
        # Parse URL
        url = URL.from_string(url_string)

        # Check if exists
        if not self.session.has_website(url):
            raise ValueError(f"Website {url} is not being monitored")

        # Clear any active alerts for this website
        self.alert_notifier.clear_alerts(url)

        # Remove from session
        website = self.session.remove_website(url)

        # Persist configuration
        self.config_repository.save_session(self.session)

        return website
