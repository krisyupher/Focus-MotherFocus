"""
OpenCV Haar Cascade Face Detector

Uses OpenCV's built-in Haar Cascade classifier for frontal face detection.
"""
import cv2
import numpy as np
from typing import Optional, Tuple
from src.application.interfaces.face_detector import IFaceDetector


class OpenCVFaceDetector(IFaceDetector):
    """
    Face detector using OpenCV Haar Cascades.

    Uses pre-trained frontal face classifier included with OpenCV.
    No external dependencies required.
    """

    def __init__(self):
        """Initialize the Haar Cascade face detector."""
        # Load pre-trained frontal face cascade
        cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
        self._face_cascade = cv2.CascadeClassifier(cascade_path)

        if self._face_cascade.empty():
            raise RuntimeError("Failed to load Haar Cascade classifier")

    def detect_face(self, image: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        """
        Detect the largest face in the image using Haar Cascades.

        Args:
            image: Input image (BGR format from OpenCV)

        Returns:
            (x, y, width, height) of largest detected face, or None if no face found
        """
        # Convert to grayscale for detection
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        # Detect faces
        faces = self._face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,  # Balance between speed and accuracy
            minNeighbors=5,   # Reduce false positives
            minSize=(100, 100)  # Minimum face size (100x100 pixels)
        )

        if len(faces) == 0:
            return None

        # Return largest face (by area)
        largest_face = max(faces, key=lambda face: face[2] * face[3])
        return tuple(largest_face)

    def crop_face(
        self,
        image: np.ndarray,
        bounding_box: Tuple[int, int, int, int],
        padding_percent: float = 0.2
    ) -> np.ndarray:
        """
        Crop face region with padding and ensure square aspect ratio.

        Args:
            image: Input image
            bounding_box: (x, y, width, height) of face
            padding_percent: Percentage to expand box (default 20%)

        Returns:
            Cropped square face image
        """
        x, y, w, h = bounding_box
        img_height, img_width = image.shape[:2]

        # Calculate padding
        pad_x = int(w * padding_percent)
        pad_y = int(h * padding_percent)

        # Expand bounding box with padding
        x1 = max(0, x - pad_x)
        y1 = max(0, y - pad_y)
        x2 = min(img_width, x + w + pad_x)
        y2 = min(img_height, y + h + pad_y)

        # Make it square (use larger dimension)
        current_w = x2 - x1
        current_h = y2 - y1
        size = max(current_w, current_h)

        # Center the square
        center_x = (x1 + x2) // 2
        center_y = (y1 + y2) // 2

        x1 = max(0, center_x - size // 2)
        y1 = max(0, center_y - size // 2)
        x2 = min(img_width, x1 + size)
        y2 = min(img_height, y1 + size)

        # Crop
        cropped = image[y1:y2, x1:x2]

        # Enhance image quality
        cropped = self._enhance_image(cropped)

        return cropped

    def _enhance_image(self, image: np.ndarray) -> np.ndarray:
        """
        Enhance image quality for better avatar appearance.

        Args:
            image: Input image

        Returns:
            Enhanced image
        """
        # Convert to LAB color space for better contrast adjustment
        lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)

        # Apply CLAHE (Contrast Limited Adaptive Histogram Equalization) to L channel
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        l = clahe.apply(l)

        # Merge channels
        enhanced_lab = cv2.merge([l, a, b])
        enhanced = cv2.cvtColor(enhanced_lab, cv2.COLOR_LAB2BGR)

        # Slight sharpening
        kernel = np.array([[-1, -1, -1],
                          [-1,  9, -1],
                          [-1, -1, -1]])
        sharpened = cv2.filter2D(enhanced, -1, kernel)

        # Blend original and sharpened (50/50)
        result = cv2.addWeighted(enhanced, 0.5, sharpened, 0.5, 0)

        return result
