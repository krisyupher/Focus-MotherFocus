"""Use case for removing an application from monitoring."""
from src.core.entities.monitoring_session import MonitoringSession
from src.core.entities.application import Application
from src.core.value_objects.process_name import ProcessName
from src.application.interfaces.config_repository import IConfigRepository


class RemoveApplicationUseCase:
    """
    Use case for removing a desktop application from the monitoring session.

    This use case handles the business logic for removing an application
    from monitoring, ensuring the change is persisted to configuration storage.
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

    def execute(self, process_name: ProcessName) -> Application:
        """
        Remove an application from the monitoring session.

        Args:
            process_name: ProcessName value object of the application to remove

        Returns:
            The Application entity that was removed

        Raises:
            ValueError: If application does not exist in the session
        """
        # Remove application from session (domain logic)
        application = self._session.remove_application(process_name)

        # Persist the updated session
        self._config_repository.save_session(self._session)

        return application
