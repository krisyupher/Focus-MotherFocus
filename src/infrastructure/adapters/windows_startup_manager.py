"""
Windows Startup Manager
Manages auto-startup via Windows Registry
"""

import winreg
import sys
import os
from typing import Optional
from ...application.interfaces.startup_manager import IStartupManager


class WindowsStartupManager(IStartupManager):
    """
    Manages application auto-startup on Windows using Registry.

    Uses the Windows Registry Run key to add/remove the application
    from startup programs. This is the standard Windows approach.
    """

    # Registry path for user startup programs
    REGISTRY_PATH = r"Software\Microsoft\Windows\CurrentVersion\Run"
    APP_NAME = "FocusMotherFocus"

    def __init__(self, app_path: Optional[str] = None):
        """
        Initialize the startup manager

        Args:
            app_path: Path to the application executable or main.py
                     If None, will auto-detect current script path
        """
        self.app_path = app_path or self._get_app_path()

    def _get_app_path(self) -> str:
        """
        Get the path to the application entry point

        Returns:
            Absolute path to main.py or executable
        """
        # Get the main.py path
        if getattr(sys, 'frozen', False):
            # Running as compiled executable
            return sys.executable
        else:
            # Running as Python script
            # Get the project root (where main.py is)
            current_dir = os.path.dirname(os.path.abspath(__file__))
            # Navigate up from src/infrastructure/adapters to project root
            project_root = os.path.abspath(os.path.join(current_dir, '..', '..', '..'))
            main_py = os.path.join(project_root, 'main.py')

            # Return command to run Python script
            python_exe = sys.executable
            return f'"{python_exe}" "{main_py}"'

    def is_enabled(self) -> bool:
        """
        Check if auto-startup is currently enabled

        Returns:
            True if application is configured to start on boot, False otherwise
        """
        try:
            # Open registry key
            key = winreg.OpenKey(
                winreg.HKEY_CURRENT_USER,
                self.REGISTRY_PATH,
                0,
                winreg.KEY_READ
            )

            try:
                # Try to read our app's value
                value, _ = winreg.QueryValueEx(key, self.APP_NAME)
                winreg.CloseKey(key)
                return True
            except FileNotFoundError:
                winreg.CloseKey(key)
                return False

        except Exception as e:
            print(f"Error checking startup status: {e}")
            return False

    def enable(self) -> bool:
        """
        Enable application to start automatically on system boot

        Returns:
            True if successfully enabled, False otherwise
        """
        try:
            # Open registry key for writing
            key = winreg.OpenKey(
                winreg.HKEY_CURRENT_USER,
                self.REGISTRY_PATH,
                0,
                winreg.KEY_WRITE
            )

            # Set the startup command
            winreg.SetValueEx(
                key,
                self.APP_NAME,
                0,
                winreg.REG_SZ,
                self.app_path
            )

            winreg.CloseKey(key)
            print(f"Auto-startup enabled: {self.app_path}")
            return True

        except Exception as e:
            print(f"Error enabling startup: {e}")
            return False

    def disable(self) -> bool:
        """
        Disable application auto-startup

        Returns:
            True if successfully disabled, False otherwise
        """
        try:
            # Open registry key for writing
            key = winreg.OpenKey(
                winreg.HKEY_CURRENT_USER,
                self.REGISTRY_PATH,
                0,
                winreg.KEY_WRITE
            )

            try:
                # Delete our app's value
                winreg.DeleteValue(key, self.APP_NAME)
                winreg.CloseKey(key)
                print("Auto-startup disabled")
                return True
            except FileNotFoundError:
                # Already not in startup
                winreg.CloseKey(key)
                return True

        except Exception as e:
            print(f"Error disabling startup: {e}")
            return False

    def get_startup_command(self) -> str:
        """
        Get the command that will be executed on startup

        Returns:
            Full command path that runs on startup
        """
        return self.app_path
