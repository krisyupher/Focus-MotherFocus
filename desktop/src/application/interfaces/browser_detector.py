"""
Browser Detector Interface
Defines contract for detecting if a URL is open in a browser
"""

from abc import ABC, abstractmethod
from typing import List
from ...core.value_objects.url import URL


class IBrowserDetector(ABC):
    """Interface for browser tab detection"""

    @abstractmethod
    def is_url_open_in_browser(self, url: URL) -> bool:
        """
        Check if the given URL is currently open in any browser tab

        Args:
            url: The URL to check

        Returns:
            True if URL is open in a browser tab, False otherwise
        """
        pass

    @abstractmethod
    def get_supported_browsers(self) -> List[str]:
        """
        Get list of browsers that can be detected

        Returns:
            List of browser names (e.g., ['chrome', 'firefox', 'edge'])
        """
        pass
