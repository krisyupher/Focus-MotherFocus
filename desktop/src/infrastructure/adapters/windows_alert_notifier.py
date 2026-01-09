"""Windows Alert Notifier Adapter"""
from dataclasses import dataclass, field
from datetime import datetime
from typing import Dict, List, Optional
import tkinter as tk
from tkinter import font as tkfont
import winsound
import random
from ...application.interfaces.alert_notifier import IAlertNotifier
from ...core.value_objects.url import URL
from .camera_manager import CameraManager
from .avatar_animator import AvatarAnimator


# Motivational messages for alerts (Zordon-style)
MOTIVATIONAL_MESSAGES = [
    "Don't waste your time, come back to your work!",
    "Focus on your mission, productivity awaits!",
    "The path to success requires discipline!",
    "Your goals won't achieve themselves!",
    "Time to return to productivity, champion!",
    "Remember why you started this journey!",
    "Every second counts towards your success!",
    "Your future self will thank you for focusing now!",
    "Discipline today creates freedom tomorrow!",
    "Stop procrastinating, start accomplishing!",
    "Your dreams require action, not distraction!",
    "Winners focus on what matters!",
    "Greatness demands your full attention!",
    "The work won't do itself!",
    "Choose progress over procrastination!",
]


@dataclass
class WindowsAlertNotifier(IAlertNotifier):
    """
    Alert notifier implementation using Windows notifications.

    This is an infrastructure adapter that implements the IAlertNotifier
    interface using tkinter pop-ups and winsound alerts for Windows.

    Features retro Zordon-style alerts with animated avatar and TTS.

    Attributes:
        parent_window: Parent tkinter window for alerts
        active_popups: Dictionary tracking active popup windows per URL
        camera_manager: Singleton camera manager for webcam access (fallback)
        avatar_animator: Optional avatar animator for speaking alerts
        _alert_states: Dictionary tracking window state (window, widgets, TTS) per target
    """
    parent_window: Optional[tk.Tk] = None
    active_popups: Dict[str, List] = field(default_factory=dict)
    camera_manager: CameraManager = field(default_factory=CameraManager)
    avatar_animator: Optional[AvatarAnimator] = None
    _alert_states: Dict[str, dict] = field(default_factory=dict)

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

    def notify(self, name: str) -> None:
        """
        Send an alert notification for a target (V2 unified interface).

        Args:
            name: Name of the target that triggered the alert
        """
        # Play sound
        self._play_alert_sound()

        # Show popup (must be done in main thread)
        if self.parent_window:
            try:
                self.parent_window.after(
                    0,
                    lambda: self._show_popup_alert_v2(name)
                )
            except:
                pass

    def clear(self, name: str) -> None:
        """
        Clear all active alerts for a specific target (V2 unified interface).

        Args:
            name: Name of the target to clear alerts for
        """
        if name in self.active_popups:
            for popup in self.active_popups[name][:]:
                try:
                    if popup.winfo_exists():
                        popup.destroy()
                except:
                    pass
            self.active_popups[name] = []

    def _show_popup_alert_v2(self, name: str) -> None:
        """
        Display a ZORDON-STYLE retro alert with ANIMATED AVATAR and TTS.

        If window already exists for this target, UPDATE the message and re-focus.
        Otherwise, create a new window.

        Args:
            name: Target name to display in alert
        """
        # CHECK IF WINDOW ALREADY EXISTS for this target
        if name in self._alert_states:
            alert_state = self._alert_states[name]
            alert_window = alert_state.get('window')

            # Verify window still exists
            try:
                if alert_window and alert_window.winfo_exists():
                    # WINDOW EXISTS - UPDATE IT (silently, no console spam)

                    # Get new random message
                    message = random.choice(MOTIVATIONAL_MESSAGES)

                    # Update message label
                    message_label = alert_state.get('message_label')
                    if message_label:
                        message_label.configure(text=message)

                    # Update timestamp
                    time_label = alert_state.get('time_label')
                    if time_label:
                        timestamp = datetime.now().strftime("%H:%M:%S")
                        time_label.configure(text=f"TIME: {timestamp}")

                    # Stop and cleanup old TTS if still speaking
                    old_tts = alert_state.get('tts')
                    if old_tts:
                        try:
                            old_tts.stop()
                            # Give engine time to cleanup
                            import time
                            time.sleep(0.1)
                        except:
                            pass

                    # Get avatar and create callbacks
                    animated_avatar = self.camera_manager._animated_avatar
                    if animated_avatar:
                        def on_update_start():
                            animated_avatar.start_speaking()

                        def on_update_complete():
                            animated_avatar.stop_speaking()

                        # Reuse same TTS instance and speak new message with avatar callbacks
                        try:
                            old_tts.speak(
                                message,
                                on_start=on_update_start,
                                on_complete=on_update_complete,
                                blocking=False
                            )
                        except Exception as e:
                            pass  # Silently ignore TTS errors
                    else:
                        # No avatar, just speak
                        try:
                            old_tts.speak(message, blocking=False)
                        except Exception as e:
                            pass  # Silently ignore TTS errors

                    # RE-FOCUS window to steal attention
                    alert_window.lift()
                    alert_window.focus_force()
                    try:
                        alert_window.grab_set()
                        alert_window.grab_set_global()
                    except:
                        pass

                    return  # DONE - window updated, don't create new one
                else:
                    # Window was destroyed, remove from tracking
                    del self._alert_states[name]
            except:
                # Error checking window, remove from tracking
                if name in self._alert_states:
                    del self._alert_states[name]

        # NO EXISTING WINDOW - CREATE NEW ONE
        print(f"[ALERT] Creating new alert window for {name}")

        alert_window = tk.Toplevel(self.parent_window) if self.parent_window else tk.Tk()
        alert_window.title("⚡ ZORDON ALERT SYSTEM ⚡")
        alert_window.geometry("600x500")
        alert_window.resizable(False, False)

        # Random motivational message
        message = random.choice(MOTIVATIONAL_MESSAGES)

        # Create animated avatar for this alert
        from src.infrastructure.adapters.animated_avatar import AnimatedAvatar
        animated_avatar = AnimatedAvatar()
        self.camera_manager.set_animated_avatar(animated_avatar)

        # Start camera for LIVE feed on each alert
        self.camera_manager.start_camera()

        # Background label (will be updated with LIVE camera frames)
        bg_label = tk.Label(alert_window)
        bg_label.place(x=0, y=0, relwidth=1, relheight=1)

        # Animation control
        animation_running = {'value': True}
        is_currently_speaking = {'value': False}

        # TTS control - create NEW TTS instance for EACH alert
        from src.infrastructure.adapters.windows_tts_service import WindowsTTSService
        alert_tts = WindowsTTSService()

        # Callbacks for TTS to control avatar
        def on_tts_start():
            is_currently_speaking['value'] = True
            animated_avatar.start_speaking()

        def on_tts_complete():
            is_currently_speaking['value'] = False
            animated_avatar.stop_speaking()

        def update_live_camera_frame():
            """Update with LIVE camera frame (15 FPS)."""
            if not animation_running['value']:
                return

            # Check if window still exists before updating
            try:
                if not alert_window.winfo_exists():
                    animation_running['value'] = False
                    self.camera_manager.stop_camera()
                    return
            except:
                animation_running['value'] = False
                self.camera_manager.stop_camera()
                return

            try:
                # Get LIVE camera frame with animated avatar
                camera_bg = self.camera_manager.get_fullscreen_background_for_tk(
                    width=600,
                    height=500,
                    is_speaking=is_currently_speaking['value']
                )
                if camera_bg:
                    bg_label.configure(image=camera_bg)
                    bg_label.image = camera_bg  # Keep reference

                # Schedule next frame (15 FPS = 67ms)
                if animation_running['value'] and alert_window.winfo_exists():
                    alert_window.after(67, update_live_camera_frame)

            except tk.TclError:
                # Window was destroyed during update - stop camera silently
                animation_running['value'] = False
                self.camera_manager.stop_camera()
            except Exception as e:
                print(f"[ALERT] Unexpected error: {e}")
                animation_running['value'] = False
                self.camera_manager.stop_camera()

        # Start TTS for THIS alert (new instance each time)
        try:
            alert_tts.speak(
                message,
                on_start=on_tts_start,
                on_complete=on_tts_complete,
                blocking=False
            )
        except Exception as e:
            pass  # Silently ignore TTS errors

        # Start LIVE camera animation loop
        update_live_camera_frame()

        # FORCE FOCUS - Steal attention from current application
        alert_window.attributes('-topmost', True)
        alert_window.attributes('-toolwindow', False)
        alert_window.lift()
        alert_window.focus_force()
        alert_window.grab_set()

        try:
            alert_window.focus()
            alert_window.grab_set_global()
        except:
            pass

        # Timestamp
        timestamp = datetime.now().strftime("%H:%M:%S")

        # Message in the CENTER - OVERLAY on camera (your face!)
        message_frame = tk.Frame(alert_window, bg='#000000', highlightbackground='#00ff00',
                                highlightthickness=4)
        message_frame.place(relx=0.5, rely=0.5, anchor='center', width=520)

        message_label = tk.Label(
            message_frame,
            text=message,
            font=('Courier New', 16, 'bold'),
            bg='#000000',
            fg='#00ff00',
            wraplength=480,
            justify=tk.CENTER,
            pady=25,
            padx=20
        )
        message_label.pack()

        # Info bar at bottom - OVERLAY on camera
        info_frame = tk.Frame(alert_window, bg='#000000', highlightbackground='#ff0000',
                             highlightthickness=3)
        info_frame.place(relx=0.5, rely=0.75, anchor='center', width=450)

        target_label = tk.Label(
            info_frame,
            text=f"⚠️ TARGET: {name} ⚠️",
            font=('Courier New', 14, 'bold'),
            bg='#000000',
            fg='#ff0000',
            pady=5
        )
        target_label.pack()

        time_label = tk.Label(
            info_frame,
            text=f"TIME: {timestamp}",
            font=('Courier New', 11),
            bg='#000000',
            fg='#00ff00',
            pady=3
        )
        time_label.pack()

        # Close button - stop animation, camera, and TTS, then destroy
        def close_alert():
            # Stop animation loop
            animation_running['value'] = False

            # Stop camera
            try:
                self.camera_manager.stop_camera()
            except:
                pass

            # Stop TTS
            try:
                alert_tts.stop()
            except:
                pass

            # Remove from alert states
            if name in self._alert_states:
                del self._alert_states[name]

            # Release grab and destroy
            try:
                alert_window.grab_release()
            except:
                pass
            alert_window.destroy()

        # Button at bottom - OVERLAY on camera
        button_frame = tk.Frame(alert_window, bg='#000000')
        button_frame.place(relx=0.5, rely=0.92, anchor='center')

        close_btn = tk.Button(
            button_frame,
            text="⚡ ACKNOWLEDGE & RETURN TO WORK ⚡",
            command=close_alert,
            font=('Courier New', 13, 'bold'),
            bg='#00ff00',
            fg='#000000',
            activebackground='#00cc00',
            activeforeground='#000000',
            padx=25,
            pady=12,
            relief=tk.RAISED,
            bd=5,
            cursor='hand2'
        )
        close_btn.pack()

        # Track this popup (for backward compatibility)
        if name not in self.active_popups:
            self.active_popups[name] = []
        self.active_popups[name].append(alert_window)

        # STORE WINDOW STATE for updates
        self._alert_states[name] = {
            'window': alert_window,
            'message_label': message_label,
            'time_label': time_label,
            'tts': alert_tts,
            'animation_running': animation_running
        }

        # Cleanup handler when window is closed manually
        def on_window_close():
            # Stop animation
            animation_running['value'] = False

            # Stop camera
            try:
                self.camera_manager.stop_camera()
            except:
                pass

            # Stop TTS
            try:
                alert_tts.stop()
            except:
                pass

            # Release grab
            try:
                alert_window.grab_release()
            except:
                pass

            # Remove from tracking
            if name in self.active_popups:
                try:
                    self.active_popups[name].remove(alert_window)
                except ValueError:
                    pass

            # Remove from alert states
            if name in self._alert_states:
                del self._alert_states[name]

            # Destroy window
            try:
                alert_window.destroy()
            except:
                pass

        # Bind close event
        alert_window.protocol("WM_DELETE_WINDOW", on_window_close)

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
