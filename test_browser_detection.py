"""
Quick test script for browser detection
Run this with a browser open to test the detection
"""

from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector
from src.core.value_objects.url import URL

def main():
    print("=" * 60)
    print("Browser Detection Test")
    print("=" * 60)

    detector = WindowsBrowserDetector()

    # Show supported browsers
    print(f"\nSupported browsers: {', '.join(detector.get_supported_browsers())}")

    # Show what's currently open
    print("\n" + "=" * 60)
    print("Currently open URLs detected:")
    print("=" * 60)
    open_urls = detector.get_open_urls()
    if open_urls:
        for url in sorted(open_urls):
            print(f"  - {url}")
    else:
        print("  No URLs detected (or no browsers running)")

    # Test specific URLs
    print("\n" + "=" * 60)
    print("Testing specific URLs:")
    print("=" * 60)

    test_urls = [
        "google.com",
        "youtube.com",
        "github.com",
        "facebook.com",
        "twitter.com"
    ]

    for url_str in test_urls:
        try:
            url = URL(url_str)
            is_open = detector.is_url_open_in_browser(url)
            status = "[OPEN]" if is_open else "[Not open]"
            print(f"  {url_str:20} -> {status}")
        except Exception as e:
            print(f"  {url_str:20} -> ERROR: {e}")

    print("\n" + "=" * 60)
    print("Instructions:")
    print("=" * 60)
    print("1. Open a browser (Chrome, Edge, Firefox, etc.)")
    print("2. Navigate to one of the test URLs above")
    print("3. Run this script again")
    print("4. The URL you opened should show as '[OPEN]'")
    print("=" * 60)

if __name__ == "__main__":
    main()
