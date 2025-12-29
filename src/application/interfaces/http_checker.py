"""HTTP Checker Interface"""
from abc import ABC, abstractmethod
from ...core.value_objects.url import URL


class IHttpChecker(ABC):
    """
    Interface for checking website availability via HTTP.

    This is a port (interface) that defines the contract for checking
    if a website is online. Infrastructure layer provides the adapter.
    """

    @abstractmethod
    def check_website(self, url: URL) -> bool:
        """
        Check if a website is online and reachable.

        Args:
            url: URL value object of the website to check

        Returns:
            True if website is online (HTTP 200), False otherwise
        """
        pass
