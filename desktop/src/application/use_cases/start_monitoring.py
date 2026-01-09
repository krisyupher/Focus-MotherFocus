"""Start Monitoring Use Case"""
from dataclasses import dataclass
from typing import Callable
from ...core.entities.monitoring_session import MonitoringSession
from ..interfaces.config_repository import IConfigRepository
from ..interfaces.monitoring_scheduler import IMonitoringScheduler


@dataclass
class StartMonitoringUseCase:
    """
    Use case for starting the monitoring session.

    This encapsulates the business logic for starting monitoring,
    including validation and scheduler coordination.

    Attributes:
        session: The monitoring session to start
        config_repository: Repository for persisting configuration
        scheduler: Scheduler for periodic checks
    """
    session: MonitoringSession
    config_repository: IConfigRepository
    scheduler: IMonitoringScheduler

    def execute(self, check_callback: Callable[[], None]) -> None:
        """
        Start the monitoring session.

        Args:
            check_callback: Callback function to execute on each monitoring interval

        Raises:
            ValueError: If session cannot be started (no websites, already active, etc.)
        """
        # Validate session can be started
        if self.session.is_active:
            raise ValueError("Monitoring session is already active")

        if self.session.get_website_count() == 0:
            raise ValueError("Cannot start monitoring with no websites")

        # Start the session
        self.session.start()

        # Persist the active state
        self.config_repository.save_session(self.session)

        # Start the scheduler
        self.scheduler.start(
            interval=self.session.monitoring_interval,
            callback=check_callback
        )
