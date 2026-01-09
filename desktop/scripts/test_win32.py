"""
Test Windows API window enumeration
"""

import ctypes
from ctypes import wintypes
import psutil

def test_enum_windows():
    user32 = ctypes.windll.user32

    # Get all browser PIDs
    browser_pids = set()
    for proc in psutil.process_iter(['pid', 'name']):
        try:
            if proc.info['name'].lower() in ['chrome.exe', 'msedge.exe', 'firefox.exe']:
                browser_pids.add(proc.info['pid'])
        except:
            pass

    print(f"Browser PIDs: {browser_pids}")
    print()

    # Define callback
    titles = []

    def enum_callback(hwnd, _):
        try:
            # Check if visible
            if not user32.IsWindowVisible(hwnd):
                return True

            # Get title
            length = user32.GetWindowTextLengthW(hwnd)
            if length == 0:
                return True

            buffer = ctypes.create_unicode_buffer(length + 1)
            user32.GetWindowTextW(hwnd, buffer, length + 1)
            title = buffer.value

            if not title:
                return True

            # Get PID
            pid = wintypes.DWORD()
            user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))

            # Print ALL windows with titles that might be browser-related
            title_lower = title.lower()
            if any(x in title_lower for x in ['chrome', 'edge', 'firefox', 'google', 'youtube', 'github', 'facebook']):
                in_browser_pid = " [BROWSER PID]" if pid.value in browser_pids else ""
                print(f"PID={pid.value:6} {in_browser_pid:15} Title: {title}")

                if pid.value in browser_pids:
                    titles.append(title)

        except Exception as e:
            print(f"Error in callback: {e}")

        return True

    # Create callback type
    EnumWindowsProc = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    callback = EnumWindowsProc(enum_callback)

    # Enumerate
    print("Enumerating windows...")
    user32.EnumWindows(callback, 0)

    print(f"\nTotal browser windows found: {len(titles)}")
    for title in titles:
        print(f"  - {title}")

if __name__ == "__main__":
    test_enum_windows()
