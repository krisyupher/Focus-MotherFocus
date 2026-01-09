"""Windows implementation of process detection using psutil."""
import psutil
from typing import Set, Dict
import time
from src.application.interfaces.process_detector import IProcessDetector
from src.core.value_objects.process_name import ProcessName


class WindowsProcessDetector(IProcessDetector):
    """
    Windows-specific implementation of process detection.

    Uses psutil library to enumerate running processes and check if a specific
    process name is currently running. Process names are matched case-insensitively.

    Includes caching to improve reliability and performance.
    """

    def __init__(self):
        """Initialize with cache."""
        self._process_cache: Dict[str, float] = {}  # name -> last_seen_timestamp
        self._cache_timeout = 0.5  # 500ms cache timeout

    def is_process_running(self, process_name: ProcessName) -> bool:
        """
        Check if a process is currently running on Windows.

        This method enumerates all running processes and checks if any match
        the given process name (case-insensitive).

        Args:
            process_name: ProcessName value object to check

        Returns:
            True if at least one instance of the process is running

        Note:
            - Process name matching is case-insensitive
            - Returns True if any instance is found (multiple instances count as True)
            - Handles access denied and process termination gracefully
        """
        target = process_name.value
        current_time = time.time()

        # Check cache first for recently seen processes
        if target in self._process_cache:
            if current_time - self._process_cache[target] < self._cache_timeout:
                return True  # Recently seen, still running

        # Enumerate processes with multiple retry attempts for reliability
        for attempt in range(3):  # Try up to 3 times
            try:
                running_processes = self._get_running_process_names()

                if target in running_processes:
                    # Update cache
                    self._process_cache[target] = current_time
                    return True
                else:
                    # Not found, but might be in cache from previous check
                    if target in self._process_cache:
                        # Remove from cache if not found
                        if current_time - self._process_cache[target] > self._cache_timeout:
                            del self._process_cache[target]
                    return False

            except Exception as e:
                # On error, retry (unless last attempt)
                if attempt < 2:
                    time.sleep(0.1)  # Brief delay before retry
                    continue
                # Last attempt failed - check cache
                if target in self._process_cache:
                    if current_time - self._process_cache[target] < self._cache_timeout:
                        return True  # Use cached value
                return False

        return False

    def _get_running_process_names(self) -> Set[str]:
        """
        Get set of all running process names (lowercase).

        Returns:
            Set of lowercase process names currently running

        Note:
            Handles psutil.NoSuchProcess and psutil.AccessDenied exceptions
            which can occur when processes terminate during enumeration or
            when accessing system processes.
        """
        processes = set()
        for proc in psutil.process_iter(['name']):
            try:
                if proc.info['name']:
                    # Normalize to lowercase for case-insensitive matching
                    processes.add(proc.info['name'].lower())
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                # Process terminated or access denied - skip it
                continue
        return processes

    def get_running_process_count(self, process_name: ProcessName) -> int:
        """
        Count how many instances of a process are running.

        Args:
            process_name: ProcessName value object to count

        Returns:
            Number of instances of the process currently running
        """
        try:
            count = 0
            target_name = process_name.value.lower()

            for proc in psutil.process_iter(['name']):
                try:
                    if proc.info['name'] and proc.info['name'].lower() == target_name:
                        count += 1
                except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                    continue

            return count
        except Exception:
            return 0
