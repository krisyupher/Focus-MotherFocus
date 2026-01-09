"""Test Zordon-style camera display"""
import tkinter as tk
from src.infrastructure.adapters.camera_manager import CameraManager

def test_zordon_display():
    """Display a test window with Zordon-style camera background."""
    root = tk.Tk()
    root.title("Zordon Camera Test")
    root.geometry("600x500")

    # Create camera manager
    camera = CameraManager()

    # Try to get camera background
    print("Attempting to capture camera frame...")
    camera_bg = camera.get_fullscreen_background_for_tk(width=600, height=500)

    if camera_bg:
        print("[OK] Camera background created successfully!")

        # Create background label
        bg_label = tk.Label(root, image=camera_bg)
        bg_label.image = camera_bg  # Keep reference
        bg_label.place(x=0, y=0, relwidth=1, relheight=1)

        # Add overlay text
        message_frame = tk.Frame(root, bg='#000000', highlightbackground='#00ff00',
                                highlightthickness=4)
        message_frame.place(relx=0.5, rely=0.5, anchor='center', width=520)

        message_label = tk.Label(
            message_frame,
            text="TEST: Can you see yourself?\nCamera should fill this window!",
            font=('Courier New', 16, 'bold'),
            bg='#000000',
            fg='#00ff00',
            wraplength=480,
            justify=tk.CENTER,
            pady=25,
            padx=20
        )
        message_label.pack()

        print("Window displayed. You should see:")
        print("  1. Your face filling the entire window (green tinted)")
        print("  2. A black box with green text in the center")
        print("\nClose the window when done testing.")

    else:
        print("[FAIL] Camera background is None!")
        print("Camera capture failed. Check:")
        print("  - Is another app using the camera?")
        print("  - Do you have camera permissions?")

        # Show error message
        error_label = tk.Label(
            root,
            text="CAMERA ERROR\n\nCould not capture camera feed.\nCheck if camera is available.",
            font=('Arial', 14, 'bold'),
            bg='#ff0000',
            fg='white',
            pady=50
        )
        error_label.pack(fill='both', expand=True)

    root.mainloop()

    # Cleanup
    camera.stop_camera()
    print("Test complete. Camera released.")

if __name__ == "__main__":
    test_zordon_display()
