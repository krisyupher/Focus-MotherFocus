"""
Animated Avatar - Real-time facial animation with mouth sync, blinking, and expressions.

This creates a dynamic speaking avatar that:
- Tracks facial landmarks in real-time using DLib
- Animates mouth movements synchronized with TTS
- Adds blinking animation
- Applies visual effects (Zordon green tint)
"""
import cv2
import numpy as np
from typing import Optional, Tuple
import time
import random


class AnimatedAvatar:
    """
    Animated avatar with facial landmarks, mouth sync, and expressions.

    Features:
    - Real-time facial landmark detection using OpenCV Haar Cascades
    - Mouth movement synchronized to speech
    - Automatic blinking animation
    - Zordon-style visual effects
    """

    def __init__(self):
        """Initialize face detector."""
        # Use OpenCV's Haar Cascade for face detection (simpler, no extra dependencies)
        self.face_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        )
        self.eye_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_eye.xml'
        )
        self.mouth_cascade = cv2.CascadeClassifier(
            cv2.data.haarcascades + 'haarcascade_smile.xml'
        )

        # Animation state
        self.is_speaking = False
        self.mouth_open_amount = 0.0  # 0.0 = closed, 1.0 = fully open
        self.last_blink_time = time.time()
        self.blink_duration = 0.15  # 150ms blink
        self.is_blinking = False
        self.blink_start = 0.0

        # Mouth animation for speech
        self.speech_intensity = 0.0
        self.mouth_animation_speed = 8.0  # Hz - how fast mouth moves
        self.last_mouth_update = time.time()

    def start_speaking(self):
        """Call when TTS starts speaking."""
        self.is_speaking = True
        self.speech_intensity = 1.0

    def stop_speaking(self):
        """Call when TTS stops speaking."""
        self.is_speaking = False
        self.speech_intensity = 0.0
        self.mouth_open_amount = 0.0

    def update_speech_intensity(self, intensity: float):
        """
        Update speech intensity (0.0 to 1.0).

        Args:
            intensity: How loud/active the speech is (affects mouth opening)
        """
        self.speech_intensity = max(0.0, min(1.0, intensity))

    def _update_mouth_animation(self):
        """Update mouth opening based on speech state."""
        current_time = time.time()

        if self.is_speaking:
            # Animate mouth opening/closing in sync with speech
            # Use sine wave for natural talking motion
            elapsed = current_time - self.last_mouth_update
            phase = (elapsed * self.mouth_animation_speed) % (2 * np.pi)

            # Mouth opens and closes rhythmically
            base_opening = (np.sin(phase) + 1) / 2  # 0 to 1
            self.mouth_open_amount = base_opening * self.speech_intensity * 0.6

        else:
            # Gradually close mouth when not speaking
            self.mouth_open_amount *= 0.8
            if self.mouth_open_amount < 0.01:
                self.mouth_open_amount = 0.0

    def _update_blinking(self):
        """Update blinking animation."""
        current_time = time.time()

        # Random blinking every 2-5 seconds
        if not self.is_blinking:
            time_since_last_blink = current_time - self.last_blink_time
            # Random blink interval between 2-5 seconds
            if time_since_last_blink > random.uniform(2.0, 5.0):
                self.is_blinking = True
                self.blink_start = current_time
                self.last_blink_time = current_time
        else:
            # Check if blink is finished
            if current_time - self.blink_start > self.blink_duration:
                self.is_blinking = False

    def process_frame(self, frame: np.ndarray, is_speaking: bool = False) -> Optional[np.ndarray]:
        """
        Process camera frame with facial animation.

        Args:
            frame: Input frame from camera (BGR)
            is_speaking: Whether TTS is currently speaking

        Returns:
            Processed frame with animations and effects
        """
        if frame is None:
            return None

        # Update speaking state
        self.is_speaking = is_speaking

        # Update animations
        self._update_mouth_animation()
        self._update_blinking()

        # Convert to grayscale for detection
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

        # Detect faces
        faces = self.face_cascade.detectMultiScale(gray, 1.3, 5)

        # Draw facial enhancements
        annotated_frame = frame.copy()

        if len(faces) > 0:
            # Get the largest face
            (x, y, w, h) = max(faces, key=lambda face: face[2] * face[3])

            # Draw green rectangle around face
            cv2.rectangle(annotated_frame, (x, y), (x + w, y + h), (0, 255, 0), 2)

            # Face region for eye and mouth detection
            face_roi_gray = gray[y:y+h, x:x+w]
            face_roi_color = annotated_frame[y:y+h, x:x+w]

            # Detect eyes
            eyes = self.eye_cascade.detectMultiScale(face_roi_gray, 1.1, 10)

            # Draw eyes
            for (ex, ey, ew, eh) in eyes[:2]:  # Only first 2 eyes
                if self.is_blinking:
                    # Draw closed eyes as horizontal lines
                    eye_center_y = ey + eh // 2
                    cv2.line(
                        face_roi_color,
                        (ex, eye_center_y),
                        (ex + ew, eye_center_y),
                        (0, 255, 0),
                        3
                    )
                else:
                    # Draw open eyes as rectangles
                    cv2.rectangle(face_roi_color, (ex, ey), (ex + ew, ey + eh), (0, 255, 0), 2)

            # Detect mouth/smile region
            mouths = self.mouth_cascade.detectMultiScale(face_roi_gray, 1.7, 11)

            # Draw animated mouth
            for (mx, my, mw, mh) in mouths[:1]:  # Only first mouth
                # Mouth animation based on speech
                if self.is_speaking and self.mouth_open_amount > 0.3:
                    # Draw open mouth as filled ellipse
                    mouth_center = (mx + mw // 2, my + mh // 2)
                    radius_x = int(mw // 2 * self.mouth_open_amount)
                    radius_y = int(mh // 2 * self.mouth_open_amount)
                    cv2.ellipse(
                        face_roi_color,
                        mouth_center,
                        (radius_x, radius_y),
                        0, 0, 360,
                        (0, 255, 0),
                        -1
                    )
                else:
                    # Draw closed mouth as line
                    mouth_y = my + mh // 2
                    cv2.line(
                        face_roi_color,
                        (mx, mouth_y),
                        (mx + mw, mouth_y),
                        (0, 255, 0),
                        2
                    )

        # Apply Zordon effect
        final_frame = self._apply_zordon_effect(annotated_frame)

        return final_frame


    def _apply_zordon_effect(self, frame: np.ndarray) -> np.ndarray:
        """
        Apply Zordon-style visual effect.

        Args:
            frame: Input frame (BGR)

        Returns:
            Frame with green tint, blur, and darkening
        """
        # Green tint overlay
        green_tint = np.zeros_like(frame)
        green_tint[:, :, 1] = 80  # Brighter green for animated avatar
        tinted = cv2.addWeighted(frame, 0.6, green_tint, 0.4, 0)

        # Slight blur for retro look
        blurred = cv2.GaussianBlur(tinted, (5, 5), 0)

        # Darken slightly for better contrast
        darkened = cv2.convertScaleAbs(blurred, alpha=0.7, beta=0)

        # Add scanline effect for retro CRT look
        scanlines = darkened.copy()
        for i in range(0, scanlines.shape[0], 4):
            scanlines[i:i+1, :] = scanlines[i:i+1, :] * 0.8

        return scanlines

    def release(self):
        """Release resources."""
        # Nothing to release for Haar Cascades
        pass
