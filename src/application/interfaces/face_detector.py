"""
Face Detector Interface

Defines the contract for face detection adapters used in avatar generation.
"""
from abc import ABC, abstractmethod
from typing import Optional, Tuple
import numpy as np


class IFaceDetector(ABC):
    """
    Interface for face detection services.

    Implementations should detect faces in images and provide bounding boxes
    for cropping avatar images.
    """

    @abstractmethod
    def detect_face(self, image: np.ndarray) -> Optional[Tuple[int, int, int, int]]:
        """
        Detect a face in the given image.

        Args:
            image: Input image as numpy array (BGR format from OpenCV)

        Returns:
            Tuple of (x, y, width, height) for the detected face bounding box,
            or None if no face detected.
            If multiple faces detected, returns the largest one.
        """
        pass

    @abstractmethod
    def crop_face(
        self,
        image: np.ndarray,
        bounding_box: Tuple[int, int, int, int],
        padding_percent: float = 0.2
    ) -> np.ndarray:
        """
        Crop face region from image with optional padding.

        Args:
            image: Input image as numpy array
            bounding_box: (x, y, width, height) of face region
            padding_percent: Percentage to expand bounding box (0.0 to 0.5)

        Returns:
            Cropped face image as numpy array (square aspect ratio)
        """
        pass
