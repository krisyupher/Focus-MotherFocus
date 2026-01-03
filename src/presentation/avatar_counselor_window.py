"""
Avatar Counselor Window - Fullscreen intervention interface.

Displays counselor avatar with user face for behavioral interventions.
Supports voice interaction, negotiation dialogues, and agreement tracking.
"""
import tkinter as tk
from tkinter import font as tkfont
from PIL import Image, ImageTk
import cv2
import numpy as np
from typing import Optional, Callable
from datetime import datetime
import threading
import time

from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent


class AvatarCounselorWindow:
    """
    Fullscreen overlay window for counselor interventions.

    Features:
    - Fullscreen Zordon-style display
    - User face from webcam (optional)
    - Animated avatar with TTS sync
    - Counselor message display
    - User response input
    - Negotiation dialogue support
    """

    def __init__(self, parent: Optional[tk.Tk] = None):
        """
        Initialize avatar counselor window.

        Args:
            parent: Parent tkinter window (optional)
        """
        self.parent = parent
        self.window: Optional[tk.Toplevel] = None
        self.is_showing = False

        # UI components
        self.avatar_canvas: Optional[tk.Canvas] = None
        self.user_face_canvas: Optional[tk.Canvas] = None
        self.message_label: Optional[tk.Label] = None
        self.input_frame: Optional[tk.Frame] = None
        self.response_callback: Optional[Callable] = None

        # Video/image data
        self.current_user_frame: Optional[np.ndarray] = None
        self.current_avatar_frame: Optional[np.ndarray] = None
        self.animation_running = False
        self.animation_thread: Optional[threading.Thread] = None

        # Colors (Zordon-style green theme)
        self.COUNSELOR_GREEN = '#00FF41'
        self.COUNSELOR_DARK_GREEN = '#003B00'
        self.COUNSELOR_BORDER = '#00AA2E'

    def show_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        user_face: Optional[np.ndarray] = None,
        avatar_frame: Optional[np.ndarray] = None,
        on_response: Optional[Callable[[str], None]] = None
    ) -> None:
        """
        Show fullscreen intervention window.

        Args:
            event: Behavioral event that triggered intervention
            recommendation: Intervention recommendation from use case
            user_face: User's face image from webcam (BGR format)
            avatar_frame: Avatar image or video frame (BGR format)
            on_response: Callback when user responds (receives response string)
        """
        if self.is_showing:
            # Update existing window instead of creating new one
            self._update_intervention(event, recommendation, user_face, avatar_frame)
            return

        self.is_showing = True
        self.response_callback = on_response
        self.current_user_frame = user_face
        self.current_avatar_frame = avatar_frame

        # Create fullscreen window
        self._create_window()

        # Build UI
        self._build_ui(event, recommendation)

        # Start animation if needed
        if avatar_frame is not None or user_face is not None:
            self._start_animation()

    def _create_window(self) -> None:
        """Create fullscreen overlay window."""
        if self.parent:
            self.window = tk.Toplevel(self.parent)
        else:
            self.window = tk.Tk()

        self.window.title("FocusMotherFocus Counselor")

        # Fullscreen settings
        self.window.attributes('-fullscreen', True)
        self.window.attributes('-topmost', True)
        self.window.configure(bg=self.COUNSELOR_DARK_GREEN)

        # Bind escape key to close
        self.window.bind('<Escape>', lambda e: self.close())

    def _build_ui(self, event: BehavioralEvent, recommendation: dict) -> None:
        """Build the counselor UI layout."""
        if not self.window:
            return

        # Get screen dimensions
        screen_width = self.window.winfo_screenwidth()
        screen_height = self.window.winfo_screenheight()

        # === TOP SECTION: User Face (if available) ===
        if self.current_user_frame is not None:
            user_face_frame = tk.Frame(
                self.window,
                bg=self.COUNSELOR_DARK_GREEN,
                highlightbackground=self.COUNSELOR_BORDER,
                highlightthickness=3
            )
            user_face_frame.place(
                x=screen_width // 2 - 250,
                y=50,
                width=500,
                height=400
            )

            # User face canvas
            self.user_face_canvas = tk.Canvas(
                user_face_frame,
                bg='black',
                highlightthickness=0
            )
            self.user_face_canvas.pack(fill=tk.BOTH, expand=True)

        # === MIDDLE SECTION: Counselor Message ===
        message_frame = tk.Frame(
            self.window,
            bg=self.COUNSELOR_GREEN,
            highlightbackground=self.COUNSELOR_BORDER,
            highlightthickness=5
        )
        message_y = 480 if self.current_user_frame is not None else 100
        message_frame.place(
            x=100,
            y=message_y,
            width=screen_width - 200,
            height=150
        )

        # Message text
        message_text = recommendation.get('message', 'I need to talk to you about your focus.')

        counselor_font = tkfont.Font(family="Arial", size=24, weight="bold")
        self.message_label = tk.Label(
            message_frame,
            text=message_text,
            font=counselor_font,
            bg=self.COUNSELOR_GREEN,
            fg=self.COUNSELOR_DARK_GREEN,
            wraplength=screen_width - 250,
            justify='center'
        )
        self.message_label.pack(expand=True, pady=20)

        # === BOTTOM SECTION: Avatar (if available) ===
        if self.current_avatar_frame is not None:
            avatar_frame_widget = tk.Frame(
                self.window,
                bg=self.COUNSELOR_DARK_GREEN,
                highlightbackground=self.COUNSELOR_BORDER,
                highlightthickness=3
            )
            avatar_frame_widget.place(
                x=screen_width // 2 - 250,
                y=screen_height - 450,
                width=500,
                height=400
            )

            # Avatar canvas
            self.avatar_canvas = tk.Canvas(
                avatar_frame_widget,
                bg='black',
                highlightthickness=0
            )
            self.avatar_canvas.pack(fill=tk.BOTH, expand=True)

        # === INPUT SECTION: User Response ===
        if recommendation.get('type') in ['negotiate', 'alert']:
            self._build_input_section(recommendation, screen_width, screen_height)

        # === CLOSE BUTTON ===
        close_button = tk.Button(
            self.window,
            text="✕ CLOSE",
            command=self.close,
            font=("Arial", 14, "bold"),
            bg=self.COUNSELOR_GREEN,
            fg=self.COUNSELOR_DARK_GREEN,
            activebackground=self.COUNSELOR_BORDER,
            padx=20,
            pady=10,
            cursor='hand2'
        )
        close_button.place(x=screen_width - 150, y=20)

        # === EVENT INFO (top left corner) ===
        info_text = f"Event: {event.event_type}\nSeverity: {event.severity}\nTime: {event.detected_at.strftime('%H:%M:%S')}"
        info_label = tk.Label(
            self.window,
            text=info_text,
            font=("Arial", 10),
            bg=self.COUNSELOR_DARK_GREEN,
            fg=self.COUNSELOR_GREEN,
            justify='left'
        )
        info_label.place(x=20, y=20)

    def _build_input_section(
        self,
        recommendation: dict,
        screen_width: int,
        screen_height: int
    ) -> None:
        """Build user input section for negotiation."""
        self.input_frame = tk.Frame(
            self.window,
            bg=self.COUNSELOR_GREEN,
            highlightbackground=self.COUNSELOR_BORDER,
            highlightthickness=3
        )
        self.input_frame.place(
            x=screen_width // 2 - 300,
            y=screen_height - 150,
            width=600,
            height=120
        )

        # Prompt
        prompt_label = tk.Label(
            self.input_frame,
            text="Your response:",
            font=("Arial", 16, "bold"),
            bg=self.COUNSELOR_GREEN,
            fg=self.COUNSELOR_DARK_GREEN
        )
        prompt_label.pack(pady=5)

        # Response entry
        response_entry = tk.Entry(
            self.input_frame,
            font=("Arial", 14),
            bg='white',
            fg='black',
            justify='center'
        )
        response_entry.pack(fill=tk.X, padx=20, pady=5)
        response_entry.focus_set()

        # Submit button
        def submit_response():
            response = response_entry.get().strip()
            if response and self.response_callback:
                self.response_callback(response)
            self.close()

        submit_button = tk.Button(
            self.input_frame,
            text="SUBMIT",
            command=submit_response,
            font=("Arial", 12, "bold"),
            bg=self.COUNSELOR_DARK_GREEN,
            fg=self.COUNSELOR_GREEN,
            activebackground=self.COUNSELOR_BORDER,
            padx=30,
            pady=5,
            cursor='hand2'
        )
        submit_button.pack(pady=5)

        # Bind Enter key
        response_entry.bind('<Return>', lambda e: submit_response())

    def _start_animation(self) -> None:
        """Start animation loop for video playback."""
        self.animation_running = True
        self.animation_thread = threading.Thread(target=self._animation_loop, daemon=True)
        self.animation_thread.start()

    def _animation_loop(self) -> None:
        """Animation loop running in background thread."""
        while self.animation_running and self.window:
            try:
                # Update user face
                if self.current_user_frame is not None and self.user_face_canvas:
                    self._update_canvas_image(
                        self.user_face_canvas,
                        self.current_user_frame
                    )

                # Update avatar
                if self.current_avatar_frame is not None and self.avatar_canvas:
                    self._update_canvas_image(
                        self.avatar_canvas,
                        self.current_avatar_frame
                    )

                time.sleep(1/30)  # 30 FPS

            except Exception as e:
                print(f"[Avatar Window] Animation error: {e}")
                break

    def _update_canvas_image(self, canvas: tk.Canvas, frame: np.ndarray) -> None:
        """
        Update canvas with new image frame.

        Args:
            canvas: Tkinter canvas
            frame: Image frame in BGR format (OpenCV)
        """
        if not canvas or frame is None:
            return

        try:
            # Convert BGR to RGB
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # Resize to canvas size
            canvas_width = canvas.winfo_width()
            canvas_height = canvas.winfo_height()

            if canvas_width > 1 and canvas_height > 1:
                resized = cv2.resize(rgb_frame, (canvas_width, canvas_height))

                # Convert to PIL Image
                pil_image = Image.fromarray(resized)
                photo = ImageTk.PhotoImage(pil_image)

                # Update canvas (must be done in main thread)
                if self.window:
                    self.window.after(0, lambda: self._set_canvas_image(canvas, photo))

        except Exception as e:
            print(f"[Avatar Window] Canvas update error: {e}")

    def _set_canvas_image(self, canvas: tk.Canvas, photo: ImageTk.PhotoImage) -> None:
        """Set image on canvas (main thread only)."""
        try:
            canvas.delete('all')
            canvas.create_image(0, 0, anchor=tk.NW, image=photo)
            canvas.image = photo  # Keep reference
        except:
            pass

    def _update_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        user_face: Optional[np.ndarray],
        avatar_frame: Optional[np.ndarray]
    ) -> None:
        """Update existing window with new intervention data."""
        # Update frames
        if user_face is not None:
            self.current_user_frame = user_face
        if avatar_frame is not None:
            self.current_avatar_frame = avatar_frame

        # Update message
        if self.message_label:
            new_message = recommendation.get('message', '')
            self.message_label.config(text=new_message)

    def close(self) -> None:
        """Close the counselor window."""
        self.is_showing = False
        self.animation_running = False

        if self.animation_thread:
            self.animation_thread.join(timeout=1)

        if self.window:
            try:
                self.window.destroy()
            except:
                pass
            self.window = None

        # Clear references
        self.avatar_canvas = None
        self.user_face_canvas = None
        self.message_label = None
        self.input_frame = None
        self.response_callback = None

    def is_visible(self) -> bool:
        """Check if window is currently visible."""
        return self.is_showing and self.window is not None
