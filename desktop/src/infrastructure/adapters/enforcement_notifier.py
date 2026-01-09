"""
Enforcement Notifier - Sends warnings and notifications for agreement enforcement.

Uses NotifyMeMaybe for interactive notifications or falls back to console.
"""
from typing import Optional, Callable


class EnforcementNotifier:
    """
    Notifier for enforcement warnings and messages.

    Provides:
    - Warning notifications before expiration
    - Grace period notifications
    - Enforcement notifications
    - Extension request dialogs
    """

    def __init__(self, notifymemaybe=None, voice_service=None):
        """
        Initialize enforcement notifier.

        Args:
            notifymemaybe: NotifyMeMaybe instance (optional)
            voice_service: Voice service for spoken warnings (optional)
        """
        self.notifymemaybe = notifymemaybe
        self.voice_service = voice_service

    def send_warning(
        self,
        message: str,
        speak: bool = False,
        on_response: Optional[Callable[[bool], None]] = None
    ) -> None:
        """
        Send warning notification.

        Args:
            message: Warning message
            speak: If True, speak the message
            on_response: Callback for user response (acknowledge)
        """
        print(f"\n⏰ WARNING: {message}")

        # Speak if voice available
        if speak and self.voice_service and self.voice_service.is_available():
            self.voice_service.speak(message, blocking=False)

        # Show notification if available
        if self.notifymemaybe and self.notifymemaybe.is_available():
            try:
                acknowledged = self.notifymemaybe.prompt_confirm(
                    title="⏰ Agreement Warning",
                    message=message,
                    yes_label="OK",
                    no_label="Dismiss",
                    timeout=10
                )

                if on_response:
                    on_response(acknowledged)

            except Exception as e:
                print(f"[Notifier] Notification error: {e}")

    def send_grace_period_notification(
        self,
        message: str,
        seconds_remaining: float,
        speak: bool = True
    ) -> None:
        """
        Send grace period notification.

        Args:
            message: Grace period message
            seconds_remaining: Seconds remaining in grace period
            speak: If True, speak the message
        """
        full_message = f"{message}\n\nGrace period: {int(seconds_remaining)}s remaining"
        print(f"\n🕐 GRACE PERIOD: {full_message}")

        # Speak if voice available
        if speak and self.voice_service and self.voice_service.is_available():
            self.voice_service.speak(full_message, blocking=False)

        # Show notification if available
        if self.notifymemaybe and self.notifymemaybe.is_available():
            try:
                self.notifymemaybe.prompt_confirm(
                    title="🕐 Grace Period",
                    message=full_message,
                    yes_label="OK",
                    no_label="Cancel",
                    timeout=int(seconds_remaining)
                )
            except Exception as e:
                print(f"[Notifier] Grace period notification error: {e}")

    def send_enforcement_notification(
        self,
        message: str,
        speak: bool = True
    ) -> None:
        """
        Send enforcement notification.

        Args:
            message: Enforcement message
            speak: If True, speak the message
        """
        print(f"\n🚫 ENFORCED: {message}")

        # Speak if voice available
        if speak and self.voice_service and self.voice_service.is_available():
            self.voice_service.speak(message, blocking=False)

        # Show notification if available
        if self.notifymemaybe and self.notifymemaybe.is_available():
            try:
                self.notifymemaybe.prompt_confirm(
                    title="🚫 Agreement Enforced",
                    message=message,
                    yes_label="OK",
                    no_label="Dismiss",
                    timeout=5
                )
            except Exception as e:
                print(f"[Notifier] Enforcement notification error: {e}")

    def request_extension(
        self,
        current_duration_minutes: float,
        on_response: Optional[Callable[[Optional[float]], None]] = None
    ) -> Optional[float]:
        """
        Request extension from user.

        Args:
            current_duration_minutes: Current agreement duration
            on_response: Callback with granted extension (minutes or None)

        Returns:
            Extension in minutes or None if denied
        """
        message = f"Current time: {current_duration_minutes} minutes\nHow much longer do you need?"

        if self.notifymemaybe and self.notifymemaybe.is_available():
            try:
                choices = [
                    "5 more minutes",
                    "10 more minutes",
                    "15 more minutes",
                    "No extension"
                ]

                selection = self.notifymemaybe.prompt_select(
                    title="⏱️ Extension Request",
                    message=message,
                    choices=choices,
                    timeout=30
                )

                if selection and "No extension" not in selection:
                    # Parse minutes from selection
                    if "5" in selection:
                        extension = 5.0
                    elif "10" in selection:
                        extension = 10.0
                    elif "15" in selection:
                        extension = 15.0
                    else:
                        extension = None

                    if on_response:
                        on_response(extension)

                    return extension

            except Exception as e:
                print(f"[Notifier] Extension request error: {e}")

        # Console fallback
        print(f"\n⏱️  EXTENSION REQUEST: {message}")
        try:
            response = input("Minutes to extend (0 for none): ").strip()
            if response.isdigit():
                extension = float(response)
                if extension > 0:
                    if on_response:
                        on_response(extension)
                    return extension
        except Exception as e:
            print(f"[Notifier] Console input error: {e}")

        if on_response:
            on_response(None)

        return None

    def is_available(self) -> bool:
        """Check if notifier is available."""
        return (self.notifymemaybe and self.notifymemaybe.is_available()) or True  # Console always available
