# FocusMotherFocus Shared Core

Shared business logic and domain models used across all platforms (Desktop, Android, iOS, Web).

## Structure

```
backend/shared/
├── core/
│   ├── entities/          # Domain entities
│   │   ├── agreement.py
│   │   ├── monitoring_target.py
│   │   └── ...
│   └── value_objects/     # Value objects
│       ├── url.py
│       └── process_name.py
│
└── services/              # Shared business services
    ├── behavioral_analyzer.py
    ├── agreement_tracker.py
    └── ...
```

## Usage

### In API (FastAPI)

```python
from backend.shared.core.entities.agreement import Agreement
from backend.shared.services.behavioral_analyzer import analyze_behavior

# Use shared logic
agreement = Agreement(...)
behavior_result = analyze_behavior(...)
```

### In Mobile (via API)

Mobile clients call the API which uses this shared logic internally.

### In Desktop

Desktop app can import directly:

```python
from backend.shared.core.entities.agreement import Agreement
```

Or continue using its own copy in `desktop/src/core/`.

## Philosophy

This shared core contains:
- ✅ Pure business logic (no platform dependencies)
- ✅ Domain models and entities
- ✅ Business rules and validation
- ❌ NO UI code
- ❌ NO platform-specific code
- ❌ NO external service dependencies

## Testing

```bash
cd backend/shared
pytest
```
