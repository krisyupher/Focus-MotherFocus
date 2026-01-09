"""
Track Agreements Use Case.

Monitors active agreements and detects when they expire or are violated.
"""
from typing import Optional, Callable, List
from datetime import datetime

from src.core.entities.agreement import Agreement
from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent


class TrackAgreementsUseCase:
    """
    Use case for tracking agreement compliance.

    Responsibilities:
    - Monitor all active agreements
    - Detect expiration
    - Detect violations (user still doing activity after expiration)
    - Trigger warnings before expiration
    - Trigger enforcement when expired
    """

    def __init__(
        self,
        grace_period_seconds: float = 30.0,
        warning_before_seconds: float = 60.0
    ):
        """
        Initialize agreement tracker.

        Args:
            grace_period_seconds: Grace period after expiration before enforcement
            warning_before_seconds: Warn user this many seconds before expiration
        """
        self.grace_period_seconds = grace_period_seconds
        self.warning_before_seconds = warning_before_seconds
        self._active_agreements: List[Agreement] = []
        self._warned_agreement_ids: set[str] = set()

    def add_agreement(self, agreement: Agreement) -> None:
        """
        Add agreement to tracking.

        Args:
            agreement: Agreement to track
        """
        if agreement.is_active:
            self._active_agreements.append(agreement)
            print(f"[Tracker] Now tracking agreement: {agreement.id} ({agreement.agreed_duration_minutes} min)")

    def remove_agreement(self, agreement_id: str) -> None:
        """
        Remove agreement from tracking.

        Args:
            agreement_id: ID of agreement to remove
        """
        self._active_agreements = [
            a for a in self._active_agreements
            if a.id != agreement_id
        ]
        self._warned_agreement_ids.discard(agreement_id)

    def get_active_agreements(self) -> List[Agreement]:
        """
        Get all active (non-expired, non-violated) agreements.

        Returns:
            List of active agreements
        """
        return [a for a in self._active_agreements if a.is_active and not a.is_expired()]

    def get_expired_agreements(self) -> List[Agreement]:
        """
        Get all expired but not yet enforced agreements.

        Returns:
            List of expired agreements
        """
        return [a for a in self._active_agreements if a.is_active and a.is_expired()]

    def check_compliance(
        self,
        current_event: Optional[BehavioralEvent],
        on_warning: Optional[Callable[[Agreement, float], None]] = None,
        on_expired: Optional[Callable[[Agreement], None]] = None,
        on_violation: Optional[Callable[[Agreement], None]] = None
    ) -> None:
        """
        Check compliance for all active agreements.

        Args:
            current_event: Current behavioral event (if any)
            on_warning: Callback when agreement approaching expiration
            on_expired: Callback when agreement expired
            on_violation: Callback when user violates expired agreement
        """
        now = datetime.now()

        for agreement in self._active_agreements[:]:  # Copy to allow modification
            if not agreement.is_active:
                continue

            # Check if approaching expiration (warning)
            time_remaining = agreement.time_remaining_minutes() * 60  # Convert to seconds

            if 0 < time_remaining <= self.warning_before_seconds:
                # Approaching expiration - warn user
                if agreement.id not in self._warned_agreement_ids:
                    self._warned_agreement_ids.add(agreement.id)
                    if on_warning:
                        on_warning(agreement, time_remaining)

            # Check if expired
            if agreement.is_expired():
                # Agreement expired
                if on_expired:
                    on_expired(agreement)

                # Check if user is violating (still doing the activity)
                if current_event and self._is_violation(agreement, current_event):
                    # User is still doing the activity after expiration!
                    agreement.mark_violated()

                    if on_violation:
                        on_violation(agreement)

                    # Remove from active tracking
                    self.remove_agreement(agreement.id)

                else:
                    # User stopped - deactivate agreement
                    agreement.deactivate()
                    self.remove_agreement(agreement.id)

    def _is_violation(self, agreement: Agreement, event: BehavioralEvent) -> bool:
        """
        Check if current event violates agreement.

        Args:
            agreement: Agreement to check
            event: Current behavioral event

        Returns:
            True if violation detected
        """
        # Check if event matches agreement target
        if agreement.url and event.url:
            # URL-based agreement
            return agreement.url.lower() in event.url.lower()

        elif agreement.process_name and event.process_name:
            # Process-based agreement
            return agreement.process_name.lower() in event.process_name.lower()

        elif agreement.event_type == event.event_type:
            # Same type of event
            return True

        return False

    def get_agreement_status(self, agreement_id: str) -> Optional[dict]:
        """
        Get detailed status of an agreement.

        Args:
            agreement_id: Agreement ID

        Returns:
            Status dict or None if not found
        """
        for agreement in self._active_agreements:
            if agreement.id == agreement_id:
                time_remaining = agreement.time_remaining_minutes()

                status = "active"
                if agreement.is_expired():
                    status = "expired"
                if agreement.is_violated:
                    status = "violated"
                if not agreement.is_active:
                    status = "completed"

                return {
                    'id': agreement.id,
                    'status': status,
                    'time_remaining_minutes': time_remaining,
                    'time_remaining_seconds': time_remaining * 60,
                    'is_expired': agreement.is_expired(),
                    'is_violated': agreement.is_violated,
                    'violation_count': agreement.violation_count,
                    'created_at': agreement.created_at.isoformat(),
                    'expires_at': agreement.expires_at.isoformat()
                }

        return None

    def get_summary(self) -> dict:
        """
        Get summary of all tracked agreements.

        Returns:
            Summary dict with statistics
        """
        active = self.get_active_agreements()
        expired = self.get_expired_agreements()
        violated = [a for a in self._active_agreements if a.is_violated]

        return {
            'total_tracked': len(self._active_agreements),
            'active': len(active),
            'expired': len(expired),
            'violated': len(violated),
            'agreements': [
                {
                    'id': a.id,
                    'event_type': a.event_type,
                    'time_remaining_minutes': a.time_remaining_minutes(),
                    'is_expired': a.is_expired()
                }
                for a in active
            ]
        }

    def cleanup_inactive(self) -> int:
        """
        Remove all inactive agreements from tracking.

        Returns:
            Number of agreements removed
        """
        before_count = len(self._active_agreements)
        self._active_agreements = [a for a in self._active_agreements if a.is_active]
        removed = before_count - len(self._active_agreements)

        if removed > 0:
            print(f"[Tracker] Cleaned up {removed} inactive agreements")

        return removed
