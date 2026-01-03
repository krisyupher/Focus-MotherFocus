# Behavioral Analysis Guide

## Overview

The **Behavioral Analysis System** is the intelligence layer of FocusMotherFocus that detects unproductive patterns and triggers counselor interventions. This is the foundation for the interactive counselor avatar feature.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    BEHAVIORAL ANALYSIS FLOW                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. DATA COLLECTION (MCP Integration)                           │
│     ┌──────────────┐          ┌──────────────┐                 │
│     │ Browser MCP  │          │ Windows MCP  │                 │
│     │ - Active tab │          │ - Active win │                 │
│     │ - Tab title  │          │ - Processes  │                 │
│     │ - URL        │          │ - UI events  │                 │
│     └──────┬───────┘          └──────┬───────┘                 │
│            └──────────┬───────────────┘                         │
│                       ▼                                          │
│  2. BEHAVIORAL ANALYSIS                                          │
│     ┌─────────────────────────────────────┐                    │
│     │   MCPBehavioralAnalyzer             │                    │
│     │   - Adult content detection         │                    │
│     │   - Endless scrolling detection     │                    │
│     │   - Distraction site detection      │                    │
│     │   - Pattern recognition             │                    │
│     └─────────────┬───────────────────────┘                    │
│                   ▼                                              │
│  3. INTERVENTION DECISION                                        │
│     ┌─────────────────────────────────────┐                    │
│     │   TriggerInterventionUseCase        │                    │
│     │   - Evaluate event severity         │                    │
│     │   - Check intervention threshold    │                    │
│     │   - Apply cooldown logic            │                    │
│     │   - Generate recommendations        │                    │
│     └─────────────┬───────────────────────┘                    │
│                   ▼                                              │
│  4. INTERVENTION EXECUTION                                       │
│     ┌──────────────┬──────────────┬──────────────┐            │
│     │ Avatar       │ Voice        │ Actions      │            │
│     │ (HeyGen)     │ (ElevenLabs) │ (Browser)    │            │
│     │ - Show face  │ - Speak msg  │ - Close tab  │            │
│     │ - Negotiate  │ - TTS alert  │ - Navigate   │            │
│     └──────────────┴──────────────┴──────────────┘            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Components

### 1. Interface: `IBehavioralAnalyzer`

**Location**: `src/application/interfaces/i_behavioral_analyzer.py`

Defines the contract for behavioral analysis:

```python
class IBehavioralAnalyzer(ABC):
    @abstractmethod
    def analyze_current_activity(self) -> Optional[BehavioralEvent]:
        """Analyze current activity and return event if detected."""
        pass

    @abstractmethod
    def get_patterns(self, lookback_minutes: int) -> list[BehavioralPattern]:
        """Identify patterns in recent events."""
        pass

    @abstractmethod
    def start_monitoring(self) -> None:
        """Start continuous behavioral monitoring."""
        pass
```

### 2. Implementation: `MCPBehavioralAnalyzer`

**Location**: `src/infrastructure/adapters/mcp_behavioral_analyzer.py`

Concrete implementation using MCP servers:

**Detection Capabilities**:

- **Adult Content Detection**: URL/title keyword matching
- **Endless Scrolling**: Infinite-scroll site detection + duration tracking
- **Distraction Sites**: Social media, streaming, gaming, shopping sites
- **Pattern Recognition**: Habitual behavior identification

**Configuration**:

```python
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_tools_mcp,
    windows_mcp=windows_mcp,  # Optional
    scroll_threshold_pixels=5000,
    scroll_time_threshold=60.0,  # Seconds
    event_history_size=100
)
```

### 3. Use Case: `TriggerInterventionUseCase`

**Location**: `src/application/use_cases/trigger_intervention.py`

Orchestrates intervention logic:

**Key Features**:

- **Event Evaluation**: Determines if intervention is needed
- **Cooldown Management**: Prevents intervention spam
- **Recommendation Engine**: Suggests appropriate intervention strategy
- **History Tracking**: Logs all interventions

**Intervention Types**:

1. **Block** (High urgency)
   - Adult content → Immediate tab closure
   - Show avatar + voice alert

2. **Negotiate** (Medium urgency)
   - Endless scrolling → "How much longer?"
   - Distraction sites → "Set a time limit?"
   - Show avatar + voice interaction

3. **Alert** (Low urgency)
   - Quick scrolling → Gentle reminder
   - Notification only, no avatar

## Data Models

### BehavioralEvent

Represents a single detected behavior:

```python
@dataclass(frozen=True)
class BehavioralEvent:
    event_type: str          # "endless_scrolling", "adult_content", etc.
    severity: str            # "low", "medium", "high"
    url: Optional[str]
    process_name: Optional[str]
    duration_seconds: float
    detected_at: datetime
    metadata: dict

    @property
    def should_trigger_intervention(self) -> bool:
        """Auto-determines if intervention is needed."""
```

**Intervention Trigger Logic**:

- `severity == "high"` → Always trigger
- `severity == "medium"` + `duration > 30s` → Trigger
- `severity == "low"` + `duration > 120s` → Trigger

### BehavioralPattern

Represents recurring behavior pattern:

```python
@dataclass(frozen=True)
class BehavioralPattern:
    pattern_type: str               # "habitual_scrolling", etc.
    frequency: int                  # Number of occurrences
    total_duration_seconds: float
    first_occurrence: datetime
    last_occurrence: datetime
    confidence: float               # 0.0 to 1.0
    recommendation: str             # Suggested intervention
```

## Usage Examples

### Basic Setup

```python
from browser_tools_mcp import BrowserToolsMCP
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from src.application.use_cases.trigger_intervention import TriggerInterventionUseCase

# Initialize MCP
browser_mcp = BrowserToolsMCP()

# Create analyzer
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_mcp,
    scroll_time_threshold=60.0  # 1 minute
)

# Create intervention handler
def on_intervention(event):
    print(f"Intervention needed: {event.event_type}")
    # Call avatar, voice, etc.

# Create use case
intervention = TriggerInterventionUseCase(
    behavioral_analyzer=analyzer,
    intervention_callback=on_intervention
)

# Start monitoring
analyzer.start_monitoring()

# In your monitoring loop
while monitoring:
    event = intervention.execute()  # Triggers callback if needed
    time.sleep(5)
```

### Get Intervention Recommendations

```python
event = intervention.execute()

if event:
    recommendation = intervention.get_intervention_recommendation(event)

    print(f"Type: {recommendation['type']}")           # "block", "negotiate", "alert"
    print(f"Message: {recommendation['message']}")     # What to say
    print(f"Action: {recommendation['action']}")       # What to do
    print(f"Show Avatar: {recommendation['show_avatar']}")  # True/False
    print(f"Use Voice: {recommendation['use_voice']}")      # True/False
```

### Analyze Patterns

```python
# Get patterns from last hour
patterns = intervention.analyze_patterns(lookback_minutes=60)

for pattern in patterns:
    print(f"Pattern: {pattern.pattern_type}")
    print(f"Frequency: {pattern.frequency}")
    print(f"Confidence: {pattern.confidence:.2%}")
    print(f"Recommendation: {pattern.recommendation}")
```

### View Intervention History

```python
history = intervention.get_intervention_history(limit=10)

for timestamp, event in history:
    print(f"[{timestamp}] {event.event_type} - {event.severity}")
```

## Running the Demo

```bash
# Install requirements
pip install -r requirements.txt

# Ensure Browser Tools MCP is available
# (Should be on PATH or configured in mcp_client_config.json)

# Run demo
python main_behavioral_demo.py
```

**What the demo does**:

1. Initializes Browser Tools MCP
2. Creates behavioral analyzer
3. Monitors browser activity every 5 seconds
4. Triggers interventions when patterns detected
5. Shows recommendations for each intervention
6. Displays periodic status updates
7. Provides session summary on exit

## Detection Rules

### Adult Content

**Triggers**: URL or page title contains keywords:
- porn, xxx, nsfw, adult, sex
- Known adult site domains

**Severity**: `high`

**Intervention**: Immediate block + avatar alert

### Endless Scrolling

**Triggers**:
- Infinite scroll sites (Reddit, Twitter, Instagram, TikTok, Facebook, Pinterest)
- Duration > `scroll_time_threshold` (default 60s)

**Severity**: `medium`

**Intervention**: Negotiate time limit after 5+ minutes

### Distraction Sites

**Categories**:
- Social Media: Facebook, Instagram, Twitter, TikTok, Reddit
- Video Streaming: YouTube, Netflix, Twitch, Hulu
- Gaming: Steam, Epic Games, Origin, Battle.net
- Shopping: Amazon, eBay, AliExpress

**Severity**: `low` (increases with duration)

**Intervention**: Negotiate after repeated visits

## Integration with Other Components

### Avatar Counselor (HeyGen MCP)

When intervention recommendation includes `show_avatar: true`:

```python
from heygen_mcp import HeyGenMCP

heygen = HeyGenMCP()

def on_intervention(event):
    recommendation = intervention.get_intervention_recommendation(event)

    if recommendation['show_avatar']:
        # Generate avatar video
        video = heygen.create_avatar_video(
            message=recommendation['message'],
            style='counselor'
        )
        # Display video in fullscreen
```

### Voice Alerts (ElevenLabs MCP)

When intervention recommendation includes `use_voice: true`:

```python
from elevenlabs_mcp import ElevenLabsMCP

elevenlabs = ElevenLabsMCP()

def on_intervention(event):
    recommendation = intervention.get_intervention_recommendation(event)

    if recommendation['use_voice']:
        # Generate TTS audio
        audio = elevenlabs.text_to_speech(
            text=recommendation['message']
        )
        # Play audio
```

### Tab Control (Playwright MCP)

When intervention recommendation includes action `close_tab_immediately`:

```python
from src.infrastructure.adapters.playwright_browser_controller import PlaywrightBrowserController

browser = PlaywrightBrowserController()

def on_intervention(event):
    recommendation = intervention.get_intervention_recommendation(event)

    if recommendation['action'] == 'close_tab_immediately':
        browser.close_tab_by_url(event.url)
```

### Agreement Storage (Memory MCP)

Store user agreements and preferences:

```python
from memory_mcp import MemoryMCP

memory = MemoryMCP()

def on_negotiation_complete(event, user_agreement):
    # User agreed to 20 minutes of scrolling
    memory.store_agreement({
        'type': event.event_type,
        'url': event.url,
        'agreed_duration_minutes': user_agreement['duration'],
        'timestamp': datetime.now().isoformat()
    })
```

## Testing

Run behavioral analysis tests:

```bash
# Run all behavioral tests
pytest tests/application/interfaces/test_i_behavioral_analyzer.py
pytest tests/application/use_cases/test_trigger_intervention.py

# Run with verbose output
pytest tests/application/use_cases/test_trigger_intervention.py -v

# Run specific test
pytest tests/application/use_cases/test_trigger_intervention.py::TestTriggerInterventionUseCase::test_high_severity_triggers_intervention
```

## Configuration

### Customizing Detection Thresholds

```python
analyzer = MCPBehavioralAnalyzer(
    browser_mcp=browser_mcp,
    scroll_threshold_pixels=10000,    # Increase scroll threshold
    scroll_time_threshold=120.0,      # Require 2 minutes of scrolling
    event_history_size=200            # Keep more history
)
```

### Customizing Intervention Cooldown

```python
intervention = TriggerInterventionUseCase(
    behavioral_analyzer=analyzer,
    intervention_callback=handler
)

# Don't intervene more than once per 2 minutes
intervention.set_cooldown(120)
```

### Adding Custom Detection Patterns

Extend `MCPBehavioralAnalyzer`:

```python
class CustomBehavioralAnalyzer(MCPBehavioralAnalyzer):
    CUSTOM_PATTERNS = [
        r'custom-distraction-site\.com'
    ]

    def _check_custom_pattern(self, url, title, timestamp):
        # Your custom detection logic
        pass
```

## Next Steps

Now that behavioral analysis is implemented, you can:

1. **Add Avatar Counselor Interface**
   - Integrate HeyGen MCP for face-to-face interventions
   - Create GUI for avatar display

2. **Add Voice Interaction**
   - Integrate ElevenLabs MCP for TTS
   - Add speech recognition for user responses

3. **Add Agreement Negotiation**
   - Build negotiation dialogue system
   - Store agreements in Memory MCP
   - Enforce time limits

4. **Add Auto-Close Enforcement**
   - Integrate Playwright MCP for tab control
   - Add countdown timers
   - Implement grace periods

## Troubleshooting

### Browser MCP Not Available

```bash
# Install browser-tools-mcp
npm install -g @browser-tools/mcp

# Or configure exe_path in mcp_client_config.json
{
  "browser_tools_mcp": {
    "exe_path": "C:\\path\\to\\browser-tools-mcp.exe"
  }
}
```

### No Events Detected

- Ensure browser tabs are open
- Check that Browser MCP can list tabs: `browser-tools-mcp list-tabs`
- Verify URLs match detection patterns
- Lower thresholds for testing

### Too Many Interventions

- Increase cooldown: `intervention.set_cooldown(120)`
- Adjust severity thresholds in `BehavioralEvent.should_trigger_intervention`
- Add more specific URL patterns to reduce false positives

## Summary

The Behavioral Analysis System provides:

✅ **Pattern Detection** - Identifies unproductive behaviors
✅ **Smart Triggering** - Only intervenes when necessary
✅ **Flexible Integration** - Works with all MCP components
✅ **Extensible Design** - Easy to add new patterns
✅ **Clean Architecture** - Follows project conventions

This is the foundation for building the complete Interactive Counselor Avatar system!
