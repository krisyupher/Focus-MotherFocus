"""HTTP Checker Adapter using requests library"""
from dataclasses import dataclass
import requests
from ...application.interfaces.http_checker import IHttpChecker
from ...core.value_objects.url import URL


@dataclass
class RequestsHttpChecker(IHttpChecker):
    """
    HTTP checker implementation using the requests library.

    This is an infrastructure adapter that implements the IHttpChecker
    interface using the popular requests library.

    Attributes:
        timeout: Request timeout in seconds
    """
    timeout: int = 5

    def check_website(self, url: URL) -> bool:
        """
        Check if a website is online and reachable.

        Args:
            url: URL value object of the website to check

        Returns:
            True if website is online (HTTP 200), False otherwise
        """
        try:
            response = requests.get(
                str(url),
                timeout=self.timeout,
                allow_redirects=True
            )
            return response.status_code == 200
        except requests.RequestException:
            return False
