"""
Avatar Storage Manager

Handles saving, loading, and managing avatar images and metadata.
"""
import os
import json
import cv2
import numpy as np
from datetime import datetime
from typing import Optional, Dict, Any


class AvatarStorage:
    """
    Manages avatar image and metadata storage.

    Stores avatar as PNG in config folder with accompanying metadata JSON.
    """

    def __init__(self, config_dir: str = "config"):
        """
        Initialize avatar storage.

        Args:
            config_dir: Directory to store avatar files (default: "config")
        """
        self._config_dir = config_dir
        self._avatar_path = os.path.join(config_dir, "avatar.png")
        self._meta_path = os.path.join(config_dir, "avatar_meta.json")

        # Ensure config directory exists
        os.makedirs(config_dir, exist_ok=True)

    def save_avatar(
        self,
        image: np.ndarray,
        metadata: Optional[Dict[str, Any]] = None
    ) -> None:
        """
        Save avatar image and metadata.

        Args:
            image: Avatar image as numpy array (BGR format)
            metadata: Optional metadata dictionary

        Raises:
            IOError: If save fails
        """
        # Resize to standard size (400x400) if needed
        if image.shape[0] != 400 or image.shape[1] != 400:
            image = cv2.resize(image, (400, 400))

        # Save image
        success = cv2.imwrite(self._avatar_path, image)
        if not success:
            raise IOError(f"Failed to save avatar to {self._avatar_path}")

        # Create metadata
        meta = {
            "created_at": datetime.now().isoformat(),
            "version": "1.0",
            "image_size": [400, 400],
            **(metadata or {})
        }

        # Save metadata
        with open(self._meta_path, 'w') as f:
            json.dump(meta, f, indent=2)

        print(f"[AVATAR] Saved to {self._avatar_path}")

    def load_avatar(self) -> Optional[np.ndarray]:
        """
        Load avatar image.

        Returns:
            Avatar image as numpy array (BGR format), or None if not found

        Raises:
            IOError: If file exists but cannot be read
        """
        if not os.path.exists(self._avatar_path):
            return None

        image = cv2.imread(self._avatar_path)
        if image is None:
            raise IOError(f"Avatar file exists but cannot be read: {self._avatar_path}")

        return image

    def load_metadata(self) -> Optional[Dict[str, Any]]:
        """
        Load avatar metadata.

        Returns:
            Metadata dictionary, or None if not found
        """
        if not os.path.exists(self._meta_path):
            return None

        try:
            with open(self._meta_path, 'r') as f:
                return json.load(f)
        except Exception as e:
            print(f"[AVATAR] Failed to load metadata: {e}")
            return None

    def avatar_exists(self) -> bool:
        """
        Check if avatar image exists.

        Returns:
            True if avatar file exists, False otherwise
        """
        return os.path.exists(self._avatar_path)

    def delete_avatar(self) -> None:
        """
        Delete avatar image and metadata.

        Used when user wants to regenerate avatar.
        """
        if os.path.exists(self._avatar_path):
            os.remove(self._avatar_path)
            print(f"[AVATAR] Deleted {self._avatar_path}")

        if os.path.exists(self._meta_path):
            os.remove(self._meta_path)
            print(f"[AVATAR] Deleted {self._meta_path}")

    def get_avatar_path(self) -> str:
        """
        Get full path to avatar image file.

        Returns:
            Absolute path to avatar.png
        """
        return os.path.abspath(self._avatar_path)
