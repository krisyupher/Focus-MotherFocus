"""
Quick test to verify Facebook detection is working.
"""
import time
from datetime import datetime


# Mock browser MCP for testing
class MockBrowserMCP:
    def __init__(self):
        self.facebook_url = "https://www.facebook.com/feed"

    def is_available(self):
        return True

    def list_tabs(self):
        return [
            {
                'url': self.facebook_url,
                'title': 'Facebook - Home',
                'active': True
            }
        ]


def test_facebook_detection():
    """Test that Facebook is detected after 20 seconds."""
    from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer

    # Create analyzer with mock browser
    mock_browser = MockBrowserMCP()
    analyzer = MCPBehavioralAnalyzer(
        browser_mcp=mock_browser,
        scroll_time_threshold=20.0  # 20 seconds
    )

    print("Testing Facebook detection...")
    print(f"URL being monitored: {mock_browser.facebook_url}")
    print(f"Threshold: 20 seconds")
    print("\nSimulating checks every 5 seconds...\n")

    # Simulate monitoring loop
    for i in range(10):  # 10 checks = 45 seconds total
        elapsed = i * 5
        print(f"[{elapsed}s] Checking activity...")

        event = analyzer.analyze_current_activity()

        if event:
            print(f"  [SUCCESS] EVENT DETECTED!")
            print(f"     Type: {event.event_type}")
            print(f"     Severity: {event.severity}")
            print(f"     Duration: {event.duration_seconds:.1f}s")
            print(f"     URL: {event.url}")
            print(f"     Metadata: {event.metadata}")
            print(f"\n  SUCCESS: Facebook was detected after {event.duration_seconds:.1f}s")
            return True
        else:
            print(f"  [WAITING] No event yet (need to wait {20 - elapsed}s more)")

        time.sleep(5)

    print("\n[FAILED] No event detected after 45 seconds")
    return False


if __name__ == "__main__":
    print("=" * 80)
    print("  Facebook Detection Test")
    print("=" * 80)
    print()

    success = test_facebook_detection()

    print()
    print("=" * 80)
    if success:
        print("  [PASS] TEST PASSED: Facebook detection is working!")
    else:
        print("  [FAIL] TEST FAILED: Facebook detection not working")
    print("=" * 80)
