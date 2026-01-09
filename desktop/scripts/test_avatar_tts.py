"""Test Avatar + TTS independently"""
import os
import tkinter as tk
from src.infrastructure.adapters.windows_tts_service import WindowsTTSService
from src.infrastructure.adapters.avatar_animator import AvatarAnimator
from src.infrastructure.storage.avatar_storage import AvatarStorage

def test_avatar_and_tts():
    """Test if avatar and TTS work."""
    print("=" * 60)
    print("AVATAR + TTS TEST")
    print("=" * 60)

    # Check if avatar exists
    avatar_storage = AvatarStorage(config_dir="config")
    avatar_path = avatar_storage.get_avatar_path()

    print(f"\n1. Checking avatar file...")
    print(f"   Path: {avatar_path}")
    print(f"   Exists: {os.path.exists(avatar_path)}")

    if not os.path.exists(avatar_path):
        print("\n[FAIL] NO AVATAR FOUND!")
        print("   Please generate avatar first using the main app.")
        return False

    # Try to load avatar
    print(f"\n2. Loading avatar image...")
    avatar_img = avatar_storage.load_avatar()
    if avatar_img is None:
        print("   [FAIL] Failed to load avatar!")
        return False
    print(f"   [OK] Avatar loaded: {avatar_img.shape}")

    # Create TTS service
    print(f"\n3. Creating TTS service...")
    try:
        tts_service = WindowsTTSService()
        print("   [OK] TTS service created")
    except Exception as e:
        print(f"   [FAIL] TTS creation failed: {e}")
        return False

    # Create avatar animator
    print(f"\n4. Creating avatar animator...")
    try:
        avatar_animator = AvatarAnimator(avatar_path, tts_service)
        print("   [OK] Avatar animator created")
    except Exception as e:
        print(f"   [FAIL] Avatar animator creation failed: {e}")
        import traceback
        traceback.print_exc()
        return False

    # Test TTS
    print(f"\n5. Testing TTS...")
    print("   You should hear: 'Hello, this is a test message'")
    try:
        tts_service.speak("Hello, this is a test message", blocking=True)
        print("   [OK] TTS played successfully")
    except Exception as e:
        print(f"   [FAIL] TTS failed: {e}")
        import traceback
        traceback.print_exc()
        return False

    # Test animated frame generation
    print(f"\n6. Testing animated frame generation...")
    try:
        # Start speaking to trigger animation
        avatar_animator.start_speaking("Don't waste your time, come back to your work!")

        # Generate a few frames
        for i in range(5):
            frame = avatar_animator.get_current_frame_for_tk(width=600, height=500)
            if frame:
                print(f"   [OK] Frame {i+1}: Generated successfully (type: {type(frame)})")
            else:
                print(f"   [FAIL] Frame {i+1}: None returned!")

        # Stop animation
        avatar_animator.stop_animation()
        print("   [OK] Animation test complete")

    except Exception as e:
        print(f"   [FAIL] Frame generation failed: {e}")
        import traceback
        traceback.print_exc()
        return False

    # Visual test
    print(f"\n7. Opening visual test window...")
    print("   A window will open showing your animated avatar.")
    print("   The avatar should speak and animate.")
    print("   Close the window when done.")

    try:
        root = tk.Tk()
        root.title("Avatar Animation Test")
        root.geometry("600x500")

        # Background label
        bg_label = tk.Label(root)
        bg_label.pack(fill=tk.BOTH, expand=True)

        # Start speaking
        avatar_animator.start_speaking("Don't waste your time, come back to your work!")

        # Animation loop
        def update_frame():
            frame = avatar_animator.get_current_frame_for_tk(width=600, height=500)
            if frame:
                bg_label.configure(image=frame)
                bg_label.image = frame
            root.after(67, update_frame)  # 15 FPS

        update_frame()

        # Close handler
        def on_close():
            avatar_animator.stop_animation()
            root.destroy()

        root.protocol("WM_DELETE_WINDOW", on_close)

        print("   Window opened!")
        root.mainloop()

    except Exception as e:
        print(f"   [FAIL] Visual test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

    print("\n" + "=" * 60)
    print("[OK] ALL TESTS PASSED!")
    print("=" * 60)
    return True

if __name__ == "__main__":
    import sys
    success = test_avatar_and_tts()
    sys.exit(0 if success else 1)
