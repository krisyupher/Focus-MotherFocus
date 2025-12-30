"""Camera Manager for capturing live webcam feed."""
import cv2
import numpy as np
from PIL import Image, ImageTk
from typing import Optional
import threading
import time


class CameraManager:
    """
    Manages webcam access and frame capture.

    Provides thread-safe access to the webcam and captures frames
    for display in alert windows.
    """

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        """Singleton pattern to ensure only one camera instance."""
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        """Initialize camera manager."""
        if self._initialized:
            return

        self._camera: Optional[cv2.VideoCapture] = None
        self._is_active = False
        self._last_frame = None
        self._frame_lock = threading.Lock()
        self._initialized = True

    def start_camera(self) -> bool:
        """
        Start the webcam capture.

        Returns:
            True if camera started successfully, False otherwise
        """
        with self._lock:
            if self._is_active:
                return True

            try:
                # Use DirectShow backend on Windows to avoid MSMF errors
                self._camera = cv2.VideoCapture(0, cv2.CAP_DSHOW)  # CAP_DSHOW for Windows

                if not self._camera.isOpened():
                    # Fallback to default backend
                    self._camera = cv2.VideoCapture(0)

                if not self._camera.isOpened():
                    return False

                # Set camera properties for better quality
                self._camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                self._camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                self._camera.set(cv2.CAP_PROP_FPS, 30)

                self._is_active = True
                return True

            except Exception as e:
                print(f"Failed to start camera: {e}")
                return False

    def stop_camera(self):
        """Stop the webcam capture and release resources."""
        with self._lock:
            if self._camera is not None:
                self._camera.release()
                self._camera = None
            self._is_active = False
            self._last_frame = None

    def capture_frame(self) -> Optional[np.ndarray]:
        """
        Capture a single frame from the webcam.

        Returns:
            Numpy array containing the frame (BGR format), or None if failed
        """
        if not self._is_active or self._camera is None:
            if not self.start_camera():
                return None

        try:
            ret, frame = self._camera.read()
            if ret:
                with self._frame_lock:
                    self._last_frame = frame.copy()
                return frame
            return None

        except Exception as e:
            print(f"Failed to capture frame: {e}")
            return None

    def get_circular_frame_for_tk(self, size: int = 150) -> Optional[ImageTk.PhotoImage]:
        """
        Get a circular frame suitable for Tkinter display (Zordon-style).

        Args:
            size: Diameter of the circular frame in pixels

        Returns:
            ImageTk.PhotoImage ready for Tkinter, or None if failed
        """
        frame = self.capture_frame()
        if frame is None:
            return None

        try:
            # Convert BGR to RGB
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # Resize to square
            frame_resized = cv2.resize(frame_rgb, (size, size))

            # Create circular mask with retro effect
            mask = np.zeros((size, size), dtype=np.uint8)
            center = (size // 2, size // 2)
            radius = size // 2
            cv2.circle(mask, center, radius, 255, -1)

            # Apply mask to create circular image
            circular_frame = cv2.bitwise_and(frame_resized, frame_resized, mask=mask)

            # Add retro green tint (Zordon effect)
            green_tint = np.zeros_like(circular_frame)
            green_tint[:, :, 1] = 30  # Add green channel boost
            circular_frame = cv2.addWeighted(circular_frame, 1.0, green_tint, 0.3, 0)

            # Create alpha channel for transparency
            alpha = mask.copy()
            rgba_frame = cv2.merge([circular_frame[:, :, 0],
                                   circular_frame[:, :, 1],
                                   circular_frame[:, :, 2],
                                   alpha])

            # Convert to PIL Image
            pil_image = Image.fromarray(rgba_frame, mode='RGBA')

            # Convert to ImageTk for Tkinter
            tk_image = ImageTk.PhotoImage(pil_image)

            return tk_image

        except Exception as e:
            print(f"Failed to process frame: {e}")
            return None

    def get_fullscreen_background_for_tk(self, width: int = 600, height: int = 500) -> Optional[ImageTk.PhotoImage]:
        """
        Get a fullscreen camera frame for background (Zordon-style).

        Args:
            width: Width of the background in pixels
            height: Height of the background in pixels

        Returns:
            ImageTk.PhotoImage ready for Tkinter background, or None if failed
        """
        frame = self.capture_frame()
        if frame is None:
            print("[CAMERA] Failed to capture frame")
            return None

        print(f"[CAMERA] Frame captured: {frame.shape}")

        try:
            # Convert BGR to RGB
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # Resize to fit window
            frame_resized = cv2.resize(frame_rgb, (width, height))

            # Add heavy retro green tint (Zordon effect)
            green_tint = np.zeros_like(frame_resized)
            green_tint[:, :, 1] = 80  # Heavy green channel boost

            # Apply tint with transparency
            tinted_frame = cv2.addWeighted(frame_resized, 0.6, green_tint, 0.4, 0)

            # Add slight blur for dreamy effect
            blurred = cv2.GaussianBlur(tinted_frame, (5, 5), 0)

            # Darken the frame a bit for text readability
            darkened = cv2.convertScaleAbs(blurred, alpha=0.7, beta=0)

            # Convert to PIL Image
            pil_image = Image.fromarray(darkened)
            print(f"[CAMERA] PIL Image created: {pil_image.size} {pil_image.mode}")

            # Convert to ImageTk for Tkinter
            tk_image = ImageTk.PhotoImage(pil_image)
            print(f"[CAMERA] ImageTk created successfully")

            return tk_image

        except Exception as e:
            print(f"[CAMERA ERROR] Failed to process fullscreen frame: {e}")
            import traceback
            traceback.print_exc()
            return None

    def is_camera_available(self) -> bool:
        """
        Check if a camera is available on the system.

        Returns:
            True if camera can be accessed, False otherwise
        """
        try:
            test_camera = cv2.VideoCapture(0, cv2.CAP_DSHOW)
            if test_camera.isOpened():
                test_camera.release()
                return True
            return False
        except:
            return False

    def __del__(self):
        """Cleanup when object is destroyed."""
        self.stop_camera()
