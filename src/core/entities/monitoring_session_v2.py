"""Unified MonitoringSession Entity - Manages monitoring targets (website + app combos)"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional
from .monitoring_target import MonitoringTarget
from ..value_objects.url import URL
from ..value_objects.process_name import ProcessName


@dataclass
class MonitoringSessionV2:
    """
    Unified monitoring session that manages targets (website + app combinations).

    Each target can monitor:
    - Just a website (e.g., "Google" -> https://google.com)
    - Just an application (e.g., "Calculator" -> calc.exe)
    - Both website AND app (e.g., "Netflix" -> netflix.com + Netflix.exe)

    Attributes:
        targets: Dictionary of target IDs to MonitoringTarget entities
        monitoring_interval: Interval in seconds between checks
        is_active: Whether monitoring is currently active
        started_at: When the current session started
        stopped_at: When the current session stopped
    """
    monitoring_interval: int = 10
    targets: Dict[str, MonitoringTarget] = field(default_factory=dict)
    is_active: bool = False
    started_at: Optional[datetime] = None
    stopped_at: Optional[datetime] = None

    def add_target(
        self,
        name: str,
        url: Optional[URL] = None,
        process_name: Optional[ProcessName] = None
    ) -> MonitoringTarget:
        """
        Add a monitoring target (website, app, or both).

        Args:
            name: Display name (e.g., "Netflix", "Spotify")
            url: Optional URL to monitor
            process_name: Optional process name to monitor

        Returns:
            The MonitoringTarget entity that was added

        Raises:
            ValueError: If target with this name already exists or no monitoring type specified

        Examples:
            # Website only
            session.add_target("Google", url=URL("google.com"))

            # App only
            session.add_target("Calculator", process_name=ProcessName("calc.exe"))

            # Both website and app
            session.add_target(
                "Netflix",
                url=URL("netflix.com"),
                process_name=ProcessName("Netflix.exe")
            )
        """
        # Check if target with this name already exists
        if any(t.name.lower() == name.lower() for t in self.targets.values()):
            raise ValueError(f"Target '{name}' already exists")

        target = MonitoringTarget(
            name=name,
            url=url,
            process_name=process_name
        )

        self.targets[target.id] = target
        return target

    def remove_target(self, target_id: str) -> MonitoringTarget:
        """
        Remove a monitoring target by ID.

        Args:
            target_id: ID of the target to remove

        Returns:
            The MonitoringTarget entity that was removed

        Raises:
            ValueError: If target does not exist
        """
        if target_id not in self.targets:
            raise ValueError(f"Target with ID {target_id} does not exist")

        return self.targets.pop(target_id)

    def remove_target_by_name(self, name: str) -> MonitoringTarget:
        """
        Remove a monitoring target by name.

        Args:
            name: Name of the target to remove

        Returns:
            The MonitoringTarget entity that was removed

        Raises:
            ValueError: If target does not exist
        """
        target = self.get_target_by_name(name)
        if not target:
            raise ValueError(f"Target '{name}' does not exist")

        return self.targets.pop(target.id)

    def get_target(self, target_id: str) -> Optional[MonitoringTarget]:
        """Get a target by ID."""
        return self.targets.get(target_id)

    def get_target_by_name(self, name: str) -> Optional[MonitoringTarget]:
        """Get a target by name (case-insensitive)."""
        name_lower = name.lower()
        for target in self.targets.values():
            if target.name.lower() == name_lower:
                return target
        return None

    def get_all_targets(self) -> List[MonitoringTarget]:
        """Get all monitoring targets."""
        return list(self.targets.values())

    def get_alerting_targets(self) -> List[MonitoringTarget]:
        """Get all currently alerting targets."""
        return [t for t in self.targets.values() if t.is_alerting]

    def get_website_targets(self) -> List[MonitoringTarget]:
        """Get all targets that monitor websites."""
        return [t for t in self.targets.values() if t.has_website()]

    def get_application_targets(self) -> List[MonitoringTarget]:
        """Get all targets that monitor applications."""
        return [t for t in self.targets.values() if t.has_application()]

    def get_hybrid_targets(self) -> List[MonitoringTarget]:
        """Get all targets that monitor both website and application."""
        return [t for t in self.targets.values() if t.is_hybrid()]

    def start(self) -> None:
        """
        Start the monitoring session.

        Raises:
            ValueError: If session is already active or no targets to monitor
        """
        if self.is_active:
            raise ValueError("Monitoring session is already active")

        if not self.targets:
            raise ValueError("Cannot start monitoring with no targets")

        self.is_active = True
        self.started_at = datetime.now()
        self.stopped_at = None

        # Reset all alert states when starting
        for target in self.targets.values():
            target.is_alerting = False

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

        # Reset all alert states when stopping
        for target in self.targets.values():
            target.is_alerting = False

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

    def get_target_count(self) -> int:
        """Get the total number of targets being monitored."""
        return len(self.targets)

    def clear_all_targets(self) -> None:
        """
        Remove all targets from the session.

        Raises:
            ValueError: If monitoring is active
        """
        if self.is_active:
            raise ValueError("Cannot clear targets while monitoring is active")

        self.targets.clear()

    def __str__(self) -> str:
        """String representation."""
        status = "ACTIVE" if self.is_active else "INACTIVE"
        return f"MonitoringSessionV2({len(self.targets)} targets, {status})"

    def __repr__(self) -> str:
        """Developer-friendly representation."""
        return (
            f"MonitoringSessionV2(targets={len(self.targets)}, "
            f"is_active={self.is_active}, interval={self.monitoring_interval}s)"
        )
