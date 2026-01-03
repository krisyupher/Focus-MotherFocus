# FocusMotherFocus - Phase System Quick Start

Get up and running with the complete AI Productivity Counselor system.

---

## Installation (One-Time Setup)

```bash
# 1. Install Python dependencies
pip install -r requirements.txt

# 2. Install Playwright for browser control
playwright install chromium

# 3. Start Chrome with debugging (for Phase 3 tab auto-close)
start_chrome_debug.bat
```

---

## Run Demos (Recommended Order)

### Phase 1: Behavioral Analysis

```bash
python main_behavioral_demo.py
```

**See it detect**:
- Endless scrolling (20+ seconds)
- Adult content (URL patterns)
- Distraction sites (social media, gaming)

### Phase 2: Avatar Counselor

```bash
python main_avatar_counselor_demo.py
```

**Experience**:
- Fullscreen intervention window
- Voice synthesis (ElevenLabs or Windows TTS)
- Natural language negotiation
- Agreement creation

### Phase 3: Agreement Enforcement

```bash
# Make sure Chrome is running with debug port first!
start_chrome_debug.bat

python main_enforcement_demo.py
```

**Watch it**:
- Show countdown timer
- Send warnings
- Enforce grace periods
- Auto-close tabs

### Phase 4: MCP Orchestration

```bash
python main_mcp_orchestration_demo.py
```

**Explore**:
- Service discovery
- Health monitoring
- Automatic fallbacks
- Service diagnostics

---

## Run All Tests

```bash
python -m pytest tests/application/interfaces/test_i_behavioral_analyzer.py tests/application/use_cases/test_trigger_intervention.py tests/core/entities/test_agreement.py tests/application/use_cases/test_negotiate_agreement.py tests/application/use_cases/test_track_agreements.py tests/application/use_cases/test_enforce_agreement.py tests/infrastructure/adapters/test_mcp_service_registry.py tests/application/use_cases/test_orchestrate_mcp_services.py -v
```

**Expected**: 87 tests pass in ~30 seconds ✅

---

## Basic Usage

### Example 1: Detect Patterns

```python
from src.infrastructure.adapters.mcp_behavioral_analyzer import MCPBehavioralAnalyzer
from browser_tools_mcp import BrowserToolsMCP

browser_tools = BrowserToolsMCP()
analyzer = MCPBehavioralAnalyzer(browser_tools_mcp=browser_tools)

event = analyzer.analyze_current_activity()
if event and event.should_trigger_intervention:
    print(f"Pattern: {event.event_type}, Severity: {event.severity}")
```

### Example 2: Create Agreement

```python
from src.core.entities.agreement import Agreement

agreement = Agreement.create(
    event_type="endless_scrolling",
    url="https://reddit.com",
    process_name=None,
    agreed_duration_minutes=10.0,
    user_response="10 minutes",
    counselor_message="Let's limit scrolling"
)

print(f"Expires: {agreement.expires_at}")
print(f"Remaining: {agreement.time_remaining_minutes()} min")
```

### Example 3: MCP Orchestration

```python
from src.infrastructure.adapters.mcp_service_factory import MCPServiceFactory
from src.application.use_cases.orchestrate_mcp_services import OrchestrateMCPServicesUseCase

factory = MCPServiceFactory()
registry = factory.create_registry()
orchestrator = OrchestrateMCPServicesUseCase(registry)

# Execute with auto-fallback
result = orchestrator.execute_with_fallback(
    capability_name="synthesize_speech",
    operation=lambda s: s.speak("Hello!")
)
```

---

## Quick Reference

### Run Demos
```bash
python main_behavioral_demo.py          # Phase 1
python main_avatar_counselor_demo.py    # Phase 2
python main_enforcement_demo.py         # Phase 3 (requires Chrome debug)
python main_mcp_orchestration_demo.py   # Phase 4
```

### Run Tests
```bash
python -m pytest tests/ -v              # All 87 tests
```

### Chrome Debug
```bash
start_chrome_debug.bat                  # Required for Phase 3
```

---

## Documentation

- [ALL_PHASES_COMPLETE.md](ALL_PHASES_COMPLETE.md) - Complete system overview
- [PHASE1_COMPLETION_SUMMARY.md](PHASE1_COMPLETION_SUMMARY.md) - Behavioral analysis
- [PHASE2_COMPLETION_SUMMARY.md](PHASE2_COMPLETION_SUMMARY.md) - Avatar counselor
- [PHASE3_COMPLETION_SUMMARY.md](PHASE3_COMPLETION_SUMMARY.md) - Enforcement
- [PHASE4_COMPLETION_SUMMARY.md](PHASE4_COMPLETION_SUMMARY.md) - Orchestration

---

**Start with Phase 1 demo and explore from there!** 🚀
