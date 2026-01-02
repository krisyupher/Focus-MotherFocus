# Camera Debug Guide

## Changes Made

### 1. ✅ Removed Auto-Close
- Alert window now **stays open permanently**
- Only closes when you click:
  - "⚡ ACKNOWLEDGE & RETURN TO WORK ⚡" button
  - X button on window
- No more 2-second auto-close timer!

### 2. ✅ Added Debug Output
- Camera now prints detailed debug messages:
  - `[CAMERA] Frame captured: (480, 640, 3)` - Frame successfully captured
  - `[CAMERA] PIL Image created: (600, 500) RGB` - Image processing success
  - `[CAMERA] ImageTk created successfully` - Ready for Tkinter
  - `[ALERT] Requesting camera background...` - Alert requesting camera
  - `[ALERT] Camera background received!` - Camera image ready

## Testing the Camera

### Test 1: Basic Camera Access
```bash
python test_camera.py
```

**Expected Output:**
```
[OK] Camera opened successfully with DirectShow!
[OK] Frame captured successfully!
  Frame shape: (480, 640, 3)
  Frame type: uint8
```

✅ **This test PASSED** - Camera hardware is working!

### Test 2: Visual Zordon Display
```bash
python test_zordon_camera.py
```

**What You Should See:**
1. A 600x500 window opens
2. **Your face fills the entire window** with green tint
3. A black box with green text in the center
4. Text says "TEST: Can you see yourself?"

**Debug Messages in Console:**
```
[CAMERA] Frame captured: (480, 640, 3)
[CAMERA] PIL Image created: (600, 500) RGB
[CAMERA] ImageTk created successfully
```

### Test 3: Full Application Test
```bash
python main_v2.py
```

**Steps:**
1. Add a target (e.g., "Netflix" with URL https://netflix.com)
2. Start monitoring
3. Open Netflix in browser
4. **Alert should appear with your face as background**

**Debug Messages You'll See:**
```
[ALERT] Requesting camera background...
[CAMERA] Frame captured: (480, 640, 3)
[CAMERA] PIL Image created: (600, 500) RGB
[CAMERA] ImageTk created successfully
[ALERT] Camera background received!
```

## Troubleshooting

### Issue: "Camera background is None"

**Possible Causes:**
1. **Another app is using camera**
   - Close Zoom, Teams, Skype, OBS, etc.
   - Check Windows Task Manager for camera-using apps

2. **Camera permission denied**
   - Go to Windows Settings → Privacy → Camera
   - Enable camera access for Python apps

3. **Camera driver issue**
   - Open Windows Camera app to test if camera works
   - Update camera drivers if needed

4. **OpenCV installation issue**
   ```bash
   pip uninstall opencv-python
   pip install opencv-python>=4.8.0
   ```

### Issue: "Cannot see myself in alert"

If debug shows camera captured but you don't see it:

1. **Check debug output** - Should show all 3 camera messages
2. **Tkinter image reference** - Already handled with `bg_label.image = camera_bg`
3. **Window layering** - Text overlays might cover camera (unlikely with current design)

### Issue: Alert closes immediately

✅ **FIXED!** Auto-close timer removed. Alert now stays open until you click acknowledge button.

## Debug Output Explanation

When alert triggers, you should see this sequence:

```
[ALERT] Requesting camera background...      ← Alert requests camera
[CAMERA] Frame captured: (480, 640, 3)       ← Camera captures frame
[CAMERA] PIL Image created: (600, 500) RGB   ← Image processed
[CAMERA] ImageTk created successfully        ← Ready for display
[ALERT] Camera background received!          ← Alert gets camera image
```

If any step fails, you'll see error messages explaining what went wrong.

## What Should Happen

When the alert displays:

```
┌──────────────────────────────────────┐
│  ⚡ ZORDON ALERT SYSTEM ⚡           │ ← Title bar (green on black)
├──────────────────────────────────────┤
│                                      │
│  [YOUR FACE WITH GREEN TINT]         │ ← Camera fills entire window
│                                      │
│  ┌────────────────────────────────┐  │
│  │ Don't waste your time,         │  │ ← Message overlay (center)
│  │ come back to your work!        │  │
│  └────────────────────────────────┘  │
│                                      │
│  ⚠️ TARGET: Netflix ⚠️              │ ← Target info (75% down)
│  TIME: 14:32:15                      │
│                                      │
│  ⚡ ACKNOWLEDGE & RETURN TO WORK ⚡  │ ← Button (bottom)
└──────────────────────────────────────┘
```

**The camera fills the ENTIRE background** (600x500 pixels)
**Text overlays are on TOP** of your face

## Next Steps

1. Run `python test_camera.py` - Verify camera hardware ✅
2. Run `python test_zordon_camera.py` - Visual test (should show your face)
3. Run `python main_v2.py` - Full app test with monitoring

If test_camera.py passes but you still can't see yourself:
- Share the debug output from the console
- Check if test_zordon_camera.py window shows your face
- Verify no error messages appear

The camera is working at the hardware level. If you can't see it, it's likely a display/layering issue that the debug output will reveal.
