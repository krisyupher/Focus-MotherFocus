# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FocusMother is a native Android app built with Kotlin that monitors phone usage and helps users stay focused through AI-powered conversations. A 3D AI avatar named "Zordon" negotiates time agreements with users when excessive usage is detected. The app uses Claude API for conversations and Ready Player Me for avatar generation.

## Build & Development Commands

```bash
# Build
./gradlew assembleDebug                    # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease                  # Release APK (requires keystore)
./gradlew installDebug                     # Install on device

# Testing
./gradlew test                             # Unit tests
./gradlew connectedAndroidTest             # Instrumented tests (requires device/emulator)
./gradlew test --tests "*.ClassName"       # Single test class
./gradlew test --tests "*SmokeTest"        # Run smoke tests

# Quality checks
./gradlew lint                             # Android lint
./gradlew ktlintCheck                      # Kotlin code style
./gradlew detekt                           # Static code analysis
./gradlew test lint ktlintCheck detekt     # All quality checks
```

## Architecture

### Layer Structure

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer                                               │
│  Activities + Compose Screens + ViewModels              │
├─────────────────────────────────────────────────────────┤
│  Domain Layer                                           │
│  NegotiationManager, AgreementEnforcer, CategoryManager │
├─────────────────────────────────────────────────────────┤
│  Data Layer                                             │
│  Repositories + Room Database + API Services            │
├─────────────────────────────────────────────────────────┤
│  Service Layer                                          │
│  MonitoringService (foreground) + Receivers             │
└─────────────────────────────────────────────────────────┘
```

### Key Components

**MonitoringService** (`service/MonitoringService.kt`)
- Foreground service checking usage every 2 seconds (testing) / 1 minute (production)
- Priority: agreement violations → adult content → category thresholds → continuous usage
- Launches `ConversationActivity` when intervention is needed
- Supports snooze (5 min) and quiet hours

**NegotiationManager** (`domain/NegotiationManager.kt`)
- State machine: Initial → ProposedTime → Negotiating → AgreementReached/Rejected
- Max 3 negotiation rounds before forcing agreement
- Uses `ResponseParser` to extract time durations from user messages

**Room Database** (`data/database/FocusMotherDatabase.kt`)
- Entities: `Agreement`, `ConversationMessage`, `AppCategoryMapping`, `AvatarConfig`
- DAOs provide Flow-based queries for reactive UI updates

**ConversationRepository** (`data/repository/ConversationRepository.kt`)
- Rate limiting: 1 request/second minimum, 100 requests/hour max
- Prompt caching to reduce API costs
- Auto-prunes messages older than 30 days

**CategoryManager** (`domain/CategoryManager.kt`)
- App categorization with configurable thresholds per category
- Pre-seeded categories for common apps (social, games, entertainment, etc.)

### Data Flow: Intervention Trigger

1. `MonitoringService` detects threshold exceeded
2. Launches `ConversationActivity` with context (app, reason)
3. `ConversationViewModel` manages chat with Claude API
4. `NegotiationManager` tracks negotiation state
5. When `AgreementReached`, `AgreementRepository` persists to Room
6. `AgreementEnforcer` validates active agreements on subsequent checks

## Testing Thresholds

Currently set for testing (low values for quick triggers):

| Constant | Location | Testing | Production |
|----------|----------|---------|------------|
| CHECK_INTERVAL_MS | MonitoringService:342 | 2000 (2s) | 60000 (1m) |
| interventionCooldown | MonitoringService:44 | 30s | 15m |
| detectContinuousUsage | MonitoringService:195 | 1 min | 30 min |

## Permissions

**Critical** - App requires Usage Access permission (Settings → Apps → Special access):
- `PACKAGE_USAGE_STATS` - Monitor app usage
- `FOREGROUND_SERVICE` - Background monitoring
- `POST_NOTIFICATIONS` (Android 13+) - Alerts

Check permission: `UsageMonitor.hasUsageStatsPermission()`

## API Integration

**Claude API** (`data/api/ClaudeApiService.kt`)
- Model: `claude-3-5-sonnet-20241022`
- Max tokens: 300 (short responses)
- API key stored via `SecureApiKeyProvider`

**Ready Player Me** (`data/api/ReadyPlayerMeApiService.kt`)
- 3D avatar generation from selfie
- GLB model rendering via SceneView

## Dependencies

- **Min SDK**: 28 (SceneView requirement)
- **Target SDK**: 34
- **Java**: 17 (required for Kotlin 2.0 and Android Gradle plugin)
- **Kotlin**: 2.0.0
- **Room**: 2.6.1 (KSP compiler)
- **SceneView**: 2.0.0 (3D avatar rendering)
- **CameraX**: 1.3.1 (selfie capture)

## Security Notes

- Certificate pinning enabled for `api.anthropic.com` and `api.readyplayer.me`
- ProGuard enabled for release builds with obfuscation
- Secure file deletion (DoD 5220.22-M) for avatar images (GDPR compliance)
- API rate limiting prevents cost exploitation

## CI/CD

GitHub Actions workflows in `.github/workflows/`:
- `android-ci.yml` - Runs 4 parallel jobs on push/PR:
  - `test`: lint, unit tests, coverage report, debug APK build
  - `security-scan`: dependency vulnerability check, TruffleHog secret scanning
  - `build-quality`: ktlint, detekt, APK size analysis
  - `ci-summary`: aggregates results
- `android-release.yml` - Signed release on version tags
- `health-check.yml` - Post-deployment validation

Release process: `git tag -a v1.0.0 -m "Release" && git push origin v1.0.0`

See `KEYSTORE_SETUP.md` for release signing configuration.
