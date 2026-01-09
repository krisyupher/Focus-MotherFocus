"""
Tests for behavioral analyzer interface and data classes.
"""
import pytest
from datetime import datetime

from src.application.interfaces.i_behavioral_analyzer import (
    BehavioralEvent,
    BehavioralPattern
)


class TestBehavioralEvent:
    """Test BehavioralEvent value object."""

    def test_create_event(self):
        """Test creating a behavioral event."""
        event = BehavioralEvent(
            event_type="endless_scrolling",
            severity="medium",
            url="https://reddit.com",
            process_name=None,
            duration_seconds=120.0,
            detected_at=datetime.now(),
            metadata={'scroll_distance': 5000}
        )

        assert event.event_type == "endless_scrolling"
        assert event.severity == "medium"
        assert event.url == "https://reddit.com"
        assert event.duration_seconds == 120.0

    def test_high_severity_triggers_intervention(self):
        """High severity events always trigger intervention."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://example.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )

        assert event.should_trigger_intervention is True

    def test_medium_severity_with_duration_triggers(self):
        """Medium severity with >30s duration triggers intervention."""
        event = BehavioralEvent(
            event_type="distraction_site",
            severity="medium",
            url="https://youtube.com",
            process_name=None,
            duration_seconds=35.0,
            detected_at=datetime.now(),
            metadata={}
        )

        assert event.should_trigger_intervention is True

    def test_medium_severity_short_duration_no_trigger(self):
        """Medium severity with <30s duration doesn't trigger."""
        event = BehavioralEvent(
            event_type="distraction_site",
            severity="medium",
            url="https://youtube.com",
            process_name=None,
            duration_seconds=20.0,
            detected_at=datetime.now(),
            metadata={}
        )

        assert event.should_trigger_intervention is False

    def test_low_severity_long_duration_triggers(self):
        """Low severity with >120s duration triggers intervention."""
        event = BehavioralEvent(
            event_type="distraction_site",
            severity="low",
            url="https://news.com",
            process_name=None,
            duration_seconds=150.0,
            detected_at=datetime.now(),
            metadata={}
        )

        assert event.should_trigger_intervention is True

    def test_event_immutability(self):
        """BehavioralEvent should be immutable."""
        event = BehavioralEvent(
            event_type="test",
            severity="low",
            url="https://example.com",
            process_name=None,
            duration_seconds=10.0,
            detected_at=datetime.now(),
            metadata={}
        )

        with pytest.raises(AttributeError):
            event.severity = "high"  # type: ignore


class TestBehavioralPattern:
    """Test BehavioralPattern value object."""

    def test_create_pattern(self):
        """Test creating a behavioral pattern."""
        now = datetime.now()
        pattern = BehavioralPattern(
            pattern_type="habitual_scrolling",
            frequency=5,
            total_duration_seconds=300.0,
            first_occurrence=now,
            last_occurrence=now,
            confidence=0.8,
            recommendation="Negotiate time limit"
        )

        assert pattern.pattern_type == "habitual_scrolling"
        assert pattern.frequency == 5
        assert pattern.confidence == 0.8

    def test_pattern_immutability(self):
        """BehavioralPattern should be immutable."""
        pattern = BehavioralPattern(
            pattern_type="test",
            frequency=1,
            total_duration_seconds=10.0,
            first_occurrence=datetime.now(),
            last_occurrence=datetime.now(),
            confidence=0.5,
            recommendation="Test"
        )

        with pytest.raises(AttributeError):
            pattern.frequency = 10  # type: ignore
