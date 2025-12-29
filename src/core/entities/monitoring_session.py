"""MonitoringSession Entity - Manages the monitoring session state"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional, Set
from .website import Website
from ..value_objects.url import URL


@dataclass
class MonitoringSession:
    """
    Entity representing a monitoring session.

    Manages the collection of websites being monitored and the session state.
    This is an aggregate root that ensures consistency of the monitoring domain.

    Attributes:
        websites: Dictionary of URL strings to Website entities
        monitoring_interval: Interval in seconds between checks
        is_active: Whether monitoring is currently active
        started_at: When the current session started
        stopped_at: When the current session stopped
    """
    monitoring_interval: int = 10
    websites: Dict[str, Website] = field(default_factory=dict)
    is_active: bool = False
    started_at: Optional[datetime] = None
    stopped_at: Optional[datetime] = None

    def add_website(self, url: URL) -> Website:
        """
        Add a website to the monitoring session.

        Args:
            url: URL value object of the website to add

        Returns:
            The Website entity that was added

        Raises:
            ValueError: If website with this URL already exists
        """
        url_str = str(url)

        if url_str in self.websites:
            raise ValueError(f"Website {url_str} is already being monitored")

        website = Website(url=url)
        self.websites[url_str] = website
        return website

    def remove_website(self, url: URL) -> Website:
        """
        Remove a website from the monitoring session.

        Args:
            url: URL value object of the website to remove

        Returns:
            The Website entity that was removed

        Raises:
            ValueError: If website does not exist
        """
        url_str = str(url)

        if url_str not in self.websites:
            raise ValueError(f"Website {url_str} is not being monitored")

        return self.websites.pop(url_str)

    def get_website(self, url: URL) -> Optional[Website]:
        """
        Get a website by URL.

        Args:
            url: URL value object to look up

        Returns:
            Website entity if found, None otherwise
        """
        return self.websites.get(str(url))

    def has_website(self, url: URL) -> bool:
        """
        Check if a website is being monitored.

        Args:
            url: URL value object to check

        Returns:
            True if website exists in session
        """
        return str(url) in self.websites

    def get_all_websites(self) -> List[Website]:
        """
        Get all websites in the session.

        Returns:
            List of all Website entities
        """
        return list(self.websites.values())

    def get_online_websites(self) -> List[Website]:
        """
        Get all currently online websites.

        Returns:
            List of Website entities that are online
        """
        return [w for w in self.websites.values() if w.is_online]

    def get_offline_websites(self) -> List[Website]:
        """
        Get all currently offline websites.

        Returns:
            List of Website entities that are offline
        """
        return [w for w in self.websites.values() if not w.is_online]

    def update_website_status(self, url: URL, is_online: bool,
                              check_time: Optional[datetime] = None) -> bool:
        """
        Update the status of a website.

        Args:
            url: URL of the website to update
            is_online: New online status
            check_time: Time of the check (defaults to now)

        Returns:
            True if status changed, False otherwise

        Raises:
            ValueError: If website does not exist
        """
        website = self.get_website(url)
        if not website:
            raise ValueError(f"Website {url} is not being monitored")

        return website.update_status(is_online, check_time)

    def start(self) -> None:
        """
        Start the monitoring session.

        Raises:
            ValueError: If session is already active or no websites to monitor
        """
        if self.is_active:
            raise ValueError("Monitoring session is already active")

        if not self.websites:
            raise ValueError("Cannot start monitoring with no websites")

        self.is_active = True
        self.started_at = datetime.now()
        self.stopped_at = None

        # Reset all website statuses to offline when starting
        for website in self.websites.values():
            website.is_online = False
            website.last_check_time = None

    def stop(self) -> None:
        """
        Stop the monitoring session.

        Raises:
            ValueError: If session is not active
        """
        if not self.is_active:
            raise ValueError("Monitoring session is not active")

        self.is_active = False
        self.stopped_at = datetime.now()

        # Reset all website statuses when stopping
        for website in self.websites.values():
            website.is_online = False

    def set_monitoring_interval(self, interval: int) -> None:
        """
        Set the monitoring interval.

        Args:
            interval: Interval in seconds (must be > 0)

        Raises:
            ValueError: If interval is invalid
        """
        if interval <= 0:
            raise ValueError("Monitoring interval must be greater than 0")

        self.monitoring_interval = interval

    def get_website_count(self) -> int:
        """
        Get the total number of websites being monitored.

        Returns:
            Count of websites
        """
        return len(self.websites)

    def clear_all_websites(self) -> None:
        """
        Remove all websites from the session.

        Raises:
            ValueError: If monitoring is active
        """
        if self.is_active:
            raise ValueError("Cannot clear websites while monitoring is active")

        self.websites.clear()

    def __str__(self) -> str:
        """String representation"""
        status = "ACTIVE" if self.is_active else "INACTIVE"
        return f"MonitoringSession({len(self.websites)} websites, {status})"

    def __repr__(self) -> str:
        """Developer-friendly representation"""
        return (f"MonitoringSession(websites={len(self.websites)}, "
                f"is_active={self.is_active}, interval={self.monitoring_interval}s)")
