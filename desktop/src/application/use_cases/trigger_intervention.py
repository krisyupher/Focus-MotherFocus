"""
Trigger Intervention Use Case.

Monitors behavioral patterns and triggers counselor interventions
when unproductive behavior is detected.
"""
from typing import Optional, Callable
from datetime import datetime

from src.application.interfaces.i_behavioral_analyzer import (
    IBehavioralAnalyzer,
    BehavioralEvent,
    BehavioralPattern
)


class TriggerInterventionUseCase:
    """
    Use case for triggering counselor interventions.

    Flow:
    1. Continuously monitor behavioral patterns
    2. Detect intervention-worthy events
    3. Trigger appropriate intervention (alert, avatar counselor, etc.)
    4. Log intervention history
    """

    def __init__(
        self,
        behavioral_analyzer: IBehavioralAnalyzer,
        intervention_callback: Optional[Callable[[BehavioralEvent], None]] = None
    ):
        """
        Initialize intervention trigger.

        Args:
            behavioral_analyzer: Analyzer for detecting behavioral patterns
            intervention_callback: Function to call when intervention is needed
                                 Should accept BehavioralEvent as parameter
        """
        self.behavioral_analyzer = behavioral_analyzer
        self.intervention_callback = intervention_callback
        self._intervention_history: list[tuple[datetime, BehavioralEvent]] = []
        self._cooldown_seconds = 60  # Don't intervene more than once per minute

    def execute(self) -> Optional[BehavioralEvent]:
        """
        Execute behavioral monitoring and trigger interventions.

        Returns:
            BehavioralEvent if intervention was triggered, None otherwise
        """
        # Analyze current activity
        event = self.behavioral_analyzer.analyze_current_activity()

        if not event:
            return None

        # Check if intervention is needed
        if not event.should_trigger_intervention:
            return None

        # Check cooldown - don't intervene too frequently
        if self._is_in_cooldown():
            print(f"[Intervention] Cooldown active, skipping intervention for {event.event_type}")
            return None

        # Log intervention
        self._intervention_history.append((datetime.now(), event))

        # Trigger intervention callback
        if self.intervention_callback:
            try:
                self.intervention_callback(event)
                print(f"[Intervention] Triggered for {event.event_type} (severity: {event.severity})")
            except Exception as e:
                print(f"[Intervention] Error executing callback: {e}")

        return event

    def get_intervention_recommendation(self, event: BehavioralEvent) -> dict:
        """
        Get recommended intervention strategy for an event.

        Returns:
            Dictionary with intervention details:
            - type: "alert", "avatar", "block", "negotiate"
            - message: What to say to user
            - action: What action to take
            - urgency: "low", "medium", "high"
        """
        if event.event_type == "adult_content":
            return {
                'type': 'block',
                'message': "I've detected you're viewing inappropriate content. Let's take a break.",
                'action': 'close_tab_immediately',
                'urgency': 'high',
                'show_avatar': True,
                'use_voice': True
            }

        elif event.event_type == "endless_scrolling":
            if event.duration_seconds > 300:  # 5+ minutes
                return {
                    'type': 'negotiate',
                    'message': f"You've been scrolling for {int(event.duration_seconds/60)} minutes. How much longer do you need?",
                    'action': 'start_negotiation',
                    'urgency': 'medium',
                    'show_avatar': True,
                    'use_voice': True
                }
            else:
                return {
                    'type': 'alert',
                    'message': "I notice you're scrolling. Let me know if you need help staying focused.",
                    'action': 'show_notification',
                    'urgency': 'low',
                    'show_avatar': False,
                    'use_voice': False
                }

        elif event.event_type == "distraction_site":
            category = event.metadata.get('category', 'unknown')
            return {
                'type': 'negotiate',
                'message': f"You're on {category}. Is this for work or leisure? Let's set a time limit.",
                'action': 'start_negotiation',
                'urgency': 'medium',
                'show_avatar': True,
                'use_voice': True
            }

        else:
            return {
                'type': 'alert',
                'message': "I'm monitoring your activity to help you stay focused.",
                'action': 'show_notification',
                'urgency': 'low',
                'show_avatar': False,
                'use_voice': False
            }

    def analyze_patterns(self, lookback_minutes: int = 60) -> list[BehavioralPattern]:
        """
        Analyze recent behavioral patterns.

        Args:
            lookback_minutes: How far back to analyze

        Returns:
            List of detected patterns with recommendations
        """
        return self.behavioral_analyzer.get_patterns(lookback_minutes)

    def get_intervention_history(self, limit: int = 10) -> list[tuple[datetime, BehavioralEvent]]:
        """
        Get recent intervention history.

        Args:
            limit: Maximum number of interventions to return

        Returns:
            List of (timestamp, event) tuples
        """
        return self._intervention_history[-limit:]

    def _is_in_cooldown(self) -> bool:
        """Check if we're in cooldown period since last intervention."""
        if not self._intervention_history:
            return False

        last_intervention_time = self._intervention_history[-1][0]
        elapsed = (datetime.now() - last_intervention_time).total_seconds()
        return elapsed < self._cooldown_seconds

    def set_cooldown(self, seconds: int) -> None:
        """Set the cooldown period between interventions."""
        self._cooldown_seconds = seconds

    def clear_history(self) -> None:
        """Clear intervention history."""
        self._intervention_history.clear()
