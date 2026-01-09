# FocusMother - Android App

Native Android application built with Kotlin that monitors phone usage and helps users stay focused by detecting excessive screen time and providing intelligent interventions.

## 🎯 Features

### Core Functionality
- ✅ **Usage Monitoring**: Tracks screen time using `UsageStatsManager`
- ✅ **Continuous Detection**: Background service monitors usage patterns
- ✅ **Smart Interventions**: Alerts when excessive usage is detected (with cooldown)
- ✅ **Top Apps Tracking**: Shows which apps you use most
- ✅ **Daily Statistics**: Displays today's total screen time
- ✅ **Decision Making**: User can agree to take a break or request more time

### Technical Features
- ⚙️ **Foreground Service**: Ensures monitoring continues in background
- 🔔 **Smart Notifications**: High-priority alerts with action buttons
- 🔄 **Auto-Start**: Resumes monitoring after device reboot
- 💾 **Local Storage**: Saves preferences and decisions
- 🎨 **Modern UI**: Material Design 3 with Jetpack Compose
- 🌙 **Dynamic Theme**: Adapts to system theme (light/dark)

## 📱 Screenshots

```
┌─────────────────────────┐
│   🎯 FocusMother        │
├─────────────────────────┤
│                         │
│  ┌───────────────────┐  │
│  │ Monitoring Active │  │
│  │ Watching usage    │🔘│
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ 📱 Screen Time    │  │
│  │    2h 34m         │  │
│  └───────────────────┘  │
│                         │
│  ┌───────────────────┐  │
│  │ ⭐ Most Used Apps │  │
│  │                   │  │
│  │ Instagram    45m  │  │
│  │ Twitter      32m  │  │
│  │ YouTube      28m  │  │
│  └───────────────────┘  │
│                         │
└─────────────────────────┘
```

## 🏗️ Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────┐
│           UI Layer                  │
│  (Jetpack Compose + MainActivity)   │
├─────────────────────────────────────┤
│         Service Layer               │
│  (MonitoringService + Receivers)    │
├─────────────────────────────────────┤
│        Monitor Layer                │
│    (UsageMonitor + Logic)           │
├─────────────────────────────────────┤
│        Android APIs                 │
│  (UsageStatsManager + Notifications)│
└─────────────────────────────────────┘
```

### Key Components

#### 1. **UsageMonitor** ([monitor/UsageMonitor.kt](app/src/main/java/com/focusmother/android/monitor/UsageMonitor.kt))
- Interfaces with `UsageStatsManager`
- Detects excessive usage patterns
- Provides usage statistics and analytics

#### 2. **MonitoringService** ([service/MonitoringService.kt](app/src/main/java/com/focusmother/android/service/MonitoringService.kt))
- Foreground service for continuous monitoring
- Checks usage every minute
- Triggers interventions when needed
- Manages cooldown periods (15 minutes between alerts)

#### 3. **MainActivity** ([ui/MainActivity.kt](app/src/main/java/com/focusmother/android/ui/MainActivity.kt))
- Material Design 3 UI with Jetpack Compose
- Shows screen time and top apps
- Toggle monitoring on/off
- Request permissions

#### 4. **Receivers**
- **BootReceiver**: Restarts monitoring after reboot
- **NotificationActionReceiver**: Handles notification button clicks

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Minimum Android version: 8.0 (API 26)
- Kotlin 1.9.22

### Build & Run

1. **Open in Android Studio**
   ```bash
   cd mobile/android
   # Open this folder in Android Studio
   ```

2. **Sync Gradle**
   - Android Studio will automatically sync Gradle files
   - Wait for dependencies to download

3. **Run on Device/Emulator**
   - Click "Run" (▶️) button
   - Select your device or emulator
   - App will install and launch

### Grant Permissions

**Critical**: App requires Usage Access permission to function!

1. When app first launches, you'll see a permission card
2. Tap "Grant Permission"
3. You'll be taken to Android Settings
4. Find "FocusMother" in the list
5. Toggle "Permit usage access" ON
6. Return to the app

## 📦 Project Structure

```
app/src/main/
├── java/com/focusmother/android/
│   ├── FocusMotherApplication.kt    # App class, notification channels
│   │
│   ├── monitor/
│   │   └── UsageMonitor.kt          # Usage stats monitoring
│   │
│   ├── service/
│   │   └── MonitoringService.kt     # Background monitoring service
│   │
│   ├── receiver/
│   │   ├── BootReceiver.kt          # Auto-start on boot
│   │   └── NotificationActionReceiver.kt  # Handle notification actions
│   │
│   └── ui/
│       ├── MainActivity.kt           # Main screen (Compose)
│       └── theme/
│           ├── Theme.kt              # Material 3 theme
│           └── Type.kt               # Typography
│
├── res/
│   ├── values/
│   │   ├── strings.xml               # String resources
│   │   └── themes.xml                # App themes
│   ├── drawable/
│   │   ├── ic_notification.xml       # Notification icon
│   │   ├── ic_check.xml              # Checkmark icon
│   │   └── ic_time.xml               # Clock icon
│   └── xml/
│       ├── backup_rules.xml
│       └── data_extraction_rules.xml
│
└── AndroidManifest.xml               # Permissions and components
```

## ⚙️ Configuration

### Monitoring Settings

Edit in [MonitoringService.kt](app/src/main/java/com/focusmother/android/service/MonitoringService.kt):

```kotlin
// Check interval (default: 1 minute)
private const val CHECK_INTERVAL_MS = 60_000L

// Intervention cooldown (default: 15 minutes)
private val interventionCooldown = 15 * 60 * 1000L

// Usage threshold (default: 30 minutes)
val detection = usageMonitor.detectContinuousUsage(
    thresholdMinutes = 30  // Change this value
)
```

### Detection Sensitivity

Edit in [UsageMonitor.kt](app/src/main/java/com/focusmother/android/monitor/UsageMonitor.kt):

```kotlin
// Trigger threshold (default: 80% of time period)
return UsageDetection(
    isExcessive = recentScreenTime >= thresholdMs * 0.8f,  // Change 0.8f
    // ...
)
```

## 🔔 Notifications

### Three Notification Channels

1. **Monitoring Service** (Low Priority)
   - Shows when service is running
   - Displays current screen time
   - Silent, ongoing notification

2. **Usage Alerts** (High Priority)
   - Intervention notifications
   - Vibrates to get attention
   - Action buttons: "Take a break" or "5 more minutes"

3. **Time Agreements** (Default Priority)
   - Future: Countdown timers
   - Agreement reminders

## 🧪 Testing

### Manual Testing Checklist

- [ ] App launches successfully
- [ ] Permission request shows on first launch
- [ ] Can grant Usage Access permission
- [ ] Screen time displays correctly
- [ ] Top apps list shows accurate data
- [ ] Can toggle monitoring on/off
- [ ] Service notification appears when monitoring
- [ ] Alert triggers after 30 minutes usage
- [ ] Cooldown prevents spam alerts
- [ ] Notification actions work ("Take break", "5 more minutes")
- [ ] Service restarts after device reboot

### Debug Mode

To test interventions quickly, reduce thresholds:

```kotlin
// In MonitoringService.kt
private const val CHECK_INTERVAL_MS = 10_000L  // 10 seconds (for testing)

// In performMonitoringCheck()
val detection = usageMonitor.detectContinuousUsage(
    thresholdMinutes = 1  // 1 minute (for testing)
)
```

## 🛠️ Building Release APK

### Debug Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
1. Create keystore (one-time):
   ```bash
   keytool -genkey -v -keystore focusmother.keystore -alias focusmother -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add to `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../focusmother.keystore")
               storePassword = "your_password"
               keyAlias = "focusmother"
               keyPassword = "your_password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
               // ...
           }
       }
   }
   ```

3. Build:
   ```bash
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```

## 📊 Future Enhancements

### Phase 2: API Integration
- [ ] Connect to backend API ([../../backend/api/](../../backend/api/))
- [ ] Sync agreements to cloud
- [ ] Cross-device synchronization
- [ ] User authentication

### Phase 3: Advanced Features
- [ ] Customizable intervention rules
- [ ] App-specific monitoring
- [ ] Focus modes (work, study, relax)
- [ ] Weekly/monthly reports
- [ ] Export usage data

### Phase 4: Machine Learning
- [ ] Predict when user needs a break
- [ ] Personalized thresholds
- [ ] Habit pattern recognition
- [ ] Smart suggestions

## 🐛 Known Issues

1. **Permission Reset**: Usage Access permission may reset after app updates
   - **Solution**: Re-grant permission in Android Settings

2. **Battery Optimization**: Some devices aggressively kill background services
   - **Solution**: Disable battery optimization for FocusMother

3. **Android 13+**: Notification permission required
   - **Solution**: App requests POST_NOTIFICATIONS permission automatically

## 📝 License

Part of FocusMotherFocus monorepo. See [../../README.md](../../README.md) for license details.

## 🤝 Contributing

See [../../CONTRIBUTING.md](../../CONTRIBUTING.md) for contribution guidelines.

## 🆘 Support

- **Documentation**: This README
- **Architecture**: [../../ARCHITECTURE.md](../../ARCHITECTURE.md)
- **Issues**: GitHub Issues
- **Backend API**: [../../backend/api/README.md](../../backend/api/README.md)

---

**Built with ❤️ using Kotlin + Jetpack Compose**

Minimum SDK: 26 | Target SDK: 34 | Kotlin: 1.9.22
