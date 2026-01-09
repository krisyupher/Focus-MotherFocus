"""Check Websites Use Case"""
from dataclasses import dataclass
from datetime import datetime
from typing import Optional
from ...core.entities.monitoring_session import MonitoringSession
from ...core.value_objects.url import URL
from ..interfaces.http_checker import IHttpChecker
from ..interfaces.alert_notifier import IAlertNotifier
from ..interfaces.browser_detector import IBrowserDetector


@dataclass
class CheckWebsitesUseCase:
    """
    Use case for checking all websites in the monitoring session.

    This encapsulates the business logic for periodically checking
    website availability and triggering alerts.

    Attributes:
        session: The monitoring session with websites to check
        http_checker: Checker for website availability
        alert_notifier: Notifier for sending alerts
        browser_detector: Optional detector for checking if URL is open in browser
    """
    session: MonitoringSession
    http_checker: IHttpChecker
    alert_notifier: IAlertNotifier
    browser_detector: Optional[IBrowserDetector] = None

    def execute(self) -> None:
        """
        Check all websites and send alerts for sites open in browser.

        This is called periodically by the scheduler. For each website:
        1. Check if it's online (HTTP reachable)
        2. Check if it's open in a browser tab (if browser detector available)
        3. Update the website status
        4. If online AND open in browser, send an alert
        5. If went offline or browser closed, clear alerts
        """
        if not self.session.is_active:
            return

        check_time = datetime.now()

        for website in self.session.get_all_websites():
            # Check website HTTP status
            is_http_online = self.http_checker.check_website(website.url)

            # Check if open in browser (if detector available)
            is_open_in_browser = True  # Default to True if no detector
            if self.browser_detector:
                is_open_in_browser = self.browser_detector.is_url_open_in_browser(website.url)

            # Website is considered "online" only if BOTH conditions are met:
            # 1. HTTP is reachable
            # 2. Open in browser (or no browser detection)
            is_online = is_http_online and is_open_in_browser
            was_online = website.is_online

            # Update status
            status_changed = website.update_status(is_online, check_time)

            if is_online:
                # Website is online AND open in browser - send continuous alerts
                self.alert_notifier.send_alert(website.url)
            elif was_online and not is_online:
                # Website went offline OR browser tab closed - clear alerts
                self.alert_notifier.clear_alerts(website.url)
