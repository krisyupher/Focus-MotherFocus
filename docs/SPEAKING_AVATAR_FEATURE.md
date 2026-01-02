# Speaking Avatar Feature

## Overview

The Focus Monitor now includes a **Speaking Avatar** feature that captures your face once, stores it as a static avatar, and animates it with realistic effects synchronized to text-to-speech alerts.

## Key Features

### 🎭 Animated Avatar
- **One-time face capture** from webcam using OpenCV Haar Cascade face detection
- **Static avatar** persists even when you move away from camera
- **Jaw movement animation** synced with speech
- **Eye blinking** for realistic effect
- **Pulsing green glow** when speaking
- **Zordon-style green tint** matching the retro theme
- **15 FPS animation** for smooth performance

### 🗣️ Text-to-Speech
- **Windows SAPI voices** via pyttsx3
- **Background threading** so UI remains responsive
- **15 motivational messages** that rotate randomly
- **Voice configuration**: Female voice, 175 WPM, 90% volume

### 🖼️ Avatar Persistence
- Avatar captured on first launch (with user prompt)
- Stored as `config/avatar.png` (400x400 px)
- Metadata stored in `config/avatar_meta.json`
- **Regenerate button** in GUI to recapture if needed

## How It Works

### First Launch
1. App starts without avatar
2. After 1 second, prompt asks: "Would you like to generate your avatar?"
3. If yes → Camera activates, detects face (max 30 attempts, 30s timeout)
4. Face cropped, enhanced (CLAHE, sharpening), saved as 400x400 PNG
5. Avatar ready for use!

### During Alerts
1. Target detected (Netflix open, etc.)
2. Alert triggers → Avatar animator starts
3. TTS speaks random motivational message
4. Avatar jaw moves with speech (0-70% jaw drop)
5. Green glow pulses at 2 Hz
6. Random eye blinks every 3-5 seconds
7. Alert window updates at 15 FPS
8. User clicks "ACKNOWLEDGE & RETURN TO WORK"
9. Animation stops, TTS halts, window closes

## Architecture

### Clean Architecture Layers

**Core Layer** (no changes - pure domain logic)

**Application Layer**
- `IFaceDetector` - Face detection contract
- `ITTSService` - Text-to-speech contract
- `GenerateAvatarUseCase` - Avatar generation orchestration

**Infrastructure Layer**
- `OpenCVFaceDetector` - Haar Cascade face detection
- `WindowsTTSService` - pyttsx3 TTS adapter
- `AvatarAnimator` - Animation state machine
- `AvatarStorage` - Persistent avatar storage
- `WindowsAlertNotifier` - Modified to use avatar + TTS

**Presentation Layer**
- `UnifiedMonitorGUI` - Added avatar section with status + regenerate button

### Dependency Flow
```
main_v2.py (composition root)
    ↓
Creates: OpenCVFaceDetector, WindowsTTSService, AvatarStorage
    ↓
Loads avatar → Creates AvatarAnimator(avatar, tts)
    ↓
Injects avatar_animator into WindowsAlertNotifier
    ↓
Injects generate_avatar_use_case into GUI
```

## Files Created

### Interfaces
- [src/application/interfaces/face_detector.py](src/application/interfaces/face_detector.py) - Face detection contract
- [src/application/interfaces/tts_service.py](src/application/interfaces/tts_service.py) - TTS contract

### Adapters
- [src/infrastructure/adapters/opencv_face_detector.py](src/infrastructure/adapters/opencv_face_detector.py) - Haar Cascade implementation
- [src/infrastructure/adapters/windows_tts_service.py](src/infrastructure/adapters/windows_tts_service.py) - pyttsx3 wrapper
- [src/infrastructure/adapters/avatar_animator.py](src/infrastructure/adapters/avatar_animator.py) - Animation engine

### Storage
- [src/infrastructure/storage/avatar_storage.py](src/infrastructure/storage/avatar_storage.py) - Avatar persistence

### Use Cases
- [src/application/use_cases/generate_avatar.py](src/application/use_cases/generate_avatar.py) - Avatar generation logic

## Files Modified

- [requirements.txt](requirements.txt) - Added `pyttsx3>=2.90`
- [src/infrastructure/adapters/windows_alert_notifier.py](src/infrastructure/adapters/windows_alert_notifier.py) - Avatar + TTS integration
- [src/presentation/gui_v2.py](src/presentation/gui_v2.py) - Avatar settings section
- [main_v2.py](main_v2.py) - Wired all dependencies

## Usage

### Generating Avatar (First Time)
1. Run `python main_v2.py`
2. Prompt appears: "Would you like to generate your avatar?"
3. Click **Yes**
4. Face camera with good lighting
5. Wait for face detection (up to 30 seconds)
6. Success message appears
7. Avatar saved to `config/avatar.png`

### Regenerating Avatar
1. Open app
2. Go to **🎭 Speaking Avatar Settings** section
3. Click **📷 Generate/Regenerate Avatar**
4. Confirm dialog
5. Face camera
6. New avatar captured

### Testing Alerts
1. Add target: "Netflix"
2. Start monitoring
3. Open Netflix in browser
4. **Alert appears**:
   - Your animated face fills the screen
   - Jaw moves with speech
   - Eyes blink occasionally
   - Green glow pulses
   - TTS speaks: "Don't waste your time, come back to your work!"

## Animation Details

### Jaw Movement
- **Technique**: Perspective transform on bottom 33% of face
- **Range**: 0-70% jaw drop
- **Speed**: Opens in 50ms, holds during word, closes in 100ms
- **Trigger**: Simulated word-based movement (oscillates at 8 Hz when speaking)

### Eye Blinking
- **Technique**: Darken eye regions with weighted overlay
- **Frequency**: Every 3-5 seconds (random)
- **Duration**: 150ms (fast blink)
- **Disabled**: While speaking (natural human behavior)

### Speaking Glow
- **Color**: Green (#00ff00) - matches Zordon theme
- **Pulse**: 2 Hz sine wave
- **Intensity**: 0.3 to 1.0 (always visible when speaking)
- **Border**: 8px Gaussian blurred

### Zordon Effect
- **Green tint**: +60 on green channel, 35% blend
- **Blur**: 5x5 Gaussian kernel
- **Darkening**: 75% brightness for text readability

## Performance

- **Frame Rate**: 15 FPS (67ms per frame)
- **Memory**: ~5MB (base avatar + 10 cached jaw positions)
- **CPU**: <5% on modern hardware
- **Threading**: TTS runs in background thread (non-blocking)

## Troubleshooting

### "No face detected"
**Causes:**
- Poor lighting
- Camera not connected
- Face not frontal
- Camera used by another app

**Solutions:**
- Improve lighting
- Face camera directly
- Close other camera apps (Zoom, Teams, etc.)
- Check Windows Camera app works
- Try again with better positioning

### "Camera error"
**Causes:**
- Camera drivers outdated
- Windows privacy settings block camera
- Camera hardware issue

**Solutions:**
- Update camera drivers
- Settings → Privacy → Camera → Enable for Python apps
- Test with Windows Camera app
- Restart computer

### "TTS not working"
**Causes:**
- pyttsx3 not installed
- Windows SAPI voices missing

**Solutions:**
- Run: `pip install pyttsx3>=2.90`
- Reinstall Windows SAPI voices
- Check Windows Speech settings

### "Avatar looks weird"
**Causes:**
- Face was partially detected
- Poor crop/lighting during capture
- Low-quality webcam

**Solutions:**
- Click **Regenerate Avatar** with better conditions
- Use external webcam if built-in quality is poor
- Ensure face is centered and well-lit

## Technical Notes

### Face Detection
- Uses OpenCV's `haarcascade_frontalface_default.xml`
- Scale factor: 1.1 (balance speed/accuracy)
- Min neighbors: 5 (reduce false positives)
- Min size: 100x100 pixels
- Selects largest face if multiple detected

### Image Enhancement
1. **CLAHE** (Contrast Limited Adaptive Histogram Equalization)
   - Clip limit: 2.0
   - Tile grid: 8x8
2. **Sharpening kernel** (3x3)
3. **50/50 blend** of enhanced and sharpened

### Animation Cache
Pre-generates 10 jaw positions (0%, 10%, ..., 90%) at startup:
- Frame 0: Jaw closed
- Frame 9: Jaw 90% open
- Real-time: Select cached frame + add blink + add glow

## Future Enhancements

### Planned
- [ ] MediaPipe face landmarks for more accurate lip sync
- [ ] Multiple voice options (male/female/pitch)
- [ ] User-selectable avatar styles (cartoon, pixel art)
- [ ] Emotion expressions (angry, sad, encouraging)
- [ ] Custom motivational messages via settings

### Ideas
- Avatar preview in settings
- Real-time animation test button
- Voice speed/volume controls
- Multi-language support
- Cloud TTS (Google/Azure) for better voices

## Credits

- **Face Detection**: OpenCV Haar Cascades
- **TTS Engine**: pyttsx3 (Windows SAPI)
- **Animation**: Custom perspective transforms
- **Theme**: Zordon from Power Rangers (retro green aesthetic)

## License

Same as main project.
