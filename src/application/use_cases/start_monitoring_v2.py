"""Use case for starting monitoring - Unified version for MonitoringSessionV2"""
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.interfaces.config_repository import IConfigRepository
from src.application.interfaces.monitoring_scheduler import IMonitoringScheduler
from typing import Callable


class StartMonitoringV2UseCase:
    """
    Use case for starting the unified monitoring session.

    Works with MonitoringSessionV2 and unified targets.
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        config_repository: IConfigRepository,
        scheduler: IMonitoringScheduler
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The unified monitoring session
            config_repository: Repository for persisting configuration
            scheduler: Scheduler for periodic monitoring checks
        """
        self.session = session
        self.config_repository = config_repository
        self.scheduler = scheduler

    def execute(self, check_callback: Callable[[], None]) -> None:
        """
        Start monitoring all targets.

        Args:
            check_callback: Callback function to execute periodically (CheckTargetsUseCase.execute)

        Raises:
            ValueError: If no targets to monitor or session already active
        """
        # Validate we have targets to monitor
        if self.session.get_target_count() == 0:
            raise ValueError("No targets to monitor. Please add at least one target.")

        # Start the session (validates state)
        self.session.start()

        # Start the scheduler with the check callback
        interval = self.session.monitoring_interval
        self.scheduler.start(interval, check_callback)

        # Persist the updated session state
        self.config_repository.save_session(self.session)
