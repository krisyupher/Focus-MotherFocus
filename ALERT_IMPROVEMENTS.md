# Alert System Improvements

## Changes Made

### 1. Continuous Alerts (Every Second)

**File**: [src/application/use_cases/check_targets.py](src/application/use_cases/check_targets.py#L72-L84)

**Change**: Alerts now trigger EVERY monitoring check cycle (every second) while a target is active, instead of only once when it becomes active.

**Before**:
```python
# Alert only on state transition (inactive -> active)
if is_active and not target.is_alerting:
    target.mark_as_alerting()
    self._alert_notifier.notify(target.name)
```

**After**:
```python
# CONTINUOUS ALERTS - Alert every check cycle while active
if is_active:
    if not target.is_alerting:
        target.mark_as_alerting()
    # Send alert EVERY check cycle while active
    self._alert_notifier.notify(target.name)
```

**Result**: If you're on Netflix (website OR app), you get an alert popup every second until you stop using it.

---

### 2. Force Focus - Steal Mouse Attention

**File**: [src/infrastructure/adapters/windows_alert_notifier.py](src/infrastructure/adapters/windows_alert_notifier.py#L188-L200)

**Changes**:
- Added `grab_set()` - Makes alert modal, blocks interaction with other windows
- Added `grab_set_global()` - Global modal across all windows
- Added `focus()` - Additional focus forcing
- Reduced auto-close from 5 seconds to 2 seconds (since alerts are continuous)
- Added `grab_release()` before destroying windows

**Before**:
```python
alert_window.attributes('-topmost', True)
alert_window.lift()
alert_window.focus_force()
```

**After**:
```python
# FORCE FOCUS - Steal attention from current application
alert_window.attributes('-topmost', True)
alert_window.attributes('-toolwindow', False)  # Show in taskbar
alert_window.lift()
alert_window.focus_force()
alert_window.grab_set()  # Modal - block interaction

# Additional focus forcing for Windows
try:
    alert_window.focus()
    alert_window.grab_set_global()  # Global modal
except:
    pass
```

**Result**: Alert window FORCES itself into focus, taking mouse control away from the current application (Netflix, Steam, etc.)

---

### 3. Improved Process Detection Reliability

**File**: [src/infrastructure/adapters/windows_process_detector.py](src/infrastructure/adapters/windows_process_detector.py#L19-L78)

**Changes**:
1. **Added Caching**: 500ms cache to reduce false negatives
2. **Retry Logic**: Up to 3 attempts to detect processes
3. **Better Error Handling**: Falls back to cached values on errors

**New Features**:
```python
class WindowsProcessDetector:
    def __init__(self):
        self._process_cache = {}  # name -> last_seen_timestamp
        self._cache_timeout = 0.5  # 500ms cache

    def is_process_running(self, process_name):
        # 1. Check cache first (fast path)
        if recently_in_cache:
            return True

        # 2. Try to detect process (with retries)
        for attempt in range(3):
            try:
                if found_in_processes:
                    update_cache()
                    return True
            except Exception:
                retry_with_delay()

        # 3. Fall back to cache on failure
        return check_cache()
```

**Result**:
- More reliable detection (retries on failures)
- Faster checks (cached results)
- Reduces "flickering" false negatives when process enumeration briefly fails

---

## Testing

To test these improvements:

### Test 1: Continuous Alerts
1. Run `python main_v2.py`
2. Add target "Netflix"
3. Start monitoring
4. Open Netflix.com in browser OR launch Netflix app
5. **Expected**: Alert popup appears every second while Netflix is active
6. Close Netflix
7. **Expected**: Alerts stop immediately

### Test 2: Focus Stealing
1. Start monitoring with Netflix as target
2. Open Netflix in browser
3. Click inside Netflix (give it focus)
4. **Expected**: Alert popup appears and STEALS focus from Netflix
5. Mouse should be forced to acknowledge alert
6. Without closing alert, try clicking Netflix
7. **Expected**: Can't interact with Netflix until alert is closed

### Test 3: Process Detection Reliability
1. Add "Notepad" as target
2. Start monitoring
3. Open and close Notepad.exe multiple times rapidly
4. **Expected**: Detection should be consistent, no "missed" detections

## Configuration

The monitoring interval is set in [config.json](config.json):

```json
{
  "monitoring_interval": 1  // Check every 1 second
}
```

**Recommendation**: Keep at 1 second for responsive continuous alerts.

## Alert Behavior

- **Auto-close**: 2 seconds (down from 5 seconds)
- **Sound**: Windows beep (1000 Hz for 500ms)
- **Frequency**: Every 1 second while target is active
- **Focus**: FORCED - steals focus from current application
- **Modal**: Blocks interaction with other windows

## Important Notes

### Alert Frequency
With 1-second monitoring interval:
- If on Netflix for 10 seconds = 10 alert popups
- If on Steam for 60 seconds = 60 alert popups

This is **by design** - constant reminders to stop procrastinating!

### Performance
- Process caching reduces CPU usage
- 3 retry attempts ensure reliability without excessive delay
- Cache timeout (500ms) balances accuracy vs performance

### Focus Stealing
The `grab_set()` and `grab_set_global()` make alerts VERY intrusive:
- You CANNOT ignore them
- You MUST acknowledge to continue
- Mouse focus is FORCED to alert window

This is intentional for maximum effectiveness!

## Troubleshooting

### "Alert doesn't steal focus"
- Some applications (fullscreen games) may resist focus stealing
- Try running FocusMotherFocus as Administrator for stronger focus control

### "Too many alerts!"
- This is working as intended
- To reduce frequency, increase `monitoring_interval` in config.json
- Example: Set to 5 for alerts every 5 seconds instead of every second

### "Process detection misses my app"
- Verify the exact process name using Task Manager
- Update the target resolver mapping if needed
- Check that process name ends with `.exe`

## Files Modified

1. [src/application/use_cases/check_targets.py](src/application/use_cases/check_targets.py) - Continuous alert logic
2. [src/infrastructure/adapters/windows_alert_notifier.py](src/infrastructure/adapters/windows_alert_notifier.py) - Focus stealing
3. [src/infrastructure/adapters/windows_process_detector.py](src/infrastructure/adapters/windows_process_detector.py) - Reliability improvements

## Summary

✅ **Continuous Alerts**: Alerts appear every second while target is active
✅ **Force Focus**: Alerts steal mouse focus from current application
✅ **Improved Detection**: Caching + retry logic for reliable process detection

The system is now **MUCH more aggressive** in preventing procrastination!
