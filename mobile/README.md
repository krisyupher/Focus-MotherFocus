# FocusMotherFocus Mobile

Cross-platform mobile applications for Android and iOS.

## Choose Your Framework

### Option 1: React Native (Recommended)
Most popular, large ecosystem, JavaScript/TypeScript

```bash
cd mobile/
npx react-native init FocusMotherFocus
```

### Option 2: Flutter
Excellent performance, Dart language, beautiful UI

```bash
cd mobile/
flutter create focusmother_focus
```

### Option 3: Native Development
Best performance, platform-specific

- **Android**: Android Studio + Kotlin
- **iOS**: Xcode + Swift

### Option 4: .NET MAUI
C# developers, Microsoft ecosystem

```bash
cd mobile/
dotnet new maui -n FocusMotherFocus
```

## Project Structure (Example with React Native)

```
mobile/
├── android/              # Android native code
├── ios/                  # iOS native code
├── src/
│   ├── screens/
│   │   ├── MonitoringScreen.tsx
│   │   ├── AgreementsScreen.tsx
│   │   └── AvatarScreen.tsx
│   ├── components/
│   │   ├── AvatarView.tsx
│   │   └── CountdownTimer.tsx
│   ├── services/
│   │   └── api.ts        # API client
│   └── App.tsx
├── package.json
└── README.md
```

## API Integration

All mobile apps connect to the backend API:

```typescript
// Example API client
const API_BASE_URL = 'http://your-server:8000/api/v1';

async function startMonitoring(userId: string) {
  const response = await fetch(`${API_BASE_URL}/monitoring/sessions/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ user_id: userId })
  });
  return response.json();
}
```

## Features to Implement

### Phase 1: Basic Monitoring
- [ ] View monitoring targets
- [ ] Add/remove targets
- [ ] Start/stop monitoring
- [ ] View alerts

### Phase 2: Avatar Counselor
- [ ] Display avatar
- [ ] Voice interaction
- [ ] Time negotiation

### Phase 3: Agreements
- [ ] View active agreements
- [ ] Countdown timers
- [ ] Notifications

### Phase 4: Settings
- [ ] Configure monitoring rules
- [ ] Customize avatar
- [ ] Notification preferences

## Platform-Specific Notes

### Android
- Requires background service for monitoring
- Push notifications for alerts
- AccessibilityService for app monitoring

### iOS
- Screen Time API for monitoring
- Local notifications
- Background fetch limitations

## Getting Started

1. **Choose a framework** (see options above)
2. **Set up project** in `mobile/` folder
3. **Configure API endpoint** in environment config
4. **Build and run** on your device/emulator

## Testing

```bash
# React Native
npm test

# Flutter
flutter test

# Android
./gradlew test

# iOS
xcodebuild test
```
