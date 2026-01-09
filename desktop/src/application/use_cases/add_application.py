"""Use case for adding an application to monitor."""
from src.core.entities.monitoring_session import MonitoringSession
from src.core.entities.application import Application
from src.core.value_objects.process_name import ProcessName
from src.application.interfaces.config_repository import IConfigRepository


class AddApplicationUseCase:
    """
    Use case for adding a desktop application to the monitoring session.

    This use case handles the business logic for adding an application
    to monitor, ensuring it's persisted to configuration storage.
    """

    def __init__(
        self,
        session: MonitoringSession,
        config_repository: IConfigRepository
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The monitoring session aggregate root
            config_repository: Repository for persisting configuration
        """
        self._session = session
        self._config_repository = config_repository

    def execute(self, process_name: ProcessName, display_name: str = "") -> Application:
        """
        Add an application to the monitoring session.

        Args:
            process_name: ProcessName value object of the application to monitor
            display_name: Optional human-readable name (defaults to process name)

        Returns:
            The Application entity that was added

        Raises:
            ValueError: If application with this process name already exists
        """
        # Add application to session (domain logic)
        application = self._session.add_application(process_name, display_name)

        # Persist the updated session
        self._config_repository.save_session(self._session)

        return application
