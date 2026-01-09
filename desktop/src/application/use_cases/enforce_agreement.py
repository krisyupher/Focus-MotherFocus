"""
Enforce Agreement Use Case.

Takes action when agreements expire or are violated:
- Close browser tabs
- Terminate processes
- Show notifications
- Apply escalation logic
"""
from typing import Optional, Callable
from datetime import datetime

from src.core.entities.agreement import Agreement
from src.application.interfaces.i_browser_controller import IBrowserController


class EnforceAgreementUseCase:
    """
    Use case for enforcing agreement limits.

    Enforcement Actions:
    1. Warning notification (60 seconds before)
    2. Final warning (30 seconds before)
    3. Grace period notification (at expiration)
    4. Forced closure (after grace period)
    """

    def __init__(
        self,
        browser_controller: Optional[IBrowserController] = None,
        grace_period_seconds: float = 30.0
    ):
        """
        Initialize enforcement use case.

        Args:
            browser_controller: Browser controller for tab closure
            grace_period_seconds: Grace period after expiration before forcing closure
        """
        self.browser_controller = browser_controller
        self.grace_period_seconds = grace_period_seconds
        self._grace_period_start: dict[str, datetime] = {}  # agreement_id -> start time

    def enforce(
        self,
        agreement: Agreement,
        force: bool = False,
        on_warning: Optional[Callable[[str], None]] = None,
        on_enforced: Optional[Callable[[str], None]] = None
    ) -> bool:
        """
        Enforce agreement limit.

        Args:
            agreement: Agreement to enforce
            force: If True, skip grace period
            on_warning: Callback with warning message
            on_enforced: Callback with enforcement message

        Returns:
            True if enforcement action taken, False otherwise
        """
        if not agreement.is_expired():
            return False

        # Check if in grace period
        if not force and agreement.id not in self._grace_period_start:
            # Start grace period
            self._grace_period_start[agreement.id] = datetime.now()

            message = (
                f"Time's up for {agreement.event_type}!\n"
                f"You have {self.grace_period_seconds} seconds to wrap up."
            )

            if on_warning:
                on_warning(message)

            print(f"[Enforcement] Grace period started for {agreement.id}")
            return False

        # Check if grace period expired
        if not force and agreement.id in self._grace_period_start:
            grace_start = self._grace_period_start[agreement.id]
            elapsed = (datetime.now() - grace_start).total_seconds()

            if elapsed < self.grace_period_seconds:
                # Still in grace period
                remaining = self.grace_period_seconds - elapsed
                print(f"[Enforcement] Grace period: {remaining:.0f}s remaining")
                return False

        # Grace period over (or forced) - enforce!
        return self._execute_enforcement(agreement, on_enforced)

    def _execute_enforcement(
        self,
        agreement: Agreement,
        on_enforced: Optional[Callable[[str], None]]
    ) -> bool:
        """
        Execute enforcement action.

        Args:
            agreement: Agreement to enforce
            on_enforced: Callback with enforcement message

        Returns:
            True if action taken successfully
        """
        print(f"[Enforcement] Executing enforcement for {agreement.id}")

        action_taken = False
        enforcement_message = ""

        # URL-based enforcement (close tab)
        if agreement.url:
            action_taken = self._close_browser_tab(agreement.url)
            if action_taken:
                enforcement_message = f"Closed tab: {agreement.url}"
            else:
                enforcement_message = f"Could not close tab: {agreement.url} (browser controller unavailable)"

        # Process-based enforcement (terminate process)
        elif agreement.process_name:
            # TODO: Implement process termination
            enforcement_message = f"Process termination not yet implemented: {agreement.process_name}"
            action_taken = False

        else:
            enforcement_message = f"No enforcement target specified for agreement {agreement.id}"
            action_taken = False

        # Deactivate agreement
        agreement.deactivate()

        # Clear grace period
        if agreement.id in self._grace_period_start:
            del self._grace_period_start[agreement.id]

        # Notify
        if on_enforced:
            on_enforced(enforcement_message)

        print(f"[Enforcement] {enforcement_message}")
        return action_taken

    def _close_browser_tab(self, url: str) -> bool:
        """
        Close browser tab with given URL.

        Args:
            url: URL of tab to close

        Returns:
            True if tab closed successfully
        """
        if not self.browser_controller:
            print("[Enforcement] No browser controller available")
            return False

        try:
            print(f"[Enforcement] Attempting to close tab: {url}")
            self.browser_controller.close_tab_by_url(url)
            print(f"[Enforcement] Tab closed: {url}")
            return True

        except Exception as e:
            print(f"[Enforcement] Failed to close tab: {e}")
            return False

    def send_warning(
        self,
        agreement: Agreement,
        seconds_remaining: float,
        on_warning: Optional[Callable[[str], None]] = None
    ) -> None:
        """
        Send warning before enforcement.

        Args:
            agreement: Agreement approaching expiration
            seconds_remaining: Seconds until expiration
            on_warning: Callback with warning message
        """
        minutes_remaining = int(seconds_remaining // 60)
        seconds_part = int(seconds_remaining % 60)

        if minutes_remaining > 0:
            time_str = f"{minutes_remaining} minute(s)"
        else:
            time_str = f"{seconds_part} second(s)"

        message = (
            f"⏰ Warning: {time_str} remaining\n"
            f"Activity: {agreement.event_type}\n"
            f"Target: {agreement.url or agreement.process_name or 'Unknown'}"
        )

        if on_warning:
            on_warning(message)

        print(f"[Enforcement] Warning sent: {time_str} remaining")

    def cancel_grace_period(self, agreement_id: str) -> None:
        """
        Cancel grace period for an agreement.

        Args:
            agreement_id: Agreement ID
        """
        if agreement_id in self._grace_period_start:
            del self._grace_period_start[agreement_id]
            print(f"[Enforcement] Grace period cancelled for {agreement_id}")

    def get_grace_period_remaining(self, agreement_id: str) -> Optional[float]:
        """
        Get remaining grace period time.

        Args:
            agreement_id: Agreement ID

        Returns:
            Remaining seconds or None if not in grace period
        """
        if agreement_id not in self._grace_period_start:
            return None

        elapsed = (datetime.now() - self._grace_period_start[agreement_id]).total_seconds()
        remaining = max(0, self.grace_period_seconds - elapsed)
        return remaining
