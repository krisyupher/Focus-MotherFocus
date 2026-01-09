"""Use case for adding a monitoring target."""
from typing import Optional
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.core.entities.monitoring_target import MonitoringTarget
from src.core.value_objects.url import URL
from src.core.value_objects.process_name import ProcessName
from src.application.interfaces.config_repository import IConfigRepository


class AddTargetUseCase:
    """
    Use case for adding a monitoring target (website, app, or both).

    Examples:
        # Website only
        add_target.execute("Google", url_string="google.com")

        # App only
        add_target.execute("Calculator", process_name_string="calc.exe")

        # Both (hybrid monitoring)
        add_target.execute(
            "Netflix",
            url_string="netflix.com",
            process_name_string="Netflix.exe"
        )
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        config_repository: IConfigRepository
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The unified monitoring session
            config_repository: Repository for persisting configuration
        """
        self._session = session
        self._config_repository = config_repository

    def execute(
        self,
        name: str,
        url_string: Optional[str] = None,
        process_name_string: Optional[str] = None
    ) -> MonitoringTarget:
        """
        Add a monitoring target.

        Args:
            name: Display name for the target (e.g., "Netflix")
            url_string: Optional URL to monitor (e.g., "netflix.com")
            process_name_string: Optional process name (e.g., "Netflix.exe")

        Returns:
            The MonitoringTarget entity that was created

        Raises:
            ValueError: If target already exists, or no monitoring type specified
        """
        # Convert strings to value objects
        url = URL(url_string) if url_string else None
        process_name = ProcessName(process_name_string) if process_name_string else None

        # Add target to session (domain logic)
        target = self._session.add_target(name, url, process_name)

        # Persist the updated session
        self._config_repository.save_session(self._session)

        return target
