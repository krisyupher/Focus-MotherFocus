"""
Generate Avatar Use Case

Orchestrates face detection, capture, and avatar creation from webcam.
"""
import time
from typing import Optional
from src.application.interfaces.face_detector import IFaceDetector
from src.infrastructure.adapters.camera_manager import CameraManager
from src.infrastructure.storage.avatar_storage import AvatarStorage


class GenerateAvatarUseCase:
    """
    Use case for generating user avatar from webcam.

    Captures face from camera, detects face region, crops, and saves as avatar.
    """

    def __init__(
        self,
        face_detector: IFaceDetector,
        camera_manager: CameraManager,
        avatar_storage: AvatarStorage
    ):
        """
        Initialize use case.

        Args:
            face_detector: Face detection service
            camera_manager: Camera capture service
            avatar_storage: Avatar storage service
        """
        self._face_detector = face_detector
        self._camera = camera_manager
        self._storage = avatar_storage

    def execute(self, max_attempts: int = 30, timeout_seconds: int = 30) -> bool:
        """
        Generate avatar from webcam.

        Args:
            max_attempts: Maximum number of capture attempts
            timeout_seconds: Maximum time to try capturing (seconds)

        Returns:
            True if avatar generated successfully, False otherwise

        Raises:
            Exception: If camera fails to start
        """
        print("[GENERATE AVATAR] Starting avatar generation...")

        # Start camera
        if not self._camera.start_camera():
            raise Exception("Failed to start camera")

        try:
            start_time = time.time()
            attempts = 0

            while attempts < max_attempts and (time.time() - start_time) < timeout_seconds:
                attempts += 1

                # Capture frame
                frame = self._camera.capture_frame()
                if frame is None:
                    print(f"[GENERATE AVATAR] Attempt {attempts}: No frame captured")
                    time.sleep(0.1)
                    continue

                # Detect face
                face_bbox = self._face_detector.detect_face(frame)
                if face_bbox is None:
                    print(f"[GENERATE AVATAR] Attempt {attempts}: No face detected")
                    time.sleep(0.1)
                    continue

                # Face found! Crop it
                print(f"[GENERATE AVATAR] Face detected on attempt {attempts}!")
                cropped_face = self._face_detector.crop_face(frame, face_bbox, padding_percent=0.2)

                # Save avatar
                metadata = {
                    "face_detection_method": "opencv_haar_cascade",
                    "detection_confidence": "high",
                    "attempts": attempts
                }
                self._storage.save_avatar(cropped_face, metadata)

                print("[GENERATE AVATAR] Avatar generated successfully!")
                return True

            # Failed to detect face
            print(f"[GENERATE AVATAR] Failed after {attempts} attempts")
            return False

        finally:
            # Always stop camera
            self._camera.stop_camera()

    def delete_avatar(self) -> None:
        """
        Delete existing avatar.

        Used when user wants to regenerate from scratch.
        """
        self._storage.delete_avatar()
        print("[GENERATE AVATAR] Avatar deleted")
