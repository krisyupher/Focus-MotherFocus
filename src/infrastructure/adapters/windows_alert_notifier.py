"""Windows Alert Notifier Adapter"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional
import tkinter as tk
import winsound
from ...application.interfaces.alert_notifier import IAlertNotifier
from ...core.value_objects.url import URL


@dataclass
class WindowsAlertNotifier(IAlertNotifier):
    """
    Alert notifier implementation using Windows notifications.

    This is an infrastructure adapter that implements the IAlertNotifier
    interface using tkinter pop-ups and winsound alerts for Windows.

    Attributes:
        parent_window: Parent tkinter window for alerts
        active_popups: Dictionary tracking active popup windows per URL
    """
    parent_window: Optional[tk.Tk] = None
    active_popups: Dict[str, List] = field(default_factory=dict)

    def send_alert(self, url: URL) -> None:
        """
        Send an alert notification for a website.

        Creates a popup window and plays a sound alert.

        Args:
            url: URL of the website that triggered the alert
        """
        # Play sound
        self._play_alert_sound()

        # Show popup (must be done in main thread)
        if self.parent_window:
            try:
                self.parent_window.after(
                    0,
                    lambda: self._show_popup_alert(url)
                )
            except:
                pass

    def clear_alerts(self, url: URL) -> None:
        """
        Clear all active alerts for a specific website.

        Args:
            url: URL of the website to clear alerts for
        """
        url_str = str(url)
        if url_str in self.active_popups:
            for popup in self.active_popups[url_str][:]:
                try:
                    if popup.winfo_exists():
                        popup.destroy()
                except:
                    pass
            self.active_popups[url_str] = []

    def clear_all_alerts(self) -> None:
        """
        Clear all active alerts for all websites.
        """
        for url_str in list(self.active_popups.keys()):
            url = URL.from_string(url_str)
            self.clear_alerts(url)

    def _show_popup_alert(self, url: URL) -> None:
        """
        Display a pop-up alert window.

        Args:
            url: URL to display in alert
        """
        url_str = str(url)

        alert_window = tk.Toplevel(self.parent_window) if self.parent_window else tk.Tk()
        alert_window.title("WEBSITE ONLINE ALERT")
        alert_window.geometry("400x150")
        alert_window.configure(bg='#ff4444')

        # Make window appear on top
        alert_window.attributes('-topmost', True)
        alert_window.lift()
        alert_window.focus_force()

        # Alert message
        timestamp = datetime.now().strftime("%H:%M:%S")
        message = f"ALERT: Website is ONLINE!\n\n{url_str}\n\nDetected at: {timestamp}"

        label = tk.Label(
            alert_window,
            text=message,
            font=('Arial', 12, 'bold'),
            bg='#ff4444',
            fg='white',
            pady=20
        )
        label.pack()

        # Close button
        close_btn = tk.Button(
            alert_window,
            text="ACKNOWLEDGE",
            command=alert_window.destroy,
            font=('Arial', 10, 'bold'),
            bg='white',
            fg='#ff4444',
            padx=20,
            pady=5
        )
        close_btn.pack(pady=10)

        # Track this popup
        if url_str not in self.active_popups:
            self.active_popups[url_str] = []
        self.active_popups[url_str].append(alert_window)

        # Auto-close after 5 seconds and remove from tracking
        def close_and_remove():
            try:
                if alert_window.winfo_exists():
                    alert_window.destroy()
            except:
                pass
            finally:
                if url_str in self.active_popups:
                    try:
                        self.active_popups[url_str].remove(alert_window)
                    except ValueError:
                        pass

        alert_window.after(5000, close_and_remove)

    @staticmethod
    def _play_alert_sound() -> None:
        """
        Play an alert sound using Windows sound API.
        """
        try:
            # Windows system beep (frequency, duration)
            winsound.Beep(1000, 500)  # 1000 Hz for 500ms
        except Exception:
            # Fallback to system bell
            try:
                winsound.MessageBeep(winsound.MB_ICONEXCLAMATION)
            except:
                pass
