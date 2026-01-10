# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FocusMother is a native Android app built with Kotlin that monitors phone usage and helps users stay focused by detecting excessive screen time. It uses a foreground service to continuously monitor usage patterns and trigger interventions when users spend too much time on their phones or distraction apps.

## Build & Development Commands

### Build Commands
```bash
# Debug build
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release build
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.focusmother.android.MonitoringServiceTest"
```

## Architecture

### Three-Layer Architecture

**UI Layer**: Jetpack Compose + MainActivity
- Material Design 3 UI in `ui/MainActivity.kt`
- Shows screen time, top apps, monitoring status
- Requests permissions and controls service

**Service Layer**: Background monitoring
- `service/MonitoringService.kt` - Foreground service that checks usage every 5 seconds (configurable)
- Manages intervention cooldown periods (30 seconds default, configurable)
- `receiver/BootReceiver.kt` - Restarts monitoring after device reboot
- `receiver/NotificationActionReceiver.kt` - Handles notification button actions

**Monitor Layer**: Usage detection
- `monitor/UsageMonitor.kt` - Interfaces with Android's `UsageStatsManager`
- Detects excessive usage patterns (80% threshold)
- Provides usage statistics and top apps

### Key Data Flow

1. **MonitoringService** runs in foreground → checks usage every interval
2. **UsageMonitor** queries `UsageStatsManager` → returns screen time and app usage
3. Service detects excessive usage → triggers intervention notification
4. User responds to notification → actions handled by `NotificationActionReceiver`
5. If distraction app detected → automatically closes app and shows MainActivity

### Critical Constants (MonitoringService.kt)

These values are currently set for testing and should be adjusted:

```kotlin
// Line 26: Intervention cooldown
private val interventionCooldown = 30 * 1000L // 30 seconds (testing)
// Production: 15 * 60 * 1000L (15 minutes)

// Line 75: Usage threshold for distraction apps
val usageThreshold = 5 * 60 * 1000L // 5 minutes (testing)
// Production: 30 * 60 * 1000L (30 minutes)

// Line 84: Continuous usage threshold
val detection = usageMonitor.detectContinuousUsage(thresholdMinutes = 1) // Testing
// Production: thresholdMinutes = 30

// Line 209: Check interval
private const val CHECK_INTERVAL_MS = 5000L // 5 seconds (testing)
// Production: 60_000L (1 minute)
```

### Distraction Apps List (MonitoringService.kt:29-37)

Hardcoded set of package names for apps considered "distracting":
- Facebook, Instagram, Twitter, TikTok, YouTube
- Add/remove packages in `distractionPackages` set

## Permissions Required

**Critical**: App will not function without Usage Access permission

1. `PACKAGE_USAGE_STATS` - Monitor app usage (granted through Settings)
2. `FOREGROUND_SERVICE` - Run monitoring service in background
3. `RECEIVE_BOOT_COMPLETED` - Auto-start after reboot
4. `POST_NOTIFICATIONS` (Android 13+) - Show alerts

Permission check: `UsageMonitor.hasUsageStatsPermission()` returns true if granted

## Notification Channels

Defined in `FocusMotherApplication.kt`:

1. **monitoring_service** (Low priority) - Ongoing foreground service notification
2. **usage_alerts** (High priority) - Intervention alerts with vibration
3. **time_agreements** (Default priority) - Future: countdown timers

## Dependencies

- **Kotlin**: 2.0.0
- **Compose BOM**: 2024.02.00
- **Min SDK**: 26 (Android 8.0 - required for UsageStatsManager)
- **Target SDK**: 34
- **Java**: 17

Key libraries:
- Jetpack Compose + Material3
- Coroutines (for async operations)
- DataStore (for preferences)
- Retrofit + Gson (for future API integration)
- Room (for local storage, not yet implemented)
- WorkManager (for background tasks, not yet implemented)

## Important Implementation Details

### Usage Detection Algorithm (UsageMonitor.kt:93-108)

Detects continuous usage by comparing recent screen time against a threshold:
- Queries screen time for last N minutes
- Triggers if usage >= 80% of threshold period
- Example: 30-minute threshold → triggers at 24+ minutes of usage

### Service Lifecycle

- Service runs as **foreground service** to prevent being killed
- Returns `START_STICKY` - system will restart if killed
- Uses coroutines with `SupervisorJob` for robust error handling
- Cancels all jobs in `onDestroy()`

### Intervention Flow

1. Check if current app is in `distractionPackages` OR continuous usage detected
2. Verify `shouldTriggerIntervention()` (respects cooldown)
3. Call `triggerIntervention()` → shows notification
4. If distraction app → call `closeDistractionApp()` → brings MainActivity to front

## Testing Considerations

The codebase is currently configured with aggressive testing values to make interventions trigger quickly. Before production:

1. Reset all timing constants in `MonitoringService.kt` to production values
2. Adjust detection threshold in `UsageMonitor.kt` if needed (currently 80%)
3. Consider adding debug/release build variants with different thresholds

## Future Integration

The app includes Retrofit and Room dependencies for planned features:
- Backend API integration (`backend/api/` in parent repo)
- Cloud sync of agreements and usage data
- User authentication
- Cross-device monitoring

These are not yet implemented but dependencies are in place.
