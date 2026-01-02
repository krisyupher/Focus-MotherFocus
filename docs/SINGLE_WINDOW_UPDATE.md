# Single Window Update Feature

## Summary

Changed the alert system to **reuse a single window per target** instead of creating multiple stacked windows.

## What Changed

### Before
- Every alert check (every second) created a **NEW window**
- User got **multiple stacked windows** when staying on procrastination site
- Had to close each window individually
- Multiple TTS instances speaking simultaneously

### After
- First alert creates a new window
- Subsequent alerts **UPDATE the existing window** with:
  - New random message
  - Updated timestamp
  - New TTS speech
  - Re-focused window (steals attention again)
- **Only ONE window per target** at any time
- Window stays open until user acknowledges

## How It Works

### Window Tracking
Added `_alert_states` dictionary to `WindowsAlertNotifier` that tracks:
```python
{
    'window': tk.Toplevel,           # Window reference
    'message_label': tk.Label,       # Message widget to update
    'time_label': tk.Label,          # Timestamp widget to update
    'tts': WindowsTTSService,        # TTS instance to reuse
    'animation_running': dict        # Animation control flag
}
```

### Update Logic
In `_show_popup_alert_v2()`:

1. **Check if window exists** for the target
2. **If window exists**:
   - Generate new random message
   - Update message label text
   - Update timestamp
   - Stop old TTS and speak new message
   - Re-focus window (lift, focus_force, grab_set)
   - Return early (don't create new window)
3. **If window doesn't exist**:
   - Create new window (original logic)
   - Store window state in `_alert_states`

### Cleanup
When window closes (button click or X):
- Remove from `_alert_states` dictionary
- Stop camera and TTS
- Destroy window

## Benefits

✅ **No more window spam** - Only one alert window per target
✅ **Consistent attention** - Re-focuses window to steal focus each check
✅ **Fresh messages** - New motivational message each time
✅ **Still speaks** - TTS plays on each update
✅ **User control** - Window stays open until acknowledged

## User Experience

### When Procrastinating:
1. **First check** (t=0s): Alert window appears, TTS speaks
2. **Second check** (t=1s): Same window updates message, re-focuses, TTS speaks again
3. **Third check** (t=2s): Same window updates again, re-focuses, TTS speaks
4. **...continues** until user clicks "ACKNOWLEDGE & RETURN TO WORK"

### Console Output:
```
[ALERT] Creating new alert window for Netflix
[ALERT] Starting TTS: Choose progress over procrastination!
[ALERT] Updating existing alert for Netflix
[ALERT] Speaking updated message: Focus on your mission, productivity awaits!
[ALERT] Updating existing alert for Netflix
[ALERT] Speaking updated message: Your goals won't achieve themselves!
```

## Technical Details

### Files Modified
- `src/infrastructure/adapters/windows_alert_notifier.py`
  - Added `_alert_states` field to class
  - Refactored `_show_popup_alert_v2()` to check for existing window
  - Updated cleanup handlers to remove from `_alert_states`

### New State Tracking
```python
@dataclass
class WindowsAlertNotifier(IAlertNotifier):
    # ... existing fields ...
    _alert_states: Dict[str, dict] = field(default_factory=dict)
```

### Window Update Pattern
```python
if name in self._alert_states:
    alert_state = self._alert_states[name]
    alert_window = alert_state.get('window')

    if alert_window and alert_window.winfo_exists():
        # UPDATE existing window
        message = random.choice(MOTIVATIONAL_MESSAGES)
        alert_state['message_label'].configure(text=message)
        alert_state['time_label'].configure(text=f"TIME: {timestamp}")
        alert_state['tts'].speak(message, blocking=False)
        alert_window.lift()
        alert_window.focus_force()
        return  # Don't create new window
```

## Testing

### Test File: `test_single_window.py`

Triggers 3 alerts with 3-second intervals to verify:
- First alert creates window
- Second alert updates same window (not new window)
- Third alert updates same window again

### Expected Output:
```
[ALERT] Creating new alert window for Netflix    ← First
[ALERT] Updating existing alert for Netflix      ← Second (UPDATE)
[ALERT] Updating existing alert for Netflix      ← Third (UPDATE)
```

### Verification:
✅ Console shows "Updating existing alert" instead of "Creating new"
✅ Only ONE window visible at a time
✅ Message text changes in same window
✅ Timestamp updates
✅ TTS speaks each time (may have occasional pyttsx3 quirks)

## Known Issues

### TTS "run loop already started" Error
- **Cause**: pyttsx3 engine limitation when calling speak() rapidly
- **Impact**: Error message printed, but subsequent calls work fine
- **Status**: Minor cosmetic issue, doesn't affect functionality
- **Possible Fix**: Increase sleep time after stop(), or create new engine each time

## Migration Notes

- **Backward Compatible**: Existing `active_popups` still tracked for compatibility
- **No Breaking Changes**: API unchanged, only internal implementation
- **Works with V2 GUI**: Tested with unified monitoring system

## Summary of Changes

**Lines Changed**: ~120 lines in `windows_alert_notifier.py`

**Key Additions**:
1. `_alert_states` dictionary for window tracking
2. Window existence check before creating new window
3. Widget update logic for message and timestamp
4. State cleanup in close handlers

**Result**: Clean, focused alert experience with one window per target! 🎯
