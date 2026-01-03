"""Unified MonitoringTarget entity that tracks both website and application."""
from dataclasses import dataclass, field
from typing import Optional
import uuid
import time
from ..value_objects.url import URL
from ..value_objects.process_name import ProcessName


@dataclass
class MonitoringTarget:
    """
    Unified monitoring target that can track both a website and an application.

    For example, "Netflix" would monitor:
    - Website: https://netflix.com (in browser)
    - Application: Netflix.exe (desktop app)

    Alerts trigger when EITHER the website is open in browser OR the app is running.

    Attributes:
        id: Unique identifier
        name: Display name (e.g., "Netflix", "Spotify")
        url: Optional URL to monitor (e.g., "https://netflix.com")
        process_name: Optional process name to monitor (e.g., "Netflix.exe")
        is_alerting: Whether currently alerting
        alert_start_time: When the alert started (for auto-close feature)
    """
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    name: str = ""
    url: Optional[URL] = None
    process_name: Optional[ProcessName] = None
    is_alerting: bool = False
    alert_start_time: Optional[float] = None

    def __post_init__(self):
        """Validate that at least one monitoring type is specified."""
        if not self.url and not self.process_name:
            raise ValueError("MonitoringTarget must have at least a URL or process name")

        if not self.name:
            # Auto-generate name from URL or process name
            if self.url:
                self.name = str(self.url)
            elif self.process_name:
                self.name = self.process_name.get_base_name().title()

    def has_website(self) -> bool:
        """Check if this target monitors a website."""
        return self.url is not None

    def has_application(self) -> bool:
        """Check if this target monitors an application."""
        return self.process_name is not None

    def is_hybrid(self) -> bool:
        """Check if this target monitors both website and application."""
        return self.url is not None and self.process_name is not None

    def mark_as_alerting(self) -> None:
        """Mark this target as currently alerting."""
        if not self.is_alerting:
            self.alert_start_time = time.time()
        self.is_alerting = True

    def clear_alert(self) -> None:
        """Clear the alerting state."""
        self.is_alerting = False
        self.alert_start_time = None

    def get_alert_duration(self) -> float:
        """
        Get the duration of the current alert in seconds.

        Returns:
            Alert duration in seconds, or 0 if not alerting
        """
        if not self.is_alerting or self.alert_start_time is None:
            return 0.0
        return time.time() - self.alert_start_time

    def should_auto_close(self, threshold_seconds: float = 10.0) -> bool:
        """
        Check if this target should be auto-closed.

        Args:
            threshold_seconds: Number of seconds before auto-close

        Returns:
            True if alert has been active for longer than threshold
        """
        return self.get_alert_duration() >= threshold_seconds

    def __str__(self) -> str:
        """String representation."""
        parts = []
        if self.url:
            parts.append(f"web:{self.url}")
        if self.process_name:
            parts.append(f"app:{self.process_name}")

        status = "ALERTING" if self.is_alerting else "monitoring"
        return f"{self.name} ({', '.join(parts)}) [{status}]"

    def __repr__(self) -> str:
        """Developer-friendly representation."""
        return (
            f"MonitoringTarget(id='{self.id}', name='{self.name}', "
            f"url={self.url!r}, process_name={self.process_name!r}, "
            f"is_alerting={self.is_alerting})"
        )
