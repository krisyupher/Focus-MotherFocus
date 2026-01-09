"""Application entity for desktop application monitoring."""
from dataclasses import dataclass, field
import uuid
from src.core.value_objects.process_name import ProcessName


@dataclass
class Application:
    """
    Domain entity representing a desktop application to monitor.

    Attributes:
        id: Unique identifier for this application
        process_name: Process name value object (e.g., "chrome.exe")
        display_name: Human-readable name for UI display (e.g., "Google Chrome")
        is_alerting: Whether this application is currently triggering alerts
    """
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    process_name: ProcessName = None
    display_name: str = ""
    is_alerting: bool = False

    def __post_init__(self):
        """Validate and initialize the application entity."""
        if self.process_name is None:
            raise ValueError("Process name is required")

        if not self.display_name:
            # Default display name to process name without extension
            self.display_name = self.process_name.get_base_name().title()

    def mark_as_alerting(self) -> None:
        """Mark this application as currently alerting."""
        self.is_alerting = True

    def clear_alert(self) -> None:
        """Clear the alerting state for this application."""
        self.is_alerting = False

    def is_running(self) -> bool:
        """
        Check if this application is currently running.

        This is a convenience method that delegates to external services.
        The actual detection logic is in the infrastructure layer.
        """
        # This will be determined by IProcessDetector in the application layer
        return self.is_alerting

    def __str__(self) -> str:
        """String representation of the application."""
        return f"{self.display_name} ({self.process_name.value})"

    def __repr__(self) -> str:
        """Developer-friendly representation."""
        return (
            f"Application(id='{self.id}', "
            f"process_name={self.process_name}, "
            f"display_name='{self.display_name}', "
            f"is_alerting={self.is_alerting})"
        )
