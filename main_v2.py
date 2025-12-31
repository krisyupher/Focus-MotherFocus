"""
Unified Website & Application Monitor - Clean Architecture Implementation

Main entry point with unified monitoring targets.
Each target can monitor website, application, or both.
"""
import os
import tkinter as tk
from tkinter import messagebox
from src.core.entities.monitoring_session_v2 import MonitoringSessionV2
from src.application.use_cases.add_target import AddTargetUseCase
from src.application.use_cases.remove_target import RemoveTargetUseCase
from src.application.use_cases.check_targets import CheckTargetsUseCase
from src.application.use_cases.start_monitoring_v2 import StartMonitoringV2UseCase
from src.application.use_cases.stop_monitoring_v2 import StopMonitoringV2UseCase
from src.application.use_cases.generate_avatar import GenerateAvatarUseCase
from src.infrastructure.adapters import (
    RequestsHttpChecker,
    WindowsAlertNotifier,
    ThreadedScheduler
)
from src.infrastructure.adapters.windows_browser_detector import WindowsBrowserDetector
from src.infrastructure.adapters.windows_process_detector import WindowsProcessDetector
from src.infrastructure.adapters.windows_startup_manager import WindowsStartupManager
from src.infrastructure.adapters.windows_tts_service import WindowsTTSService
from src.infrastructure.adapters.opencv_face_detector import OpenCVFaceDetector
from src.infrastructure.adapters.avatar_animator import AvatarAnimator
from src.infrastructure.adapters.camera_manager import CameraManager
from src.infrastructure.storage.avatar_storage import AvatarStorage
from src.infrastructure.persistence.json_config_repository_v2 import JsonConfigRepositoryV2
from src.presentation.gui_v2 import UnifiedMonitorGUI


def create_application() -> UnifiedMonitorGUI:
    """
    Composition root - creates and wires all dependencies for unified monitoring.

    Unified Architecture:
    - Each target can monitor website, app, or both
    - Single GUI for all monitoring
    - Alert if EITHER condition is met

    Returns:
        Fully configured UnifiedMonitorGUI ready to run
    """
    # Create tkinter root window
    root = tk.Tk()

    # Infrastructure layer - Avatar components
    tts_service = WindowsTTSService()
    face_detector = OpenCVFaceDetector()
    camera_manager = CameraManager()
    avatar_storage = AvatarStorage(config_dir="config")

    # Check if avatar exists and create animator if available
    avatar_animator = None
    avatar_path = avatar_storage.get_avatar_path()
    if os.path.exists(avatar_path):
        try:
            avatar_animator = AvatarAnimator(avatar_path, tts_service)
            print(f"[MAIN] Avatar loaded from {avatar_path}")
        except Exception as e:
            print(f"[MAIN] Failed to load avatar: {e}")

    # Infrastructure layer - adapters
    config_repository = JsonConfigRepositoryV2(config_file_path="config.json")
    http_checker = RequestsHttpChecker(timeout=5)
    browser_detector = WindowsBrowserDetector()
    process_detector = WindowsProcessDetector()
    alert_notifier = WindowsAlertNotifier(
        parent_window=root,
        avatar_animator=avatar_animator
    )
    scheduler = ThreadedScheduler()
    startup_manager = WindowsStartupManager()

    # Load or create unified monitoring session
    session = config_repository.load_session()

    # Application layer - use cases
    add_target_use_case = AddTargetUseCase(
        session=session,
        config_repository=config_repository
    )

    remove_target_use_case = RemoveTargetUseCase(
        session=session,
        config_repository=config_repository,
        alert_notifier=alert_notifier
    )

    check_targets_use_case = CheckTargetsUseCase(
        session=session,
        http_checker=http_checker,
        browser_detector=browser_detector,
        process_detector=process_detector,
        alert_notifier=alert_notifier
    )

    start_monitoring_use_case = StartMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler
    )

    stop_monitoring_use_case = StopMonitoringV2UseCase(
        session=session,
        config_repository=config_repository,
        scheduler=scheduler,
        alert_notifier=alert_notifier
    )

    generate_avatar_use_case = GenerateAvatarUseCase(
        face_detector=face_detector,
        camera_manager=camera_manager,
        avatar_storage=avatar_storage
    )

    # Presentation layer - Unified GUI
    gui = UnifiedMonitorGUI(
        session=session,
        add_target_use_case=add_target_use_case,
        remove_target_use_case=remove_target_use_case,
        start_monitoring_use_case=start_monitoring_use_case,
        stop_monitoring_use_case=stop_monitoring_use_case,
        check_targets_use_case=check_targets_use_case,
        generate_avatar_use_case=generate_avatar_use_case,
        startup_manager=startup_manager,
        root=root
    )

    # Generate avatar on first launch if not exists
    if not os.path.exists(avatar_path):
        print("[MAIN] No avatar found, will prompt user to generate one")
        # Show prompt after GUI is ready
        root.after(1000, lambda: _prompt_first_avatar_generation(generate_avatar_use_case))

    return gui


def _prompt_first_avatar_generation(generate_avatar_use_case: GenerateAvatarUseCase):
    """Prompt user to generate avatar on first launch."""
    if messagebox.askyesno(
        "Welcome to Speaking Avatar Feature!",
        "This app can now speak motivational messages using your face as an avatar!\n\n"
        "Would you like to generate your avatar now?\n\n"
        "This will capture your face from the webcam.\n"
        "(You can skip and generate later from settings)"
    ):
        try:
            success = generate_avatar_use_case.execute(max_attempts=30, timeout_seconds=30)
            if success:
                messagebox.showinfo(
                    "Success",
                    "Avatar generated! Your face will appear in alerts.\n\n"
                    "Please RESTART the application for the avatar to take effect."
                )
            else:
                messagebox.showwarning(
                    "Face Not Detected",
                    "Could not detect your face.\n\n"
                    "You can try again later from the Avatar Settings section."
                )
        except Exception as e:
            messagebox.showerror("Error", f"Failed to generate avatar:\n{e}")


def main():
    """Main entry point"""
    app = create_application()
    app.run()


if __name__ == "__main__":
    main()
