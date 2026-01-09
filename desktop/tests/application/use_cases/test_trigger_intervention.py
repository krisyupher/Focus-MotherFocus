"""
Tests for trigger intervention use case.
"""
import pytest
from datetime import datetime
from unittest.mock import Mock, MagicMock

from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase
from src.application.interfaces.i_behavioral_analyzer import (
    BehavioralEvent,
    BehavioralPattern
)


@pytest.fixture
def mock_analyzer():
    """Create mock behavioral analyzer."""
    analyzer = Mock()
    analyzer.analyze_current_activity.return_value = None
    analyzer.get_patterns.return_value = []
    return analyzer


@pytest.fixture
def mock_callback():
    """Create mock intervention callback."""
    return Mock()


class TestTriggerInterventionUseCase:
    """Test intervention triggering logic."""

    def test_no_event_no_intervention(self, mock_analyzer, mock_callback):
        """No intervention when no event is detected."""
        mock_analyzer.analyze_current_activity.return_value = None

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        result = use_case.execute()

        assert result is None
        mock_callback.assert_not_called()

    def test_event_not_worthy_no_intervention(self, mock_analyzer, mock_callback):
        """No intervention when event doesn't meet threshold."""
        event = BehavioralEvent(
            event_type="distraction_site",
            severity="low",
            url="https://news.com",
            process_name=None,
            duration_seconds=5.0,  # Too short
            detected_at=datetime.now(),
            metadata={}
        )
        mock_analyzer.analyze_current_activity.return_value = event

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        result = use_case.execute()

        assert result is None
        mock_callback.assert_not_called()

    def test_high_severity_triggers_intervention(self, mock_analyzer, mock_callback):
        """High severity event triggers intervention."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://bad-site.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )
        mock_analyzer.analyze_current_activity.return_value = event

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        result = use_case.execute()

        assert result == event
        mock_callback.assert_called_once_with(event)

    def test_cooldown_prevents_rapid_interventions(self, mock_analyzer, mock_callback):
        """Cooldown prevents interventions too close together."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://bad-site.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )
        mock_analyzer.analyze_current_activity.return_value = event

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        use_case.set_cooldown(60)  # 60 second cooldown

        # First intervention should work
        result1 = use_case.execute()
        assert result1 == event
        assert mock_callback.call_count == 1

        # Second intervention should be blocked by cooldown
        result2 = use_case.execute()
        assert result2 is None
        assert mock_callback.call_count == 1  # Still only called once

    def test_intervention_history_tracking(self, mock_analyzer, mock_callback):
        """Intervention history is properly tracked."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://bad-site.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )
        mock_analyzer.analyze_current_activity.return_value = event

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        use_case.set_cooldown(0)  # No cooldown for this test

        # Execute multiple interventions
        use_case.execute()
        use_case.execute()

        history = use_case.get_intervention_history()
        assert len(history) == 2
        assert all(isinstance(h[1], BehavioralEvent) for h in history)

    def test_adult_content_recommendation(self, mock_analyzer, mock_callback):
        """Adult content gets correct intervention recommendation."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://bad-site.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        recommendation = use_case.get_intervention_recommendation(event)

        assert recommendation['type'] == 'block'
        assert recommendation['action'] == 'close_tab_immediately'
        assert recommendation['urgency'] == 'high'
        assert recommendation['show_avatar'] is True

    def test_scrolling_recommendation_long_duration(self, mock_analyzer, mock_callback):
        """Long scrolling sessions get negotiation recommendation."""
        event = BehavioralEvent(
            event_type="endless_scrolling",
            severity="medium",
            url="https://reddit.com",
            process_name=None,
            duration_seconds=360.0,  # 6 minutes
            detected_at=datetime.now(),
            metadata={}
        )

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        recommendation = use_case.get_intervention_recommendation(event)

        assert recommendation['type'] == 'negotiate'
        assert recommendation['action'] == 'start_negotiation'
        assert recommendation['urgency'] == 'medium'
        assert recommendation['show_avatar'] is True

    def test_scrolling_recommendation_short_duration(self, mock_analyzer, mock_callback):
        """Short scrolling gets gentle alert."""
        event = BehavioralEvent(
            event_type="endless_scrolling",
            severity="medium",
            url="https://reddit.com",
            process_name=None,
            duration_seconds=60.0,  # 1 minute
            detected_at=datetime.now(),
            metadata={}
        )

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        recommendation = use_case.get_intervention_recommendation(event)

        assert recommendation['type'] == 'alert'
        assert recommendation['urgency'] == 'low'
        assert recommendation['show_avatar'] is False

    def test_analyze_patterns_delegates_to_analyzer(self, mock_analyzer, mock_callback):
        """Pattern analysis delegates to behavioral analyzer."""
        expected_patterns = [
            BehavioralPattern(
                pattern_type="habitual_scrolling",
                frequency=5,
                total_duration_seconds=300.0,
                first_occurrence=datetime.now(),
                last_occurrence=datetime.now(),
                confidence=0.8,
                recommendation="Test"
            )
        ]
        mock_analyzer.get_patterns.return_value = expected_patterns

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        patterns = use_case.analyze_patterns(lookback_minutes=30)

        assert patterns == expected_patterns
        mock_analyzer.get_patterns.assert_called_once_with(30)

    def test_clear_history(self, mock_analyzer, mock_callback):
        """History can be cleared."""
        event = BehavioralEvent(
            event_type="adult_content",
            severity="high",
            url="https://bad-site.com",
            process_name=None,
            duration_seconds=0.0,
            detected_at=datetime.now(),
            metadata={}
        )
        mock_analyzer.analyze_current_activity.return_value = event

        use_case = TriggerInterventionUseCase(mock_analyzer, mock_callback)
        use_case.execute()

        assert len(use_case.get_intervention_history()) == 1

        use_case.clear_history()

        assert len(use_case.get_intervention_history()) == 0
