"""Interface for browser tab control operations."""
from abc import ABC, abstractmethod
from src.core.value_objects.url import URL


class IBrowserController(ABC):
    """Interface for controlling browser tabs (closing, navigating, etc)."""

    @abstractmethod
    def close_tab_with_url(self, url: URL) -> bool:
        """
        Close all browser tabs that match the given URL.

        Args:
            url: The URL to match and close

        Returns:
            True if any tabs were closed, False otherwise
        """
        pass

    @abstractmethod
    def is_available(self) -> bool:
        """
        Check if browser control is available.

        Returns:
            True if browser control is available, False otherwise
        """
        pass
