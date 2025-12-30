"""Test camera functionality - Debug script"""
import cv2
import sys

def test_camera():
    """Test if camera is accessible and working."""
    print("Testing camera access...")
    print("-" * 50)

    # Try DirectShow backend (Windows)
    print("\n1. Trying DirectShow backend (CAP_DSHOW)...")
    camera = cv2.VideoCapture(0, cv2.CAP_DSHOW)

    if camera.isOpened():
        print("[OK] Camera opened successfully with DirectShow!")

        # Try to read a frame
        ret, frame = camera.read()
        if ret:
            print("[OK] Frame captured successfully!")
            print(f"  Frame shape: {frame.shape}")
            print(f"  Frame type: {frame.dtype}")
        else:
            print("[FAIL] Failed to capture frame")

        camera.release()
    else:
        print("[FAIL] Failed to open camera with DirectShow")

        # Try default backend
        print("\n2. Trying default backend...")
        camera = cv2.VideoCapture(0)

        if camera.isOpened():
            print("[OK] Camera opened with default backend!")
            ret, frame = camera.read()
            if ret:
                print(f"[OK] Frame captured!")
                print(f"  Frame shape: {frame.shape}")
            else:
                print("[FAIL] Failed to capture frame")
            camera.release()
        else:
            print("[FAIL] Camera completely inaccessible")
            print("\nPossible issues:")
            print("  - Camera is being used by another application")
            print("  - Camera privacy settings block access")
            print("  - No camera device found")
            print("  - Camera driver issue")
            return False

    print("\n" + "=" * 50)
    print("Camera test PASSED!")
    print("=" * 50)
    return True

if __name__ == "__main__":
    success = test_camera()
    sys.exit(0 if success else 1)
