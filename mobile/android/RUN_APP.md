# How to Run the Android App

## 🎯 Understanding Android App Entry Point

Unlike desktop apps with `main()`, Android apps use **Activities** as entry points.

### Your App's Entry Point

**File**: `app/src/main/java/com/focusmother/android/ui/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is like main() - called when app starts
        setContent {
            FocusMotherFocusTheme {
                MainScreen()  // Your UI loads here
            }
        }
    }
}
```

The **AndroidManifest.xml** tells Android to start MainActivity:

```xml
<activity android:name=".ui.MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 🚀 Method 1: Run in Android Studio (Recommended)

### Step 1: Open Project

1. Launch **Android Studio**
2. Click **"Open"** (or File → Open)
3. Navigate to: `C:\Users\crist\Documents\Develop\FocusMotherFocus\mobile\android`
4. Click **"OK"**

### Step 2: Wait for Gradle Sync

- Bottom of Android Studio will show progress:
  ```
  Gradle sync started...
  Gradle sync finished in 2m 15s
  ```
- **IMPORTANT**: Don't do anything until sync completes!
- First time may take 5-10 minutes (downloads dependencies)

### Step 3: Create/Select a Device

#### Option A: Use Physical Android Device

1. **Enable Developer Options** on your phone:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

2. **Enable USB Debugging**:
   - Settings → Developer Options → USB Debugging → ON

3. **Connect via USB**:
   - Plug phone into computer
   - Phone will ask "Allow USB debugging?" → Tap "Allow"
   - Phone should appear in Android Studio device dropdown

#### Option B: Create an Emulator

1. In Android Studio, click **Device Manager** icon (phone with Android logo)
2. Click **"Create Device"**
3. Select **"Pixel 6"** (or any phone) → Next
4. Download a system image:
   - Recommended: **API 34 (Android 14)**
   - Or: **API 33 (Android 13)**
   - Click "Download" next to the system image
   - Wait for download to complete
   - Click "Next"
5. Click **"Finish"**
6. Emulator will appear in device list

### Step 4: Run the App

1. **Select device** from dropdown (top toolbar)
   - Should show your phone name OR emulator name

2. **Click Run button** (▶️ green triangle)
   - Or press `Shift + F10` (Windows/Linux)
   - Or press `Control + R` (Mac)

3. **Wait for build**:
   ```
   Build > Build Output
   ✓ Compilation successful
   ✓ Installing APK
   ✓ Launching activity
   ```

4. **App launches** on device/emulator automatically!

### Step 5: Grant Permission

When app opens:
1. You'll see red "Permission Required" card
2. Tap **"Grant Permission"**
3. Settings opens → Find "FocusMother"
4. Toggle **"Permit usage access"** to ON
5. Press Back button to return to app
6. App refreshes and shows your stats!

## 🚀 Method 2: Command Line (Alternative)

If Android Studio has issues, use Gradle directly:

### Windows:

```cmd
cd C:\Users\crist\Documents\Develop\FocusMotherFocus\mobile\android

# Build the app
gradlew.bat assembleDebug

# Install on connected device/emulator
gradlew.bat installDebug

# Or build + install in one command
gradlew.bat installDebug
```

### Linux/Mac:

```bash
cd mobile/android

# Build the app
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

**Output APK location**:
```
app/build/outputs/apk/debug/app-debug.apk
```

You can manually install this APK:
```bash
# Using ADB (Android Debug Bridge)
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 Troubleshooting

### "Gradle sync failed"

**Solution 1**: Check internet connection (Gradle downloads dependencies)

**Solution 2**: Update Gradle wrapper:
```bash
cd mobile/android
./gradlew wrapper --gradle-version=8.2
```

**Solution 3**: Invalidate caches:
- Android Studio → File → Invalidate Caches → Invalidate and Restart

### "SDK not found"

**Solution**: Install Android SDK:
1. Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK
2. Check **"Android 14.0 (API 34)"**
3. Click "Apply" → SDK will download

### "No devices found"

**For Physical Device**:
- Reconnect USB cable
- Re-enable USB debugging
- Try different USB port
- Check USB cable (use data cable, not charging-only)

**For Emulator**:
- Click Device Manager → Click Play (▶️) button next to emulator
- Wait for emulator to fully boot (may take 2-3 minutes first time)

### "Installation failed"

**Solution 1**: Uninstall old version (if exists):
```bash
adb uninstall com.focusmother.android
```

**Solution 2**: Clean and rebuild:
- Android Studio → Build → Clean Project
- Build → Rebuild Project
- Then click Run

### "Permission denied"

App needs special **Usage Stats** permission:
1. Open Android Settings manually
2. Apps → Special app access → Usage access
3. Find "FocusMother"
4. Toggle ON

### "App crashes immediately"

Check logcat in Android Studio:
- View → Tool Windows → Logcat
- Look for red error messages
- Common issue: Minimum SDK 26 required (Android 8.0+)

## 📱 After App Launches

### What You Should See

1. **Permission Card** (red):
   - "Permission Required"
   - "Grant Permission" button

2. After granting permission:
   - **Monitoring Control** (toggle switch)
   - **Today's Screen Time** (e.g., "2h 34m")
   - **Most Used Apps** (list of top 5 apps)

### Enable Monitoring

1. Toggle **"Monitoring Active"** switch to ON
2. Persistent notification appears: "FocusMother is Monitoring"
3. App now checks usage every 60 seconds
4. After 30 minutes continuous usage → Alert appears!

## 🧪 Quick Test (1 minute instead of 30)

To test alerts quickly:

**Edit**: `app/src/main/java/com/focusmother/android/service/MonitoringService.kt`

Change these lines:

```kotlin
// Line ~31 (was 60_000L)
private const val CHECK_INTERVAL_MS = 10_000L  // Check every 10 seconds

// Line ~32 (was 15 * 60 * 1000L)
private val interventionCooldown = 1 * 60 * 1000L  // 1 minute cooldown

// Line ~62 (was 30)
val detection = usageMonitor.detectContinuousUsage(
    thresholdMinutes = 1  // Alert after 1 minute
)
```

**Rebuild and run** → Use phone for 1 minute → Alert appears!

**Don't forget to revert these changes for production!**

## 📊 App Lifecycle

Understanding how the app works:

```
1. App Launches
   ↓
2. FocusMotherApplication.onCreate()
   - Creates notification channels
   ↓
3. MainActivity.onCreate()
   - Shows UI with Jetpack Compose
   ↓
4. User toggles "Monitoring Active" → ON
   ↓
5. MonitoringService starts
   - Foreground service notification appears
   - Checks usage every 60 seconds
   ↓
6. Every check:
   - UsageMonitor.detectContinuousUsage()
   - If excessive → triggerIntervention()
   ↓
7. Intervention Alert appears
   - User clicks "Take break" or "5 more minutes"
   - NotificationActionReceiver handles action
   ↓
8. 15-minute cooldown
   ↓
9. Monitoring continues...
```

## 🎯 Key Files to Know

| File | Purpose | Like `main()`? |
|------|---------|----------------|
| **MainActivity.kt** | First screen shown | ✅ Yes - entry point |
| **FocusMotherApplication.kt** | App initialization | ⚠️ Runs before MainActivity |
| **MonitoringService.kt** | Background monitoring | ❌ No - service |
| **UsageMonitor.kt** | Usage detection logic | ❌ No - utility class |

## 💡 Pro Tips

1. **Keep Logcat open** (View → Tool Windows → Logcat) to see logs
2. **Use emulator** for faster testing (no USB cable needed)
3. **Enable auto-import** (Settings → Editor → Auto Import → Optimize imports on the fly)
4. **Use instant run** (faster rebuilds) - enabled by default

## 🆘 Still Having Issues?

1. **Check minimum requirements**:
   - Android Studio: 2023.1.1+
   - JDK: 17
   - Gradle: 8.2+
   - Android SDK: API 34

2. **Completely rebuild**:
   ```bash
   # Delete build folders
   rm -rf app/build
   rm -rf .gradle

   # Rebuild
   ./gradlew clean build
   ```

3. **Try different device**: Emulator vs real device

4. **Check USB cable**: Must be data cable (not charge-only)

5. **Update Android Studio**: Help → Check for Updates

## ✅ Success Checklist

- [ ] Android Studio opened
- [ ] Project opened from `mobile/android` folder
- [ ] Gradle sync completed successfully
- [ ] Device/emulator selected in dropdown
- [ ] Clicked Run button (▶️)
- [ ] App installed on device
- [ ] App launched successfully
- [ ] Permission card shows
- [ ] Granted Usage Access permission
- [ ] Screen time displays
- [ ] Monitoring toggle works
- [ ] Service notification appears

---

**You're ready to go!** 🎉

No `main()` needed - Android handles everything through Activities and the Manifest!
