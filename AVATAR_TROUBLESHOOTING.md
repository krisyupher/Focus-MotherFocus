# Avatar Troubleshooting Guide

## Quick Check: Is the Avatar Working?

### Step 1: Check if Avatar Exists
```bash
dir config\avatar.png
```

**If file exists**: Good! Proceed to Step 2.
**If file NOT exists**: Generate avatar first (see "Generating Avatar" below)

### Step 2: Test Avatar + TTS Independently
```bash
python test_avatar_tts.py
```

**What you should see:**
1. Console output showing tests passing
2. **A window opens with your animated face**
3. **You HEAR the TTS say**: "Don't waste your time, come back to your work!"
4. **Your avatar's jaw moves** as it speaks
5. **Eyes blink** occasionally
6. **Green glow pulses** around the face

**If test PASSES**: Avatar system works! Problem is in main app integration.
**If test FAILS**: Check error messages and fix avatar/TTS setup.

### Step 3: Run Main App and Check Console
```bash
python main_v2.py
```

**Look for these console messages:**

✅ **GOOD** (Avatar Working):
```
[MAIN] Avatar loaded from c:\...\config\avatar.png
[ALERT] Avatar animator available: True
[ALERT] Starting TTS with message: Don't waste your time...
[ALERT] Requesting animated avatar frame...
[ALERT] Got avatar frame, updating display
```

❌ **BAD** (Avatar NOT Working):
```
[ALERT] Avatar animator available: False
[ALERT] No avatar animator, using live camera
```

---

## Generating Avatar

### First Time Setup

1. **Run app**: `python main_v2.py`
2. **Prompt appears**: "Welcome to Speaking Avatar Feature!"
3. **Click YES**
4. **Face the camera** with good lighting
5. **Wait 5-30 seconds** for face detection
6. **Success dialog** appears
7. **RESTART the app**: Close and run `python main_v2.py` again
8. **Avatar now loaded!**

### Regenerating Avatar (if it looks bad)

1. Open app: `python main_v2.py`
2. Scroll to **"🎭 Speaking Avatar Settings"** section
3. Click **"📷 Generate/Regenerate Avatar"**
4. Click **Yes** on confirmation
5. Face camera with good lighting
6. Wait for capture
7. **RESTART the app** for new avatar to load

---

## Common Issues

### Issue 1: "I see the live camera, not my avatar"

**Cause**: Avatar not loaded OR app not restarted after generation

**Fix**:
1. Check `config\avatar.png` exists
2. If exists, **restart the app** (close and run again)
3. Check console for `[MAIN] Avatar loaded` message
4. If still shows camera, avatar_animator is None - check Step 3 above

---

### Issue 2: "I don't hear the TTS"

**Cause**: pyttsx3 not installed OR Windows SAPI issue

**Fix**:
```bash
# Reinstall pyttsx3
pip uninstall pyttsx3
pip install pyttsx3>=2.90

# Check Windows Speech settings
# Settings → Time & Language → Speech → Make sure voices are installed
```

**Test TTS directly**:
```python
import pyttsx3
engine = pyttsx3.init()
engine.say("Hello, this is a test")
engine.runAndWait()
```

---

### Issue 3: "Face detection fails"

**Causes**:
- Poor lighting
- Face not frontal
- Camera used by another app
- No camera connected

**Fixes**:
1. **Improve lighting**: Bright, even lighting on face
2. **Face camera directly**: Look straight at webcam
3. **Close other apps**: Zoom, Teams, Skype, OBS
4. **Test camera**: Open Windows Camera app to verify it works
5. **Try again**: Click Regenerate Avatar button

---

### Issue 4: "Avatar looks distorted/weird"

**Cause**: Face was partially detected or bad crop

**Fix**:
1. **Regenerate with better conditions**:
   - Centered face
   - Good lighting
   - Face camera directly
2. Click **"📷 Generate/Regenerate Avatar"**
3. **Restart app** after regeneration

---

### Issue 5: "Jaw doesn't move / No animation"

**Cause**: Avatar animator not receiving TTS callbacks

**Check**:
1. Look for console message: `[ALERT] Starting TTS...`
2. If missing, TTS not starting
3. Check `avatar_animator` is not None
4. Run `test_avatar_tts.py` to verify animation works

**Debug**:
- Check console for `[ALERT]` messages
- Look for errors/exceptions
- Verify `[ALERT] Got avatar frame` appears repeatedly (15 times/sec)

---

## Technical Debug

### Check Avatar Animator Creation

**In main_v2.py around line 50-55:**
```python
if os.path.exists(avatar_path):
    try:
        avatar_animator = AvatarAnimator(avatar_path, tts_service)
        print(f"[MAIN] Avatar loaded from {avatar_path}")  # THIS SHOULD PRINT
    except Exception as e:
        print(f"[MAIN] Failed to load avatar: {e}")  # ERROR HERE?
```

**If you see `[MAIN] Failed to load avatar`:**
- Avatar file is corrupted
- TTS service failed to initialize
- Check full error traceback

---

### Check Alert Notifier Receives Avatar

**In windows_alert_notifier.py around line 222-224:**
```python
print(f"[ALERT] Avatar animator available: {self.avatar_animator is not None}")
# Should print: [ALERT] Avatar animator available: True
```

**If prints False:**
- Avatar wasn't passed to WindowsAlertNotifier
- Check main_v2.py line 62-65:
  ```python
  alert_notifier = WindowsAlertNotifier(
      parent_window=root,
      avatar_animator=avatar_animator  # Must not be None!
  )
  ```

---

### Check Frame Generation

**In windows_alert_notifier.py around line 241-248:**
```python
print("[ALERT] Requesting animated avatar frame...")
avatar_frame = self.avatar_animator.get_current_frame_for_tk(width=600, height=500)
if avatar_frame:
    print("[ALERT] Got avatar frame, updating display")  # THIS SHOULD PRINT
else:
    print("[ALERT] Avatar frame is None!")  # ERROR!
```

**If "Avatar frame is None":**
- Avatar animator broken
- Check avatar_animator.py for errors
- Run test_avatar_tts.py to verify

---

## Step-by-Step Verification

### Verify Avatar System (Checklist)

- [ ] **Avatar file exists**: `dir config\avatar.png` shows file
- [ ] **Test passes**: `python test_avatar_tts.py` works (shows face, speaks)
- [ ] **App shows loaded**: Console shows `[MAIN] Avatar loaded`
- [ ] **Animator available**: Console shows `[ALERT] Avatar animator available: True`
- [ ] **TTS starts**: Console shows `[ALERT] Starting TTS with message:`
- [ ] **Frames generated**: Console shows `[ALERT] Got avatar frame` repeatedly
- [ ] **You SEE your face**: Alert window shows your animated avatar (not live camera)
- [ ] **You HEAR TTS**: Speakers/headphones play motivational message
- [ ] **Jaw moves**: Avatar mouth opens/closes with speech
- [ ] **Eyes blink**: Avatar blinks occasionally

---

## Expected Behavior

### When Alert Triggers:

1. **Alert window opens** (600x500 pixels)
2. **Your static face appears** (from avatar.png, NOT live camera)
3. **TTS starts speaking** (you hear it from speakers)
4. **Jaw animates** (bottom 33% of face moves up/down)
5. **Eyes blink** every 3-5 seconds
6. **Green glow pulses** around face border at 2 Hz
7. **Message overlay** appears in center (green text on black)
8. **Target info** at bottom (red text)
9. **Acknowledge button** at very bottom
10. **Animation updates** 15 times per second (smooth motion)

### When You Close Alert:

1. Click button OR close window (X)
2. **TTS stops** immediately
3. **Animation stops**
4. **Window closes**

---

## Files to Check

**Avatar Storage**:
- `config/avatar.png` - Your captured face (400x400 px)
- `config/avatar_meta.json` - Metadata about capture

**Main Files**:
- `main_v2.py` - Wires avatar_animator into alert_notifier
- `src/infrastructure/adapters/windows_alert_notifier.py` - Uses avatar for alerts
- `src/infrastructure/adapters/avatar_animator.py` - Animation engine
- `src/infrastructure/adapters/windows_tts_service.py` - TTS engine

**Test Files**:
- `test_avatar_tts.py` - Independent test of avatar + TTS

---

## Getting Help

If still not working after all these steps:

1. **Run**: `python test_avatar_tts.py`
2. **Run**: `python main_v2.py`
3. **Trigger alert** (add Netflix, start monitoring, open Netflix)
4. **Copy ALL console output**
5. **Share output** showing:
   - All `[MAIN]` messages
   - All `[ALERT]` messages
   - Any errors/tracebacks

This will show exactly where the problem is!

---

## Quick Fix Summary

**Problem**: I see live camera, not avatar
**Solution**: Restart app after generating avatar

**Problem**: No sound
**Solution**: `pip install pyttsx3>=2.90`

**Problem**: Face detection fails
**Solution**: Better lighting, face camera, close other apps

**Problem**: Avatar distorted
**Solution**: Regenerate with better conditions

**Problem**: No animation
**Solution**: Check console for errors, run test_avatar_tts.py
