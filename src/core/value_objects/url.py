"""URL Value Object - Immutable representation of a website URL"""
from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class URL:
    """
    Value object representing a website URL.

    Immutable and self-validating. Ensures URLs are properly formatted
    with protocol prefix and validates basic structure.

    Attributes:
        value: The normalized URL string with protocol

    Raises:
        ValueError: If the URL is invalid or empty
    """
    value: str

    def __post_init__(self):
        """Validate URL after initialization"""
        if not self.value or not self.value.strip():
            raise ValueError("URL cannot be empty")

        # Validate the normalized value
        normalized = self._normalize(self.value)
        if not self._is_valid_url(normalized):
            raise ValueError(f"Invalid URL format: {self.value}")

        # Replace value with normalized version using object.__setattr__
        # (needed because dataclass is frozen)
        object.__setattr__(self, 'value', normalized)

    @staticmethod
    def _normalize(url: str) -> str:
        """
        Normalize URL by adding protocol if missing.

        Args:
            url: Raw URL string

        Returns:
            Normalized URL with https:// prefix
        """
        url = url.strip()

        # If already has protocol, return as-is
        if url.startswith(('http://', 'https://')):
            return url

        # Add https:// prefix for bare domains
        return f'https://{url}'

    @staticmethod
    def _is_valid_url(url: str) -> bool:
        """
        Basic URL validation.

        Args:
            url: URL to validate

        Returns:
            True if URL appears valid
        """
        # Must start with http:// or https://
        if not url.startswith(('http://', 'https://')):
            return False

        # Must have content after protocol
        domain_part = url.split('://', 1)[1]
        if not domain_part or len(domain_part.strip()) == 0:
            return False

        # Must not have spaces
        if ' ' in url:
            return False

        return True

    @classmethod
    def from_string(cls, url_string: str) -> 'URL':
        """
        Factory method to create URL from string.

        Args:
            url_string: URL string to parse

        Returns:
            URL value object

        Raises:
            ValueError: If URL is invalid
        """
        return cls(value=url_string)

    def __str__(self) -> str:
        """String representation returns the URL value"""
        return self.value

    def __repr__(self) -> str:
        """Developer-friendly representation"""
        return f"URL('{self.value}')"

    def __eq__(self, other) -> bool:
        """URLs are equal if their values match"""
        if not isinstance(other, URL):
            return False
        return self.value == other.value

    def __hash__(self) -> int:
        """Allow URLs to be used in sets and as dict keys"""
        return hash(self.value)
