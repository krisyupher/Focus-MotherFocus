"""
Avatar Animator

Animates static avatar image with jaw movement, blinking, and speaking glow effects.
Synchronized with TTS for realistic speaking animation.
"""
import cv2
import numpy as np
import time
import math
import threading
from PIL import Image, ImageTk
from typing import Optional
from src.application.interfaces.tts_service import ITTSService


class AvatarAnimator:
    """
    Animates avatar with jaw movement, blinking, and glow effects.

    Coordinates animation state synchronized with TTS speech.
    """

    def __init__(self, avatar_path: str, tts_service: ITTSService):
        """
        Initialize avatar animator.

        Args:
            avatar_path: Path to avatar image file
            tts_service: TTS service for speaking
        """
        self._avatar_path = avatar_path
        self._tts_service = tts_service

        # Load base avatar
        self._base_avatar = cv2.imread(avatar_path)
        if self._base_avatar is None:
            raise FileNotFoundError(f"Avatar image not found: {avatar_path}")

        # Animation state
        self._is_speaking = False
        self._jaw_position = 0.0  # 0.0 (closed) to 1.0 (fully open)
        self._blink_position = 0.0  # 0.0 (open) to 1.0 (fully closed)
        self._glow_intensity = 0.0  # 0.0 to 1.0
        self._last_blink_time = time.time()
        self._lock = threading.Lock()

        # Pre-generate jaw position cache for performance
        self._jaw_cache = self._generate_jaw_cache()

        # Animation timing
        self._last_frame_time = time.time()
        self._frame_rate = 15  # FPS

    def _generate_jaw_cache(self) -> list:
        """
        Pre-generate jaw positions for performance.

        Returns:
            List of 10 pre-rendered jaw positions (0% to 90% open)
        """
        cache = []
        for i in range(10):
            jaw_percent = i / 10.0
            frame = self._animate_jaw(self._base_avatar.copy(), jaw_percent)
            cache.append(frame)
        return cache

    def _animate_jaw(self, image: np.ndarray, jaw_drop_percent: float) -> np.ndarray:
        """
        Animate jaw by warping bottom third of face.

        Args:
            image: Input avatar image
            jaw_drop_percent: 0.0 (closed) to 1.0 (fully open)

        Returns:
            Image with animated jaw
        """
        if jaw_drop_percent <= 0.01:
            return image

        h, w = image.shape[:2]
        jaw_start_y = int(h * 0.67)  # Bottom third

        # Define source points (original jaw position)
        src_points = np.float32([
            [0, jaw_start_y],
            [w, jaw_start_y],
            [0, h],
            [w, h]
        ])

        # Define destination points (stretched jaw)
        drop_pixels = int(15 * jaw_drop_percent)  # Max 15px drop
        dst_points = np.float32([
            [0, jaw_start_y],
            [w, jaw_start_y],
            [0, h + drop_pixels],
            [w, h + drop_pixels]
        ])

        # Apply perspective transform
        matrix = cv2.getPerspectiveTransform(src_points, dst_points)
        result = cv2.warpPerspective(image, matrix, (w, h + drop_pixels))

        # Crop back to original size and darken jaw area (mouth interior)
        result = result[:h, :]
        if drop_pixels > 0:
            result[jaw_start_y:, :] = cv2.addWeighted(
                result[jaw_start_y:, :], 0.7,
                np.zeros_like(result[jaw_start_y:, :]), 0, -30
            )

        return result

    def _add_blink(self, image: np.ndarray, blink_percent: float) -> np.ndarray:
        """
        Add blinking effect by darkening eye regions.

        Args:
            image: Input image
            blink_percent: 0.0 (open) to 1.0 (fully closed)

        Returns:
            Image with blink effect
        """
        if blink_percent <= 0.01:
            return image

        result = image.copy()
        h, w = result.shape[:2]

        # Estimate eye positions (facial symmetry)
        eye_y = int(h * 0.35)
        left_eye_x = int(w * 0.35)
        right_eye_x = int(w * 0.65)
        eye_width = int(w * 0.08)
        eye_height = int(h * 0.04)

        # Calculate blink coverage
        blink_height = int(eye_height * blink_percent)

        if blink_height > 0:
            for eye_x in [left_eye_x, right_eye_x]:
                x1 = max(0, eye_x - eye_width // 2)
                x2 = min(w, eye_x + eye_width // 2)
                y1 = max(0, eye_y - blink_height // 2)
                y2 = min(h, eye_y + blink_height // 2)

                if y2 > y1 and x2 > x1:
                    result[y1:y2, x1:x2] = cv2.addWeighted(
                        result[y1:y2, x1:x2], 0.3,
                        np.zeros_like(result[y1:y2, x1:x2]), 0, -50
                    )

        return result

    def _add_speaking_glow(self, image: np.ndarray, intensity: float) -> np.ndarray:
        """
        Add pulsing green glow border when speaking.

        Args:
            image: Input image
            intensity: 0.0 to 1.0 (pulsing effect)

        Returns:
            Image with glow border
        """
        if intensity <= 0.01:
            return image

        border_size = 8
        color = (0, 255, 0)  # Green (BGR)

        # Add border
        result = cv2.copyMakeBorder(
            image,
            border_size, border_size, border_size, border_size,
            cv2.BORDER_CONSTANT,
            value=color
        )

        # Create glow effect with Gaussian blur
        glow = result.copy()
        glow = cv2.GaussianBlur(glow, (21, 21), 0)

        # Blend based on intensity
        alpha = intensity * 0.6  # Max 60% glow
        result = cv2.addWeighted(result, 1 - alpha, glow, alpha, 0)

        return result

    def _apply_zordon_effect(self, image: np.ndarray) -> np.ndarray:
        """
        Apply Zordon-style green tint and effects.

        Args:
            image: Input image

        Returns:
            Image with Zordon effect
        """
        # Add green tint
        green_tint = np.zeros_like(image)
        green_tint[:, :, 1] = 60  # Green channel boost

        tinted = cv2.addWeighted(image, 0.65, green_tint, 0.35, 0)

        # Slight blur
        blurred = cv2.GaussianBlur(tinted, (5, 5), 0)

        # Darken for text readability
        darkened = cv2.convertScaleAbs(blurred, alpha=0.75, beta=0)

        return darkened

    def start_speaking(self, text: str) -> None:
        """
        Start speaking with synchronized animation.

        Args:
            text: Text to speak
        """
        with self._lock:
            self._is_speaking = True

        # Start TTS with callbacks
        self._tts_service.speak(
            text,
            on_start=self._on_speech_start,
            on_complete=self._on_speech_complete,
            blocking=False
        )

    def _on_speech_start(self) -> None:
        """Callback when speech starts."""
        print("[ANIMATOR] Speech started")

    def _on_speech_complete(self) -> None:
        """Callback when speech completes."""
        print("[ANIMATOR] Speech completed")
        with self._lock:
            self._is_speaking = False
            self._jaw_position = 0.0
            self._glow_intensity = 0.0

    def _update_animation_state(self) -> None:
        """
        Update animation state based on current time.

        Called every frame to animate jaw, blink, and glow.
        """
        current_time = time.time()
        delta_time = current_time - self._last_frame_time
        self._last_frame_time = current_time

        with self._lock:
            # Animate jaw if speaking
            if self._is_speaking:
                # Simulate jaw movement (oscillate between 0.5 and 0.8)
                self._jaw_position = 0.65 + 0.15 * math.sin(current_time * 8)

                # Pulse glow at 2 Hz
                self._glow_intensity = 0.5 + 0.5 * math.sin(current_time * 4 * math.pi)

            else:
                # Close jaw smoothly
                if self._jaw_position > 0:
                    self._jaw_position = max(0, self._jaw_position - delta_time * 2)

                # Fade out glow
                if self._glow_intensity > 0:
                    self._glow_intensity = max(0, self._glow_intensity - delta_time * 3)

            # Random blinking (every 3-5 seconds when not speaking)
            if not self._is_speaking:
                time_since_blink = current_time - self._last_blink_time

                if time_since_blink > 3 and self._blink_position == 0:
                    # Start blink
                    if np.random.random() < 0.1:  # 10% chance per frame
                        self._blink_position = 1.0
                        self._last_blink_time = current_time

                # Close blink after 150ms
                if self._blink_position > 0:
                    self._blink_position = max(0, self._blink_position - delta_time * 6)

    def get_current_frame_for_tk(self, width: int = 600, height: int = 500) -> Optional[ImageTk.PhotoImage]:
        """
        Get current animated frame for Tkinter display.

        Args:
            width: Target width in pixels
            height: Target height in pixels

        Returns:
            ImageTk.PhotoImage ready for Tkinter, or None if failed
        """
        try:
            # Update animation state
            self._update_animation_state()

            # Get cached jaw frame
            jaw_index = int(self._jaw_position * 9)  # 0-9 index
            jaw_index = min(9, max(0, jaw_index))
            frame = self._jaw_cache[jaw_index].copy()

            # Add blink
            frame = self._add_blink(frame, self._blink_position)

            # Add glow if speaking
            if self._glow_intensity > 0.1:
                frame = self._add_speaking_glow(frame, self._glow_intensity)

            # Apply Zordon effect
            frame = self._apply_zordon_effect(frame)

            # Resize to target dimensions
            frame = cv2.resize(frame, (width, height))

            # Convert to RGB for PIL
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # Convert to PIL Image
            pil_image = Image.fromarray(frame_rgb)

            # Convert to ImageTk
            tk_image = ImageTk.PhotoImage(pil_image)

            return tk_image

        except Exception as e:
            print(f"[ANIMATOR] Frame generation error: {e}")
            return None

    def stop_animation(self) -> None:
        """Stop all animation and TTS."""
        self._tts_service.stop()
        with self._lock:
            self._is_speaking = False
            self._jaw_position = 0.0
            self._blink_position = 0.0
            self._glow_intensity = 0.0
