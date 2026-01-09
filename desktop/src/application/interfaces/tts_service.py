"""
Text-to-Speech Service Interface

Defines the contract for text-to-speech adapters that can speak
motivational messages with synchronized animation callbacks.
"""
from abc import ABC, abstractmethod
from typing import Callable, Optional


class ITTSService(ABC):
    """
    Interface for text-to-speech services.

    Implementations should support background speech with callbacks
    for animation synchronization.
    """

    @abstractmethod
    def speak(
        self,
        text: str,
        on_start: Optional[Callable[[], None]] = None,
        on_word: Optional[Callable[[str], None]] = None,
        on_complete: Optional[Callable[[], None]] = None,
        blocking: bool = False
    ) -> None:
        """
        Speak the given text with optional callbacks for animation.

        Args:
            text: Text to speak aloud
            on_start: Callback when speech starts (for starting animation)
            on_word: Callback for each word spoken (word text passed as argument)
            on_complete: Callback when speech completes (for stopping animation)
            blocking: If True, block until speech completes; if False, run in background

        Raises:
            Exception: If TTS engine fails to initialize or speak
        """
        pass

    @abstractmethod
    def stop(self) -> None:
        """
        Stop current speech immediately.

        Should interrupt any ongoing speech and trigger on_complete callback.
        """
        pass

    @abstractmethod
    def is_speaking(self) -> bool:
        """
        Check if currently speaking.

        Returns:
            True if speech is in progress, False otherwise
        """
        pass
