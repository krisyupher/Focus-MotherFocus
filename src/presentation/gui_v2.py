"""Unified Monitor GUI - Single interface for website + application monitoring"""
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
from typing import Optional
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.use_cases.add_target import AddTargetUseCase
from src.application.use_cases.remove_target import RemoveTargetUseCase
from src.application.use_cases.check_targets import CheckTargetsUseCase
from src.application.use_cases.start_monitoring import StartMonitoringUseCase
from src.application.use_cases.stop_monitoring import StopMonitoringUseCase
from src.infrastructure.adapters.windows_startup_manager import WindowsStartupManager


class UnifiedMonitorGUI:
    """
    Unified GUI for monitoring targets (websites, applications, or both).

    Features:
    - Add targets with optional website URL and/or process name
    - Single list showing all targets
    - Visual indicators for target type (web, app, hybrid)
    - Start/stop monitoring
    - Auto-startup configuration
    """

    def __init__(
        self,
        session: MonitoringSessionV2,
        add_target_use_case: AddTargetUseCase,
        remove_target_use_case: RemoveTargetUseCase,
        start_monitoring_use_case: StartMonitoringUseCase,
        stop_monitoring_use_case: StopMonitoringUseCase,
        check_targets_use_case: CheckTargetsUseCase,
        startup_manager: WindowsStartupManager,
        root: tk.Tk
    ):
        self._session = session
        self._add_target = add_target_use_case
        self._remove_target = remove_target_use_case
        self._start_monitoring = start_monitoring_use_case
        self._stop_monitoring = stop_monitoring_use_case
        self._check_targets = check_targets_use_case
        self._startup_manager = startup_manager
        self._root = root

        self._setup_window()
        self._create_widgets()
        self._refresh_target_list()

    def _setup_window(self):
        """Configure main window."""
        self._root.title("Focus Monitor - Unified")
        self._root.geometry("700x600")
        self._root.minsize(600, 500)

    def _create_widgets(self):
        """Create all GUI widgets."""
        # Main container
        main_frame = ttk.Frame(self._root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        self._root.columnconfigure(0, weight=1)
        self._root.rowconfigure(0, weight=1)

        # Title
        title = ttk.Label(
            main_frame,
            text="🎯 Focus Monitor - Unified Tracking",
            font=("Segoe UI", 16, "bold")
        )
        title.grid(row=0, column=0, columnspan=2, pady=(0, 20))

        # Add Target Section
        self._create_add_target_section(main_frame)

        # Target List Section
        self._create_target_list_section(main_frame)

        # Control Section
        self._create_control_section(main_frame)

        # Status bar
        self._status_label = ttk.Label(main_frame, text="Ready", relief=tk.SUNKEN)
        self._status_label.grid(row=4, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(10, 0))

    def _create_add_target_section(self, parent):
        """Create the add target input section - SIMPLE VERSION."""
        frame = ttk.LabelFrame(parent, text="Add Target (auto-detects website + app)", padding="10")
        frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))

        # Simple single input
        input_frame = ttk.Frame(frame)
        input_frame.pack(fill=tk.X)

        ttk.Label(input_frame, text="Target Name:", font=("Segoe UI", 10)).pack(side=tk.LEFT, padx=(0, 10))

        self._name_entry = ttk.Entry(input_frame, width=40, font=("Segoe UI", 11))
        self._name_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 10))
        self._name_entry.bind('<Return>', lambda e: self._on_add_target())
        self._name_entry.focus()

        add_btn = ttk.Button(input_frame, text="➕ Add", command=self._on_add_target)
        add_btn.pack(side=tk.LEFT)

        # Examples label
        examples = ttk.Label(
            frame,
            text='Examples: "Netflix", "Spotify", "Google", "Slack", "Steam", "Calculator"',
            foreground="#666666",
            font=("Segoe UI", 9)
        )
        examples.pack(pady=(10, 0))

    def _create_target_list_section(self, parent):
        """Create the target list display section."""
        frame = ttk.LabelFrame(parent, text="Monitoring Targets", padding="10")
        frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E, tk.N, tk.S), pady=(0, 10))
        parent.rowconfigure(2, weight=1)

        # Create text widget with scrollbar
        text_frame = ttk.Frame(frame)
        text_frame.pack(fill=tk.BOTH, expand=True)

        scrollbar = ttk.Scrollbar(text_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self._target_text = tk.Text(
            text_frame,
            height=15,
            wrap=tk.WORD,
            yscrollcommand=scrollbar.set,
            font=("Consolas", 10),
            state=tk.DISABLED
        )
        self._target_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self._target_text.yview)

        # Configure text tags for styling
        self._target_text.tag_config("name", font=("Segoe UI", 11, "bold"))
        self._target_text.tag_config("website", foreground="#0066cc")
        self._target_text.tag_config("app", foreground="#00aa00")
        self._target_text.tag_config("alerting", foreground="#ff0000", font=("Segoe UI", 10, "bold"))
        self._target_text.tag_config("inactive", foreground="#666666")

        # Buttons
        btn_frame = ttk.Frame(frame)
        btn_frame.pack(fill=tk.X, pady=(10, 0))

        ttk.Button(btn_frame, text="🗑️ Remove Selected", command=self._on_remove_target).pack(side=tk.LEFT, padx=(0, 5))
        ttk.Button(btn_frame, text="🔄 Refresh", command=self._refresh_target_list).pack(side=tk.LEFT)

    def _create_control_section(self, parent):
        """Create the monitoring control section."""
        frame = ttk.LabelFrame(parent, text="Monitoring Controls", padding="10")
        frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E))

        # Interval setting
        ttk.Label(frame, text="Check Interval (seconds):").pack(side=tk.LEFT, padx=(0, 5))

        self._interval_var = tk.StringVar(value=str(self._session.monitoring_interval))
        interval_entry = ttk.Entry(frame, textvariable=self._interval_var, width=10)
        interval_entry.pack(side=tk.LEFT, padx=(0, 10))

        ttk.Button(frame, text="Set", command=self._on_set_interval).pack(side=tk.LEFT, padx=(0, 20))

        # Start/Stop buttons
        self._start_btn = ttk.Button(
            frame,
            text="▶️ START MONITORING",
            command=self._on_start_monitoring,
            style="Accent.TButton"
        )
        self._start_btn.pack(side=tk.LEFT, padx=(0, 5))

        self._stop_btn = ttk.Button(
            frame,
            text="⏹️ STOP MONITORING",
            command=self._on_stop_monitoring,
            state=tk.DISABLED
        )
        self._stop_btn.pack(side=tk.LEFT)

        # Auto-startup checkbox
        self._autostart_var = tk.BooleanVar(value=self._startup_manager.is_enabled())
        autostart_check = ttk.Checkbutton(
            frame,
            text="Start on system boot",
            variable=self._autostart_var,
            command=self._on_toggle_autostart
        )
        autostart_check.pack(side=tk.RIGHT)

    def _on_add_target(self):
        """Handle add target button click - AUTO-RESOLVE version."""
        from src.core.services.target_resolver import TargetResolver

        name = self._name_entry.get().strip()
        if not name:
            messagebox.showwarning("Invalid Input", "Please enter a target name")
            return

        # Auto-resolve to URL and process name
        url, process_name, display_name = TargetResolver.resolve(name)

        # Convert to strings for the use case
        url_string = str(url) if url else None
        app_string = str(process_name) if process_name else None

        try:
            self._add_target.execute(display_name, url_string, app_string)
            self._name_entry.delete(0, tk.END)
            self._refresh_target_list()

            # Show what was added
            parts = []
            if url:
                parts.append(f"🌐 {url}")
            if process_name:
                parts.append(f"📱 {process_name}")
            detail = " + ".join(parts) if parts else "configured"

            self._set_status(f"Added: {display_name} ({detail})")
        except ValueError as e:
            messagebox.showerror("Error", str(e))

    def _on_remove_target(self):
        """Handle remove target button click."""
        # For simplicity, remove the first target
        # In a real implementation, track selection
        targets = self._session.get_all_targets()
        if not targets:
            messagebox.showinfo("No Targets", "No targets to remove")
            return

        target = targets[0]
        if messagebox.askyesno("Confirm", f"Remove target '{target.name}'?"):
            try:
                self._remove_target.execute(target.id)
                self._refresh_target_list()
                self._set_status(f"Removed target: {target.name}")
            except ValueError as e:
                messagebox.showerror("Error", str(e))

    def _on_start_monitoring(self):
        """Handle start monitoring button click."""
        try:
            self._start_monitoring.execute(self._check_targets.execute)
            self._start_btn.config(state=tk.DISABLED)
            self._stop_btn.config(state=tk.NORMAL)
            self._set_status("Monitoring started")
        except ValueError as e:
            messagebox.showerror("Error", str(e))

    def _on_stop_monitoring(self):
        """Handle stop monitoring button click."""
        try:
            self._stop_monitoring.execute()
            self._start_btn.config(state=tk.NORMAL)
            self._stop_btn.config(state=tk.DISABLED)
            self._set_status("Monitoring stopped")
            self._refresh_target_list()
        except ValueError as e:
            messagebox.showerror("Error", str(e))

    def _on_set_interval(self):
        """Handle set interval button click."""
        try:
            interval = int(self._interval_var.get())
            self._session.set_monitoring_interval(interval)
            self._set_status(f"Interval set to {interval} seconds")
        except ValueError:
            messagebox.showerror("Error", "Please enter a valid number")

    def _on_toggle_autostart(self):
        """Handle autostart checkbox toggle."""
        if self._autostart_var.get():
            self._startup_manager.enable()
            self._set_status("Auto-startup enabled")
        else:
            self._startup_manager.disable()
            self._set_status("Auto-startup disabled")

    def _refresh_target_list(self):
        """Refresh the target list display."""
        self._target_text.config(state=tk.NORMAL)
        self._target_text.delete(1.0, tk.END)

        targets = self._session.get_all_targets()
        if not targets:
            self._target_text.insert(tk.END, "No targets added yet.\n\n")
            self._target_text.insert(tk.END, "Add a target above to get started!")
        else:
            for i, target in enumerate(targets, 1):
                # Target name
                status_icon = "●" if target.is_alerting else "○"
                self._target_text.insert(tk.END, f"{status_icon} ", "alerting" if target.is_alerting else "inactive")
                self._target_text.insert(tk.END, f"{target.name}\n", "name")

                # Website info
                if target.has_website():
                    self._target_text.insert(tk.END, f"  🌐 {target.url}\n", "website")

                # Application info
                if target.has_application():
                    self._target_text.insert(tk.END, f"  📱 {target.process_name}\n", "app")

                # Status
                if target.is_alerting:
                    self._target_text.insert(tk.END, "  [ALERTING]\n", "alerting")

                self._target_text.insert(tk.END, "\n")

        self._target_text.config(state=tk.DISABLED)

    def _set_status(self, message: str):
        """Update status bar message."""
        self._status_label.config(text=message)

    def run(self):
        """Start the GUI main loop."""
        self._root.mainloop()
