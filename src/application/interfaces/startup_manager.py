"""
Startup Manager Interface
Defines contract for managing application auto-startup
"""

from abc import ABC, abstractmethod


class IStartupManager(ABC):
    """Interface for managing application auto-startup behavior"""

    @abstractmethod
    def is_enabled(self) -> bool:
        """
        Check if auto-startup is currently enabled

        Returns:
            True if application is configured to start on boot, False otherwise
        """
        pass

    @abstractmethod
    def enable(self) -> bool:
        """
        Enable application to start automatically on system boot

        Returns:
            True if successfully enabled, False otherwise
        """
        pass

    @abstractmethod
    def disable(self) -> bool:
        """
        Disable application auto-startup

        Returns:
            True if successfully disabled, False otherwise
        """
        pass

    @abstractmethod
    def get_startup_command(self) -> str:
        """
        Get the command that will be executed on startup

        Returns:
            Full command path that runs on startup
        """
        pass
