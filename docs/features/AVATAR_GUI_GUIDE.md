# Avatar Counselor GUI - User Guide

The Avatar Counselor GUI is a minimal, distraction-free interface that lets you interact with an AI productivity counselor through voice and visual feedback.

---

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
playwright install chromium
```

### 2. Start Chrome with Debugging (for tab auto-close)

```bash
start_chrome_debug.bat
```

### 3. Run the Avatar GUI

```bash
python main_avatar_gui.py
```

---

## Interface Overview

The GUI has been simplified to remove all distractions:

```
┌─────────────────────────────────────────┐
│                                         │
│      🎯 FocusMotherFocus                │
│   AI Productivity Counselor             │
│                                         │
│   ┌───────────────────────────┐         │
│   │                           │         │
│   │     👤 Avatar Display     │         │
│   │                           │         │
│   │    (Messages appear       │         │
│   │     here)                 │         │
│   │                           │         │
│   └───────────────────────────┘         │
│                                         │
│      [▶ Start Monitoring]               │
│                                         │
│     Ready to start                      │
│     Services: 7/9 available             │
│                                         │
└─────────────────────────────────────────┘
```

**That's it!** Just one button. No complex menus, no lists, no configuration.

---

## How It Works

### Step 1: Click "Start Monitoring"

The system begins monitoring your browser activity in the background:
- Detects endless scrolling (20+ seconds)
- Identifies adult content
- Recognizes distraction patterns

### Step 2: Avatar Intervenes When Needed

When the system detects unproductive behavior:

1. **Avatar appears with message**:
   ```
   👤

   💬 Noticed you've been scrolling...
   How much longer do you need?
   ```

2. **Voice speaks to you**:
   - Uses ElevenLabs for natural-sounding voice
   - Falls back to Windows TTS if needed

3. **You respond** (currently auto-accepts "10 minutes" for demo):
   - Future: Voice recognition for natural conversation
   - Future: Text input option

### Step 3: Countdown Timer Appears

After agreement:

```
┌────────────────────────┐
│  ⏰ AGREEMENT TIMER    │
│                        │
│       09:58            │  ← Countdown in real-time
│                        │
│    Good time           │  ← Color-coded status
│                        │
│  [⏱ Extend]  [✓ OK]   │
└────────────────────────┘
```

**Colors**:
- 🟢 Green: > 2 minutes remaining
- 🟡 Yellow: 1-2 minutes remaining
- 🔴 Red: < 1 minute remaining

### Step 4: Warnings and Enforcement

**1 minute before expiration**:
```
👤

⏰ 1 minute remaining
```
- Voice: "1 minute remaining!"
- Notification popup

**At expiration**:
```
👤

🕐 Time's up! Wrapping up...
```
- Voice: "Time's up! 30 seconds to wrap up."
- Grace period starts

**After grace period (if still active)**:
```
👤

🚫 Closing tab - agreement time exceeded
```
- Tab closes automatically
- Voice: "Tab closed: reddit.com"
- Countdown timer disappears

---

## Complete Workflow Example

```
1. You: [Click "Start Monitoring"]
   Avatar: "Hello! I'm your productivity counselor."

2. [You open Reddit and scroll for 25 seconds]

3. Avatar: 💬 "Noticed you've been scrolling... How much longer?"
   Voice: "You've been scrolling on Reddit. How much longer do you need?"

4. You: "10 minutes" (auto-accepted in demo)
   Avatar: ✅ "Okay, 10 minutes for scrolling. Stay focused!"

5. [Countdown timer appears: 09:58... 09:57...]

6. [After 9 minutes]
   Avatar: ⏰ "1 minute remaining"
   Voice: "1 minute remaining!"
   Notification: "⏰ Warning: 1 minute(s) remaining"

7. [After 10 minutes]
   Avatar: 🕐 "Time's up! Wrapping up..."
   Voice: "Time's up! 30 seconds to wrap up."
   Countdown: TIME'S UP! (red)

8. [If you continue scrolling after 30s grace period]
   Avatar: 🚫 "Closing tab - agreement exceeded"
   Voice: "Tab closed: reddit.com"
   [Reddit tab closes automatically]
   Countdown timer disappears
```

---

## Features

### ✅ Minimal Interface
- Single button to start/stop
- No complex menus
- No distracting elements
- Clean, dark theme

### ✅ Avatar-Based Interaction
- Visual feedback through avatar messages
- Voice communication
- Empathetic counseling approach
- No camera required (you don't need to show your face)

### ✅ Automatic Monitoring
- Runs in background
- Detects patterns automatically
- No manual intervention needed
- Smart cooldown (won't spam you)

### ✅ Voice Interaction
- Natural language communication
- High-quality ElevenLabs voice (if available)
- Windows TTS fallback
- Non-intrusive audio

### ✅ Smart Enforcement
- Countdown timers
- Warning notifications
- Grace periods
- Automatic tab closure
- Extension requests

### ✅ Service Orchestration (Phase 4)
- Automatic service discovery
- Health monitoring
- Intelligent fallbacks
- Self-healing architecture

---

## Keyboard Shortcuts

Currently:
- **None** - Just click the button!

Future enhancements:
- `Space` - Start/Stop monitoring
- `Ctrl+E` - Request extension
- `Esc` - Dismiss notifications

---

## Customization

### Adjust Detection Thresholds

Edit `src/presentation/avatar_counselor_gui.py`:

```python
# Endless scrolling: require 30 seconds instead of 20
self.behavioral_analyzer = MCPBehavioralAnalyzer(
    browser_tools_mcp=self._get_service('browser_tools'),
    scrolling_threshold_seconds=30.0  # Default: 20.0
)
```

### Adjust Enforcement Timing

```python
# Longer grace period: 60 seconds
self.agreement_tracker = TrackAgreementsUseCase(
    grace_period_seconds=60.0,      # Default: 30.0
    warning_before_seconds=120.0    # Default: 60.0
)
```

### Adjust Cooldown

```python
# Prevent interventions every 2 minutes instead of 1
self.intervention_trigger = TriggerInterventionUseCase(
    analyzer=self.behavioral_analyzer,
    cooldown_seconds=120.0  # Default: 60.0
)
```

### Change Avatar Messages

Edit the `_update_avatar_display()` calls in `avatar_counselor_gui.py`:

```python
self._update_avatar_display("Your custom message here")
```

### Customize Timer Colors

```python
# In _handle_negotiate_intervention, after creating countdown_timer
self.countdown_timer.COLOR_SAFE = '#00FF00'      # Custom green
self.countdown_timer.COLOR_WARNING = '#FFAA00'   # Custom yellow
self.countdown_timer.COLOR_CRITICAL = '#FF0000'  # Custom red
```

---

## Troubleshooting

### Issue: "Browser Tools MCP not available"

**Solution**: Make sure `browser_tools_mcp.py` is in the project root.

### Issue: "Playwright connection failed"

**Solution**: Start Chrome with debugging:
```bash
start_chrome_debug.bat
```

### Issue: Voice not working

**Solution**:
- System automatically falls back to Windows TTS
- Check if speakers/headphones are connected
- Adjust volume

### Issue: Avatar not updating

**Solution**:
- Messages appear as text in the avatar display area
- Check console output for errors

### Issue: Tab not auto-closing

**Solution**:
1. Ensure Chrome is running with debug port:
   ```bash
   start_chrome_debug.bat
   ```
2. Check console for Playwright errors
3. Verify "Playwright" service is available in startup logs

---

## System Requirements

### Minimum
- Python 3.9+
- Windows 10+
- 4GB RAM
- Chrome browser

### Recommended
- Python 3.11+
- Windows 11
- 8GB RAM
- Chrome browser
- Speakers or headphones

---

## Architecture

The Avatar GUI integrates all 4 phases:

```
Avatar Counselor GUI
├── Phase 4: MCP Orchestration
│   ├── Service discovery
│   ├── Health monitoring
│   └── Automatic fallbacks
│
├── Phase 1: Behavioral Analysis
│   ├── Pattern detection
│   └── Intervention triggering
│
├── Phase 2: Avatar Counseling
│   ├── Voice interaction
│   ├── Agreement negotiation
│   └── Memory storage
│
└── Phase 3: Enforcement
    ├── Agreement tracking
    ├── Countdown timers
    └── Tab auto-close
```

---

## Future Enhancements

### Voice Input (Coming Soon)
- Speak your responses instead of typing
- Natural conversation with avatar
- Voice commands for control

### Animated Avatar (Planned)
- HeyGen integration for realistic avatar
- Lip-sync with speech
- Expressive gestures

### Advanced Analytics (Planned)
- Productivity trends
- Pattern analysis
- Weekly reports
- Achievement tracking

---

## Comparison: Old GUI vs Avatar GUI

### Old GUI (gui_v2.py)
```
✗ Complex interface with many controls
✗ Lists of targets to manage
✗ Manual configuration required
✗ AI command input needed
✗ Multiple buttons and options
✓ Auto-startup configuration
✓ Target management
```

### Avatar GUI (avatar_counselor_gui.py)
```
✓ Minimal: Just one button
✓ Automatic monitoring
✓ No configuration needed
✓ Voice-based interaction
✓ Avatar counselor display
✓ All 4 phases integrated
✓ Self-managing system
```

**The Avatar GUI is designed for users who want to "set and forget" - just click start and let the AI counselor handle everything else.**

---

## Tips for Best Results

1. **Start monitoring when you begin work** - The system learns your patterns over time

2. **Honor your agreements** - The grace period is there to help you wrap up, not to keep going

3. **Request extensions if needed** - Click "Extend" on the countdown timer when you legitimately need more time

4. **Keep Chrome running with debug mode** - Required for tab auto-close feature

5. **Adjust thresholds to your workflow** - Some people need more time for legitimate research

6. **Listen to the voice feedback** - The avatar provides helpful context

7. **Don't fight the system** - Work with it to build better habits

---

## Need Help?

- **Issues**: Check console output for error messages
- **Documentation**: Read [ALL_PHASES_COMPLETE.md](ALL_PHASES_COMPLETE.md)
- **Architecture**: See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Demos**: Try individual phase demos first

---

**Enjoy your focused productivity journey!** 🎯
