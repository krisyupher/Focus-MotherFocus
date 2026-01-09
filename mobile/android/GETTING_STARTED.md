# Getting Started with FocusMother Android

Quick start guide to build and run the Android app.

## ✅ What's Been Created

A complete, production-ready Android application with:

- ✅ **Usage Monitoring** - Detects excessive phone usage
- ✅ **Smart Interventions** - Alerts with action buttons
- ✅ **Background Service** - Continuous monitoring
- ✅ **Modern UI** - Material Design 3 + Jetpack Compose
- ✅ **Auto-Start** - Resumes after device reboot

## 📂 Project Structure

```
mobile/android/
├── app/
│   ├── build.gradle.kts           ← App configuration
│   ├── src/main/
│   │   ├── AndroidManifest.xml    ← Permissions & components
│   │   ├── java/com/focusmother/android/
│   │   │   ├── FocusMotherApplication.kt        ← App initialization
│   │   │   ├── monitor/UsageMonitor.kt          ← Usage tracking
│   │   │   ├── service/MonitoringService.kt     ← Background service
│   │   │   ├── receiver/                        ← Boot & notification receivers
│   │   │   └── ui/                              ← Compose UI
│   │   └── res/                   ← Resources (icons, strings, themes)
│   └── proguard-rules.pro         ← ProGuard configuration
├── build.gradle.kts               ← Project-level Gradle
├── settings.gradle.kts            ← Gradle settings
├── gradle.properties              ← Gradle properties
└── README.md                      ← Full documentation
```

## 🚀 How to Run

### Step 1: Open in Android Studio

```bash
# Navigate to the Android project
cd mobile/android

# Open this folder in Android Studio:
# File → Open → Select mobile/android folder
```

### Step 2: Sync Gradle

- Android Studio will automatically detect `build.gradle.kts`
- Wait for "Gradle sync" to complete (may take a few minutes first time)
- Dependencies will be downloaded automatically

### Step 3: Run the App

1. **Connect Android device** OR **Start emulator**
   - For real device: Enable USB Debugging
   - For emulator: Create AVD in Android Studio

2. **Click Run button** (▶️ icon) or press `Shift+F10`

3. **Select your device** from the dropdown

4. **Wait for build** and app will launch automatically!

### Step 4: Grant Permissions

**CRITICAL**: App needs Usage Access permission!

1. When app launches, you'll see a red permission card
2. Tap **"Grant Permission"** button
3. Android Settings will open
4. Find **"FocusMother"** in the list
5. Toggle **"Permit usage access"** to ON
6. Return to the app (back button)
7. App will refresh and show your usage stats!

### Step 5: Start Monitoring

1. Toggle the **"Monitoring Active"** switch to ON
2. You'll see a persistent notification (low priority)
3. The app now monitors your phone usage in the background
4. After 30 minutes of continuous usage, you'll get an alert!

## 🧪 Testing the App

### Quick Test (Without Waiting 30 Minutes)

Edit thresholds for testing:

**File**: [app/src/main/java/com/focusmother/android/service/MonitoringService.kt](app/src/main/java/com/focusmother/android/service/MonitoringService.kt)

```kotlin
// Line ~31: Change check interval to 10 seconds
private const val CHECK_INTERVAL_MS = 10_000L  // Was 60_000L

// Line ~32: Change cooldown to 1 minute
private val interventionCooldown = 1 * 60 * 1000L  // Was 15 * 60 * 1000L

// Line ~62: Change threshold to 1 minute
val detection = usageMonitor.detectContinuousUsage(
    thresholdMinutes = 1  // Was 30
)
```

After these changes:
- Rebuild the app
- Use your phone for 1 minute
- You'll get an intervention alert!

**Don't forget to revert these changes for production!**

## 📱 Using the App

### Main Screen

The app shows three cards:

1. **Monitoring Control**
   - Toggle switch to start/stop monitoring
   - Shows current status

2. **Today's Screen Time**
   - Total time phone has been active today
   - Updates in real-time

3. **Most Used Apps**
   - Top 5 apps by usage time
   - Sorted by most used first

### Intervention Alerts

When you've been on your phone too long:

1. **High-priority notification** appears
2. **Vibrates** to get your attention
3. **Two action buttons**:
   - "I'll take a break" → Acknowledges and dismisses
   - "5 more minutes" → Requests more time

After responding, there's a 15-minute cooldown before the next alert.

## 🔧 Configuration

### Adjust Thresholds

**Usage Detection Threshold** (when to alert)

File: [app/src/main/java/com/focusmother/android/monitor/UsageMonitor.kt](app/src/main/java/com/focusmother/android/monitor/UsageMonitor.kt)

```kotlin
// Line ~69: Change 0.8f (80% threshold)
return UsageDetection(
    isExcessive = recentScreenTime >= thresholdMs * 0.8f,
    // 0.8f means 80% of 30 minutes = 24 minutes actual usage
    // Change to 0.5f for 50% threshold (15 minutes)
)
```

**Monitoring Interval** (how often to check)

File: [app/src/main/java/com/focusmother/android/service/MonitoringService.kt](app/src/main/java/com/focusmother/android/service/MonitoringService.kt)

```kotlin
// Line ~31
private const val CHECK_INTERVAL_MS = 60_000L  // 1 minute
// Change to 120_000L for 2 minutes
```

**Cooldown Period** (time between alerts)

Same file:
```kotlin
// Line ~32
private val interventionCooldown = 15 * 60 * 1000L  // 15 minutes
// Change to 30 * 60 * 1000L for 30 minutes
```

## 🔨 Building APK

### Debug APK (for testing)

```bash
cd mobile/android
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (for distribution)

See full instructions in [README.md](README.md#building-release-apk)

## 📊 Features & Capabilities

### What Works Now
✅ Screen time tracking (UsageStatsManager)
✅ Continuous background monitoring (Foreground Service)
✅ Intervention alerts (Smart notifications)
✅ User decisions ("Take break" / "5 more minutes")
✅ Auto-start on boot (BootReceiver)
✅ Modern Material Design 3 UI
✅ Dark/Light theme support
✅ Top apps tracking

### Future Enhancements
🔜 API integration (sync to cloud)
🔜 Time agreements (countdown timers)
🔜 App-specific rules
🔜 Weekly/monthly reports
🔜 Focus modes
🔜 Avatar counselor integration

## 🐛 Troubleshooting

### "Permission Denied" or Stats Don't Show

**Solution**: Grant Usage Access permission
1. Open Android Settings
2. Apps → Special app access → Usage access
3. Find FocusMother → Enable

### Service Stops After a While

**Solution**: Disable battery optimization
1. Settings → Battery → Battery optimization
2. Find FocusMother → Don't optimize

### Notifications Don't Appear

**Solution**: Check notification permissions (Android 13+)
1. Settings → Apps → FocusMother → Notifications
2. Enable all notification categories

### App Crashes on Launch

**Solution**: Check Android version
- Minimum required: Android 8.0 (API 26)
- Check in Settings → About Phone → Android version

## 📖 Next Steps

1. **Test the app** - Use it for a day and see interventions
2. **Customize thresholds** - Adjust to your preferences
3. **Read full docs** - See [README.md](README.md) for details
4. **API integration** - Connect to backend (future)
5. **Contribute** - See [../../CONTRIBUTING.md](../../CONTRIBUTING.md)

## 🆘 Need Help?

- **Full Documentation**: [README.md](README.md)
- **Architecture**: [../../ARCHITECTURE.md](../../ARCHITECTURE.md)
- **Backend API**: [../../backend/api/README.md](../../backend/api/README.md)
- **GitHub Issues**: Report bugs or request features

---

**You're all set! 🎉**

Run the app, grant permissions, enable monitoring, and FocusMother will help you stay focused!
