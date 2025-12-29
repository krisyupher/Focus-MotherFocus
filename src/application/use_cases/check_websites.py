"""Check Websites Use Case"""
from dataclasses import dataclass
from datetime import datetime
from ...core.entities.monitoring_session import MonitoringSession
from ...core.value_objects.url import URL
from ..interfaces.http_checker import IHttpChecker
from ..interfaces.alert_notifier import IAlertNotifier


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
    """
    session: MonitoringSession
    http_checker: IHttpChecker
    alert_notifier: IAlertNotifier

    def execute(self) -> None:
        """
        Check all websites and send alerts for online sites.

        This is called periodically by the scheduler. For each website:
        1. Check if it's online
        2. Update the website status
        3. If online, send an alert
        4. If went offline, clear alerts
        """
        if not self.session.is_active:
            return

        check_time = datetime.now()

        for website in self.session.get_all_websites():
            # Check website status
            is_online = self.http_checker.check_website(website.url)
            was_online = website.is_online

            # Update status
            status_changed = website.update_status(is_online, check_time)

            if is_online:
                # Website is online - send continuous alerts
                self.alert_notifier.send_alert(website.url)
            elif was_online and not is_online:
                # Website just went offline - clear alerts
                self.alert_notifier.clear_alerts(website.url)
