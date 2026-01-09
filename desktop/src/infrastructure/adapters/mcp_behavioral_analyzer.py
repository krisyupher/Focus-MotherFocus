"""
MCP-based behavioral analyzer implementation.

Uses Browser Tools MCP and Windows MCP to detect unproductive patterns:
- Endless scrolling
- Adult content sites
- Excessive social media use
- Time-wasting applications
"""
import time
import re
from datetime import datetime, timedelta
from typing import Optional
from collections import deque

from src.application.interfaces.i_behavioral_analyzer import (
    IBehavioralAnalyzer,
    BehavioralEvent,
    BehavioralPattern
)


class MCPBehavioralAnalyzer(IBehavioralAnalyzer):
    """
    Behavioral analyzer using MCP servers.

    Integrates with:
    - Browser Tools MCP: Detect scrolling, URL patterns, tab activity
    - Windows MCP: Detect active windows, running processes
    - Memory MCP: Store and retrieve patterns (future integration)
    """

    # Adult content detection patterns (keywords in URL or title)
    ADULT_CONTENT_PATTERNS = [
        r'porn', r'xxx', r'nsfw', r'adult', r'sex',
        r'onlyfans', r'redtube', r'pornhub', r'xvideos'
    ]

    # Distraction site patterns
    DISTRACTION_PATTERNS = {
        'social_media': [r'facebook\.com', r'instagram\.com', r'twitter\.com', r'tiktok\.com', r'reddit\.com'],
        'video_streaming': [r'youtube\.com', r'netflix\.com', r'twitch\.tv', r'hulu\.com'],
        'gaming': [r'steam', r'epicgames', r'origin', r'battlenet'],
        'shopping': [r'amazon\.com', r'ebay\.com', r'aliexpress\.com']
    }

    def __init__(
        self,
        browser_mcp,  # BrowserToolsMCP instance
        windows_mcp=None,  # WindowsMCP instance (optional)
        scroll_threshold_pixels: int = 5000,  # Pixels scrolled before flagging
        scroll_time_threshold: float = 20.0,  # Seconds on distraction site before flagging (REDUCED from 60)
        event_history_size: int = 100
    ):
        """
        Initialize MCP behavioral analyzer.

        Args:
            browser_mcp: Browser Tools MCP instance
            windows_mcp: Windows MCP instance (optional)
            scroll_threshold_pixels: Pixels scrolled to trigger endless scrolling detection
            scroll_time_threshold: Seconds spent scrolling to trigger detection
            event_history_size: Number of events to keep in memory
        """
        self.browser_mcp = browser_mcp
        self.windows_mcp = windows_mcp
        self.scroll_threshold_pixels = scroll_threshold_pixels
        self.scroll_time_threshold = scroll_time_threshold

        self._monitoring = False
        self._event_history: deque[BehavioralEvent] = deque(maxlen=event_history_size)

        # Tracking state
        self._last_check_time = None
        self._current_scroll_session = {
            'start_time': None,
            'total_pixels': 0,
            'url': None
        }

        # Track site visit durations
        self._site_visit_tracker = {}  # {url: {'start_time': datetime, 'last_alert_time': datetime}}

    def analyze_current_activity(self) -> Optional[BehavioralEvent]:
        """
        Analyze current browser/application activity.

        Returns:
            BehavioralEvent if unproductive behavior detected
        """
        now = datetime.now()

        # Check browser activity
        if self.browser_mcp and self.browser_mcp.is_available():
            try:
                tabs_data = self.browser_mcp.list_tabs()

                # Extract tabs list from response
                tabs = []
                if isinstance(tabs_data, dict) and 'result' in tabs_data:
                    tabs = tabs_data['result']
                elif isinstance(tabs_data, list):
                    tabs = tabs_data

                if tabs:
                    # Get active tab (first tab or one marked as active)
                    active_tab = tabs[0]
                    if isinstance(tabs, list):
                        for tab in tabs:
                            if isinstance(tab, dict) and tab.get('active'):
                                active_tab = tab
                                break

                    # Extract URL and title
                    url = active_tab.get('url', '') if isinstance(active_tab, dict) else ''
                    title = active_tab.get('title', '') if isinstance(active_tab, dict) else ''

                    # Check for adult content
                    event = self._check_adult_content(url, title, now)
                    if event:
                        self._event_history.append(event)
                        return event

                    # Check for distraction sites
                    event = self._check_distraction_site(url, title, now)
                    if event:
                        self._event_history.append(event)
                        return event

                    # Check for scrolling behavior (requires network/console logs)
                    # This is simplified - real implementation would track scroll events
                    event = self._check_scrolling_behavior(active_tab, now)
                    if event:
                        self._event_history.append(event)
                        return event

            except Exception as e:
                print(f"[Behavioral Analyzer] Browser check error: {e}")

        # Check Windows processes (if available)
        if self.windows_mcp:
            try:
                # Get active window
                active_window = self.windows_mcp.get_active_window()
                if active_window:
                    event = self._check_application_activity(active_window, now)
                    if event:
                        self._event_history.append(event)
                        return event
            except Exception as e:
                print(f"[Behavioral Analyzer] Windows check error: {e}")

        return None

    def _check_adult_content(self, url: str, title: str, timestamp: datetime) -> Optional[BehavioralEvent]:
        """Check if current page is adult content."""
        combined = f"{url} {title}".lower()

        for pattern in self.ADULT_CONTENT_PATTERNS:
            if re.search(pattern, combined, re.IGNORECASE):
                return BehavioralEvent(
                    event_type="adult_content",
                    severity="high",
                    url=url,
                    process_name=None,
                    duration_seconds=0.0,  # Instant detection
                    detected_at=timestamp,
                    metadata={
                        'title': title,
                        'matched_pattern': pattern
                    }
                )
        return None

    def _check_distraction_site(self, url: str, title: str, timestamp: datetime) -> Optional[BehavioralEvent]:
        """Check if current site is a known distraction and track duration."""
        for category, patterns in self.DISTRACTION_PATTERNS.items():
            for pattern in patterns:
                if re.search(pattern, url, re.IGNORECASE):
                    # Track visit duration
                    if url not in self._site_visit_tracker:
                        # First visit to this site
                        self._site_visit_tracker[url] = {
                            'start_time': timestamp,
                            'last_alert_time': None
                        }
                        print(f"[Behavioral Analyzer] Started tracking {url}")
                        return None  # Don't alert on first detection

                    # Calculate time spent on site
                    visit_info = self._site_visit_tracker[url]
                    duration = (timestamp - visit_info['start_time']).total_seconds()

                    # Check if we should alert (after threshold, and not too soon after last alert)
                    should_alert = duration >= self.scroll_time_threshold

                    # If we've never alerted, or it's been 5+ minutes since last alert
                    if visit_info['last_alert_time']:
                        time_since_last_alert = (timestamp - visit_info['last_alert_time']).total_seconds()
                        should_alert = should_alert and (time_since_last_alert >= 300)  # 5 minutes

                    if should_alert:
                        print(f"[Behavioral Analyzer] Duration threshold exceeded: {duration}s on {url}")

                        # Update last alert time
                        visit_info['last_alert_time'] = timestamp

                        # Calculate severity based on total time
                        if duration > 1200:  # 20+ minutes
                            severity = "high"
                        elif duration > 600:  # 10+ minutes
                            severity = "medium"
                        else:  # 20 seconds - 10 minutes
                            severity = "medium"

                        return BehavioralEvent(
                            event_type="distraction_site",
                            severity=severity,
                            url=url,
                            process_name=None,
                            duration_seconds=duration,
                            detected_at=timestamp,
                            metadata={
                                'category': category,
                                'title': title,
                                'matched_pattern': pattern,
                                'total_time_on_site': duration
                            }
                        )

        # Clean up tracker for sites we're no longer visiting
        current_urls = {url for patterns_list in self.DISTRACTION_PATTERNS.values()
                       for pattern in patterns_list if re.search(pattern, url, re.IGNORECASE)}
        if url not in current_urls:
            # User left distraction sites - reset all trackers
            self._site_visit_tracker.clear()

        return None

    def _check_scrolling_behavior(self, tab_data: dict, timestamp: datetime) -> Optional[BehavioralEvent]:
        """
        Detect endless scrolling behavior.

        Note: This is a simplified implementation. Full implementation would:
        1. Track scroll position changes via browser console logs
        2. Monitor scroll events over time
        3. Detect rapid consecutive scrolling
        """
        # Placeholder logic - would need console logs to track actual scrolling
        # For now, we'll use URL patterns that indicate infinite scroll sites

        url = tab_data.get('url', '') if isinstance(tab_data, dict) else ''

        # Sites known for infinite scrolling
        infinite_scroll_sites = [
            r'reddit\.com', r'twitter\.com', r'instagram\.com',
            r'tiktok\.com', r'facebook\.com', r'pinterest\.com'
        ]

        for pattern in infinite_scroll_sites:
            if re.search(pattern, url, re.IGNORECASE):
                # Track scroll session
                if self._current_scroll_session['url'] != url:
                    # New scroll session
                    self._current_scroll_session = {
                        'start_time': timestamp,
                        'total_pixels': 0,
                        'url': url
                    }
                else:
                    # Continuing scroll session
                    duration = (timestamp - self._current_scroll_session['start_time']).total_seconds()

                    if duration > self.scroll_time_threshold:
                        return BehavioralEvent(
                            event_type="endless_scrolling",
                            severity="medium",
                            url=url,
                            process_name=None,
                            duration_seconds=duration,
                            detected_at=timestamp,
                            metadata={
                                'scroll_duration': duration,
                                'site_type': 'infinite_scroll'
                            }
                        )
        return None

    def _check_application_activity(self, window_data: dict, timestamp: datetime) -> Optional[BehavioralEvent]:
        """Check if current application is unproductive."""
        # Placeholder for Windows application monitoring
        # Would integrate with Windows MCP to detect:
        # - Gaming applications during work hours
        # - Social media apps
        # - Streaming apps
        return None

    def get_patterns(self, lookback_minutes: int = 60) -> list[BehavioralPattern]:
        """
        Identify patterns in recent behavioral events.

        Args:
            lookback_minutes: How far back to analyze

        Returns:
            List of identified behavioral patterns
        """
        cutoff_time = datetime.now() - timedelta(minutes=lookback_minutes)
        recent_events = [e for e in self._event_history if e.detected_at >= cutoff_time]

        if not recent_events:
            return []

        patterns = []

        # Group events by type
        event_groups = {}
        for event in recent_events:
            key = event.event_type
            if key not in event_groups:
                event_groups[key] = []
            event_groups[key].append(event)

        # Analyze each group for patterns
        for event_type, events in event_groups.items():
            if len(events) >= 2:  # Pattern requires at least 2 occurrences
                total_duration = sum(e.duration_seconds for e in events)
                confidence = min(len(events) / 10.0, 1.0)  # More events = higher confidence

                # Determine pattern type and recommendation
                pattern_type = f"habitual_{event_type}"
                recommendation = self._get_recommendation(event_type, len(events), total_duration)

                pattern = BehavioralPattern(
                    pattern_type=pattern_type,
                    frequency=len(events),
                    total_duration_seconds=total_duration,
                    first_occurrence=events[0].detected_at,
                    last_occurrence=events[-1].detected_at,
                    confidence=confidence,
                    recommendation=recommendation
                )
                patterns.append(pattern)

        return patterns

    def _get_recommendation(self, event_type: str, frequency: int, total_duration: float) -> str:
        """Generate intervention recommendation based on pattern."""
        if event_type == "adult_content":
            return "Immediate intervention required. Block access and notify user."
        elif event_type == "endless_scrolling":
            if total_duration > 300:  # 5 minutes
                return "Strong intervention: Close tab and suggest break."
            else:
                return "Gentle reminder: Alert user about scrolling time."
        elif event_type == "distraction_site":
            if frequency > 5:
                return "Pattern detected: Negotiate time limit with user."
            else:
                return "Monitor: Track but don't intervene yet."
        else:
            return "Monitor and gather more data."

    def start_monitoring(self) -> None:
        """Start continuous behavioral monitoring."""
        self._monitoring = True
        self._last_check_time = datetime.now()
        print("[Behavioral Analyzer] Monitoring started")

    def stop_monitoring(self) -> None:
        """Stop behavioral monitoring."""
        self._monitoring = False
        print("[Behavioral Analyzer] Monitoring stopped")

    def is_monitoring(self) -> bool:
        """Check if monitoring is active."""
        return self._monitoring
