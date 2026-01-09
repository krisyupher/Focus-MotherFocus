"""
Counselor Voice Service - Text-to-speech for avatar counselor.

Integrates with ElevenLabs MCP for high-quality voice synthesis.
"""
import os
import tempfile
import threading
from typing import Optional
import pygame


class CounselorVoiceService:
    """
    Voice service for counselor interventions.

    Uses ElevenLabs MCP for text-to-speech with emotional, empathetic voice.
    Manages audio playback and synchronization with avatar animations.
    """

    def __init__(self, elevenlabs_mcp=None, voice_name: str = "alloy"):
        """
        Initialize counselor voice service.

        Args:
            elevenlabs_mcp: ElevenLabsMCP instance (optional)
            voice_name: Voice to use for counselor (default: "alloy")
        """
        self.elevenlabs_mcp = elevenlabs_mcp
        self.voice_name = voice_name
        self.is_speaking = False
        self._current_audio_thread: Optional[threading.Thread] = None

        # Initialize pygame mixer for audio playback
        try:
            pygame.mixer.init()
            self._pygame_available = True
        except Exception as e:
            print(f"[Voice Service] pygame mixer init failed: {e}")
            self._pygame_available = False

    def speak(self, text: str, blocking: bool = False) -> bool:
        """
        Speak text using TTS.

        Args:
            text: Text to speak
            blocking: If True, wait for speech to complete

        Returns:
            True if speech started successfully, False otherwise
        """
        if not text or not text.strip():
            return False

        if not self.elevenlabs_mcp:
            print(f"[Voice Service] No ElevenLabs MCP available, text: {text}")
            return False

        if not self._pygame_available:
            print(f"[Voice Service] pygame not available for audio playback")
            return False

        # Stop any current speech
        self.stop()

        # Generate audio in background
        if blocking:
            return self._speak_sync(text)
        else:
            self._current_audio_thread = threading.Thread(
                target=self._speak_sync,
                args=(text,),
                daemon=True
            )
            self._current_audio_thread.start()
            return True

    def _speak_sync(self, text: str) -> bool:
        """
        Synchronous speech generation and playback.

        Args:
            text: Text to speak

        Returns:
            True if successful, False otherwise
        """
        try:
            self.is_speaking = True

            # Generate TTS audio
            print(f"[Voice Service] Generating TTS: {text[:50]}...")
            audio_data = self.elevenlabs_mcp.synthesize_text(
                text=text,
                voice=self.voice_name,
                fmt="mp3"
            )

            if not audio_data:
                print("[Voice Service] No audio data returned")
                return False

            # Save to temporary file
            with tempfile.NamedTemporaryFile(
                suffix='.mp3',
                delete=False
            ) as temp_audio:
                temp_audio.write(audio_data)
                temp_path = temp_audio.name

            # Play audio
            print(f"[Voice Service] Playing audio: {len(audio_data)} bytes")
            pygame.mixer.music.load(temp_path)
            pygame.mixer.music.play()

            # Wait for playback to complete
            while pygame.mixer.music.get_busy():
                pygame.time.Clock().tick(10)

            # Cleanup
            try:
                os.unlink(temp_path)
            except:
                pass

            print("[Voice Service] Speech completed")
            return True

        except Exception as e:
            print(f"[Voice Service] Speech error: {e}")
            return False

        finally:
            self.is_speaking = False

    def stop(self) -> None:
        """Stop current speech."""
        if self._pygame_available:
            try:
                pygame.mixer.music.stop()
            except:
                pass

        self.is_speaking = False

    def is_available(self) -> bool:
        """
        Check if voice service is available.

        Returns:
            True if ElevenLabs MCP is available and pygame is initialized
        """
        if not self._pygame_available:
            return False

        if not self.elevenlabs_mcp:
            return False

        try:
            return self.elevenlabs_mcp.is_available()
        except:
            return False

    def get_available_voices(self) -> list[str]:
        """
        Get list of available voices.

        Returns:
            List of voice names
        """
        if not self.elevenlabs_mcp:
            return []

        try:
            voices_data = self.elevenlabs_mcp.list_voices()

            # Parse response format
            if isinstance(voices_data, dict) and 'voices' in voices_data:
                return [v.get('voice_id') or v.get('name') for v in voices_data['voices']]
            elif isinstance(voices_data, list):
                return [v.get('voice_id') or v.get('name') for v in voices_data if isinstance(v, dict)]
            else:
                return []

        except Exception as e:
            print(f"[Voice Service] Error listing voices: {e}")
            return []

    def set_voice(self, voice_name: str) -> None:
        """
        Set the voice to use for TTS.

        Args:
            voice_name: Name of the voice
        """
        self.voice_name = voice_name
        print(f"[Voice Service] Voice set to: {voice_name}")

    def test_speak(self) -> bool:
        """
        Test speech with a simple message.

        Returns:
            True if test successful, False otherwise
        """
        return self.speak("Hello, I am your productivity counselor.", blocking=True)


class FallbackVoiceService(CounselorVoiceService):
    """
    Fallback voice service using Windows TTS when ElevenLabs is not available.
    """

    def __init__(self):
        """Initialize fallback voice service without ElevenLabs."""
        super().__init__(elevenlabs_mcp=None, voice_name="system")
        self._pygame_available = False  # Don't use pygame for fallback

    def speak(self, text: str, blocking: bool = False) -> bool:
        """
        Speak using Windows TTS (winsound + SAPI).

        Args:
            text: Text to speak
            blocking: Ignored for fallback

        Returns:
            True if speech started
        """
        try:
            import win32com.client
            speaker = win32com.client.Dispatch("SAPI.SpVoice")

            # Speak in background thread
            def _speak_thread():
                self.is_speaking = True
                try:
                    speaker.Speak(text)
                finally:
                    self.is_speaking = False

            thread = threading.Thread(target=_speak_thread, daemon=True)
            thread.start()

            if blocking:
                thread.join()

            return True

        except Exception as e:
            print(f"[Fallback Voice] Error: {e}")
            # Last resort: print to console
            print(f"[COUNSELOR SAYS]: {text}")
            return False

    def is_available(self) -> bool:
        """Fallback is always available."""
        return True

    def stop(self) -> None:
        """Stop not supported in fallback."""
        self.is_speaking = False
