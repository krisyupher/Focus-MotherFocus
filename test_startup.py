"""
Test auto-startup functionality
"""

from src.infrastructure.adapters.windows_startup_manager import WindowsStartupManager

def main():
    print("=" * 60)
    print("AUTO-STARTUP TEST")
    print("=" * 60)

    # Create manager
    manager = WindowsStartupManager()

    # Get startup command
    command = manager.get_startup_command()
    print(f"\nStartup command that will be used:")
    print(f"  {command}")

    # Check current state
    is_enabled = manager.is_enabled()
    print(f"\nCurrent state: {'ENABLED' if is_enabled else 'DISABLED'}")

    # Test enable/disable
    print("\n" + "-" * 60)
    print("Testing enable...")
    print("-" * 60)

    if manager.enable():
        print("[OK] Successfully enabled auto-startup")
        print(f"     Check Registry: HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run")
        print(f"     Key name: FocusMonitor")
    else:
        print("[ERROR] Failed to enable auto-startup")

    # Verify
    if manager.is_enabled():
        print("[OK] Verified: Auto-startup is enabled")
    else:
        print("[ERROR] Verification failed: Not enabled")

    print("\n" + "-" * 60)
    print("Testing disable...")
    print("-" * 60)

    if manager.disable():
        print("[OK] Successfully disabled auto-startup")
    else:
        print("[ERROR] Failed to disable auto-startup")

    # Verify
    if not manager.is_enabled():
        print("[OK] Verified: Auto-startup is disabled")
    else:
        print("[ERROR] Verification failed: Still enabled")

    print("\n" + "=" * 60)
    print("INSTRUCTIONS FOR MANUAL TEST:")
    print("=" * 60)
    print("1. Run the main application: python main.py")
    print("2. Check the box: 'Start automatically when computer turns on'")
    print("3. Restart your computer")
    print("4. Verify the application starts automatically")
    print("=" * 60)

if __name__ == "__main__":
    main()
