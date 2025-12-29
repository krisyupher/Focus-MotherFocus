"""Alert Notifier Interface"""
from abc import ABC, abstractmethod
from ...core.value_objects.url import URL


class IAlertNotifier(ABC):
    """
    Interface for sending alerts when websites are online.

    This is a port (interface) that defines the contract for notification.
    Infrastructure layer provides the adapter (Windows, email, etc.).
    """

    @abstractmethod
    def send_alert(self, url: URL) -> None:
        """
        Send an alert notification for a website.

        Args:
            url: URL of the website that triggered the alert
        """
        pass

    @abstractmethod
    def clear_alerts(self, url: URL) -> None:
        """
        Clear all active alerts for a specific website.

        Args:
            url: URL of the website to clear alerts for
        """
        pass

    @abstractmethod
    def clear_all_alerts(self) -> None:
        """
        Clear all active alerts for all websites.
        """
        pass
