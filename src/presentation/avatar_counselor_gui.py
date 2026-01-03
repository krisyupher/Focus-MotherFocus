"""
Avatar Counselor GUI - Minimal interface with interactive avatar.

Complete integration of all 4 phases:
- Phase 1: Behavioral analysis and detection
- Phase 2: Avatar counselor with voice interaction
- Phase 3: Agreement enforcement
- Phase 4: MCP service orchestration
"""
import tkinter as tk
from tkinter import ttk
from typing import Optional
import threading
import time
from PIL import Image, ImageTk
import numpy as np
import cv2

from src.infrastructure.adapters.mcp_service_factory import MCPServiceFactory
from src.application.use_cases.orchestrate_mcp_services import OrchestrateMCPServicesUseCase
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase
from src.application.use_cases.negotiate_agreement import NegotiateAgreementUseCase
from src.application.use_cases.track_agreements import TrackAgreementsUseCase
from src.application.use_cases.enforce_agreement import EnforceAgreementUseCase
from src.infrastructure.adapters.counselor_voice_service import CounselorVoiceService
from src.infrastructure.adapters.enforcement_notifier import EnforcementNotifier
from src.presentation.countdown_timer_widget import CountdownTimerWidget


class AvatarCounselorGUI:
    """
    Minimal GUI with interactive avatar counselor.

    Features:
    - Single "Start Monitoring" button
    - Full-screen avatar display (no camera)
    - Voice-based interaction
    - Automatic behavioral monitoring
    - Smart intervention and enforcement
    """

    def __init__(self, root: tk.Tk):
        """Initialize avatar counselor GUI."""
        self._root = root
        self._monitoring = False
        self._monitoring_thread: Optional[threading.Thread] = None

        # Phase 4: Initialize MCP orchestration
        print("\n[GUI] Initializing MCP Service Orchestration...")
        factory = MCPServiceFactory()
        self.registry = factory.create_registry()
        self.orchestrator = OrchestrateMCPServicesUseCase(self.registry)

        # Get service status
        status = self.orchestrator.get_service_status()
        print(f"[GUI] Services: {status['summary']['available']}/{status['summary']['total']} available")

        # Initialize services through orchestrator
        self._initialize_services()

        # Phase 1: Behavioral analysis
        self.behavioral_analyzer = MCPBehavioralAnalyzer(
            browser_mcp=self._get_service('browser_tools'),
            windows_mcp=self._get_service('windows')
        )
        self.intervention_trigger = TriggerInterventionUseCase(
            behavioral_analyzer=self.behavioral_analyzer
        )

        # Phase 2: Agreement negotiation
        self.negotiation = NegotiateAgreementUseCase(max_negotiation_rounds=3)

        # Phase 3: Agreement tracking and enforcement
        self.agreement_tracker = TrackAgreementsUseCase(
            grace_period_seconds=30.0,
            warning_before_seconds=60.0
        )

        # Browser controller (optional - for tab auto-close)
        browser_controller = self._get_service('playwright')
        if browser_controller:
            print("[GUI] Tab auto-close enabled")
        else:
            print("[GUI] Tab auto-close disabled (Playwright not available)")

        self.enforcement = EnforceAgreementUseCase(
            browser_controller=browser_controller,
            grace_period_seconds=30.0
        )
        self.notifier = EnforcementNotifier(
            notifymemaybe=self._get_service('notify'),
            voice_service=self.voice_service
        )

        # Countdown timer
        self.countdown_timer: Optional[CountdownTimerWidget] = None

        # Avatar display
        self._avatar_label: Optional[tk.Label] = None
        self._avatar_frame: Optional[np.ndarray] = None

        # Setup window
        self._setup_window()
        self._create_widgets()

        print("[GUI] ✅ Avatar Counselor GUI initialized")
        print("[GUI] All 4 phases integrated and ready")

    def _initialize_services(self) -> None:
        """Initialize services through orchestrator."""
        # Voice service with fallback
        elevenlabs = self._get_service('elevenlabs')
        windows_tts = self._get_service('windows')

        if elevenlabs:
            print("[GUI] Using ElevenLabs for voice synthesis")
            self.voice_service = CounselorVoiceService(
                elevenlabs_mcp=elevenlabs,
                voice_name="Dorothy"
            )
        elif windows_tts:
            print("[GUI] Using Windows TTS for voice synthesis")
            self.voice_service = CounselorVoiceService(
                elevenlabs_mcp=None,
                voice_name=None
            )
        else:
            print("[GUI] No voice service available")
            self.voice_service = None

        # Webcam (for future use)
        self._webcam = self._get_service('webcam')

        # Memory for agreement storage
        self._memory = self._get_service('memory')

        print(f"[GUI] Voice: {'✓' if self.voice_service else '✗'}")
        print(f"[GUI] Webcam: {'✓' if self._webcam else '✗'}")
        print(f"[GUI] Memory: {'✓' if self._memory else '✗'}")

    def _get_service(self, service_name: str):
        """Get service through orchestrator."""
        from src.application.interfaces.i_mcp_service_registry import ServiceType

        service_map = {
            'browser_tools': ServiceType.BROWSER_TOOLS,
            'webcam': ServiceType.WEBCAM,
            'heygen': ServiceType.HEYGEN,
            'elevenlabs': ServiceType.ELEVENLABS,
            'memory': ServiceType.MEMORY,
            'filesystem': ServiceType.FILESYSTEM,
            'windows': ServiceType.WINDOWS,
            'notify': ServiceType.NOTIFY,
            'playwright': ServiceType.PLAYWRIGHT
        }

        service_type = service_map.get(service_name)
        if service_type:
            return self.registry.get_service(service_type)
        return None

    def _setup_window(self) -> None:
        """Configure main window."""
        self._root.title("FocusMotherFocus - Avatar Counselor")
        self._root.geometry("800x600")
        self._root.configure(bg='#0a0a0a')

    def _create_widgets(self) -> None:
        """Create minimal GUI widgets."""
        # Main container
        main_frame = tk.Frame(self._root, bg='#0a0a0a')
        main_frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)

        # Title
        title = tk.Label(
            main_frame,
            text="🎯 FocusMotherFocus",
            font=("Arial", 24, "bold"),
            fg='#00FF41',
            bg='#0a0a0a'
        )
        title.pack(pady=(0, 20))

        # Subtitle
        subtitle = tk.Label(
            main_frame,
            text="AI Productivity Counselor with Avatar",
            font=("Arial", 12),
            fg='#00FF41',
            bg='#0a0a0a'
        )
        subtitle.pack(pady=(0, 30))

        # Avatar display area
        avatar_frame = tk.Frame(main_frame, bg='#1a1a1a', width=640, height=360)
        avatar_frame.pack(pady=20, fill=tk.BOTH, expand=True)
        avatar_frame.pack_propagate(False)

        self._avatar_label = tk.Label(
            avatar_frame,
            text="👤\n\nAvatar will appear here\n\nClick 'Start Monitoring' to begin",
            font=("Arial", 16),
            fg='#00FF41',
            bg='#1a1a1a'
        )
        self._avatar_label.pack(expand=True)

        # Start/Stop button
        self._start_button = tk.Button(
            main_frame,
            text="▶ Start Monitoring",
            font=("Arial", 14, "bold"),
            fg='white',
            bg='#00AA00',
            activebackground='#00FF00',
            activeforeground='white',
            cursor='hand2',
            padx=40,
            pady=15,
            command=self._toggle_monitoring
        )
        self._start_button.pack(pady=20)

        # Status label
        self._status_label = tk.Label(
            main_frame,
            text="Ready to start",
            font=("Arial", 10),
            fg='#888888',
            bg='#0a0a0a'
        )
        self._status_label.pack()

        # Service status
        status = self.orchestrator.get_service_status()
        service_text = f"Services: {status['summary']['available']}/{status['summary']['total']} available"

        service_label = tk.Label(
            main_frame,
            text=service_text,
            font=("Arial", 9),
            fg='#555555',
            bg='#0a0a0a'
        )
        service_label.pack(pady=(5, 0))

    def _toggle_monitoring(self) -> None:
        """Toggle monitoring on/off."""
        if not self._monitoring:
            self._start_monitoring()
        else:
            self._stop_monitoring()

    def _start_monitoring(self) -> None:
        """Start monitoring behavioral patterns."""
        self._monitoring = True
        self._start_button.config(
            text="⏸ Stop Monitoring",
            bg='#AA0000',
            activebackground='#FF0000'
        )
        self._status_label.config(text="Monitoring active - Watching for patterns...")

        # Start monitoring thread
        self._monitoring_thread = threading.Thread(target=self._monitoring_loop, daemon=True)
        self._monitoring_thread.start()

        # Speak greeting
        if self.voice_service:
            self.voice_service.speak(
                "Hello! I'm your productivity counselor. I'll help you stay focused.",
                blocking=False
            )

        # Update avatar
        self._update_avatar_display("Monitoring your activity...")

        print("[GUI] ✅ Monitoring started")

    def _stop_monitoring(self) -> None:
        """Stop monitoring."""
        self._monitoring = False
        self._start_button.config(
            text="▶ Start Monitoring",
            bg='#00AA00',
            activebackground='#00FF00'
        )
        self._status_label.config(text="Monitoring stopped")

        # Hide countdown timer if showing
        if self.countdown_timer and self.countdown_timer.is_visible():
            self.countdown_timer.hide()

        # Update avatar
        self._update_avatar_display("Monitoring stopped. Click Start to resume.")

        print("[GUI] Monitoring stopped")

    def _monitoring_loop(self) -> None:
        """Main monitoring loop - runs in background thread."""
        print("[GUI] Monitoring loop started")

        while self._monitoring:
            try:
                # Phase 1: Analyze current activity
                event = self.behavioral_analyzer.analyze_current_activity()

                if event:
                    print(f"[GUI] Event detected: {event.event_type} (severity: {event.severity})")

                    # Check if intervention needed
                    intervention_event = self.intervention_trigger.execute()

                    if intervention_event:
                        # Get recommendation for how to handle this event
                        recommendation = self.intervention_trigger.get_intervention_recommendation(intervention_event)
                        print(f"[GUI] Intervention recommended: {recommendation['type']}")

                        # Phase 2: Trigger intervention based on type
                        if recommendation['type'] == 'block':
                            self._handle_block_intervention(intervention_event, recommendation)
                        elif recommendation['type'] == 'negotiate':
                            self._handle_negotiate_intervention(intervention_event, recommendation)

                # Phase 3: Check agreement compliance
                self.agreement_tracker.check_compliance(
                    current_event=event,
                    on_warning=self._on_agreement_warning,
                    on_expired=self._on_agreement_expired,
                    on_violation=self._on_agreement_violation
                )

                # Sleep before next check
                time.sleep(5.0)

            except Exception as e:
                print(f"[GUI] Monitoring error: {e}")
                time.sleep(5.0)

        print("[GUI] Monitoring loop ended")

    def _handle_block_intervention(self, event, recommendation) -> None:
        """Handle immediate block intervention (adult content)."""
        print(f"[GUI] Blocking: {event.url}")

        # Update avatar
        self._update_avatar_display("⛔ Blocking inappropriate content")

        # Speak message
        if self.voice_service:
            self.voice_service.speak("This content is not appropriate. Closing now.", blocking=False)

        # Close tab immediately
        if self.enforcement.browser_controller:
            try:
                self.enforcement.browser_controller.close_tab_by_url(event.url)
                print("[GUI] ✅ Tab closed")
            except Exception as e:
                print(f"[GUI] Failed to close tab: {e}")

    def _handle_negotiate_intervention(self, event, recommendation) -> None:
        """Handle negotiation intervention (endless scrolling, distractions)."""
        print(f"[GUI] Starting negotiation for: {event.event_type}")

        # Update avatar
        self._update_avatar_display(f"💬 Noticed you've been {event.event_type.replace('_', ' ')}...")

        # Start negotiation
        negotiation_message = self.negotiation.start_negotiation(event)

        # Speak to user
        if self.voice_service:
            self.voice_service.speak(negotiation_message, blocking=False)

        # For demo: auto-accept 10 minutes (in real app, would wait for user input)
        time.sleep(2)
        user_response = "10 minutes"

        # Process response
        response_message, is_complete, agreement = self.negotiation.process_user_response(
            user_response,
            event,
            negotiation_message
        )

        if is_complete and agreement:
            print(f"[GUI] ✅ Agreement created: {agreement.agreed_duration_minutes} minutes")

            # Speak confirmation
            if self.voice_service:
                self.voice_service.speak(response_message, blocking=False)

            # Phase 3: Add to tracking
            self.agreement_tracker.add_agreement(agreement)

            # Show countdown timer
            if not self.countdown_timer:
                self.countdown_timer = CountdownTimerWidget(parent=self._root)

            self.countdown_timer.show(
                agreement=agreement,
                on_extend=self._on_extend_request,
                on_dismiss=lambda agr: self.countdown_timer.hide()
            )

            # Store in memory
            if self._memory:
                try:
                    self._memory.add_event({
                        'type': 'agreement_created',
                        'event_type': agreement.event_type,
                        'duration_minutes': agreement.agreed_duration_minutes,
                        'url': agreement.url,
                        'timestamp': agreement.created_at.isoformat()
                    })
                except Exception as e:
                    print(f"[GUI] Failed to store in memory: {e}")

    def _on_agreement_warning(self, agreement, seconds_remaining: float) -> None:
        """Handle agreement warning (approaching expiration)."""
        print(f"[GUI] ⏰ Warning: {seconds_remaining}s remaining for {agreement.url}")

        # Update avatar
        minutes = int(seconds_remaining / 60)
        self._update_avatar_display(f"⏰ {minutes} minute(s) remaining")

        # Send notification
        self.notifier.send_warning(
            f"{minutes} minute(s) remaining on your {agreement.event_type.replace('_', ' ')} agreement",
            speak=True
        )

    def _on_agreement_expired(self, agreement) -> None:
        """Handle agreement expiration."""
        print(f"[GUI] 🕐 Agreement expired: {agreement.url}")

        # Update avatar
        self._update_avatar_display("🕐 Time's up! Wrapping up...")

        # Start grace period
        self.notifier.send_grace_period_notification(
            "Time's up! Please wrap up in the next 30 seconds.",
            seconds_remaining=30.0,
            speak=True
        )

    def _on_agreement_violation(self, agreement) -> None:
        """Handle agreement violation (continued after grace period)."""
        print(f"[GUI] 🚫 Violation detected: {agreement.url}")

        # Update avatar
        self._update_avatar_display("🚫 Closing tab - agreement time exceeded")

        # Enforce closure
        self.enforcement.enforce(
            agreement,
            force=True,
            on_enforced=lambda msg: print(f"[GUI] {msg}")
        )

        # Send notification
        self.notifier.send_enforcement_notification(
            f"Tab closed: {agreement.url}",
            speak=True
        )

        # Hide countdown timer
        if self.countdown_timer and self.countdown_timer.is_visible():
            self.countdown_timer.hide()

    def _on_extend_request(self, agreement) -> None:
        """Handle extension request."""
        print(f"[GUI] Extension requested for {agreement.url}")

        # Ask for extension
        extension = self.notifier.request_extension(
            current_duration_minutes=agreement.agreed_duration_minutes
        )

        if extension:
            print(f"[GUI] ✅ Extension granted: {extension} minutes")

            # Extend agreement
            agreement.extend(extension)

            # Update countdown timer
            if self.countdown_timer:
                self.countdown_timer.update_agreement(agreement)

            # Speak confirmation
            if self.voice_service:
                self.voice_service.speak(
                    f"Okay, {int(extension)} more minutes granted. Stay focused!",
                    blocking=False
                )
        else:
            print("[GUI] Extension denied")

    def _update_avatar_display(self, message: str) -> None:
        """Update avatar display with message."""
        if self._avatar_label:
            self._avatar_label.config(text=f"👤\n\n{message}")

    def run(self) -> None:
        """Run the GUI main loop."""
        print("[GUI] Starting main loop...")
        self._root.mainloop()
