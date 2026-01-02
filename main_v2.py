"""
Unified Website & Application Monitor - Clean Architecture Implementation

Main entry point with unified monitoring targets.
Each target can monitor website, application, or both.
"""
import tkinter as tk
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.use_cases.add_target import AddTargetUseCase
from src.application.use_cases.remove_target import RemoveTargetUseCase
from src.application.use_cases.check_targets import CheckTargetsUseCase
from src.application.use_cases.start_monitoring_v2 import StartMonitoringV2UseCase
from src.application.use_cases.stop_monitoring_v2 import StopMonitoringV2UseCase
from src.infrastructure.adapters import (
    RequestsHttpChecker,
    WindowsAlertNotifier,
    ThreadedScheduler
)
from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector
from src.infrastructure.adapters.windows_process_detector import WindowsProcessDetector
from src.infrastructure.adapters.windows_startup_manager import WindowsStartupManager
from src.infrastructure.persistence.json_config_repository_v2 import JsonConfigRepositoryV2
from src.presentation.gui_v2 import UnifiedMonitorGUI


def create_application() -> UnifiedMonitorGUI:
    """
    Composition root - creates and wires all dependencies for unified monitoring.

    Unified Architecture:
    - Each target can monitor website, app, or both
    - Single GUI for all monitoring
    - Alert if EITHER condition is met

    Returns:
        Fully configured UnifiedMonitorGUI ready to run
    """
    # Create tkinter root window
    root = tk.Tk()

    # Infrastructure layer - adapters
    config_repository = JsonConfigRepositoryV2(config_file_path="config.json")
    http_checker = RequestsHttpChecker(timeout=5)
    browser_detector = WindowsBrowserDetector()
    process_detector = WindowsProcessDetector()
    alert_notifier = WindowsAlertNotifier(parent_window=root)
    scheduler = ThreadedScheduler()
    startup_manager = WindowsStartupManager()

    # Load or create unified monitoring session
    session = config_repository.load_session()

    # Application layer - use cases
    add_target_use_case = AddTargetUseCase(
        session=session,
        config_repository=config_repository
    )

    remove_target_use_case = RemoveTargetUseCase(
        session=session,
        config_repository=config_repository,
        alert_notifier=alert_notifier
    )

    check_targets_use_case = CheckTargetsUseCase(
        session=session,
        http_checker=http_checker,
        browser_detector=browser_detector,
        process_detector=process_detector,
        alert_notifier=alert_notifier
    )

    start_monitoring_use_case = StartMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler
    )

    stop_monitoring_use_case = StopMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler,
        alert_notifier=alert_notifier
    )

    # Presentation layer - Unified GUI
    gui = UnifiedMonitorGUI(
        session=session,
        add_target_use_case=add_target_use_case,
        remove_target_use_case=remove_target_use_case,
        start_monitoring_use_case=start_monitoring_use_case,
        stop_monitoring_use_case=stop_monitoring_use_case,
        check_targets_use_case=check_targets_use_case,
        startup_manager=startup_manager,
        root=root
    )

    return gui


def main():
    """Main entry point"""
    app = create_application()
    app.run()


if __name__ == "__main__":
    main()
