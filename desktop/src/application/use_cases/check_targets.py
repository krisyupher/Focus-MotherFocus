"""Unified use case for checking monitoring targets (websites + applications)."""
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.interfaces.http_checker import IHttpChecker
from src.application.interfaces.browser_detector import IBrowserDetector
from src.application.interfaces.process_detector import IProcessDetector
from src.application.interfaces.alert_notifier import IAlertNotifier
from src.application.interfaces.i_browser_controller import IBrowserController
from typing import Optional


class CheckTargetsUseCase:
    """
    Unified use case for checking all monitoring targets.

    For each target, checks:
    - If it has a website: Is it reachable AND open in browser?
    - If it has an application: Is the process running?

    Alert logic:
    - Trigger alert if EITHER condition is true (website active OR app running)
    - Clear alert if BOTH conditions are false
    - Auto-close browser tabs after 10 seconds if configured
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        http_checker: IHttpChecker,
        browser_detector: Optional[IBrowserDetector],
        process_detector: IProcessDetector,
        alert_notifier: IAlertNotifier,
        browser_controller: Optional[IBrowserController] = None,
        auto_close_threshold: float = 10.0
    ):
        """
        Initialize the use case with dependencies.

        Args:
            session: The unified monitoring session
            http_checker: Service for checking HTTP reachability
            browser_detector: Optional service for detecting browser tabs
            process_detector: Service for detecting running processes
            alert_notifier: Service for sending/clearing alerts
            browser_controller: Optional service for controlling browser tabs
            auto_close_threshold: Seconds before auto-closing browser tabs
        """
        self._session = session
        self._http_checker = http_checker
        self._browser_detector = browser_detector
        self._process_detector = process_detector
        self._alert_notifier = alert_notifier
        self._browser_controller = browser_controller
        self._auto_close_threshold = auto_close_threshold

    def execute(self) -> None:
        """
        Check all monitoring targets and manage alerts.

        For each target:
        1. Check website (if configured): HTTP reachable + browser open
        2. Check application (if configured): Process running
        3. Alert if EITHER is active
        """
        if not self._session.is_active:
            return

        for target in self._session.get_all_targets():
            is_active = False

            # Check website condition (if target has URL)
            if target.has_website():
                is_website_active = self._check_website(target)
                is_active = is_active or is_website_active

            # Check application condition (if target has process name)
            if target.has_application():
                is_app_active = self._check_application(target)
                is_active = is_active or is_app_active

            # Manage alert state - CONTINUOUS ALERTS
            if is_active:
                # Mark as alerting if not already
                if not target.is_alerting:
                    target.mark_as_alerting()

                # Send alert EVERY check cycle while active (continuous alerts)
                self._alert_notifier.notify(target.name)

                # Auto-close browser tab if threshold exceeded
                if (self._browser_controller and
                    target.has_website() and
                    target.should_auto_close(self._auto_close_threshold)):

                    # Only print when actually attempting to close
                    closed = self._browser_controller.close_tab_with_url(target.url)
                    if closed:
                        print(f"[AutoClose] Closed {target.name} after {target.get_alert_duration():.0f}s")
            else:
                # Clear alert when target becomes inactive
                if target.is_alerting:
                    target.clear_alert()
                    self._alert_notifier.clear(target.name)

    def _check_website(self, target) -> bool:
        """
        Check if target's website is active (reachable + in browser).

        Args:
            target: MonitoringTarget with URL

        Returns:
            True if website is reachable AND open in browser
        """
        if not target.url:
            return False

        # Check HTTP reachability
        if not self._http_checker.check_website(target.url):
            return False

        # Check browser presence (if detector available)
        if self._browser_detector:
            return self._browser_detector.is_url_open_in_browser(target.url)

        # If no browser detector, just use HTTP check
        return True

    def _check_application(self, target) -> bool:
        """
        Check if target's application is running.

        Args:
            target: MonitoringTarget with process_name

        Returns:
            True if application process is running
        """
        if not target.process_name:
            return False

        return self._process_detector.is_process_running(target.process_name)
