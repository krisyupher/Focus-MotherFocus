"""Process name value object for application identification."""
from dataclasses import dataclass
import os


@dataclass(frozen=True)
class ProcessName:
    """
    Immutable value object representing a Windows process name.

    Process names are normalized to lowercase with .exe extension for consistency.
    Supports path extraction and case-insensitive matching.
    """
    value: str

    def __init__(self, name: str):
        """
        Create a ProcessName from a string.

        Args:
            name: Process name, can be:
                - Simple name: "chrome"
                - With extension: "chrome.exe"
                - Full path: "C:\\Program Files\\Google\\Chrome\\chrome.exe"

        Raises:
            ValueError: If name is empty or whitespace-only
            TypeError: If name is not a string
        """
        if not isinstance(name, str):
            raise TypeError("Process name must be a string")

        name = name.strip()
        if not name:
            raise ValueError("Process name cannot be empty")

        # Extract filename from path if provided
        base_name = os.path.basename(name)

        # Normalize to lowercase
        base_name = base_name.lower()

        # Ensure .exe extension
        if not base_name.endswith('.exe'):
            # Remove any other extension and add .exe
            base_name = os.path.splitext(base_name)[0] + '.exe'

        # Use object.__setattr__ because dataclass is frozen
        object.__setattr__(self, 'value', base_name)

    def __str__(self) -> str:
        """String representation returns the process name value."""
        return self.value

    def __repr__(self) -> str:
        """Developer-friendly representation."""
        return f"ProcessName(value='{self.value}')"

    def __eq__(self, other) -> bool:
        """Case-insensitive equality comparison."""
        if not isinstance(other, ProcessName):
            return False
        return self.value.lower() == other.value.lower()

    def __hash__(self) -> int:
        """Hash based on lowercase value for use in sets/dicts."""
        return hash(self.value.lower())

    @classmethod
    def from_string(cls, name: str) -> 'ProcessName':
        """Factory method to create ProcessName from string."""
        return cls(name)

    def get_base_name(self) -> str:
        """
        Get the process name without the .exe extension.

        Returns:
            Process name without extension (e.g., "chrome" from "chrome.exe")
        """
        return os.path.splitext(self.value)[0]

    def matches(self, other: str) -> bool:
        """
        Check if this process name matches another string (case-insensitive).

        Args:
            other: String to compare against (can be with or without .exe)

        Returns:
            True if names match (case-insensitive)
        """
        other = other.lower().strip()

        # Try exact match first
        if self.value.lower() == other:
            return True

        # Try matching without extension
        if self.get_base_name() == other:
            return True

        # Try adding .exe to other
        if not other.endswith('.exe'):
            if self.value.lower() == other + '.exe':
                return True

        return False
