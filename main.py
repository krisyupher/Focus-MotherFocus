"""
Website Monitor Application - Clean Architecture Implementation

Main entry point and composition root for dependency injection.
This file wires together all the layers following the dependency inversion principle.
"""
import tkinter as tk
from src.core.entities.monitoring_session import MonitoringSession
from src.application.use_cases import (
    AddWebsiteUseCase,
    RemoveWebsiteUseCase,
    StartMonitoringUseCase,
    StopMonitoringUseCase,
    CheckWebsitesUseCase
)
from src.infrastructure.adapters import (
    RequestsHttpChecker,
    WindowsAlertNotifier,
    ThreadedScheduler
)
from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector
from src.infrastructure.persistence import JsonConfigRepository
from src.presentation import WebsiteMonitorGUI


def create_application() -> WebsiteMonitorGUI:
    """
    Composition root - creates and wires all dependencies.

    This is where dependency injection happens. All layers are instantiated
    and connected here, following the dependency inversion principle.

    Returns:
        Fully configured WebsiteMonitorGUI ready to run
    """
    # Create tkinter root window
    root = tk.Tk()

    # Infrastructure layer - adapters
    config_repository = JsonConfigRepository(config_file_path="config.json")
    http_checker = RequestsHttpChecker(timeout=5)
    alert_notifier = WindowsAlertNotifier(parent_window=root)
    scheduler = ThreadedScheduler()
    browser_detector = WindowsBrowserDetector()  # NEW: Browser tab detection

    # Load or create monitoring session
    session = config_repository.load_session()

    # Application layer - use cases
    add_website_use_case = AddWebsiteUseCase(
        session=session,
        config_repository=config_repository
    )

    remove_website_use_case = RemoveWebsiteUseCase(
        session=session,
        config_repository=config_repository,
        alert_notifier=alert_notifier
    )

    start_monitoring_use_case = StartMonitoringUseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler
    )

    stop_monitoring_use_case = StopMonitoringUseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler,
        alert_notifier=alert_notifier
    )

    check_websites_use_case = CheckWebsitesUseCase(
        session=session,
        http_checker=http_checker,
        alert_notifier=alert_notifier,
        browser_detector=browser_detector  # NEW: Inject browser detector
    )

    # Presentation layer - GUI
    gui = WebsiteMonitorGUI(
        session=session,
        add_website_use_case=add_website_use_case,
        remove_website_use_case=remove_website_use_case,
        start_monitoring_use_case=start_monitoring_use_case,
        stop_monitoring_use_case=stop_monitoring_use_case,
        check_websites_use_case=check_websites_use_case,
        root=root
    )

    return gui


def main():
    """Main entry point"""
    app = create_application()
    app.run()


if __name__ == "__main__":
    main()
