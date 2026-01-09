"""Website Entity - Represents a monitored website"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from ..value_objects.url import URL


@dataclass
class Website:
    """
    Entity representing a website being monitored.

    This is a core domain entity that encapsulates the state and behavior
    of a monitored website. It tracks whether the site is currently online
    and maintains its URL identity.

    Attributes:
        url: URL value object representing the website
        is_online: Current online status of the website
        last_check_time: Timestamp of the last status check
        added_at: Timestamp when website was added to monitoring
    """
    url: URL
    is_online: bool = False
    last_check_time: Optional[datetime] = None
    added_at: datetime = field(default_factory=datetime.now)

    def mark_online(self, check_time: Optional[datetime] = None) -> None:
        """
        Mark the website as online.

        Args:
            check_time: Time of the check (defaults to now)
        """
        self.is_online = True
        self.last_check_time = check_time or datetime.now()

    def mark_offline(self, check_time: Optional[datetime] = None) -> None:
        """
        Mark the website as offline.

        Args:
            check_time: Time of the check (defaults to now)
        """
        self.is_online = False
        self.last_check_time = check_time or datetime.now()

    def update_status(self, is_online: bool, check_time: Optional[datetime] = None) -> bool:
        """
        Update the website status.

        Args:
            is_online: New online status
            check_time: Time of the check (defaults to now)

        Returns:
            True if status changed, False otherwise
        """
        status_changed = self.is_online != is_online

        if is_online:
            self.mark_online(check_time)
        else:
            self.mark_offline(check_time)

        return status_changed

    def get_url_string(self) -> str:
        """
        Get the URL as a string.

        Returns:
            URL string representation
        """
        return str(self.url)

    def __eq__(self, other) -> bool:
        """Websites are equal if they have the same URL"""
        if not isinstance(other, Website):
            return False
        return self.url == other.url

    def __hash__(self) -> int:
        """Allow websites to be used in sets and as dict keys"""
        return hash(self.url)

    def __str__(self) -> str:
        """String representation"""
        status = "ONLINE" if self.is_online else "OFFLINE"
        return f"Website({self.url}, {status})"

    def __repr__(self) -> str:
        """Developer-friendly representation"""
        return (f"Website(url={self.url!r}, is_online={self.is_online}, "
                f"last_check_time={self.last_check_time!r})")
