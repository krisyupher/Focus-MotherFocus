"""
Behavioral Analysis Demo - Integrating Browser MCP with intervention system.

This demo shows:
1. Behavioral analysis detecting scrolling, adult content, distractions
2. Intervention triggering based on patterns
3. Integration with existing monitoring system
"""
import time
import json
from datetime import datetime

from browser_tools_mcp import BrowserToolsMCP
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase


def intervention_handler(event):
    """
    Handle intervention when unproductive behavior is detected.

    This is where you'd integrate:
    - Avatar counselor (HeyGen MCP)
    - Voice alerts (ElevenLabs MCP)
    - Interactive notifications (NotifyMeMaybe)
    - Tab closing (Playwright MCP)
    """
    print("\n" + "=" * 70)
    print("🚨 INTERVENTION TRIGGERED 🚨")
    print("=" * 70)
    print(f"Event Type: {event.event_type}")
    print(f"Severity: {event.severity}")
    print(f"URL: {event.url}")
    print(f"Duration: {event.duration_seconds}s")
    print(f"Detected At: {event.detected_at.strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Metadata: {json.dumps(event.metadata, indent=2)}")
    print("=" * 70 + "\n")

    # This is where you'd call:
    # - show_avatar_counselor(event)
    # - speak_intervention_message(event)
    # - negotiate_with_user(event)
    # - close_tab_if_needed(event)


def show_intervention_recommendation(use_case, event):
    """Display recommended intervention strategy."""
    recommendation = use_case.get_intervention_recommendation(event)

    print("\n📋 INTERVENTION RECOMMENDATION:")
    print(f"  Type: {recommendation['type']}")
    print(f"  Message: {recommendation['message']}")
    print(f"  Action: {recommendation['action']}")
    print(f"  Urgency: {recommendation['urgency']}")
    print(f"  Show Avatar: {recommendation['show_avatar']}")
    print(f"  Use Voice: {recommendation['use_voice']}")


def main():
    """Run behavioral analysis demo."""
    print("=" * 70)
    print("FocusMotherFocus - Behavioral Analysis Demo")
    print("=" * 70)
    print("\nThis demo monitors your browser activity and triggers interventions")
    print("when unproductive patterns are detected.\n")

    # Initialize Browser Tools MCP
    print("1. Initializing Browser Tools MCP...")
    browser_mcp = BrowserToolsMCP()

    if not browser_mcp.is_available():
        print("❌ Browser Tools MCP not available!")
        print("   Please ensure browser-tools-mcp executable is installed and on PATH")
        return

    print("✅ Browser MCP initialized")

    # Initialize behavioral analyzer
    print("\n2. Initializing Behavioral Analyzer...")
    analyzer = MCPBehavioralAnalyzer(
        browser_mcp=browser_mcp,
        scroll_threshold_pixels=5000,
        scroll_time_threshold=60.0  # 1 minute of scrolling
    )
    print("✅ Behavioral Analyzer ready")

    # Initialize intervention trigger
    print("\n3. Initializing Intervention System...")
    intervention_use_case = TriggerInterventionUseCase(
        behavioral_analyzer=analyzer,
        intervention_callback=intervention_handler
    )
    intervention_use_case.set_cooldown(30)  # 30 second cooldown between interventions
    print("✅ Intervention System ready")

    # Start monitoring
    print("\n4. Starting Behavioral Monitoring...")
    analyzer.start_monitoring()
    print("✅ Monitoring active\n")

    print("=" * 70)
    print("Monitoring your browser activity...")
    print("Press Ctrl+C to stop")
    print("=" * 70 + "\n")

    try:
        check_count = 0
        while True:
            check_count += 1

            # Trigger intervention check
            event = intervention_use_case.execute()

            if event:
                # Show detailed recommendation
                show_intervention_recommendation(intervention_use_case, event)

            # Show periodic status
            if check_count % 10 == 0:
                print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Status check #{check_count}")

                # Get current activity (without triggering intervention)
                current_event = analyzer.analyze_current_activity()
                if current_event:
                    print(f"  Current Activity: {current_event.event_type} "
                          f"(severity: {current_event.severity})")
                else:
                    print("  Current Activity: Productive ✅")

                # Show patterns
                patterns = intervention_use_case.analyze_patterns(lookback_minutes=30)
                if patterns:
                    print(f"  Patterns Detected: {len(patterns)}")
                    for pattern in patterns:
                        print(f"    - {pattern.pattern_type}: "
                              f"{pattern.frequency} occurrences, "
                              f"confidence: {pattern.confidence:.2f}")
                        print(f"      → {pattern.recommendation}")

                # Show intervention history
                history = intervention_use_case.get_intervention_history(limit=3)
                if history:
                    print(f"  Recent Interventions: {len(history)}")
                    for timestamp, hist_event in history[-3:]:
                        print(f"    - [{timestamp.strftime('%H:%M:%S')}] "
                              f"{hist_event.event_type}")

            # Check every 5 seconds
            time.sleep(5)

    except KeyboardInterrupt:
        print("\n\nStopping monitoring...")
        analyzer.stop_monitoring()

        # Final summary
        print("\n" + "=" * 70)
        print("SESSION SUMMARY")
        print("=" * 70)

        history = intervention_use_case.get_intervention_history()
        print(f"\nTotal Interventions: {len(history)}")

        patterns = intervention_use_case.analyze_patterns(lookback_minutes=60)
        print(f"Patterns Identified: {len(patterns)}")

        if patterns:
            print("\nDetected Patterns:")
            for pattern in patterns:
                print(f"\n  {pattern.pattern_type}:")
                print(f"    Frequency: {pattern.frequency}")
                print(f"    Total Duration: {pattern.total_duration_seconds:.1f}s")
                print(f"    Confidence: {pattern.confidence:.2%}")
                print(f"    Recommendation: {pattern.recommendation}")

        print("\n" + "=" * 70)
        print("Thank you for using FocusMotherFocus!")
        print("=" * 70)


if __name__ == "__main__":
    main()
