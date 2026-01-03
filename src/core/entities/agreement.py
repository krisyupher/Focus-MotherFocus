"""
Agreement Entity - Represents negotiated agreements with user.

Core domain entity for tracking user commitments and time limits.
"""
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Optional
import uuid


@dataclass
class Agreement:
    """
    Represents an agreement between user and counselor.

    Examples:
    - "I'll scroll Reddit for 10 more minutes"
    - "I'll stop browsing adult sites immediately"
    - "I'll watch YouTube for this video only (15 minutes)"
    """

    id: str
    event_type: str  # "endless_scrolling", "adult_content", etc.
    url: Optional[str]
    process_name: Optional[str]
    agreed_duration_minutes: float
    created_at: datetime
    expires_at: datetime
    user_response: str  # What the user said
    counselor_message: str  # What counselor said
    is_active: bool = True
    is_violated: bool = False
    violation_count: int = 0

    @staticmethod
    def create(
        event_type: str,
        url: Optional[str],
        process_name: Optional[str],
        agreed_duration_minutes: float,
        user_response: str,
        counselor_message: str
    ) -> 'Agreement':
        """
        Create a new agreement.

        Args:
            event_type: Type of behavioral event
            url: URL if web-based
            process_name: Process name if application-based
            agreed_duration_minutes: How long user agreed to
            user_response: User's response text
            counselor_message: Counselor's message

        Returns:
            New Agreement instance
        """
        now = datetime.now()
        expires_at = now + timedelta(minutes=agreed_duration_minutes)

        return Agreement(
            id=str(uuid.uuid4()),
            event_type=event_type,
            url=url,
            process_name=process_name,
            agreed_duration_minutes=agreed_duration_minutes,
            created_at=now,
            expires_at=expires_at,
            user_response=user_response,
            counselor_message=counselor_message,
            is_active=True,
            is_violated=False,
            violation_count=0
        )

    def is_expired(self) -> bool:
        """Check if agreement time has expired."""
        return datetime.now() >= self.expires_at

    def time_remaining_minutes(self) -> float:
        """Get remaining time in minutes."""
        if self.is_expired():
            return 0.0

        delta = self.expires_at - datetime.now()
        return delta.total_seconds() / 60.0

    def mark_violated(self) -> None:
        """Mark agreement as violated."""
        self.is_violated = True
        self.violation_count += 1

    def deactivate(self) -> None:
        """Deactivate agreement (completed or cancelled)."""
        self.is_active = False

    def extend(self, additional_minutes: float) -> None:
        """
        Extend agreement time.

        Args:
            additional_minutes: Additional minutes to add
        """
        self.expires_at += timedelta(minutes=additional_minutes)

    def __str__(self) -> str:
        """String representation of agreement."""
        status = "ACTIVE" if self.is_active else "INACTIVE"
        if self.is_violated:
            status = "VIOLATED"

        return (
            f"Agreement({self.event_type}, "
            f"{self.agreed_duration_minutes}min, "
            f"{status})"
        )
