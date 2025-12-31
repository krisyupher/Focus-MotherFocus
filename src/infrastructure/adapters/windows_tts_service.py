"""
Windows Text-to-Speech Service

Uses pyttsx3 (Windows SAPI) for text-to-speech with animation callbacks.
"""
import pyttsx3
import threading
from typing import Callable, Optional
from src.application.interfaces.tts_service import ITTSService


class WindowsTTSService(ITTSService):
    """
    Windows TTS implementation using pyttsx3 and SAPI voices.

    Runs speech in background thread with callbacks for animation synchronization.
    """

    def __init__(self):
        """Initialize the Windows TTS engine."""
        self._engine = None
        self._is_speaking = False
        self._speech_thread = None
        self._lock = threading.Lock()

        # Callbacks
        self._on_start_callback = None
        self._on_word_callback = None
        self._on_complete_callback = None

        # Initialize engine
        try:
            self._engine = pyttsx3.init()

            # Configure voice settings
            self._engine.setProperty('rate', 175)  # Words per minute (slightly faster for urgency)
            self._engine.setProperty('volume', 0.9)  # 90% volume

            # Try to set a female voice (friendlier tone)
            voices = self._engine.getProperty('voices')
            for voice in voices:
                if 'female' in voice.name.lower() or 'zira' in voice.name.lower():
                    self._engine.setProperty('voice', voice.id)
                    break

        except Exception as e:
            print(f"[TTS] Failed to initialize pyttsx3: {e}")
            self._engine = None

    def speak(
        self,
        text: str,
        on_start: Optional[Callable[[], None]] = None,
        on_word: Optional[Callable[[str], None]] = None,
        on_complete: Optional[Callable[[], None]] = None,
        blocking: bool = False
    ) -> None:
        """
        Speak text with callbacks for animation synchronization.

        Args:
            text: Text to speak
            on_start: Callback when speech starts
            on_word: Callback for each word (receives word text)
            on_complete: Callback when speech finishes
            blocking: If True, block until complete; if False, run in background
        """
        if not self._engine:
            print("[TTS] Engine not available")
            if on_complete:
                on_complete()
            return

        # Store callbacks
        self._on_start_callback = on_start
        self._on_word_callback = on_word
        self._on_complete_callback = on_complete

        if blocking:
            self._speak_blocking(text)
        else:
            self._speak_async(text)

    def _speak_blocking(self, text: str) -> None:
        """Speak in the current thread (blocking)."""
        with self._lock:
            self._is_speaking = True

        try:
            # Trigger start callback
            if self._on_start_callback:
                self._on_start_callback()

            # Speak
            self._engine.say(text)
            self._engine.runAndWait()

        except Exception as e:
            print(f"[TTS] Speech error: {e}")

        finally:
            with self._lock:
                self._is_speaking = False

            # Trigger complete callback
            if self._on_complete_callback:
                self._on_complete_callback()

    def _speak_async(self, text: str) -> None:
        """Speak in background thread (non-blocking)."""
        # Stop any existing speech
        self.stop()

        # Start new speech thread
        self._speech_thread = threading.Thread(
            target=self._speak_blocking,
            args=(text,),
            daemon=True
        )
        self._speech_thread.start()

    def stop(self) -> None:
        """Stop current speech immediately."""
        if not self._engine:
            return

        try:
            with self._lock:
                if self._is_speaking:
                    self._engine.stop()
                    self._is_speaking = False

                    # Trigger complete callback
                    if self._on_complete_callback:
                        self._on_complete_callback()

        except Exception as e:
            print(f"[TTS] Stop error: {e}")

    def is_speaking(self) -> bool:
        """Check if currently speaking."""
        with self._lock:
            return self._is_speaking
