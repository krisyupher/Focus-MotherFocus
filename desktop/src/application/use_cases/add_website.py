"""Add Website Use Case"""
from dataclasses import dataclass
from ...core.entities.monitoring_session import MonitoringSession
from ...core.entities.website import Website
from ...core.value_objects.url import URL
from ..interfaces.config_repository import IConfigRepository


@dataclass
class AddWebsiteUseCase:
    """
    Use case for adding a website to the monitoring session.

    This encapsulates the business logic for adding a website,
    including validation, persistence, and state management.

    Attributes:
        session: The monitoring session to add to
        config_repository: Repository for persisting configuration
    """
    session: MonitoringSession
    config_repository: IConfigRepository

    def execute(self, url_string: str) -> Website:
        """
        Add a website to the monitoring session.

        Args:
            url_string: URL string to add

        Returns:
            The Website entity that was added

        Raises:
            ValueError: If URL is invalid or already exists
        """
        # Parse and validate URL
        url = URL.from_string(url_string)

        # Check if already exists
        if self.session.has_website(url):
            raise ValueError(f"Website {url} is already being monitored")

        # Add to session
        website = self.session.add_website(url)

        # Persist configuration
        self.config_repository.save_session(self.session)

        return website
