"""
FocusMotherFocus - AI Productivity Counselor with Avatar

Complete integration of all 4 phases:
- Phase 1: Behavioral analysis and detection
- Phase 2: Avatar counselor with voice interaction
- Phase 3: Agreement enforcement with countdown timers
- Phase 4: MCP service orchestration with health monitoring

Single entry point - just run this file!
"""
import tkinter as tk
import sys

from src.presentation.avatar_counselor_gui import AvatarCounselorGUI


def main():
    """Main entry point - Avatar Counselor GUI with all phases integrated"""
    print("=" * 80)
    print("  FocusMotherFocus - AI Productivity Counselor")
    print("=" * 80)
    print()
    print("Initializing complete system...")
    print("  • Phase 1: Behavioral Analysis")
    print("  • Phase 2: Avatar Counselor & Negotiation")
    print("  • Phase 3: Agreement Enforcement")
    print("  • Phase 4: MCP Service Orchestration")
    print()

    try:
        # Create main window
        root = tk.Tk()

        # Create Avatar Counselor GUI (all phases integrated)
        gui = AvatarCounselorGUI(root)

        print()
        print("=" * 80)
        print("  ✅ Ready! Click 'Start Monitoring' to begin")
        print("=" * 80)
        print()
        print("Features:")
        print("  • Automatic behavioral monitoring")
        print("  • Voice-based avatar interaction")
        print("  • Smart agreement negotiation")
        print("  • Countdown timers with warnings")
        print("  • Automatic tab enforcement")
        print("  • Service health monitoring")
        print()
        print("Just click the START button - everything else is automatic!")
        print()

        # Run main loop
        gui.run()

    except KeyboardInterrupt:
        print("\n\n[Main] Shutting down...")
        sys.exit(0)
    except Exception as e:
        print(f"\n[Main] Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
