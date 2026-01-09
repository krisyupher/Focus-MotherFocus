"""
Interface for behavioral analysis.

Defines the contract for analyzing user behavior patterns
to detect unproductive activities.
"""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from datetime import datetime
from typing import Optional


@dataclass(frozen=True)
class BehavioralEvent:
    """Represents a detected behavioral pattern."""

    event_type: str  # "endless_scrolling", "adult_content", "distraction_site", etc.
    severity: str  # "low", "medium", "high"
    url: Optional[str]
    process_name: Optional[str]
    duration_seconds: float
    detected_at: datetime
    metadata: dict  # Additional context (scroll distance, page title, etc.)

    @property
    def should_trigger_intervention(self) -> bool:
        """Determine if this event warrants counselor intervention."""
        if self.severity == "high":
            return True
        if self.severity == "medium" and self.duration_seconds > 30:
            return True
        if self.severity == "low" and self.duration_seconds > 120:
            return True
        return False


@dataclass(frozen=True)
class BehavioralPattern:
    """Represents an identified pattern across multiple events."""

    pattern_type: str  # "habitual_scrolling", "frequent_adult_sites", etc.
    frequency: int  # Number of occurrences
    total_duration_seconds: float
    first_occurrence: datetime
    last_occurrence: datetime
    confidence: float  # 0.0 to 1.0
    recommendation: str  # What intervention to apply


class IBehavioralAnalyzer(ABC):
    """
    Port for analyzing user behavior patterns.

    Implementations should integrate with monitoring systems
    (browser, process, etc.) to detect unproductive patterns.
    """

    @abstractmethod
    def analyze_current_activity(self) -> Optional[BehavioralEvent]:
        """
        Analyze current user activity and detect behavioral events.

        Returns:
            BehavioralEvent if unproductive behavior detected, None otherwise
        """
        pass

    @abstractmethod
    def get_patterns(self, lookback_minutes: int = 60) -> list[BehavioralPattern]:
        """
        Identify patterns in recent behavioral events.

        Args:
            lookback_minutes: How far back to analyze (default 60)

        Returns:
            List of identified patterns
        """
        pass

    @abstractmethod
    def start_monitoring(self) -> None:
        """Start continuous behavioral monitoring."""
        pass

    @abstractmethod
    def stop_monitoring(self) -> None:
        """Stop behavioral monitoring."""
        pass

    @abstractmethod
    def is_monitoring(self) -> bool:
        """Check if behavioral monitoring is active."""
        pass
