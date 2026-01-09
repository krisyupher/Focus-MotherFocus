"""
FocusMotherFocus - Avatar Counselor GUI

Minimal interface with interactive avatar counselor.
Integrates all 4 phases for complete productivity monitoring.

Usage:
    python main_avatar_gui.py

Features:
    - Single start button
    - Avatar-based interaction (no camera needed)
    - Automatic behavioral monitoring
    - Voice-based counseling
    - Smart agreement enforcement
"""
import tkinter as tk
import sys

from src.presentation.avatar_counselor_gui import AvatarCounselorGUI


def main():
    """Run the Avatar Counselor GUI."""
    print("=" * 80)
    print("  FocusMotherFocus - Avatar Counselor")
    print("=" * 80)
    print()
    print("Initializing AI Productivity Counselor...")
    print()

    try:
        # Create main window
        root = tk.Tk()

        # Create GUI
        gui = AvatarCounselorGUI(root)

        print()
        print("=" * 80)
        print("  ✅ Ready! Click 'Start Monitoring' to begin")
        print("=" * 80)
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
