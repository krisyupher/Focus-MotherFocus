"""Interface for detecting running processes."""
from abc import ABC, abstractmethod
from src.core.value_objects.process_name import ProcessName


class IProcessDetector(ABC):
    """
    Interface for detecting if applications/processes are currently running.

    This interface defines the contract for process detection implementations,
    allowing the application layer to check if monitored applications are running
    without depending on specific Windows APIs or process management libraries.
    """

    @abstractmethod
    def is_process_running(self, process_name: ProcessName) -> bool:
        """
        Check if a process is currently running.

        Args:
            process_name: ProcessName value object to check

        Returns:
            True if at least one instance of the process is running, False otherwise

        Examples:
            >>> detector.is_process_running(ProcessName("chrome.exe"))
            True
            >>> detector.is_process_running(ProcessName("nonexistent.exe"))
            False
        """
        pass
