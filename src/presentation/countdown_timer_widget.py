"""
Countdown Timer Widget - Visual countdown display for agreements.

Shows remaining time for active agreements in a non-intrusive overlay.
"""
import tkinter as tk
from tkinter import font as tkfont
from typing import Optional, Callable
from datetime import datetime

from src.core.entities.agreement import Agreement


class CountdownTimerWidget:
    """
    Visual countdown timer for agreements.

    Features:
    - Small overlay window (bottom-right corner)
    - Shows time remaining
    - Color-coded by urgency (green → yellow → red)
    - Click to extend/dismiss
    - Auto-hides when time up
    """

    def __init__(self, parent: Optional[tk.Tk] = None):
        """
        Initialize countdown timer widget.

        Args:
            parent: Parent tkinter window (optional)
        """
        self.parent = parent
        self.window: Optional[tk.Toplevel] = None
        self.is_showing = False
        self.current_agreement: Optional[Agreement] = None

        # UI components
        self.time_label: Optional[tk.Label] = None
        self.message_label: Optional[tk.Label] = None
        self.update_timer_id: Optional[str] = None

        # Callbacks
        self.on_extend_request: Optional[Callable[[Agreement], None]] = None
        self.on_dismiss: Optional[Callable[[Agreement], None]] = None

        # Colors
        self.COLOR_SAFE = '#00FF41'  # Green - plenty of time
        self.COLOR_WARNING = '#FFD700'  # Yellow - running low
        self.COLOR_CRITICAL = '#FF4444'  # Red - almost up
        self.BG_COLOR = '#1a1a1a'  # Dark background

    def show(
        self,
        agreement: Agreement,
        on_extend: Optional[Callable[[Agreement], None]] = None,
        on_dismiss: Optional[Callable[[Agreement], None]] = None
    ) -> None:
        """
        Show countdown timer for agreement.

        Args:
            agreement: Agreement to show timer for
            on_extend: Callback when user wants to extend
            on_dismiss: Callback when user dismisses
        """
        self.current_agreement = agreement
        self.on_extend_request = on_extend
        self.on_dismiss = on_dismiss

        if not self.is_showing:
            self._create_window()

        self._update_display()
        self.is_showing = True

    def _create_window(self) -> None:
        """Create the timer overlay window."""
        if self.parent:
            self.window = tk.Toplevel(self.parent)
        else:
            self.window = tk.Tk()

        # Window settings
        self.window.title("Agreement Timer")
        self.window.configure(bg=self.BG_COLOR)
        self.window.attributes('-topmost', True)

        # Position in bottom-right corner
        window_width = 300
        window_height = 120
        screen_width = self.window.winfo_screenwidth()
        screen_height = self.window.winfo_screenheight()
        x = screen_width - window_width - 20
        y = screen_height - window_height - 60  # Above taskbar

        self.window.geometry(f"{window_width}x{window_height}+{x}+{y}")

        # Remove window decorations for sleek look
        self.window.overrideredirect(True)

        # Main frame
        main_frame = tk.Frame(
            self.window,
            bg=self.BG_COLOR,
            highlightbackground=self.COLOR_SAFE,
            highlightthickness=2
        )
        main_frame.pack(fill=tk.BOTH, expand=True, padx=2, pady=2)

        # Title
        title_label = tk.Label(
            main_frame,
            text="⏰ AGREEMENT TIMER",
            font=("Arial", 10, "bold"),
            bg=self.BG_COLOR,
            fg=self.COLOR_SAFE
        )
        title_label.pack(pady=(5, 0))

        # Time display
        time_font = tkfont.Font(family="Courier", size=24, weight="bold")
        self.time_label = tk.Label(
            main_frame,
            text="00:00",
            font=time_font,
            bg=self.BG_COLOR,
            fg=self.COLOR_SAFE
        )
        self.time_label.pack(pady=5)

        # Message
        self.message_label = tk.Label(
            main_frame,
            text="",
            font=("Arial", 9),
            bg=self.BG_COLOR,
            fg='white'
        )
        self.message_label.pack(pady=2)

        # Buttons frame
        button_frame = tk.Frame(main_frame, bg=self.BG_COLOR)
        button_frame.pack(pady=5)

        # Extend button
        extend_btn = tk.Button(
            button_frame,
            text="⏱ Extend",
            command=self._on_extend_clicked,
            font=("Arial", 8),
            bg='#333',
            fg='white',
            activebackground='#555',
            padx=10,
            pady=2,
            cursor='hand2',
            borderwidth=0
        )
        extend_btn.pack(side=tk.LEFT, padx=5)

        # Dismiss button
        dismiss_btn = tk.Button(
            button_frame,
            text="✓ OK",
            command=self._on_dismiss_clicked,
            font=("Arial", 8),
            bg='#333',
            fg='white',
            activebackground='#555',
            padx=10,
            pady=2,
            cursor='hand2',
            borderwidth=0
        )
        dismiss_btn.pack(side=tk.LEFT, padx=5)

    def _update_display(self) -> None:
        """Update the timer display."""
        if not self.window or not self.current_agreement:
            return

        # Calculate time remaining
        remaining_seconds = self.current_agreement.time_remaining_minutes() * 60

        if remaining_seconds <= 0:
            # Time's up!
            self.time_label.config(text="TIME'S UP!", fg=self.COLOR_CRITICAL)
            self.message_label.config(text="Agreement expired")
            self.window.configure(highlightbackground=self.COLOR_CRITICAL)

            # Cancel update timer
            if self.update_timer_id:
                self.window.after_cancel(self.update_timer_id)
                self.update_timer_id = None

            return

        # Format time
        minutes = int(remaining_seconds // 60)
        seconds = int(remaining_seconds % 60)
        time_str = f"{minutes:02d}:{seconds:02d}"

        # Determine color based on time remaining
        if remaining_seconds > 120:  # > 2 minutes
            color = self.COLOR_SAFE
            urgency = "Good time"
        elif remaining_seconds > 60:  # > 1 minute
            color = self.COLOR_WARNING
            urgency = "Running low"
        else:  # < 1 minute
            color = self.COLOR_CRITICAL
            urgency = "Almost up!"

        # Update display
        self.time_label.config(text=time_str, fg=color)
        self.message_label.config(text=urgency)

        # Update window border color
        main_frame = self.window.winfo_children()[0]
        main_frame.config(highlightbackground=color)

        # Schedule next update (every second)
        self.update_timer_id = self.window.after(1000, self._update_display)

    def _on_extend_clicked(self) -> None:
        """Handle extend button click."""
        if self.on_extend_request and self.current_agreement:
            self.on_extend_request(self.current_agreement)

    def _on_dismiss_clicked(self) -> None:
        """Handle dismiss button click."""
        if self.on_dismiss and self.current_agreement:
            self.on_dismiss(self.current_agreement)
        self.hide()

    def hide(self) -> None:
        """Hide the timer widget."""
        if self.update_timer_id and self.window:
            self.window.after_cancel(self.update_timer_id)
            self.update_timer_id = None

        if self.window:
            try:
                self.window.destroy()
            except:
                pass
            self.window = None

        self.is_showing = False
        self.current_agreement = None

    def update_agreement(self, agreement: Agreement) -> None:
        """
        Update the displayed agreement.

        Args:
            agreement: Updated agreement
        """
        self.current_agreement = agreement
        if self.is_showing:
            self._update_display()

    def is_visible(self) -> bool:
        """Check if timer is currently visible."""
        return self.is_showing and self.window is not None
