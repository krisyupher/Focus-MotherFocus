# FocusMotherFocus - Multi-Platform Architecture

## Overview

FocusMotherFocus is organized as a **monorepo** with three main components:

```
┌─────────────────────────────────────────────────────────┐
│                     MONOREPO                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Desktop    │  │   Backend    │  │   Mobile     │  │
│  │   (Python)   │  │   (FastAPI)  │  │ (iOS/Android)│  │
│  │              │  │              │  │              │  │
│  │  Standalone  │  │   REST API   │  │ React Native │  │
│  │    or uses   │◄─┤              ├─►│  / Flutter   │  │
│  │     API      │  │              │  │  / Native    │  │
│  └──────────────┘  └──────┬───────┘  └──────────────┘  │
│                           │                             │
│                    ┌──────▼───────┐                     │
│                    │    Shared    │                     │
│                    │     Core     │                     │
│                    │   (Python)   │                     │
│                    └──────────────┘                     │
└─────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Desktop Application (`desktop/`)

**Purpose**: Standalone Windows/Mac/Linux desktop application

**Technology Stack**:
- Python 3.13+
- Tkinter (GUI)
- OpenCV (Avatar animation)
- OpenAI GPT-4o-mini (AI)
- Windows APIs (Process monitoring)

**Architecture**: Clean Architecture with 4 layers
```
Presentation → Application → Infrastructure → Core
```

**Operation Modes**:
- **Standalone**: Runs independently, all logic local
- **Connected**: Can optionally use Backend API for sync

**Status**: ✅ Complete (All 4 phases implemented)

### 2. Backend API (`backend/api/`)

**Purpose**: REST API server for mobile clients and desktop sync

**Technology Stack**:
- FastAPI (Web framework)
- Pydantic (Data validation)
- Uvicorn (ASGI server)
- SQLite/PostgreSQL (Database - future)

**Endpoints**:
```
/api/v1/monitoring/*   - Activity monitoring
/api/v1/agreements/*   - Time agreements
/api/v1/avatar/*       - Counselor interactions
```

**Status**: 🟡 Scaffold ready (needs implementation)

### 3. Shared Core (`backend/shared/`)

**Purpose**: Shared business logic and domain models

**Contents**:
- Domain entities (Agreement, MonitoringTarget, etc.)
- Value objects (URL, ProcessName, etc.)
- Business rules and validation

**Used By**:
- Desktop app (imports directly)
- Backend API (imports directly)
- Mobile apps (via API, indirectly)

**Principles**:
- Pure Python (no external dependencies)
- No UI code
- No platform-specific code
- No infrastructure code

### 4. Mobile Applications (`mobile/`)

**Purpose**: iOS and Android mobile apps

**Framework Options**:
1. **React Native** (JavaScript/TypeScript)
2. **Flutter** (Dart)
3. **.NET MAUI** (C#)
4. **Native** (Kotlin + Swift)

**Communication**: All mobile apps use Backend API via REST

**Features**:
- View monitoring targets
- Start/stop monitoring
- Receive interventions
- Manage agreements
- View avatar counselor

**Status**: 📋 Ready for development

## Data Flow

### Desktop Standalone Mode
```
User Action → GUI → Use Case → Adapter → External Service
                      ↓
                   Entity ← Core Logic
```

### Mobile + API Mode
```
Mobile App → HTTP Request → API Endpoint → Use Case → Shared Core
                                              ↓
                                           Database
                                              ↓
                                       HTTP Response ← JSON
```

### Desktop + API Sync Mode (Future)
```
Desktop App ←→ Backend API ←→ Cloud Storage ←→ Mobile App
                    ↓
               Shared Core
```

## Deployment Scenarios

### Scenario 1: Desktop Only
User runs `desktop/main_v2.py` directly.
- No server needed
- All processing local
- Data stored locally

### Scenario 2: Mobile + Server
1. Deploy Backend API to cloud (Heroku, AWS, etc.)
2. Mobile apps connect to API URL
3. Data stored in cloud database

### Scenario 3: Multi-Platform Sync (Future)
1. Backend API running in cloud
2. Desktop app syncs to cloud
3. Mobile apps sync to cloud
4. All devices see same data

## Development Workflow

### Working on Desktop
```bash
cd desktop
pip install -r requirements.txt
python main_v2.py
```

### Working on Backend
```bash
cd backend/api
pip install -r requirements.txt
python main.py
# API at http://localhost:8000
```

### Working on Mobile
```bash
cd mobile
# Choose framework, initialize project
# Configure API endpoint
# Start development
```

## Technology Decisions

### Why Python for Desktop?
- Rapid development
- Rich ecosystem (Windows APIs, OpenCV, AI libraries)
- Easy to build executable (PyInstaller)

### Why FastAPI for Backend?
- High performance (async)
- Automatic API documentation (Swagger)
- Modern Python features (type hints)
- Easy to deploy

### Why Monorepo?
- Shared code in one place
- Easier to keep in sync
- Single version control
- Better collaboration

### Why Multiple Mobile Options?
- Let developers choose their preference
- Different projects have different needs
- All consume same API anyway

## Security Considerations

### Desktop
- API keys stored in `.env` (gitignored)
- Local data only (no cloud sync yet)

### Backend API
- CORS configured for mobile origins
- Authentication needed (future: JWT)
- Rate limiting (future)
- Input validation (Pydantic)

### Mobile
- HTTPS only for API calls
- Secure storage for tokens
- No sensitive data in local storage

## Scalability

### Current State
- Desktop: Single user, local processing
- Backend: Ready for multiple users
- Mobile: Not yet implemented

### Future Scaling
- Database: SQLite → PostgreSQL
- Caching: Redis for frequent queries
- Background jobs: Celery for async tasks
- WebSocket: Real-time interventions

## Testing Strategy

### Desktop
```bash
cd desktop
pytest  # 87 tests
```

### Backend
```bash
cd backend/api
pytest  # To be implemented
```

### Mobile
Framework-specific testing:
- React Native: Jest
- Flutter: flutter test
- Native: JUnit/XCTest

## Build & Deployment

### Desktop
```bash
cd desktop
build.bat  # Creates Windows executable
```

### Backend
```bash
cd backend/api
docker build -t focusmother-api .
docker run -p 8000:8000 focusmother-api
```

### Mobile
- **Android**: `./gradlew assembleRelease`
- **iOS**: Xcode Archive → App Store
- **React Native**: `npx react-native run-android/ios`
- **Flutter**: `flutter build apk/ios`

## Next Steps

1. **Implement Backend API**: Connect to shared core, add database
2. **Choose Mobile Framework**: React Native recommended
3. **Build Mobile App**: Implement UI + API integration
4. **Add Authentication**: JWT tokens for multi-user support
5. **Cloud Deployment**: Deploy API to production
6. **Desktop Sync**: Connect desktop to API (optional)

## Questions?

- **Desktop**: See [desktop/docs/](desktop/docs/)
- **Backend**: See [backend/api/README.md](backend/api/README.md)
- **Mobile**: See [mobile/README.md](mobile/README.md)
- **Shared Core**: See [backend/shared/README.md](backend/shared/README.md)
