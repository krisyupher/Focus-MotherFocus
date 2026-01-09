"""Configuration Repository Interface"""
from abc import ABC, abstractmethod
from typing import List
from ...core.entities.monitoring_session import MonitoringSession


class IConfigRepository(ABC):
    """
    Interface for persisting and loading configuration.

    This is a port (interface) that defines the contract for configuration
    persistence. Infrastructure layer provides the adapter (JSON, DB, etc.).
    """

    @abstractmethod
    def save_session(self, session: MonitoringSession) -> bool:
        """
        Save the monitoring session configuration.

        Args:
            session: MonitoringSession to persist

        Returns:
            True if save succeeded, False otherwise
        """
        pass

    @abstractmethod
    def load_session(self) -> MonitoringSession:
        """
        Load the monitoring session configuration.

        Returns:
            MonitoringSession loaded from persistence, or empty session if none exists
        """
        pass
