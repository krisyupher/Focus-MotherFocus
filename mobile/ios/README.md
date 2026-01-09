# iOS App

Native iOS application or iOS part of cross-platform framework.

## Setup

### For React Native
Automatically generated when running `npx react-native init`

### For Flutter
Automatically generated when running `flutter create`

### For Native iOS (Swift)

Create new project in Xcode:
1. New → App
2. Language: Swift
3. Interface: SwiftUI
4. Minimum iOS: 15.0

## Structure

```
ios/
├── FocusMotherFocus/
│   ├── App/
│   │   ├── FocusMotherFocusApp.swift
│   │   └── ContentView.swift
│   ├── Services/
│   │   ├── APIService.swift
│   │   └── MonitoringService.swift
│   ├── Views/
│   │   ├── MonitoringView.swift
│   │   ├── AgreementsView.swift
│   │   └── AvatarView.swift
│   └── Info.plist
├── Podfile
└── FocusMotherFocus.xcodeproj/
```

## Required Permissions

Add to `Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>To display your avatar counselor</string>
<key>NSMicrophoneUsageDescription</key>
<string>For voice interaction with counselor</string>
```

## Background Monitoring

iOS has strict background limitations. Use:
- Background Fetch
- Local Notifications
- Screen Time API (Family Controls framework)

```swift
import FamilyControls

// Request authorization
AuthorizationCenter.shared.requestAuthorization { result in
    // Handle result
}
```

## Build

```bash
# Install dependencies
pod install

# Open workspace
open FocusMotherFocus.xcworkspace

# Build
xcodebuild -workspace FocusMotherFocus.xcworkspace -scheme FocusMotherFocus build
```

## App Store Submission

1. Configure signing
2. Archive build
3. Upload to App Store Connect
4. Submit for review
