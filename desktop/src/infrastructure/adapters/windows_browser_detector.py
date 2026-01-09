"""
Windows Browser Detector
Detects if URLs are open in browser tabs on Windows using window titles
"""

import psutil
import re
import ctypes
from ctypes import wintypes
from typing import List, Set, Dict
from urllib.parse import urlparse
from ...application.interfaces.browser_detector import IBrowserDetector
from ...core.value_objects.url import URL


class WindowsBrowserDetector(IBrowserDetector):
    """
    Detects open browser tabs on Windows by examining browser window titles.

    Modern browsers don't expose URLs in command-line arguments, so we use
    Windows API to enumerate windows and check their titles which often contain
    the page title and domain.
    """

    # Browser process names to check
    BROWSER_PROCESSES = {
        'chrome.exe': 'Chrome',
        'firefox.exe': 'Firefox',
        'msedge.exe': 'Edge',
        'opera.exe': 'Opera',
        'brave.exe': 'Brave',
        'iexplore.exe': 'Internet Explorer'
    }

    def __init__(self):
        """Initialize the browser detector"""
        self._supported_browsers = list(self.BROWSER_PROCESSES.values())
        self._setup_win32_api()

    def _setup_win32_api(self):
        """Setup Windows API functions for window enumeration"""
        try:
            self.user32 = ctypes.windll.user32
            self.EnumWindowsProc = ctypes.WINFUNCTYPE(
                wintypes.BOOL,
                wintypes.HWND,
                wintypes.LPARAM
            )
        except Exception as e:
            print(f"Warning: Could not setup Win32 API: {e}")
            self.user32 = None

    def is_url_open_in_browser(self, url: URL) -> bool:
        """
        Check if the given URL is currently open in any browser tab

        This works by:
        1. Getting all browser window titles
        2. Checking if the target domain appears in any title

        Args:
            url: The URL to check

        Returns:
            True if URL is open in a browser tab, False otherwise
        """
        try:
            target_domain = self._extract_domain(str(url))
            if not target_domain:
                return False

            # Get all browser window titles
            browser_titles = self._get_browser_window_titles()

            # Check if domain appears in any window title
            for title in browser_titles:
                if self._domain_matches_title(target_domain, title):
                    return True

            return False

        except Exception as e:
            # If detection fails, don't alert (safer to not alert than false positive)
            print(f"Browser detection error: {e}")
            return False

    def get_supported_browsers(self) -> List[str]:
        """
        Get list of browsers that can be detected

        Returns:
            List of browser names
        """
        return self._supported_browsers.copy()

    def _get_browser_window_titles(self) -> List[str]:
        """
        Get all window titles from browser processes

        Returns:
            List of window titles from browser windows
        """
        titles = []

        if not self.user32:
            return titles

        try:
            # Browser indicators in window titles
            BROWSER_INDICATORS = [
                ' - Google Chrome',
                ' - Microsoft Edge',
                ' - Brave',
                ' - Firefox',
                ' - Opera',
                ' - Internet Explorer'
            ]

            # Enumerate all windows and get titles that look like browser windows
            def enum_callback(hwnd, _):
                try:
                    # Check if window is visible
                    if not self.user32.IsWindowVisible(hwnd):
                        return True

                    # Get window title
                    length = self.user32.GetWindowTextLengthW(hwnd)
                    if length == 0:
                        return True

                    buffer = ctypes.create_unicode_buffer(length + 1)
                    self.user32.GetWindowTextW(hwnd, buffer, length + 1)
                    title = buffer.value

                    if not title:
                        return True

                    # Check if title ends with a browser indicator
                    # Browser windows typically end with " - Chrome", " - Edge", etc.
                    if any(title.endswith(indicator) for indicator in BROWSER_INDICATORS):
                        titles.append(title)

                except Exception:
                    pass

                return True

            # Enumerate all windows
            callback = self.EnumWindowsProc(enum_callback)
            self.user32.EnumWindows(callback, 0)

        except Exception as e:
            print(f"Error getting browser window titles: {e}")

        return titles

    def _domain_matches_title(self, domain: str, title: str) -> bool:
        """
        Check if a domain appears in a window title

        Browser window titles typically have formats like:
        - "Page Title - Google Chrome"
        - "Page Title - Microsoft Edge"
        - "YouTube"
        - "Google"

        Args:
            domain: Domain to search for (e.g., 'youtube.com')
            title: Window title to check

        Returns:
            True if domain found in title, False otherwise
        """
        if not title:
            return False

        title_lower = title.lower()
        domain_lower = domain.lower()

        # Extract base domain (e.g., 'youtube' from 'youtube.com')
        base_domain = domain_lower.split('.')[0]

        # Check if base domain appears in title
        if base_domain in title_lower:
            return True

        # Check if full domain appears
        if domain_lower in title_lower:
            return True

        return False

    def _extract_domain(self, url_str: str) -> str:
        """
        Extract the domain from a URL

        Args:
            url_str: Full URL string

        Returns:
            Domain name (e.g., 'google.com' from 'https://www.google.com/search')
        """
        try:
            parsed = urlparse(url_str)
            domain = parsed.netloc or parsed.path

            # Remove www. prefix
            if domain.startswith('www.'):
                domain = domain[4:]

            # Remove port if present
            if ':' in domain:
                domain = domain.split(':')[0]

            return domain

        except Exception:
            return ""

    def get_open_urls(self) -> Set[str]:
        """
        Get all browser window titles (for debugging/testing)

        Returns:
            Set of window titles from browser windows
        """
        return set(self._get_browser_window_titles())
