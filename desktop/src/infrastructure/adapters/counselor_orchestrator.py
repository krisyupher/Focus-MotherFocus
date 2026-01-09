"""
Counselor Orchestrator - Coordinates all MCP services for interventions.

This is the central coordinator that brings together:
- Behavioral analysis
- Avatar display
- Voice synthesis
- User negotiation
- Agreement storage
"""
import cv2
import numpy as np
from typing import Optional, Callable
from datetime import datetime

from src.application.interfaces.i_behavioral_analyzer import BehavioralEvent
from src.application.use_cases.negotiate_agreement import NegotiateAgreementUseCase
from src.core.entities.agreement import Agreement
from src.presentation.avatar_counselor_window import AvatarCounselorWindow
from src.infrastructure.adapters.counselor_voice_service import CounselorVoiceService


class CounselorOrchestrator:
    """
    Orchestrates full counselor intervention workflow.

    Coordinates:
    1. Webcam MCP - Capture user's face
    2. HeyGen MCP - Generate avatar video (optional)
    3. ElevenLabs MCP - Voice synthesis
    4. Avatar Window - Display fullscreen intervention
    5. Negotiation - Multi-turn dialogue
    6. Memory MCP - Store agreements
    """

    def __init__(
        self,
        webcam_mcp=None,
        heygen_mcp=None,
        elevenlabs_mcp=None,
        memory_mcp=None,
        parent_window=None
    ):
        """
        Initialize counselor orchestrator.

        Args:
            webcam_mcp: WebcamMCP instance (optional)
            heygen_mcp: HeyGenMCP instance (optional)
            elevenlabs_mcp: ElevenLabsMCP instance (optional)
            memory_mcp: MemoryMCP instance (optional)
            parent_window: Parent tkinter window
        """
        self.webcam_mcp = webcam_mcp
        self.heygen_mcp = heygen_mcp
        self.elevenlabs_mcp = elevenlabs_mcp
        self.memory_mcp = memory_mcp
        self.parent_window = parent_window

        # Initialize services
        self.voice_service = CounselorVoiceService(
            elevenlabs_mcp=elevenlabs_mcp,
            voice_name="alloy"
        )

        self.avatar_window = AvatarCounselorWindow(parent=parent_window)
        self.negotiation_use_case = NegotiateAgreementUseCase(max_negotiation_rounds=3)

        # State
        self.current_agreement: Optional[Agreement] = None
        self.active_agreements: list[Agreement] = []

    def execute_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        on_complete: Optional[Callable[[Optional[Agreement]], None]] = None
    ) -> None:
        """
        Execute full counselor intervention workflow.

        Args:
            event: Behavioral event that triggered intervention
            recommendation: Intervention recommendation from trigger use case
            on_complete: Callback when intervention completes (receives Agreement or None)
        """
        print(f"\n[Counselor] Starting intervention for {event.event_type}")

        # 1. Capture user's face (if webcam available)
        user_face = self._capture_user_face()

        # 2. Generate avatar frame (optional - can use static image)
        avatar_frame = self._get_avatar_frame()

        # 3. Determine intervention type
        intervention_type = recommendation.get('type', 'alert')
        print(f"[Counselor] Intervention type: {intervention_type}")

        if intervention_type == 'block':
            self._execute_block_intervention(event, recommendation, user_face, avatar_frame, on_complete)

        elif intervention_type == 'negotiate':
            self._execute_negotiation_intervention(event, recommendation, user_face, avatar_frame, on_complete)

        elif intervention_type == 'alert':
            self._execute_alert_intervention(event, recommendation, user_face, avatar_frame, on_complete)

        else:
            print(f"[Counselor] Unknown intervention type: {intervention_type}")
            if on_complete:
                on_complete(None)

    def _execute_block_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        user_face: Optional[np.ndarray],
        avatar_frame: Optional[np.ndarray],
        on_complete: Optional[Callable]
    ) -> None:
        """Execute immediate block intervention (adult content, etc.)."""
        message = recommendation.get('message', 'This activity must stop immediately.')

        # Speak message if voice enabled
        if recommendation.get('use_voice') and self.voice_service.is_available():
            print("[Counselor] Speaking block message...")
            self.voice_service.speak(message, blocking=False)

        # Show avatar window
        if recommendation.get('show_avatar'):
            print("[Counselor] Showing block intervention window...")

            def on_user_acknowledges(response: str):
                print(f"[Counselor] User acknowledged: {response}")

                # Create agreement (immediate stop)
                agreement = Agreement.create(
                    event_type=event.event_type,
                    url=event.url,
                    process_name=event.process_name,
                    agreed_duration_minutes=0.0,
                    user_response=response or "acknowledged",
                    counselor_message=message
                )

                self._store_agreement(agreement)

                if on_complete:
                    on_complete(agreement)

            # Update recommendation for block display
            block_recommendation = recommendation.copy()
            block_recommendation['message'] = f"{message}\n\nClick CLOSE to acknowledge."

            self.avatar_window.show_intervention(
                event=event,
                recommendation=block_recommendation,
                user_face=user_face,
                avatar_frame=avatar_frame,
                on_response=on_user_acknowledges
            )
        else:
            # No avatar, just callback
            if on_complete:
                on_complete(None)

    def _execute_negotiation_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        user_face: Optional[np.ndarray],
        avatar_frame: Optional[np.ndarray],
        on_complete: Optional[Callable]
    ) -> None:
        """Execute negotiation intervention with multi-turn dialogue."""
        # Start negotiation
        self.negotiation_use_case.reset()
        opening_message, requires_response = self.negotiation_use_case.start_negotiation(
            event, recommendation
        )

        print(f"[Counselor] Negotiation started: {opening_message[:50]}...")

        # Speak opening message
        if recommendation.get('use_voice') and self.voice_service.is_available():
            self.voice_service.speak(opening_message, blocking=False)

        # Show avatar window with negotiation input
        def on_user_response(response: str):
            print(f"[Counselor] User response: {response}")

            # Process response
            next_message, continue_negotiating, agreement = self.negotiation_use_case.process_user_response(
                user_response=response,
                event=event,
                original_message=opening_message
            )

            if agreement:
                # Agreement reached
                print(f"[Counselor] Agreement reached: {agreement}")

                # Speak final message
                if self.voice_service.is_available():
                    final_message = agreement.counselor_message
                    self.voice_service.speak(final_message, blocking=False)

                # Store agreement
                self._store_agreement(agreement)
                self.current_agreement = agreement
                self.active_agreements.append(agreement)

                # Close window
                self.avatar_window.close()

                if on_complete:
                    on_complete(agreement)

            elif continue_negotiating:
                # Continue negotiation
                print(f"[Counselor] Continuing negotiation: {next_message[:50]}...")

                # Speak next message
                if self.voice_service.is_available():
                    self.voice_service.speak(next_message, blocking=False)

                # Update window message
                updated_recommendation = recommendation.copy()
                updated_recommendation['message'] = next_message

                self.avatar_window.show_intervention(
                    event=event,
                    recommendation=updated_recommendation,
                    user_face=user_face,
                    avatar_frame=avatar_frame,
                    on_response=on_user_response  # Same callback for next round
                )

            else:
                # Negotiation ended without agreement (shouldn't happen)
                print("[Counselor] Negotiation ended without agreement")
                self.avatar_window.close()
                if on_complete:
                    on_complete(None)

        # Show initial negotiation window
        self.avatar_window.show_intervention(
            event=event,
            recommendation={'type': 'negotiate', 'message': opening_message, **recommendation},
            user_face=user_face,
            avatar_frame=avatar_frame,
            on_response=on_user_response
        )

    def _execute_alert_intervention(
        self,
        event: BehavioralEvent,
        recommendation: dict,
        user_face: Optional[np.ndarray],
        avatar_frame: Optional[np.ndarray],
        on_complete: Optional[Callable]
    ) -> None:
        """Execute gentle alert intervention (notification only)."""
        message = recommendation.get('message', 'Stay focused on your goals.')

        print(f"[Counselor] Alert: {message}")

        # For alerts, we can just show a simpler window without negotiation
        # Or print to console if no avatar needed
        if recommendation.get('show_avatar'):
            def on_close(response: str):
                if on_complete:
                    on_complete(None)

            self.avatar_window.show_intervention(
                event=event,
                recommendation=recommendation,
                user_face=user_face,
                avatar_frame=avatar_frame,
                on_response=on_close
            )
        else:
            # Just print alert
            print(f"[ALERT] {message}")
            if on_complete:
                on_complete(None)

    def _capture_user_face(self) -> Optional[np.ndarray]:
        """Capture user's face from webcam."""
        if not self.webcam_mcp:
            return None

        try:
            print("[Counselor] Capturing user face...")
            image_data = self.webcam_mcp.capture_image()

            if image_data:
                # Convert bytes to numpy array
                nparr = np.frombuffer(image_data, np.uint8)
                img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                print(f"[Counselor] User face captured: {img.shape if img is not None else 'None'}")
                return img
            else:
                print("[Counselor] No image data returned from webcam")
                return None

        except Exception as e:
            print(f"[Counselor] Failed to capture user face: {e}")
            return None

    def _get_avatar_frame(self) -> Optional[np.ndarray]:
        """
        Get avatar frame.

        For now, returns None (could integrate HeyGen MCP for dynamic avatar generation).
        """
        # TODO: Integrate HeyGen MCP for dynamic avatar generation
        # For now, we can use a static counselor image if available
        return None

    def _store_agreement(self, agreement: Agreement) -> None:
        """Store agreement in Memory MCP."""
        if not self.memory_mcp:
            print(f"[Counselor] No Memory MCP available, agreement not stored: {agreement}")
            return

        try:
            # Convert agreement to dict for storage
            agreement_data = {
                'id': agreement.id,
                'event_type': agreement.event_type,
                'url': agreement.url,
                'process_name': agreement.process_name,
                'agreed_duration_minutes': agreement.agreed_duration_minutes,
                'created_at': agreement.created_at.isoformat(),
                'expires_at': agreement.expires_at.isoformat(),
                'user_response': agreement.user_response,
                'counselor_message': agreement.counselor_message,
                'is_active': agreement.is_active
            }

            # Store in Memory MCP using add_event
            self.memory_mcp.add_event(
                event_type='agreement',
                payload=agreement_data,
                timestamp=datetime.now().timestamp()
            )

            print(f"[Counselor] Agreement stored: {agreement.id}")

        except Exception as e:
            print(f"[Counselor] Failed to store agreement: {e}")

    def get_active_agreements(self) -> list[Agreement]:
        """Get all active agreements."""
        return [a for a in self.active_agreements if a.is_active and not a.is_expired()]

    def check_agreement_compliance(self, event: BehavioralEvent) -> Optional[Agreement]:
        """
        Check if current activity violates an active agreement.

        Args:
            event: Current behavioral event

        Returns:
            Violated agreement if found, None otherwise
        """
        for agreement in self.get_active_agreements():
            # Check if event matches agreement target
            matches = False

            if agreement.url and event.url and agreement.url in event.url:
                matches = True
            elif agreement.process_name and event.process_name and agreement.process_name in event.process_name:
                matches = True

            if matches and agreement.is_expired():
                # Agreement time expired - violation!
                agreement.mark_violated()
                print(f"[Counselor] Agreement violated: {agreement}")
                return agreement

        return None

    def cleanup(self) -> None:
        """Cleanup resources."""
        if self.avatar_window:
            self.avatar_window.close()

        if self.voice_service:
            self.voice_service.stop()
