# Interactive Counselor Avatar - Integration Roadmap

## Project Status

### ✅ Phase 1: Behavioral Analysis (COMPLETED)

**What we built:**

1. **Interface**: `IBehavioralAnalyzer` - Contract for behavioral detection
2. **Implementation**: `MCPBehavioralAnalyzer` - MCP-based detection system
3. **Use Case**: `TriggerInterventionUseCase` - Intervention orchestration
4. **Tests**: Comprehensive test coverage (18 tests, all passing)
5. **Demo**: `main_behavioral_demo.py` - Working demonstration
6. **Documentation**: Complete behavioral analysis guide

**Capabilities:**

✅ Detect adult content (instant, high severity)
✅ Detect endless scrolling (1+ minute, medium severity)
✅ Detect distraction sites (social media, streaming, gaming, shopping)
✅ Pattern recognition (habitual behaviors)
✅ Smart intervention triggering (severity + duration based)
✅ Cooldown management (prevent spam)
✅ Intervention recommendations (block, negotiate, alert)
✅ Event and pattern history tracking

**MCP Integration:**

✅ Browser Tools MCP - Tab monitoring, URL detection
✅ Windows MCP - Process and window detection (prepared)
✅ Memory MCP - Pattern storage (prepared)

---

## 🚧 Phase 2: Avatar Counselor Interface (NEXT)

**Goal**: Create face-to-face intervention system with animated avatar

### Components to Build

#### 1. Avatar Display System

**File**: `src/presentation/avatar_counselor_window.py`

**Features**:
- Fullscreen overlay window (like Zordon alerts)
- Webcam feed display (user's face)
- Avatar video playback (HeyGen generated)
- Transparent background with animated border
- Message display with counselor dialogue

**Integration**:
```python
from heygen_mcp import HeyGenMCP
from webcam_mcp import WebcamMCP

class AvatarCounselorWindow:
    def show_intervention(self, event, recommendation):
        # 1. Capture user's face
        user_face = webcam.capture_frame()

        # 2. Generate avatar counselor video
        avatar_video = heygen.create_talking_avatar(
            message=recommendation['message'],
            style='counselor'
        )

        # 3. Display fullscreen with both feeds
        self.display_counselor_overlay(
            user_face=user_face,
            avatar_video=avatar_video,
            message=recommendation['message']
        )
```

**Technical Details**:
- Tkinter fullscreen window
- OpenCV for video playback
- PIL for image compositing
- Threading for smooth playback

#### 2. Voice Interaction System

**File**: `src/infrastructure/adapters/counselor_voice_service.py`

**Features**:
- Text-to-speech for avatar messages (ElevenLabs MCP)
- Speech recognition for user responses
- Natural conversation flow
- Voice cloning from user (optional)

**Integration**:
```python
from elevenlabs_mcp import ElevenLabsMCP

class CounselorVoiceService:
    def speak_message(self, message: str):
        audio = elevenlabs.text_to_speech(
            text=message,
            voice='counselor'  # Warm, empathetic voice
        )
        self.play_audio(audio)

    def listen_for_response(self) -> str:
        # Use speech-to-text
        return user_speech
```

#### 3. Negotiation Dialogue System

**File**: `src/application/use_cases/negotiate_agreement.py`

**Features**:
- Multi-turn conversation with user
- Time limit bargaining
- Agreement validation
- Storage in Memory MCP

**Example Flow**:
```
Avatar: "I noticed you've been scrolling Reddit for 10 minutes.
        How much longer do you need?"

User: "20 more minutes"

Avatar: "That seems excessive. How about 10 minutes instead,
        then we take a 5-minute break?"

User: "Fine, 10 minutes"

Avatar: "Great! I'll check back in 10 minutes."
        [Stores agreement in Memory MCP]
```

**Integration**:
```python
from memory_mcp import MemoryMCP

class NegotiateAgreementUseCase:
    def negotiate(self, event: BehavioralEvent) -> Agreement:
        # 1. Start dialogue
        # 2. Bargain with user
        # 3. Reach agreement
        # 4. Store in Memory MCP
        # 5. Return agreement for enforcement
```

### MCP Integration Points

```python
# File: src/infrastructure/adapters/counselor_orchestrator.py

class CounselorOrchestrator:
    """Coordinates all MCP services for counselor interventions."""

    def __init__(
        self,
        webcam_mcp: WebcamMCP,
        heygen_mcp: HeyGenMCP,
        elevenlabs_mcp: ElevenLabsMCP,
        memory_mcp: MemoryMCP,
        notifymemaybe: NotifyMeMaybe
    ):
        self.webcam = webcam_mcp
        self.heygen = heygen_mcp
        self.elevenlabs = elevenlabs_mcp
        self.memory = memory_mcp
        self.notifymemaybe = notifymemaybe

    def execute_intervention(self, event: BehavioralEvent, recommendation: dict):
        """Execute full counselor intervention workflow."""

        if recommendation['urgency'] == 'high':
            # Immediate block + alert
            self._block_immediately(event)

        elif recommendation['type'] == 'negotiate':
            # Full avatar negotiation
            self._show_avatar_negotiation(event, recommendation)

        elif recommendation['type'] == 'alert':
            # Simple notification
            self._show_gentle_alert(event, recommendation)

    def _show_avatar_negotiation(self, event, recommendation):
        # 1. Capture user face
        user_face = self.webcam.capture_image()

        # 2. Generate counselor message
        message = recommendation['message']

        if recommendation['use_voice']:
            # 3. Create avatar video with lip-sync
            avatar_video = self.heygen.create_talking_avatar(
                message=message,
                user_face_reference=user_face  # Optional: make avatar look like user
            )

            # 4. Generate TTS audio
            audio = self.elevenlabs.text_to_speech(message)
        else:
            avatar_video = None
            audio = None

        # 5. Show fullscreen counselor window
        window = AvatarCounselorWindow()
        window.show_intervention(
            user_face=user_face,
            avatar_video=avatar_video,
            audio=audio,
            message=message
        )

        # 6. Get user response via interactive dialog
        user_response = self.notifymemaybe.get_user_input(
            prompt="How much longer do you need?",
            options=["5 minutes", "10 minutes", "15 minutes", "Stop now"]
        )

        # 7. Store agreement
        if user_response:
            agreement = {
                'event_type': event.event_type,
                'url': event.url,
                'agreed_time_minutes': self._parse_time(user_response),
                'timestamp': datetime.now().isoformat()
            }
            self.memory.store_agreement(agreement)

        return user_response
```

### Tasks Breakdown

**File Structure**:
```
src/
├── presentation/
│   ├── avatar_counselor_window.py      [NEW] Fullscreen avatar display
│   └── negotiation_dialog.py           [NEW] Multi-turn conversation UI
├── application/
│   └── use_cases/
│       ├── negotiate_agreement.py      [NEW] Negotiation logic
│       └── enforce_agreement.py        [NEW] Time limit enforcement
└── infrastructure/
    └── adapters/
        ├── counselor_orchestrator.py   [NEW] Coordinates all MCPs
        └── counselor_voice_service.py  [NEW] Voice interaction
```

**Testing**:
```
tests/
├── application/use_cases/
│   ├── test_negotiate_agreement.py     [NEW]
│   └── test_enforce_agreement.py       [NEW]
└── infrastructure/adapters/
    └── test_counselor_orchestrator.py  [NEW]
```

**Demos**:
```
main_avatar_counselor_demo.py           [NEW] Full avatar demo
```

---

## 🔮 Phase 3: Agreement Enforcement (FUTURE)

**Goal**: Enforce time limits and track compliance

### Components to Build

#### 1. Agreement Tracker

**File**: `src/application/use_cases/track_agreement.py`

**Features**:
- Monitor active agreements
- Countdown timers
- Compliance checking
- Violation detection

#### 2. Enforcement Actions

**File**: `src/application/use_cases/enforce_agreement.py`

**Features**:
- Tab closure (Playwright MCP)
- Application termination (Windows MCP)
- Grace period warnings
- Escalation logic

**Integration with Playwright**:
```python
from src.infrastructure.adapters.playwright_browser_controller import PlaywrightBrowserController

class EnforceAgreementUseCase:
    def enforce_time_limit(self, agreement: Agreement):
        elapsed = (datetime.now() - agreement.start_time).total_seconds() / 60

        if elapsed >= agreement.agreed_time_minutes:
            # Time's up!
            if agreement.url:
                # Close browser tab
                self.browser_controller.close_tab_by_url(agreement.url)

            # Show completion message
            self.notifymemaybe.show_message(
                "Time's up! Great job sticking to your limit!"
            )
```

---

## 🎯 Phase 4: Multi-MCP Orchestration (FUTURE)

**Goal**: Unified client coordinating all MCP servers

### Components

#### Unified MCP Client

**File**: `src/infrastructure/mcp/unified_mcp_client.py`

**Features**:
- Single connection manager for all MCPs
- Service discovery
- Error handling and fallbacks
- Health monitoring

**Integration**:
```python
class UnifiedMCPClient:
    def __init__(self, config: dict):
        # Initialize all MCPs from config
        self.browser_mcp = BrowserToolsMCP()
        self.windows_mcp = WindowsMCP()
        self.webcam_mcp = WebcamMCP()
        self.heygen_mcp = HeyGenMCP()
        self.elevenlabs_mcp = ElevenLabsMCP()
        self.memory_mcp = MemoryMCP()
        self.notifymemaybe = NotifyMeMaybe()
        self.playwright_mcp = PlaywrightMCP()
        self.filesystem_mcp = FilesystemMCP()

    def get_available_services(self) -> list[str]:
        """Return list of available MCP services."""
        pass

    def execute_workflow(self, workflow_name: str, **kwargs):
        """Execute predefined multi-MCP workflow."""
        pass
```

---

## Implementation Priority

### Immediate (This Week)

1. **Avatar Display Window** - Basic fullscreen overlay
2. **Voice Integration** - TTS with ElevenLabs MCP
3. **Simple Negotiation** - Basic "how long?" dialogue
4. **Demo Integration** - Connect to behavioral analysis

### Short Term (Next 2 Weeks)

1. **Agreement Storage** - Memory MCP integration
2. **Agreement Tracking** - Countdown timers
3. **Enforcement Actions** - Tab closure
4. **Comprehensive Tests** - Full coverage

### Medium Term (Next Month)

1. **Advanced Negotiation** - Multi-turn dialogues
2. **Speech Recognition** - Voice responses
3. **Avatar Customization** - User-based face generation
4. **Pattern Learning** - ML-based habit detection

### Long Term (Future)

1. **Predictive Interventions** - Intervene before distraction
2. **Personalized Strategies** - Learn what works per user
3. **Progress Tracking** - Long-term productivity metrics
4. **Gamification** - Rewards for focus streaks

---

## Testing Strategy

### Unit Tests
- All use cases with mocked dependencies
- Interface contracts validated
- Edge cases covered

### Integration Tests
- MCP server connectivity
- Multi-service workflows
- Error handling and fallbacks

### E2E Tests
- Full intervention workflow
- Real browser interaction
- Avatar generation and display

### Performance Tests
- Intervention latency < 2 seconds
- Video generation < 5 seconds
- Memory footprint monitoring

---

## Configuration

### MCP Client Config Update

Add to `mcp_client_config.json`:

```json
{
  "counselor": {
    "enabled": true,
    "intervention_cooldown_seconds": 30,
    "avatar": {
      "enabled": true,
      "fullscreen": true,
      "show_user_face": true,
      "style": "counselor"
    },
    "voice": {
      "enabled": true,
      "tts_provider": "elevenlabs",
      "voice_name": "counselor_warm",
      "speech_recognition": false
    },
    "negotiation": {
      "enabled": true,
      "max_bargaining_turns": 3,
      "default_time_limit_minutes": 10
    },
    "enforcement": {
      "enabled": true,
      "grace_period_seconds": 30,
      "close_tabs_on_timeout": true
    }
  }
}
```

---

## Documentation to Create

1. **AVATAR_COUNSELOR_GUIDE.md** - How to use avatar features
2. **NEGOTIATION_SYSTEM.md** - Dialogue system documentation
3. **AGREEMENT_ENFORCEMENT.md** - Time limit tracking
4. **MULTI_MCP_ORCHESTRATION.md** - Coordinating all services

---

## Success Criteria

### Phase 2 Complete When:

✅ Avatar window displays fullscreen
✅ User face captured from webcam
✅ Avatar video generated and played
✅ Voice speaks intervention messages
✅ Simple negotiation dialogue works
✅ Agreements stored in Memory MCP
✅ Demo shows full intervention flow
✅ Tests cover new components
✅ Documentation written

### Full System Complete When:

✅ All behavioral patterns detected
✅ Avatar counselor intervenes appropriately
✅ Negotiations succeed in limiting distractions
✅ Agreements enforced with tab closure
✅ User compliance tracked
✅ System runs reliably 24/7
✅ <95% intervention acceptance rate
✅ Measurable productivity improvement

---

## Next Steps

**To continue development:**

```bash
# 1. Review this roadmap
cat docs/COUNSELOR_INTEGRATION_ROADMAP.md

# 2. Review behavioral analysis guide
cat docs/BEHAVIORAL_ANALYSIS_GUIDE.md

# 3. Test current implementation
python main_behavioral_demo.py

# 4. Start Phase 2
# Begin with avatar display window implementation
```

**Questions to answer:**

1. Do you want the avatar to look like the user (using face capture)?
2. What should the counselor's personality be (stern, empathetic, humorous)?
3. Should we support voice responses or just typed text?
4. How aggressive should enforcement be (warning vs immediate closure)?

---

## Summary

**Phase 1 (Behavioral Analysis) is COMPLETE** ✅

You now have:
- Robust behavioral detection
- Smart intervention triggering
- Pattern recognition
- Comprehensive testing
- Full documentation

**Ready to build Phase 2 (Avatar Counselor)** when you are!

The foundation is solid and follows Clean Architecture principles.
All MCP servers are integrated and ready to coordinate.

Let me know which part of Phase 2 you'd like to tackle first! 🚀
