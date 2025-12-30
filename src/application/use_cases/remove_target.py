"""Use case for removing a monitoring target."""
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.core.entities.monitoring_target import MonitoringTarget
from src.application.interfaces.config_repository import IConfigRepository
from src.application.interfaces.alert_notifier import IAlertNotifier


class RemoveTargetUseCase:
    """
    Use case for removing a monitoring target.

    Ensures alerts are cleared before removal.
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        config_repository: IConfigRepository,
        alert_notifier: IAlertNotifier
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The unified monitoring session
            config_repository: Repository for persisting configuration
            alert_notifier: Service for clearing alerts
        """
        self._session = session
        self._config_repository = config_repository
        self._alert_notifier = alert_notifier

    def execute(self, target_id: str) -> MonitoringTarget:
        """
        Remove a monitoring target by ID.

        Args:
            target_id: ID of the target to remove

        Returns:
            The MonitoringTarget entity that was removed

        Raises:
            ValueError: If target does not exist
        """
        # Get target before removal
        target = self._session.get_target(target_id)
        if not target:
            raise ValueError(f"Target with ID {target_id} does not exist")

        # Clear any active alerts
        if target.is_alerting:
            self._alert_notifier.clear(target.name)

        # Remove from session
        removed_target = self._session.remove_target(target_id)

        # Persist the updated session
        self._config_repository.save_session(self._session)

        return removed_target

    def execute_by_name(self, name: str) -> MonitoringTarget:
        """
        Remove a monitoring target by name.

        Args:
            name: Name of the target to remove

        Returns:
            The MonitoringTarget entity that was removed

        Raises:
            ValueError: If target does not exist
        """
        # Get target before removal
        target = self._session.get_target_by_name(name)
        if not target:
            raise ValueError(f"Target '{name}' does not exist")

        # Clear any active alerts
        if target.is_alerting:
            self._alert_notifier.clear(target.name)

        # Remove from session
        removed_target = self._session.remove_target_by_name(name)

        # Persist the updated session
        self._config_repository.save_session(self._session)

        return removed_target
