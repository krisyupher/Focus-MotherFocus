"""Threaded Scheduler Adapter"""
from dataclasses import dataclass, field
import threading
import time
from typing import Callable, Optional
from ...application.interfaces.monitoring_scheduler import IMonitoringScheduler


@dataclass
class ThreadedScheduler(IMonitoringScheduler):
    """
    Scheduler implementation using threading.

    This is an infrastructure adapter that implements the IMonitoringScheduler
    interface using Python's threading module for periodic task execution.

    Attributes:
        _is_running: Flag indicating if scheduler is active
        _thread: Background thread running the scheduler
        _interval: Current interval in seconds
        _callback: Current callback function
    """
    _is_running: bool = field(default=False, init=False)
    _thread: Optional[threading.Thread] = field(default=None, init=False)
    _interval: int = field(default=10, init=False)
    _callback: Optional[Callable[[], None]] = field(default=None, init=False)

    def start(self, interval: int, callback: Callable[[], None]) -> None:
        """
        Start the scheduler with a periodic callback.

        Args:
            interval: Interval in seconds between callbacks
            callback: Function to call periodically

        Raises:
            ValueError: If scheduler is already running
        """
        if self._is_running:
            raise ValueError("Scheduler is already running")

        if interval <= 0:
            raise ValueError("Interval must be greater than 0")

        self._interval = interval
        self._callback = callback
        self._is_running = True

        # Start background thread
        self._thread = threading.Thread(target=self._run_loop, daemon=True)
        self._thread.start()

    def stop(self) -> None:
        """
        Stop the scheduler.

        Waits for the current iteration to complete before stopping.
        """
        if not self._is_running:
            return

        self._is_running = False

        # Wait for thread to finish (with timeout)
        if self._thread:
            self._thread.join(timeout=2)
            self._thread = None

        self._callback = None

    def is_running(self) -> bool:
        """
        Check if the scheduler is currently running.

        Returns:
            True if scheduler is active
        """
        return self._is_running

    def _run_loop(self) -> None:
        """
        Main loop that runs in background thread.

        Executes the callback periodically at the specified interval.
        """
        while self._is_running:
            if self._callback:
                try:
                    self._callback()
                except Exception as e:
                    print(f"Error in scheduler callback: {e}")

            # Sleep in small increments to allow quick shutdown
            sleep_time = 0
            while sleep_time < self._interval and self._is_running:
                time.sleep(0.1)
                sleep_time += 0.1
