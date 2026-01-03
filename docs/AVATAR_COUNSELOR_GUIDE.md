```# Avatar Counselor Guide

Complete guide to using the FocusMotherFocus Avatar Counselor system.

## Overview

The Avatar Counselor is an intelligent intervention system that:
1. Monitors your behavior (browsing, scrolling, applications)
2. Detects unproductive patterns
3. Intervenes with fullscreen avatar display
4. Negotiates time limits with you
5. Stores and enforces agreements

---

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt

# For voice synthesis (optional)
pip install pygame

# For avatar display
# (tkinter comes with Python)
```

### 2. Setup MCP Servers

**Required**:
- **Browser Tools MCP** - For monitoring browser tabs

**Optional** (enhances experience):
- **Webcam MCP** - Shows your face in interventions
- **ElevenLabs MCP** - High-quality voice
- **Memory MCP** - Stores agreements persistently
- **HeyGen MCP** - Generate talking avatar (future)

### 3. Run Demo

```bash
python main_avatar_counselor_demo.py
```

The system will:
- Monitor your browser activity
- Detect patterns (scrolling, distractions, adult content)
- Show fullscreen counselor when needed
- Speak messages (if voice available)
- Negotiate time limits
- Track agreements

---

## Features

### Behavioral Detection (Phase 1)

**Adult Content** (High Severity):
- Instant detection via URL/title keywords
- Immediate intervention required
- No negotiation - must stop now

**Endless Scrolling** (Medium Severity):
- Triggered after 30+ seconds on infinite-scroll sites
  - Reddit, Twitter, Instagram, TikTok, Facebook, Pinterest
- Negotiable time limits
- Counter-offers if request excessive

**Distraction Sites** (Low Severity):
- Social Media: Facebook, Instagram, Twitter
- Video Streaming: YouTube, Netflix, Twitch
- Gaming: Steam, Epic Games
- Shopping: Amazon, eBay
- Severity increases with duration

### Avatar Counselor (Phase 2)

**Fullscreen Display**:
- User's face (if webcam available)
- Counselor message
- Interactive response input
- Event information
- Retro green Zordon theme

**Voice Synthesis**:
- Speaks intervention messages
- Natural, empathetic tone
- ElevenLabs premium voice (if available)
- Windows TTS fallback

**Negotiation System**:
- Multi-turn dialogue (up to 3 rounds)
- Intelligent time parsing
- Reasonable limit evaluation
- Counter-offers when needed
- Final limit if no agreement

---

## Intervention Types

### 1. Block (High Urgency)

**When**: Adult content, severe violations

**What Happens**:
1. Fullscreen window appears immediately
2. Voice says: "I've detected inappropriate content..."
3. No negotiation - must stop now
4. Agreement: 0 minutes (immediate stop)
5. Tab closed (if Playwright MCP available)

**Example**:
```
🚨 INTERVENTION TRIGGERED 🚨
Event Type: adult_content
Severity: high

[Counselor speaks]: "I've detected inappropriate content.
This must stop immediately."

[Agreement created: 0 minutes]
[Tab closed]
```

### 2. Negotiate (Medium Urgency)

**When**: Endless scrolling, distraction sites

**What Happens**:
1. Fullscreen window with your face
2. Voice asks: "You've been scrolling... How much longer?"
3. You type response: "20 minutes"
4. Evaluates: Too long, counter-offers
5. Voice: "How about 10 minutes instead?"
6. Continue negotiating (max 3 rounds)
7. Agreement reached or limit imposed
8. Stored in Memory MCP

**Example Dialogue**:
```
[Counselor]: "You've been scrolling Reddit for 6 minutes.
How much longer do you need?"

[You type]: "30 minutes"

[Counselor]: "30 minutes seems excessive. How about 10 minutes instead?"

[You type]: "20"

[Counselor]: "We've negotiated enough. 10 minutes, final offer."

✅ Agreement: 10 minutes
Expires at: 15:45:30
```

### 3. Alert (Low Urgency)

**When**: Quick visits, low-severity distractions

**What Happens**:
1. Simple notification (optional avatar)
2. Gentle reminder
3. No negotiation required

**Example**:
```
ℹ️ ALERT
"I notice you're on YouTube. Stay focused on your goals!"
```

---

## Using The System

### Basic Usage

```python
import tkinter as tk
from browser_tools_mcp import BrowserToolsMCP
from elevenlabs_mcp import ElevenLabsMCP
from webcam_mcp import WebcamMCP
from memory_mcp import MemoryMCP

# Phase 1 imports
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase

# Phase 2 imports
from src.infrastructure.adapters.counselor_orchestrator import CounselorOrchestrator

# 1. Create tkinter root
root = tk.Tk()
root.withdraw()

# 2. Initialize MCP clients
browser_mcp = BrowserToolsMCP()
webcam_mcp = WebcamMCP()
elevenlabs_mcp = ElevenLabsMCP()
memory_mcp = MemoryMCP()

# 3. Create behavioral analyzer
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_mcp,
    scroll_time_threshold=60.0  # 1 minute
)
analyzer.start_monitoring()

# 4. Create counselor orchestrator
orchestrator = CounselorOrchestrator(
    webcam_mcp=webcam_mcp,
    elevenlabs_mcp=elevenlabs_mcp,
    memory_mcp=memory_mcp,
    parent_window=root
)

# 5. Create intervention handler
def on_intervention(event):
    recommendation = intervention.get_intervention_recommendation(event)

    orchestrator.execute_intervention(
        event=event,
        recommendation=recommendation,
        on_complete=lambda agreement: print(f"Done: {agreement}")
    )

intervention = TriggerInterventionUseCase(
    behavioral_analyzer=analyzer,
    intervention_callback=on_intervention
)

# 6. Monitoring loop
import time
while True:
    root.update()  # Process tkinter events
    intervention.execute()  # Check for interventions
    time.sleep(5)
```

### Customizing Intervention Behavior

**Change Cooldown**:
```python
intervention.set_cooldown(60)  # Don't intervene more than once per minute
```

**Change Detection Thresholds**:
```python
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_mcp,
    scroll_threshold_pixels=10000,   # More lenient scrolling
    scroll_time_threshold=120.0,     # 2 minutes before flagging
)
```

**Change Negotiation Rounds**:
```python
negotiation = NegotiateAgreementUseCase(
    max_negotiation_rounds=5  # More patient, allow 5 rounds
)
```

**Change Voice**:
```python
voice_service = CounselorVoiceService(
    elevenlabs_mcp=elevenlabs,
    voice_name="nova"  # Different voice
)

# Get available voices
voices = voice_service.get_available_voices()
print(f"Available voices: {voices}")
```

---

## Agreement System

### Agreement Structure

```python
Agreement(
    id="uuid-here",
    event_type="endless_scrolling",
    url="https://reddit.com",
    process_name=None,
    agreed_duration_minutes=10.0,
    created_at=datetime(2026, 1, 2, 14, 30),
    expires_at=datetime(2026, 1, 2, 14, 40),
    user_response="10 minutes",
    counselor_message="Okay, 10 minutes. Stay focused!",
    is_active=True,
    is_violated=False,
    violation_count=0
)
```

### Checking Active Agreements

```python
active_agreements = orchestrator.get_active_agreements()

for agreement in active_agreements:
    remaining = agreement.time_remaining_minutes()
    print(f"{agreement.event_type}: {remaining:.1f} min remaining")
```

### Compliance Checking

```python
# Check if current activity violates agreement
violated = orchestrator.check_agreement_compliance(current_event)

if violated:
    print(f"Agreement violated: {violated}")
    print(f"Violation count: {violated.violation_count}")
```

### Extending Agreements

```python
agreement.extend(5.0)  # Add 5 more minutes
```

---

## Negotiation Examples

### Example 1: Reasonable Request (Accepted)

```
Counselor: "You've been scrolling Reddit for 3 minutes. How much longer?"
User: "10 minutes"
Counselor: "Okay, I'll check back in 10 minutes. Stay focused!"

✅ Agreement: 10 minutes
```

### Example 2: Excessive Request (Counter-Offered)

```
Counselor: "You've been on YouTube for 5 minutes. How long do you need?"
User: "2 hours"
Counselor: "2 hours seems excessive. How about 15 minutes instead?"
User: "30 minutes"
Counselor: "Still too long. How about 20 minutes?"
User: "ok"

✅ Agreement: 20 minutes
```

### Example 3: No Time Specified (Asked Again)

```
Counselor: "How much longer do you need on Instagram?"
User: "just a bit"
Counselor: "Please tell me how many minutes. For example: '10 minutes'."
User: "5"

✅ Agreement: 5 minutes
```

### Example 4: Max Rounds Reached (Limit Imposed)

```
Counselor: "How long?"
User: "I don't know"
Counselor: "Please specify minutes."
User: "whatever"
Counselor: "I need a number."
User: "fine"
Counselor: "We've negotiated enough. 10 minutes, final offer."

✅ Agreement: 10 minutes (imposed)
```

---

## Time Parsing

The system understands various time formats:

| User Says | Parsed As |
|-----------|-----------|
| "10 minutes" | 10.0 minutes |
| "5 min" | 5.0 minutes |
| "15m" | 15.0 minutes |
| "half an hour" | 30.0 minutes |
| "1 hour" | 60.0 minutes |
| "2 hours" | 120.0 minutes |
| "just 5" | 5.0 minutes |
| "20" | 20.0 minutes |
| "okay" | None (asks again) |

---

## Troubleshooting

### Voice Not Working

**Check ElevenLabs MCP**:
```python
if not elevenlabs_mcp.is_available():
    print("ElevenLabs not available")
    # Will use Windows TTS fallback
```

**Test Voice**:
```python
voice_service.test_speak()  # Should hear: "Hello, I am your..."
```

**Check pygame**:
```bash
pip install pygame
```

### Webcam Not Showing User Face

**Check Webcam MCP**:
```python
if not webcam_mcp.is_available():
    print("Webcam not available")
    # Will show intervention without user face
```

**Test Webcam**:
```python
image = webcam_mcp.capture_image()
print(f"Captured: {len(image)} bytes")
```

### No Interventions Triggering

**Check Browser MCP**:
```python
if not browser_mcp.is_available():
    print("Browser MCP not available - can't monitor tabs")
```

**Lower Thresholds for Testing**:
```python
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_mcp,
    scroll_time_threshold=10.0  # 10 seconds (very sensitive)
)
```

**Check Cooldown**:
```python
intervention.set_cooldown(0)  # No cooldown for testing
```

### Window Not Fullscreen

**Check tkinter**:
```python
# Make sure parent window is created
root = tk.Tk()
root.withdraw()  # Hide it
```

**Manual fullscreen**:
```python
window.window.attributes('-fullscreen', True)
window.window.attributes('-topmost', True)
```

---

## Best Practices

### 1. Start With Defaults

Don't customize too much initially:
- Default 60-second scroll threshold is good
- Default 30-second cooldown prevents spam
- Default 3 negotiation rounds is reasonable

### 2. Provide Context

Add metadata to help counselor understand:
```python
event.metadata = {
    'category': 'work_related',  # Was this for work?
    'urgency': 'low',
    'notes': 'Researching for project'
}
```

### 3. Review Agreements Regularly

```python
# Daily summary
all_agreements = orchestrator.active_agreements
kept = [a for a in all_agreements if not a.is_violated]
violated = [a for a in all_agreements if a.is_violated]

print(f"Today: {len(kept)} kept, {len(violated)} violated")
```

### 4. Adjust Based on Patterns

```python
patterns = intervention.analyze_patterns(lookback_minutes=1440)  # Last day

for pattern in patterns:
    if pattern.confidence > 0.8:
        print(f"Strong habit: {pattern.pattern_type}")
        print(f"Recommendation: {pattern.recommendation}")
```

---

## Advanced Usage

### Custom Intervention Messages

```python
def custom_intervention_callback(event):
    # Custom message generation
    if event.event_type == "endless_scrolling":
        messages = [
            "Time to get back to work!",
            "Scrolling won't achieve your goals.",
            "Your future self will thank you for stopping now."
        ]
        recommendation = {
            'type': 'negotiate',
            'message': random.choice(messages),
            'show_avatar': True,
            'use_voice': True
        }
    else:
        recommendation = intervention.get_intervention_recommendation(event)

    orchestrator.execute_intervention(event, recommendation)
```

### Integration with Pomodoro

```python
# Start Pomodoro session with counselor
def start_pomodoro_session(duration_minutes=25):
    agreement = Agreement.create(
        event_type="pomodoro_session",
        url=None,
        process_name=None,
        agreed_duration_minutes=duration_minutes,
        user_response=f"{duration_minutes} min focus session",
        counselor_message=f"Great! Focus for {duration_minutes} minutes."
    )

    orchestrator.active_agreements.append(agreement)

    # Monitor compliance
    while not agreement.is_expired():
        # Check if user got distracted
        event = analyzer.analyze_current_activity()
        if event:
            orchestrator.check_agreement_compliance(event)
        time.sleep(30)

    print("Pomodoro complete! Take a break.")
```

---

## Summary

The Avatar Counselor provides:

✅ **Intelligent Detection** - Knows when you're getting distracted
✅ **Face-to-Face Intervention** - Shows your face for accountability
✅ **Voice Interaction** - Speaks empathetically
✅ **Negotiation** - Works WITH you, not against you
✅ **Agreement Tracking** - Remembers what you committed to
✅ **Compliance Checking** - Holds you accountable

**Use it to**:
- Reduce endless scrolling
- Block inappropriate content
- Limit social media time
- Stay focused on work
- Build better habits

Ready to take control of your focus! 🎯
```