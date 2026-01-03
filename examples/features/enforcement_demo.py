"""
Agreement Enforcement Demo - Complete Phase 3 system.

Demonstrates:
1. Behavioral analysis (Phase 1)
2. Avatar counselor intervention (Phase 2)
3. Agreement tracking with countdown timers (Phase 3)
4. Grace period warnings (Phase 3)
5. Automatic tab closure on expiration (Phase 3)
6. Complete workflow from detection → negotiation → enforcement
"""
import tkinter as tk
import time
from datetime import datetime

# MCP imports
from browser_tools_mcp import BrowserToolsMCP
from webcam_mcp import WebcamMCP
from elevenlabs_mcp import ElevenLabsMCP
from memory_mcp import MemoryMCP
from notifymemaybe import NotifyMeMaybe

# Phase 1 imports
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase

# Phase 2 imports
from src.infrastructure.adapters.counselor_orchestrator import CounselorOrchestrator

# Phase 3 imports
from src.application.use_cases.track_agreements import TrackAgreementsUseCase
from src.application.use_cases.enforce_agreement import EnforceAgreementUseCase
from src.infrastructure.adapters.playwright_browser_controller import PlaywrightBrowserController
from src.infrastructure.adapters.enforcement_notifier import EnforcementNotifier
from src.infrastructure.adapters.counselor_voice_service import CounselorVoiceService
from src.presentation.countdown_timer_widget import CountdownTimerWidget


def main():
    """Run complete enforcement demo."""
    print("=" * 70)
    print("FocusMotherFocus - Complete System Demo (Phase 1 + 2 + 3)")
    print("=" * 70)
    print("\nThis demo shows the COMPLETE productivity counselor:")
    print("  ✓ Phase 1: Behavioral Analysis")
    print("  ✓ Phase 2: Avatar Counselor & Negotiation")
    print("  ✓ Phase 3: Agreement Enforcement & Tab Auto-Close")
    print()

    # Create tkinter root
    print("1. Initializing GUI...")
    root = tk.Tk()
    root.withdraw()
    print("✅ GUI initialized")

    # Initialize MCP clients
    print("\n2. Initializing MCP clients...")

    browser_mcp = BrowserToolsMCP()
    if not browser_mcp.is_available():
        print("❌ Browser Tools MCP not available!")
        return
    print("✅ Browser Tools MCP")

    webcam_mcp = WebcamMCP() if WebcamMCP().is_available() else None
    if webcam_mcp:
        print("✅ Webcam MCP")
    else:
        print("⚠️  Webcam MCP not available")

    elevenlabs_mcp = ElevenLabsMCP() if ElevenLabsMCP().is_available() else None
    if elevenlabs_mcp:
        print("✅ ElevenLabs MCP")
    else:
        print("⚠️  ElevenLabs MCP not available")

    memory_mcp = MemoryMCP() if MemoryMCP().is_available() else None
    if memory_mcp:
        print("✅ Memory MCP")
    else:
        print("⚠️  Memory MCP not available")

    notifymemaybe = NotifyMeMaybe()
    if notifymemaybe.is_available():
        print("✅ NotifyMeMaybe")
    else:
        print("⚠️  NotifyMeMaybe not available")
        notifymemaybe = None

    # Initialize Playwright browser controller
    print("\n3. Initializing Browser Controller...")
    browser_controller = PlaywrightBrowserController()
    if browser_controller.is_available():
        print("✅ Playwright Browser Controller")
        print("   → Tab auto-close enabled!")
    else:
        print("⚠️  Playwright not available - tab auto-close disabled")
        print("   Start Chrome with: start_chrome_debug.bat")
        browser_controller = None

    # Initialize Phase 1: Behavioral Analyzer
    print("\n4. Initializing Behavioral Analyzer...")
    analyzer = MCPBehavioralAnalyzer(
        browser_mcp=browser_mcp,
        scroll_time_threshold=20.0  # Short threshold for demo
    )
    analyzer.start_monitoring()
    print("✅ Behavioral Analyzer ready")

    # Initialize Phase 2: Counselor Orchestrator
    print("\n5. Initializing Counselor Orchestrator...")
    orchestrator = CounselorOrchestrator(
        webcam_mcp=webcam_mcp,
        elevenlabs_mcp=elevenlabs_mcp,
        memory_mcp=memory_mcp,
        parent_window=root
    )
    print("✅ Counselor Orchestrator ready")

    # Initialize Phase 3: Enforcement System
    print("\n6. Initializing Enforcement System...")

    # Voice service
    voice_service = CounselorVoiceService(
        elevenlabs_mcp=elevenlabs_mcp
    )

    # Enforcement notifier
    enforcement_notifier = EnforcementNotifier(
        notifymemaybe=notifymemaybe,
        voice_service=voice_service
    )

    # Agreement tracker
    agreement_tracker = TrackAgreementsUseCase(
        grace_period_seconds=15.0,  # Short grace period for demo
        warning_before_seconds=30.0  # Warn 30s before expiration
    )

    # Enforcement use case
    enforcement_use_case = EnforceAgreementUseCase(
        browser_controller=browser_controller,
        grace_period_seconds=15.0
    )

    # Countdown timer widget
    countdown_timer = CountdownTimerWidget(parent=root)

    print("✅ Enforcement System ready")

    # Intervention callback (Phase 2)
    def on_intervention(event):
        """Handle intervention when triggered."""
        print(f"\n[INTERVENTION] {event.event_type} detected!")

        recommendation = intervention_use_case.get_intervention_recommendation(event)

        print(f"[INTERVENTION] Type: {recommendation['type']}")
        print(f"[INTERVENTION] Message: {recommendation['message'][:60]}...")

        def on_agreement_reached(agreement):
            if agreement:
                print(f"\n[AGREEMENT] Reached:")
                print(f"  - Duration: {agreement.agreed_duration_minutes} minutes")
                print(f"  - Expires: {agreement.expires_at.strftime('%H:%M:%S')}")

                # Add to tracker (Phase 3)
                agreement_tracker.add_agreement(agreement)
                orchestrator.active_agreements.append(agreement)

                # Show countdown timer (Phase 3)
                def on_extend_request(agr):
                    extension = enforcement_notifier.request_extension(
                        current_duration_minutes=agr.agreed_duration_minutes
                    )
                    if extension:
                        agr.extend(extension)
                        print(f"[AGREEMENT] Extended by {extension} minutes")
                        countdown_timer.update_agreement(agr)

                countdown_timer.show(
                    agreement=agreement,
                    on_extend=on_extend_request,
                    on_dismiss=lambda a: countdown_timer.hide()
                )

        orchestrator.execute_intervention(
            event=event,
            recommendation=recommendation,
            on_complete=on_agreement_reached
        )

    # Initialize Phase 1: Intervention trigger
    intervention_use_case = TriggerInterventionUseCase(
        behavioral_analyzer=analyzer,
        intervention_callback=on_intervention
    )
    intervention_use_case.set_cooldown(10)  # Short cooldown for demo

    # Warning/Enforcement callbacks (Phase 3)
    def on_warning(agreement, seconds_remaining):
        """Called when agreement approaching expiration."""
        print(f"\n⏰ WARNING: {agreement.event_type} expires in {seconds_remaining:.0f}s")

        message = f"Your {agreement.event_type} agreement expires in {int(seconds_remaining)} seconds!"
        enforcement_notifier.send_warning(message, speak=True)

    def on_expired(agreement):
        """Called when agreement expired."""
        print(f"\n🕐 EXPIRED: {agreement.event_type} (entering grace period)")

        message = f"Time's up for {agreement.event_type}! Grace period: {enforcement_use_case.grace_period_seconds}s"
        enforcement_notifier.send_grace_period_notification(
            message,
            enforcement_use_case.grace_period_seconds,
            speak=True
        )

    def on_violation(agreement):
        """Called when user violates agreement."""
        print(f"\n🚫 VIOLATION: {agreement.event_type}")

        # Enforce (close tab)
        def on_enforced_message(msg):
            enforcement_notifier.send_enforcement_notification(msg, speak=True)

        enforcement_use_case.enforce(
            agreement=agreement,
            force=True,
            on_enforced=on_enforced_message
        )

        # Hide countdown timer
        countdown_timer.hide()

    print("\n7. Starting Monitoring...")
    print("=" * 70)
    print("COMPLETE SYSTEM ACTIVE")
    print("=" * 70)
    print("\nMonitoring browser activity...")
    print("  • Detects patterns (20+ seconds)")
    print("  • Shows avatar counselor")
    print("  • Negotiates agreements")
    print("  • Shows countdown timer")
    print("  • Warns before expiration")
    print("  • Auto-closes tabs when time's up!")
    print("\nPress Ctrl+C to stop")
    print("=" * 70 + "\n")

    # Monitoring loop
    check_count = 0
    intervention_count = 0

    try:
        while True:
            check_count += 1

            # Process tkinter events
            root.update()

            # Phase 1 + 2: Trigger interventions
            event = intervention_use_case.execute()
            if event:
                intervention_count += 1

            # Phase 3: Check agreement compliance
            current_event = analyzer.analyze_current_activity()
            agreement_tracker.check_compliance(
                current_event=current_event,
                on_warning=on_warning,
                on_expired=on_expired,
                on_violation=on_violation
            )

            # Periodic status (every 30 seconds)
            if check_count % 6 == 0:
                print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Status Check #{check_count // 6}")

                # Current activity
                if current_event:
                    print(f"  Current: {current_event.event_type} (severity: {current_event.severity})")
                else:
                    print("  Current: Productive ✅")

                # Active agreements
                active_agreements = agreement_tracker.get_active_agreements()
                if active_agreements:
                    print(f"  Active Agreements: {len(active_agreements)}")
                    for agreement in active_agreements:
                        remaining = agreement.time_remaining_minutes()
                        print(f"    - {agreement.event_type}: {remaining:.1f} min left")

                # Summary
                summary = agreement_tracker.get_summary()
                print(f"  Total Tracked: {summary['total_tracked']}")
                print(f"  Violated: {summary['violated']}")
                print(f"  Interventions: {intervention_count}")
                print()

            # Check every 5 seconds
            time.sleep(5)

    except KeyboardInterrupt:
        print("\n\nStopping...")

        # Cleanup
        analyzer.stop_monitoring()
        orchestrator.cleanup()
        countdown_timer.hide()

        # Final summary
        print("\n" + "=" * 70)
        print("SESSION SUMMARY")
        print("=" * 70)

        summary = agreement_tracker.get_summary()
        print(f"\nAgreements Tracked: {summary['total_tracked']}")
        print(f"Violations: {summary['violated']}")
        print(f"Interventions: {intervention_count}")

        all_agreements = orchestrator.active_agreements
        if all_agreements:
            print(f"\nAll Agreements:")
            for agreement in all_agreements:
                status = "✅ KEPT" if not agreement.is_violated else "❌ VIOLATED"
                print(f"\n  {agreement.event_type} ({status}):")
                print(f"    Duration: {agreement.agreed_duration_minutes} min")
                print(f"    User: {agreement.user_response}")

        print("\n" + "=" * 70)
        print("Thank you for using FocusMotherFocus!")
        print("=" * 70)

        try:
            root.destroy()
        except:
            pass


if __name__ == "__main__":
    main()
