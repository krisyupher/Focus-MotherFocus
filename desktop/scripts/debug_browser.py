"""
Debug script to see what browser processes and URLs are detected
"""

import psutil
from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector

def main():
    print("=" * 80)
    print("BROWSER PROCESS DEBUGGING")
    print("=" * 80)

    detector = WindowsBrowserDetector()

    # Check if browsers are running
    print("\n1. Looking for browser processes...")
    print("-" * 80)

    import psutil
    browser_procs = []
    for proc in psutil.process_iter(['name']):
        try:
            proc_name = proc.info['name']
            if proc_name and proc_name.lower() in ['chrome.exe', 'msedge.exe', 'firefox.exe']:
                browser_procs.append(proc)
        except:
            pass

    if not browser_procs:
        print("[X] NO BROWSER PROCESSES FOUND!")
        print("\nPlease make sure you have a browser open and try again.")
        return

    print(f"[OK] Found {len(browser_procs)} browser process(es)")

    # Test window title extraction
    print("\n" + "=" * 80)
    print("2. Browser window titles detected:")
    print("-" * 80)

    window_titles = detector.get_open_urls()  # Now returns window titles

    if window_titles:
        print(f"\n[OK] Found {len(window_titles)} browser window(s):")
        for title in sorted(window_titles):
            print(f"  - {title}")
    else:
        print("\n[X] NO BROWSER WINDOWS DETECTED")
        print("\nThis might mean:")
        print("  1. No browser windows are open")
        print("  2. Error accessing window titles")
        print("  3. Browser windows are minimized")

    # Test specific URL detection
    print("\n" + "=" * 80)
    print("3. Testing specific URL matching...")
    print("-" * 80)

    test_urls = ["google.com", "youtube.com", "github.com", "facebook.com"]

    print("\nPlease open one of these URLs in your browser:")
    for url in test_urls:
        print(f"  - https://{url}")

    print("\nChecking if these are detected:")
    for url_str in test_urls:
        try:
            from src.core.value_objects.url import URL
            url = URL(url_str)
            is_open = detector.is_url_open_in_browser(url)
            status = "[OK] DETECTED" if is_open else "[X] Not detected"
            print(f"  {url_str:20} {status}")
        except Exception as e:
            print(f"  {url_str:20} [!] ERROR: {e}")

    print("\n" + "=" * 80)
    print("\nTIPS:")
    print("- Window titles should contain the page name or domain")
    print("- If you see 'google' in a window title, google.com will be detected")
    print("- If you see 'youtube' in a window title, youtube.com will be detected")
    print("- Make sure your browser window is not minimized")
    print("=" * 80)

if __name__ == "__main__":
    main()
