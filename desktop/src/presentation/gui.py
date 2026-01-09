"""Website Monitor GUI - Clean Architecture Version"""
import tkinter as tk
from tkinter import messagebox, simpledialog
from dataclasses import dataclass
from typing import Optional
from ..application.use_cases import (
    AddWebsiteUseCase,
    RemoveWebsiteUseCase,
    StartMonitoringUseCase,
    StopMonitoringUseCase,
    CheckWebsitesUseCase
)
from ..application.interfaces.startup_manager import IStartupManager
from ..core.entities.monitoring_session import MonitoringSession


@dataclass
class WebsiteMonitorGUI:
    """
    Main GUI application for website monitoring.

    This is the presentation layer that depends on use cases via dependency injection.
    All business logic is delegated to use cases, keeping the GUI thin.

    Attributes:
        session: The monitoring session
        add_website_use_case: Use case for adding websites
        remove_website_use_case: Use case for removing websites
        start_monitoring_use_case: Use case for starting monitoring
        stop_monitoring_use_case: Use case for stopping monitoring
        check_websites_use_case: Use case for checking websites
        startup_manager: Optional manager for auto-startup configuration
        root: Optional tkinter root window (created if not provided)
    """
    session: MonitoringSession
    add_website_use_case: AddWebsiteUseCase
    remove_website_use_case: RemoveWebsiteUseCase
    start_monitoring_use_case: StartMonitoringUseCase
    stop_monitoring_use_case: StopMonitoringUseCase
    check_websites_use_case: CheckWebsitesUseCase
    startup_manager: Optional[IStartupManager] = None
    root: Optional[tk.Tk] = None

    def __post_init__(self):
        """Initialize the GUI after dataclass initialization"""
        if self.root is None:
            self.root = tk.Tk()

        self.root.title("Focus MotherFocus - Website Monitor")
        self.root.geometry("700x550")

        # GUI components (will be initialized in setup)
        self.website_listbox: Optional[tk.Listbox] = None
        self.interval_var: Optional[tk.StringVar] = None
        self.start_btn: Optional[tk.Button] = None
        self.stop_btn: Optional[tk.Button] = None
        self.status_label: Optional[tk.Label] = None

        # Setup GUI
        self._setup_gui()

        # Load saved websites
        self._load_saved_websites()

        # Handle window close
        self.root.protocol("WM_DELETE_WINDOW", self._on_closing)

    def _setup_gui(self):
        """Setup the GUI components"""
        # Title
        title_frame = tk.Frame(self.root, bg='#2c3e50', pady=10)
        title_frame.pack(fill=tk.X)

        title_label = tk.Label(
            title_frame,
            text="Website Monitor & Alert System",
            font=('Arial', 16, 'bold'),
            bg='#2c3e50',
            fg='white'
        )
        title_label.pack()

        # Website list management frame
        list_frame = tk.LabelFrame(
            self.root,
            text="Monitored Websites",
            font=('Arial', 11, 'bold'),
            padx=10,
            pady=10
        )
        list_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # Listbox with scrollbar
        list_container = tk.Frame(list_frame)
        list_container.pack(fill=tk.BOTH, expand=True)

        scrollbar = tk.Scrollbar(list_container)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.website_listbox = tk.Listbox(
            list_container,
            yscrollcommand=scrollbar.set,
            font=('Consolas', 10),
            height=10
        )
        self.website_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.website_listbox.yview)

        # Website management buttons
        btn_frame = tk.Frame(list_frame)
        btn_frame.pack(fill=tk.X, pady=(10, 0))

        tk.Button(
            btn_frame,
            text="Add Website",
            command=self._add_website,
            bg='#27ae60',
            fg='white',
            font=('Arial', 10, 'bold'),
            padx=10,
            pady=5
        ).pack(side=tk.LEFT, padx=5)

        tk.Button(
            btn_frame,
            text="Remove Selected",
            command=self._remove_website,
            bg='#e74c3c',
            fg='white',
            font=('Arial', 10, 'bold'),
            padx=10,
            pady=5
        ).pack(side=tk.LEFT, padx=5)

        # Monitoring controls frame
        control_frame = tk.LabelFrame(
            self.root,
            text="Monitoring Controls",
            font=('Arial', 11, 'bold'),
            padx=10,
            pady=10
        )
        control_frame.pack(fill=tk.X, padx=10, pady=10)

        # Interval setting
        interval_frame = tk.Frame(control_frame)
        interval_frame.pack(fill=tk.X, pady=(0, 10))

        tk.Label(
            interval_frame,
            text="Check Interval (seconds):",
            font=('Arial', 10)
        ).pack(side=tk.LEFT, padx=5)

        self.interval_var = tk.StringVar(value=str(self.session.monitoring_interval))
        interval_entry = tk.Entry(
            interval_frame,
            textvariable=self.interval_var,
            width=10,
            font=('Arial', 10)
        )
        interval_entry.pack(side=tk.LEFT, padx=5)

        tk.Button(
            interval_frame,
            text="Set Interval",
            command=self._set_interval,
            bg='#3498db',
            fg='white',
            font=('Arial', 9),
            padx=10,
            pady=2
        ).pack(side=tk.LEFT, padx=5)

        # Start/Stop buttons
        control_btn_frame = tk.Frame(control_frame)
        control_btn_frame.pack()

        self.start_btn = tk.Button(
            control_btn_frame,
            text="START MONITORING",
            command=self._start_monitoring,
            bg='#27ae60',
            fg='white',
            font=('Arial', 12, 'bold'),
            padx=20,
            pady=10,
            state=tk.NORMAL
        )
        self.start_btn.pack(side=tk.LEFT, padx=10)

        self.stop_btn = tk.Button(
            control_btn_frame,
            text="STOP MONITORING",
            command=self._stop_monitoring,
            bg='#e74c3c',
            fg='white',
            font=('Arial', 12, 'bold'),
            padx=20,
            pady=10,
            state=tk.DISABLED
        )
        self.stop_btn.pack(side=tk.LEFT, padx=10)

        # Auto-startup checkbox (if startup manager available)
        if self.startup_manager:
            startup_frame = tk.Frame(control_frame)
            startup_frame.pack(pady=(10, 0))

            self.startup_var = tk.BooleanVar(value=self.startup_manager.is_enabled())
            startup_check = tk.Checkbutton(
                startup_frame,
                text="Start automatically when computer turns on",
                variable=self.startup_var,
                command=self._toggle_startup,
                font=('Arial', 10)
            )
            startup_check.pack()

        # Status bar
        self.status_label = tk.Label(
            self.root,
            text="Status: Ready",
            bg='#34495e',
            fg='white',
            font=('Arial', 9),
            anchor=tk.W,
            padx=10,
            pady=5
        )
        self.status_label.pack(fill=tk.X, side=tk.BOTTOM)

    def _load_saved_websites(self):
        """Load saved websites from session into the listbox"""
        for website in self.session.get_all_websites():
            self.website_listbox.insert(tk.END, str(website.url))

        if self.session.get_website_count() > 0:
            self._update_status(f"Loaded {self.session.get_website_count()} saved website(s)")

    def _add_website(self):
        """Add a new website to the monitoring list"""
        url = simpledialog.askstring("Add Website", "Enter website (e.g., google.com):")
        if url:
            try:
                website = self.add_website_use_case.execute(url.strip())
                self.website_listbox.insert(tk.END, str(website.url))
                self._update_status(f"Added: {website.url}")
            except ValueError as e:
                messagebox.showerror("Error", str(e))

    def _remove_website(self):
        """Remove the selected website"""
        selection = self.website_listbox.curselection()
        if not selection:
            messagebox.showwarning("No Selection", "Please select a website to remove!")
            return

        index = selection[0]
        url = self.website_listbox.get(index)

        if messagebox.askyesno("Confirm Removal", f"Remove {url} from monitoring list?"):
            try:
                website = self.remove_website_use_case.execute(url)
                self.website_listbox.delete(index)
                self._update_status(f"Removed: {website.url}")
            except ValueError as e:
                messagebox.showerror("Error", str(e))

    def _set_interval(self):
        """Set the monitoring interval"""
        try:
            interval = int(self.interval_var.get())
            if interval < 1:
                raise ValueError("Interval must be at least 1 second")
            self.session.set_monitoring_interval(interval)
            self._update_status(f"Monitoring interval set to {interval} seconds")
        except ValueError as e:
            messagebox.showerror("Invalid Interval", f"Please enter a valid number (minimum 1): {e}")

    def _start_monitoring(self):
        """Start monitoring websites"""
        if self.session.get_website_count() == 0:
            messagebox.showwarning("No Websites", "Please add at least one website to monitor!")
            return

        try:
            # Start monitoring with check callback
            self.start_monitoring_use_case.execute(
                check_callback=self.check_websites_use_case.execute
            )

            self.start_btn.config(state=tk.DISABLED)
            self.stop_btn.config(state=tk.NORMAL)
            self._update_status(
                f"MONITORING {self.session.get_website_count()} website(s) - Alerts ACTIVE"
            )
            messagebox.showinfo(
                "Monitoring Started",
                f"Now monitoring {self.session.get_website_count()} website(s).\n\n"
                "You will receive continuous pop-up and sound alerts while any monitored website is online.\n\n"
                "Click STOP MONITORING to end alerts."
            )
        except ValueError as e:
            messagebox.showerror("Error", str(e))

    def _stop_monitoring(self):
        """Stop monitoring websites"""
        try:
            self.stop_monitoring_use_case.execute()
            self.start_btn.config(state=tk.NORMAL)
            self.stop_btn.config(state=tk.DISABLED)
            self._update_status("Monitoring stopped")
        except ValueError as e:
            messagebox.showerror("Error", str(e))

    def _update_status(self, message: str):
        """Update status bar message"""
        if self.status_label:
            self.status_label.config(text=f"Status: {message}")

    def _toggle_startup(self):
        """Toggle auto-startup on/off"""
        if not self.startup_manager:
            return

        if self.startup_var.get():
            # Enable auto-startup
            if self.startup_manager.enable():
                self._update_status("Auto-startup enabled")
                messagebox.showinfo(
                    "Auto-Startup Enabled",
                    "The application will now start automatically when you turn on your computer.\n\n"
                    f"Startup command:\n{self.startup_manager.get_startup_command()}"
                )
            else:
                # Failed to enable
                self.startup_var.set(False)
                messagebox.showerror(
                    "Error",
                    "Failed to enable auto-startup. Please run as administrator."
                )
        else:
            # Disable auto-startup
            if self.startup_manager.disable():
                self._update_status("Auto-startup disabled")
            else:
                # Failed to disable
                self.startup_var.set(True)
                messagebox.showerror(
                    "Error",
                    "Failed to disable auto-startup. Please run as administrator."
                )

    def _on_closing(self):
        """Handle window close event"""
        if self.session.is_active:
            if messagebox.askokcancel("Quit", "Monitoring is active. Stop monitoring and quit?"):
                self.stop_monitoring_use_case.execute()
                self.root.destroy()
        else:
            self.root.destroy()

    def run(self):
        """Start the GUI main loop"""
        self.root.mainloop()
