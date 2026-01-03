"""
Avatar Counselor Demo - Full integration of Phase 2 components.

Demonstrates:
1. Behavioral analysis detecting unproductive patterns
2. Avatar counselor window with fullscreen intervention
3. Voice synthesis with ElevenLabs MCP
4. Multi-turn negotiation dialogue
5. Agreement storage in Memory MCP
6. Complete workflow from detection to agreement
"""
import tkinter as tk
import time
from datetime import datetime

# MCP imports
from browser_tools_mcp import BrowserToolsMCP
from webcam_mcp import WebcamMCP
from heygen_mcp import HeyGenMCP
from elevenlabs_mcp import ElevenLabsMCP
from memory_mcp import MemoryMCP

# Phase 1 imports (Behavioral Analysis)
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase

# Phase 2 imports (Avatar Counselor)
from src.infrastructure.adapters.counselor_orchestrator import CounselorOrchestrator


def main():
    """Run avatar counselor demo."""
    print("=" * 70)
    print("FocusMotherFocus - Avatar Counselor Demo (Phase 2)")
    print("=" * 70)
    print("\nThis demo shows the COMPLETE counselor intervention system:")
    print("  ✓ Behavioral analysis (Phase 1)")
    print("  ✓ Avatar counselor window (Phase 2)")
    print("  ✓ Voice synthesis (Phase 2)")
    print("  ✓ Negotiation dialogue (Phase 2)")
    print("  ✓ Agreement storage (Phase 2)")
    print()

    # Create tkinter root window (needed for avatar window)
    print("1. Initializing GUI...")
    root = tk.Tk()
    root.withdraw()  # Hide root window
    print("✅ GUI initialized")

    # Initialize MCP clients
    print("\n2. Initializing MCP clients...")

    # Browser MCP (required)
    browser_mcp = BrowserToolsMCP()
    if not browser_mcp.is_available():
        print("❌ Browser Tools MCP not available!")
        print("   Please ensure browser-tools-mcp is installed")
        return

    print("✅ Browser Tools MCP initialized")

    # Webcam MCP (optional)
    webcam_mcp = WebcamMCP()
    if webcam_mcp.is_available():
        print("✅ Webcam MCP initialized")
    else:
        print("⚠️  Webcam MCP not available (face capture disabled)")
        webcam_mcp = None

    # HeyGen MCP (optional)
    heygen_mcp = HeyGenMCP()
    if heygen_mcp.is_available():
        print("✅ HeyGen MCP initialized")
    else:
        print("⚠️  HeyGen MCP not available (avatar generation disabled)")
        heygen_mcp = None

    # ElevenLabs MCP (optional)
    elevenlabs_mcp = ElevenLabsMCP()
    if elevenlabs_mcp.is_available():
        print("✅ ElevenLabs MCP initialized")
    else:
        print("⚠️  ElevenLabs MCP not available (voice disabled)")
        elevenlabs_mcp = None

    # Memory MCP (optional)
    memory_mcp = MemoryMCP()
    if memory_mcp.is_available():
        print("✅ Memory MCP initialized")
    else:
        print("⚠️  Memory MCP not available (agreement storage disabled)")
        memory_mcp = None

    # Initialize behavioral analyzer (Phase 1)
    print("\n3. Initializing Behavioral Analyzer...")
    analyzer = MCPBehavioralAnalyzer(
        browser_mcp=browser_mcp,
        scroll_time_threshold=30.0  # Shorter threshold for demo
    )
    analyzer.start_monitoring()
    print("✅ Behavioral Analyzer ready")

    # Initialize counselor orchestrator (Phase 2)
    print("\n4. Initializing Counselor Orchestrator...")
    orchestrator = CounselorOrchestrator(
        webcam_mcp=webcam_mcp,
        heygen_mcp=heygen_mcp,
        elevenlabs_mcp=elevenlabs_mcp,
        memory_mcp=memory_mcp,
        parent_window=root
    )
    print("✅ Counselor Orchestrator ready")

    # Intervention callback
    def on_intervention(event):
        """Handle intervention when triggered."""
        print(f"\n[INTERVENTION] {event.event_type} detected!")

        # Get intervention recommendation
        recommendation = intervention_use_case.get_intervention_recommendation(event)

        print(f"[INTERVENTION] Type: {recommendation['type']}")
        print(f"[INTERVENTION] Message: {recommendation['message'][:60]}...")
        print(f"[INTERVENTION] Show Avatar: {recommendation['show_avatar']}")
        print(f"[INTERVENTION] Use Voice: {recommendation['use_voice']}")

        # Execute counselor intervention
        def on_intervention_complete(agreement):
            if agreement:
                print(f"\n[SUCCESS] Agreement reached:")
                print(f"  - Event: {agreement.event_type}")
                print(f"  - Duration: {agreement.agreed_duration_minutes} minutes")
                print(f"  - User said: {agreement.user_response}")
                print(f"  - Expires: {agreement.expires_at.strftime('%H:%M:%S')}")
                print()
            else:
                print("\n[INFO] Intervention completed (no agreement)")
                print()

        orchestrator.execute_intervention(
            event=event,
            recommendation=recommendation,
            on_complete=on_intervention_complete
        )

    # Initialize intervention trigger (Phase 1)
    intervention_use_case = TriggerInterventionUseCase(
        behavioral_analyzer=analyzer,
        intervention_callback=on_intervention
    )
    intervention_use_case.set_cooldown(20)  # Shorter cooldown for demo

    print("\n5. Starting Monitoring...")
    print("=" * 70)
    print("MONITORING ACTIVE")
    print("=" * 70)
    print("\nWatching for behavioral patterns...")
    print("  • Endless scrolling (30+ seconds)")
    print("  • Adult content (instant)")
    print("  • Distraction sites (social media, streaming)")
    print("\nWhen detected, avatar counselor will intervene!")
    print("\nPress Ctrl+C to stop")
    print("=" * 70 + "\n")

    # Monitoring loop
    check_count = 0
    intervention_count = 0

    try:
        while True:
            check_count += 1

            # Process tkinter events (for avatar window)
            root.update()

            # Trigger intervention check (Phase 1 + Phase 2)
            event = intervention_use_case.execute()

            if event:
                intervention_count += 1

            # Periodic status
            if check_count % 6 == 0:  # Every 30 seconds
                print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Status Check #{check_count // 6}")

                # Current activity
                current_event = analyzer.analyze_current_activity()
                if current_event:
                    print(f"  Current Activity: {current_event.event_type} "
                          f"(severity: {current_event.severity})")
                else:
                    print("  Current Activity: Productive ✅")

                # Patterns
                patterns = intervention_use_case.analyze_patterns(lookback_minutes=30)
                if patterns:
                    print(f"  Patterns: {len(patterns)} detected")
                    for pattern in patterns[:2]:  # Show top 2
                        print(f"    - {pattern.pattern_type}: {pattern.frequency}x "
                              f"(confidence: {pattern.confidence:.2f})")

                # Agreements
                active_agreements = orchestrator.get_active_agreements()
                if active_agreements:
                    print(f"  Active Agreements: {len(active_agreements)}")
                    for agreement in active_agreements:
                        remaining = agreement.time_remaining_minutes()
                        print(f"    - {agreement.event_type}: {remaining:.1f} min remaining")

                # Interventions
                print(f"  Total Interventions: {intervention_count}")
                print()

            # Check every 5 seconds
            time.sleep(5)

    except KeyboardInterrupt:
        print("\n\nStopping demo...")

        # Cleanup
        analyzer.stop_monitoring()
        orchestrator.cleanup()

        # Final summary
        print("\n" + "=" * 70)
        print("SESSION SUMMARY")
        print("=" * 70)

        history = intervention_use_case.get_intervention_history()
        print(f"\nTotal Interventions: {len(history)}")

        if history:
            print("\nIntervention History:")
            for timestamp, event in history[-5:]:
                print(f"  [{timestamp.strftime('%H:%M:%S')}] {event.event_type} "
                      f"(severity: {event.severity})")

        patterns = intervention_use_case.analyze_patterns(lookback_minutes=60)
        if patterns:
            print(f"\nBehavioral Patterns: {len(patterns)}")
            for pattern in patterns:
                print(f"\n  {pattern.pattern_type}:")
                print(f"    Frequency: {pattern.frequency}")
                print(f"    Duration: {pattern.total_duration_seconds:.1f}s")
                print(f"    Confidence: {pattern.confidence:.2%}")
                print(f"    Recommendation: {pattern.recommendation}")

        all_agreements = orchestrator.active_agreements
        if all_agreements:
            print(f"\nAgreements Made: {len(all_agreements)}")
            for agreement in all_agreements:
                status = "✅ KEPT" if not agreement.is_violated else "❌ VIOLATED"
                print(f"\n  {agreement.event_type} ({status}):")
                print(f"    Duration: {agreement.agreed_duration_minutes} minutes")
                print(f"    User: {agreement.user_response}")

        print("\n" + "=" * 70)
        print("Thank you for using FocusMotherFocus Avatar Counselor!")
        print("=" * 70)

        # Destroy tkinter window
        try:
            root.destroy()
        except:
            pass


if __name__ == "__main__":
    main()
