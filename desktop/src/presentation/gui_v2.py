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

        # Initialize AI command processor
        self._ai_processor = None
        try:
            from src.infrastructure.adapters.ai_command_processor import AICommandProcessor
            self._ai_processor = AICommandProcessor()
            print("[GUI] AI Command Processor initialized")
        except Exception as e:
            print(f"[GUI] AI Command Processor not available: {e}")

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

        # AI Command Section (if available)
        if self._ai_processor:
            self._create_ai_command_section(main_frame)

        # Target List Section
        self._create_target_list_section(main_frame)

        # Control Section
        self._create_control_section(main_frame)

        # Status bar
        self._status_label = ttk.Label(main_frame, text="Ready", relief=tk.SUNKEN)
        self._status_label.grid(row=4, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(10, 0))

    def _create_ai_command_section(self, parent):
        """Create AI natural language command input section."""
        frame = ttk.LabelFrame(parent, text="🤖 AI Assistant - Type Natural Language Commands", padding="10")
        frame.grid(row=1, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))

        # Input frame
        input_frame = ttk.Frame(frame)
        input_frame.pack(fill=tk.X)

        ttk.Label(input_frame, text="Command:", font=("Segoe UI", 10, "bold")).pack(side=tk.LEFT, padx=(0, 10))

        self._ai_entry = ttk.Entry(input_frame, width=50, font=("Segoe UI", 11))
        self._ai_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 10))
        self._ai_entry.bind('<Return>', lambda e: self._on_ai_command())

        ai_btn = ttk.Button(input_frame, text="🚀 Execute", command=self._on_ai_command)
        ai_btn.pack(side=tk.LEFT)

        # Examples
        examples = ttk.Label(
            frame,
            text='Try: "Monitor Netflix and YouTube" • "Remove Facebook" • "Start monitoring" • "Show my targets"',
            foreground="#0066cc",
            font=("Segoe UI", 9)
        )
        examples.pack(pady=(10, 0))

        # Response label
        self._ai_response_label = ttk.Label(
            frame,
            text="",
            foreground="#006600",
            font=("Segoe UI", 9, "italic"),
            wraplength=600
        )
        self._ai_response_label.pack(pady=(5, 0))

    def _create_add_target_section(self, parent):
        """Create the add target input section - SIMPLE VERSION."""
        frame = ttk.LabelFrame(parent, text="Add Target (auto-detects website + app)", padding="10")
        frame.grid(row=2, column=0, columnspan=2, sticky=(tk.W, tk.E), pady=(0, 10))

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

        ttk.Button(btn_frame, text="🔄 Refresh", command=self._refresh_target_list).pack(side=tk.LEFT)

    def _create_control_section(self, parent):
        """Create the monitoring control section."""
        frame = ttk.LabelFrame(parent, text="Monitoring Controls", padding="10")
        frame.grid(row=3, column=0, columnspan=2, sticky=(tk.W, tk.E))

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


    def _on_ai_command(self):
        """Handle AI natural language command."""
        if not self._ai_processor:
            messagebox.showwarning("AI Not Available", "AI Command Processor is not configured")
            return

        command = self._ai_entry.get().strip()
        if not command:
            return

        # Show processing
        self._ai_response_label.config(text="🤔 Processing...", foreground="#666666")
        self._root.update()

        try:
            # Process command with AI
            response = self._ai_processor.process_command_simple(
                command,
                add_target_fn=self._ai_add_target,
                remove_target_fn=self._ai_remove_target,
                start_monitoring_fn=self._ai_start_monitoring,
                stop_monitoring_fn=self._ai_stop_monitoring,
                list_targets_fn=self._ai_list_targets
            )

            # Show response
            self._ai_response_label.config(text=f"✅ {response}", foreground="#006600")

            # Clear input
            self._ai_entry.delete(0, tk.END)

            # Refresh display
            self._refresh_target_list()

        except Exception as e:
            self._ai_response_label.config(text=f"❌ Error: {str(e)}", foreground="#cc0000")

    def _ai_add_target(self, name: str) -> str:
        """AI callback: Add target."""
        from src.core.services.target_resolver import TargetResolver
        url, process_name, display_name = TargetResolver.resolve(name)

        try:
            self._add_target.execute(
                name=display_name,
                url_str=url.value if url else None,
                process_name_str=process_name.value if process_name else None
            )
            return f"Added {display_name}"
        except Exception as e:
            return f"Failed to add {name}: {str(e)}"

    def _ai_remove_target(self, name: str) -> str:
        """AI callback: Remove target."""
        try:
            self._remove_target.execute(name=name)
            return f"Removed {name}"
        except Exception as e:
            return f"Failed to remove {name}: {str(e)}"

    def _ai_start_monitoring(self) -> str:
        """AI callback: Start monitoring."""
        try:
            self._start_monitoring.execute(
                check_callback=self._check_targets.execute,
                interval_seconds=1
            )
            return "Monitoring started"
        except Exception as e:
            return f"Failed to start: {str(e)}"

    def _ai_stop_monitoring(self) -> str:
        """AI callback: Stop monitoring."""
        try:
            self._stop_monitoring.execute()
            return "Monitoring stopped"
        except Exception as e:
            return f"Failed to stop: {str(e)}"

    def _ai_list_targets(self) -> str:
        """AI callback: List targets."""
        targets = self._session.get_all_targets()
        if not targets:
            return "No targets"
        return f"Monitoring {len(targets)} targets: {', '.join(t.name for t in targets)}"

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
