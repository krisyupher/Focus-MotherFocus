# Android Studio Quick Start Guide

## 📱 No main() in Android Apps!

Android apps don't have a traditional `main()` function. Instead:

```
Traditional App:          Android App:
┌─────────────┐          ┌──────────────────┐
│  main() {   │          │ AndroidManifest  │
│    start    │          │ declares:        │
│  }          │          │  - MainActivity  │
└─────────────┘          └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │ MainActivity     │
                         │ onCreate() {     │
                         │   // Entry point │
                         │ }                │
                         └──────────────────┘
```

## 🎯 Your App's Entry Points

### 1. Application Class (Runs First)
**File**: `app/src/main/java/com/focusmother/android/FocusMotherApplication.kt`

```kotlin
class FocusMotherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // This runs FIRST when app starts
        // Like global initialization
        createNotificationChannels()
    }
}
```

### 2. Main Activity (UI Entry Point)
**File**: `app/src/main/java/com/focusmother/android/ui/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This is like main() for your UI
        // Called when user taps app icon
        setContent {
            MainScreen()  // Show UI
        }
    }
}
```

### 3. Manifest Configuration
**File**: `app/src/main/AndroidManifest.xml`

```xml
<application android:name=".FocusMotherApplication">
    <activity android:name=".ui.MainActivity">
        <!-- This makes MainActivity the launcher -->
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

## 🚀 Step-by-Step: Run Your App

### Step 1: Open Android Studio

```
┌─────────────────────────────────────┐
│  Android Studio                  ⚙  │
├─────────────────────────────────────┤
│                                     │
│     📁 Open                          │
│     🆕 New Project                   │
│     📖 Get from VCS                  │
│                                     │
└─────────────────────────────────────┘
```

1. Click **"Open"**
2. Navigate to: `C:\Users\crist\Documents\Develop\FocusMotherFocus\mobile\android`
3. Click **OK**

### Step 2: Wait for Gradle Sync (IMPORTANT!)

```
Bottom of Android Studio:
┌─────────────────────────────────────┐
│ 🔄 Gradle sync in progress...      │
│ ▓▓▓▓▓▓▓▓▓░░░░░░░░░░ 45%            │
└─────────────────────────────────────┘

After a few minutes:
┌─────────────────────────────────────┐
│ ✅ Gradle sync finished             │
└─────────────────────────────────────┘
```

**DO NOT CLICK ANYTHING** until sync completes!

First time may take **5-10 minutes** (downloads libraries).

### Step 3: Setup Device

#### Option A: Use Your Android Phone

```
Your Phone                    Computer
┌──────────┐                 ┌──────────┐
│          │    USB Cable    │          │
│  📱      │ ═══════════════ │  💻      │
│          │                 │          │
└──────────┘                 └──────────┘

Enable on Phone:
Settings → Developer Options → USB Debugging
```

**Enable Developer Options**:
1. Settings → About Phone
2. Tap "Build Number" **7 times**
3. Go back → "Developer Options" appears
4. Enter Developer Options → Toggle "USB Debugging" ON
5. Connect USB cable
6. Phone asks "Allow USB debugging?" → **Allow**

#### Option B: Use Android Emulator

In Android Studio:

```
Top Right Corner:
┌─────────────────────┐
│ 📱 Device Manager   │
└─────────────────────┘
         ↓
Click "Create Device"
         ↓
┌─────────────────────────────┐
│ Select Hardware             │
│  ⚪ Pixel 6                 │
│  ⚪ Pixel 7                 │
│  ⚪ Pixel 7 Pro             │
│                             │
│  [Next]                     │
└─────────────────────────────┘
         ↓
┌─────────────────────────────┐
│ System Image                │
│  📥 API 34 (Android 14)     │
│  📥 API 33 (Android 13)     │
│                             │
│  [Download] [Next]          │
└─────────────────────────────┘
         ↓
Emulator Created!
```

### Step 4: Select Device

```
Top Toolbar in Android Studio:
┌────────────────────────────────────────────┐
│ app │ Pixel 6 API 34 ▼ │ ▶️ Run │ 🐛 Debug │
└────────────────────────────────────────────┘
           ↑
    Click here to select device
           ↓
┌─────────────────────────┐
│ Available Devices       │
│  📱 Samsung Galaxy S21  │  ← Your phone
│  📱 Pixel 6 API 34      │  ← Emulator
│  📱 Pixel 7 API 33      │  ← Emulator
└─────────────────────────┘
```

### Step 5: Click Run!

```
Top Toolbar:
┌────────────────────────────────────┐
│ app │ Pixel 6 ▼ │  ▶️ Run  │ 🐛  │
│                    ↑                │
│              CLICK HERE!            │
└────────────────────────────────────┘

OR Press: Shift + F10
```

### Step 6: Watch Build Progress

```
Bottom Panel - Build Output:
┌─────────────────────────────────────┐
│ ⚙️  Executing tasks...              │
│ > Task :app:compileDebugKotlin      │
│ > Task :app:mergeDebugResources     │
│ > Task :app:processDebugManifest    │
│ ✅ BUILD SUCCESSFUL in 45s          │
│                                     │
│ Installing APK...                   │
│ Launching MainActivity...           │
└─────────────────────────────────────┘
```

### Step 7: App Launches!

```
Your Device/Emulator:
┌─────────────────────────┐
│   🎯 FocusMother        │
├─────────────────────────┤
│                         │
│  ⚠️  Permission Required│
│                         │
│  FocusMother needs      │
│  Usage Access to        │
│  monitor your usage.    │
│                         │
│  [Grant Permission]     │
│                         │
└─────────────────────────┘
```

Tap **"Grant Permission"** → Settings opens → Enable → Back to app!

## 🔍 Android Studio Layout

```
┌─────────────────────────────────────────────────────────┐
│ File  Edit  View  Navigate  Code  Build  Run  Tools    │
├────┬────────────────────────────────────────────────────┤
│ 📁 │ app │ Pixel 6 ▼ │ ▶️ Run │ 🐛 Debug │ ⚙️        │ ← Toolbar
├────┼────────────────────────────────────────────────────┤
│ 📂 │                                                    │
│ 📂 │  Editor - Your code appears here                  │
│ 📄 │                                                    │
│ 📄 │  MainActivity.kt                                  │
│ 📄 │                                                    │
│    │  class MainActivity {                             │
│    │    override fun onCreate() {                      │
│    │      // Entry point!                              │
│    │    }                                              │
│    │  }                                                │
├────┼────────────────────────────────────────────────────┤
│    │ 📊 Logcat  ⚙️ Build  🔍 Run  ⚠️ Problems        │ ← Bottom Panel
└────┴────────────────────────────────────────────────────┘
  ↑
Left Panel: Project structure
```

## 🎓 Understanding the Project Structure

When you open Android Studio, you'll see:

```
app/
├── manifests/
│   └── AndroidManifest.xml          ← Declares MainActivity as launcher
│
├── java/
│   └── com.focusmother.android/
│       ├── FocusMotherApplication   ← Runs FIRST
│       ├── ui/
│       │   └── MainActivity         ← Entry point for UI (like main)
│       ├── monitor/
│       │   └── UsageMonitor         ← Business logic
│       └── service/
│           └── MonitoringService    ← Background task
│
└── res/
    ├── drawable/                     ← Icons
    ├── values/                       ← Strings, themes
    └── xml/                          ← Configuration
```

## ⚡ Quick Actions

| Action | Shortcut | What it does |
|--------|----------|--------------|
| **Run App** | `Shift + F10` | Build + Install + Launch |
| **Debug App** | `Shift + F9` | Run with debugger |
| **Stop App** | `Ctrl + F2` | Stop running app |
| **Rebuild** | `Ctrl + Shift + F9` | Clean + Build |
| **Open Logcat** | `Alt + 6` | View app logs |
| **Find File** | `Ctrl + Shift + N` | Quick file search |
| **Build APK** | `Build → Build Bundle/APK → Build APK` | Create installable file |

## 🐛 View Logs (Like console.log)

```
View → Tool Windows → Logcat

┌─────────────────────────────────────┐
│ 🔍 Filter: com.focusmother         │
├─────────────────────────────────────┤
│ D  Screen time: 45m                 │
│ I  Usage detection triggered        │
│ W  Service cooldown active          │
│ E  Error: Permission denied         │
└─────────────────────────────────────┘
```

In your code, use:
```kotlin
Log.d("FocusMother", "Screen time: $screenTime")
Log.i("FocusMother", "Monitoring started")
Log.w("FocusMother", "Low battery warning")
Log.e("FocusMother", "Error occurred: ${e.message}")
```

## 🎯 Where to Add Your Code

### Want to change the UI?
👉 Edit: `app/src/main/java/com/focusmother/android/ui/MainActivity.kt`

### Want to adjust monitoring logic?
👉 Edit: `app/src/main/java/com/focusmother/android/service/MonitoringService.kt`

### Want to change detection thresholds?
👉 Edit: `app/src/main/java/com/focusmother/android/monitor/UsageMonitor.kt`

### Want to change text/strings?
👉 Edit: `app/src/main/res/values/strings.xml`

### Want to change app icon?
👉 Replace: `app/src/main/res/mipmap-*/ic_launcher.png`

## 🔧 Common Issues & Solutions

### "Gradle sync failed"
```
✗ Problem: Can't download dependencies
✓ Solution: Check internet connection
✓ Or: File → Invalidate Caches → Restart
```

### "No device found"
```
✗ Problem: Device not showing in dropdown
✓ Solution (Phone): Re-enable USB debugging
✓ Solution (Emulator): Start emulator first
```

### "App crashes on launch"
```
✗ Problem: Immediate crash
✓ Solution: Check Logcat for red error (Alt+6)
✓ Common: Need Android 8.0+ (API 26)
```

### "Build failed"
```
✗ Problem: Compilation errors
✓ Solution: Build → Clean Project
✓ Then: Build → Rebuild Project
```

## ✅ Verification Checklist

Run through this checklist:

- [ ] Android Studio is open
- [ ] Project folder is `mobile/android` (not `mobile` or root)
- [ ] Gradle sync completed successfully (bottom shows "✅ Gradle sync finished")
- [ ] Device/emulator appears in dropdown at top
- [ ] Clicked Run button (▶️) or pressed Shift+F10
- [ ] Build Output shows "BUILD SUCCESSFUL"
- [ ] App installed on device (you see "Installing APK")
- [ ] App launched (you see "Launching MainActivity")
- [ ] App screen appears with FocusMother UI

## 🎉 Success!

If you see the app screen with:
- Red "Permission Required" card
- Or (after granting permission) screen time display

**You did it!** The app is running! 🎉

No `main()` needed - Android Studio + the Manifest handle everything automatically!

## 📚 Learn More

- **Official Docs**: https://developer.android.com/studio/run
- **Activity Lifecycle**: https://developer.android.com/guide/components/activities/activity-lifecycle
- **Jetpack Compose**: https://developer.android.com/jetpack/compose

---

**Pro Tip**: Keep Logcat open (`Alt+6`) to see what's happening behind the scenes!
