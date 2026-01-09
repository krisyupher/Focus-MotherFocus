"""Monitoring Scheduler Interface"""
from abc import ABC, abstractmethod
from typing import Callable


class IMonitoringScheduler(ABC):
    """
    Interface for scheduling periodic monitoring tasks.

    This is a port (interface) that defines the contract for scheduling
    monitoring checks. Infrastructure layer provides the adapter (threading, asyncio, etc.).
    """

    @abstractmethod
    def start(self, interval: int, callback: Callable[[], None]) -> None:
        """
        Start the scheduler with a periodic callback.

        Args:
            interval: Interval in seconds between callbacks
            callback: Function to call periodically
        """
        pass

    @abstractmethod
    def stop(self) -> None:
        """
        Stop the scheduler.
        """
        pass

    @abstractmethod
    def is_running(self) -> bool:
        """
        Check if the scheduler is currently running.

        Returns:
            True if scheduler is active
        """
        pass
