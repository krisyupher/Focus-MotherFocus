# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Unified Website & Application Monitor** - A Python desktop application that monitors both websites AND desktop applications, generating alerts when they are active. Built with Clean Architecture principles.

**Platform**: Windows-only (uses winsound, pywinauto, win32 APIs for browser detection and process monitoring)

**Current Version**: V2 (Unified) - Single-input interface with auto-detection
- V1 (`main.py`): Original website-only monitoring
- **V2 (`main_v2.py`)**: Unified website + application monitoring [ACTIVE VERSION]

## Development Commands

### Running the Application
```bash
# V2 (Unified - RECOMMENDED)
python main_v2.py

# V1 (Legacy - website only)
python main.py
```

### Testing
```bash
# Run all tests with coverage
pytest

# Run specific test file
pytest tests/core/entities/test_website.py

# Run specific test function
pytest tests/application/use_cases/test_add_website.py::test_add_website_success

# Run tests by marker
pytest -m unit
pytest -m integration

# Run with verbose output
pytest -v

# Generate coverage report
pytest --cov=src --cov-report=html
```

### Installing Dependencies
```bash
pip install -r requirements.txt
```

### Building Executable
```bash
# Build standalone .exe using PyInstaller
build.bat

# Or manually:
pyinstaller --clean FocusMonitor.spec
```

### Python Syntax Check
```bash
python -m py_compile <file.py>
```

## Architecture

This codebase follows **Clean Architecture** with strict layer separation and dependency inversion. Originally a 600-line monolithic [index.py](index.py), it has been refactored into a layered architecture.

### Layer Dependencies (dependencies point inward only)
```
Presentation → Application → Core ← Infrastructure
```

### Key Architectural Concepts

**Composition Root**: [main.py](main.py) is the dependency injection root. All layers are instantiated and wired together here. Never inject dependencies elsewhere.

**Dependency Inversion**: High-level modules depend on abstractions (interfaces), not concrete implementations. All interfaces are defined in [src/application/interfaces/](src/application/interfaces/).

**Use Case Pattern**: All application logic is encapsulated in use cases at [src/application/use_cases/](src/application/use_cases/). The GUI layer delegates to use cases, never contains business logic.

**Aggregate Root**: `MonitoringSession` at [src/core/entities/monitoring_session.py](src/core/entities/monitoring_session.py) is the aggregate root that controls all website operations.

### Core Layer ([src/core/](src/core/))
- **Zero external dependencies** - only pure Python
- **Entities**: `Website`, `MonitoringSession` - domain objects with business logic
- **Value Objects**: `URL` - immutable, self-validating types
- Never imports from other layers

### Application Layer ([src/application/](src/application/))
- **Depends only on Core layer**
- **Interfaces (Ports)**: Define contracts for external services
  - `IHttpChecker` - check website availability
  - `IAlertNotifier` - send/clear alerts
  - `IBrowserDetector` - detect if URL is open in browser
  - `IConfigRepository` - persist configuration
  - `IMonitoringScheduler` - schedule periodic tasks
  - `IStartupManager` - manage auto-startup on system boot
- **Use Cases**: Orchestrate domain entities
  - `AddWebsiteUseCase`, `RemoveWebsiteUseCase`
  - `StartMonitoringUseCase`, `StopMonitoringUseCase`
  - `CheckWebsitesUseCase` - core monitoring logic

### Infrastructure Layer ([src/infrastructure/](src/infrastructure/))
- **Implements application interfaces**
- **Adapters**:
  - `RequestsHttpChecker` - HTTP checking via requests library
  - `WindowsAlertNotifier` - Windows pop-ups and sound alerts
  - `WindowsBrowserDetector` - Browser tab detection (uses psutil, pywinauto, win32)
  - `WindowsStartupManager` - Auto-startup configuration via Windows registry
  - `ThreadedScheduler` - Threading-based periodic scheduler
- **Persistence**:
  - `JsonConfigRepository` - stores config in [config.json](config.json)

### Presentation Layer ([src/presentation/](src/presentation/))
- **Thin layer with no business logic**
- `WebsiteMonitorGUI` at [src/presentation/gui.py](src/presentation/gui.py) - Tkinter GUI
- Receives all use cases via dependency injection
- Delegates all operations to use cases

## Key Business Logic

### Alert Triggering Logic
Alerts trigger when BOTH conditions are met (see [src/application/use_cases/check_websites.py:59](src/application/use_cases/check_websites.py#L59)):
1. Website is HTTP-reachable (returns 200)
2. Website is open in a browser tab (if browser detector is available)

If either condition becomes false, alerts are cleared automatically.

### Browser Detection
The application uses `WindowsBrowserDetector` to detect if a URL is open in Chrome, Firefox, or Edge browser tabs. This integrates with the monitoring loop to only alert when a website is both online AND actively open in a browser.

## Testing Strategy

### Test Structure
- Tests mirror the source structure: `tests/core/`, `tests/application/`, `tests/infrastructure/`
- Shared fixtures in [tests/conftest.py](tests/conftest.py)

### Testing Principles by Layer
1. **Core Layer**: Pure unit tests, no mocks, fast execution
2. **Application Layer**: Use mocks for interfaces (IHttpChecker, IAlertNotifier, etc.)
3. **Infrastructure Layer**: May use real dependencies, integration test characteristics

### Pytest Configuration
- Config in [pytest.ini](pytest.ini)
- Markers: `@pytest.mark.unit`, `@pytest.mark.integration`, `@pytest.mark.slow`
- Coverage target: 90%+ (currently achieved)

## Adding New Features

To add a new feature (e.g., email notifications):

1. **Define interface** in `src/application/interfaces/`:
```python
class IEmailNotifier(ABC):
    @abstractmethod
    def send_email(self, url: URL, message: str) -> None:
        pass
```

2. **Implement adapter** in `src/infrastructure/adapters/`:
```python
class SmtpEmailNotifier(IEmailNotifier):
    def send_email(self, url: URL, message: str) -> None:
        # Implementation
```

3. **Update use case** to accept the new interface via constructor
4. **Wire in composition root** ([main.py](main.py))
5. **Write tests** at each layer

**Important**: Never modify existing core layer or use cases. Extend via new interfaces and adapters.

## Configuration

[config.json](config.json) stores:
- List of monitored websites
- Monitoring interval in seconds

Format:
```json
{
    "websites": ["https://example.com"],
    "monitoring_interval": 10
}
```

## Common Patterns

### Creating Value Objects
```python
from src.core.value_objects.url import URL

url = URL("google.com")  # Automatically normalized to https://google.com
# url.value = "other"  # Error: immutable!
```

### Working with MonitoringSession
```python
session = MonitoringSession()
website = session.add_website(URL("example.com"))  # Returns Website entity
session.start()  # Validates state before starting
session.get_all_websites()  # Returns list of Website entities
```

### Dependency Injection in Tests
```python
def test_example(mock_http_checker, mock_config_repository):
    use_case = AddWebsiteUseCase(
        session=MonitoringSession(),
        config_repository=mock_config_repository
    )
    # Test the use case
```

## Important Notes

- **Windows-only**: Uses `winsound`, `pywinauto`, and `win32` APIs (registry, COM)
- **Thread Safety**: Alert pop-ups are scheduled on main thread using tkinter's `after()` method
- **Immutability**: Value objects (like `URL`) are frozen dataclasses
- **No Framework Dependencies in Core**: Core layer has zero external dependencies
- **Legacy Version**: [index.py](index.py) contains the original monolithic implementation for reference
- **Distribution**: Use [build.bat](build.bat) or PyInstaller with [FocusMonitor.spec](FocusMonitor.spec) to create standalone executable

## V2 Unified Architecture (CURRENT)

The V2 system implements **unified monitoring** where a single target can monitor website, application, or both.

### Key V2 Components

**Core Layer**:
- `MonitoringTarget` - Unified entity with optional URL and process_name
- `MonitoringSessionV2` - Manages collection of unified targets
- `TargetResolver` - Auto-resolves names (e.g., "Netflix") to URL + process name
- `ProcessName` - Value object for Windows process names

**Application Layer**:
- `AddTargetUseCase` - Add unified targets
- `CheckTargetsUseCase` - Check BOTH website and application status (OR logic)
- `StartMonitoringV2UseCase` - Start unified monitoring
- `StopMonitoringV2UseCase` - Stop unified monitoring

**Infrastructure Layer**:
- `WindowsProcessDetector` - Detect running Windows processes (uses psutil)
- `JsonConfigRepositoryV2` - Persist unified targets with backward compatibility
- `WindowsBrowserDetector` - Detect browser tabs (unchanged from V1)

**Presentation Layer**:
- `UnifiedMonitorGUI` (gui_v2.py) - Single input field interface

### V2 Alert Logic

Alerts trigger when **EITHER** condition is met (OR logic):
1. Website is HTTP-reachable AND open in browser tab, **OR**
2. Application process is running

### V2 Auto-Resolution

TargetResolver maps simple names to monitoring configuration:
```python
"Netflix" -> URL("netflix.com") + ProcessName("Netflix.exe")
"Calculator" -> None + ProcessName("calc.exe")
"Google" -> URL("google.com") + None
```

Pre-configured knowledge of 40+ popular services (streaming, social media, productivity, gaming, etc.)

### V2 Config Format

```json
{
  "targets": [
    {
      "id": "uuid",
      "name": "Netflix",
      "url": "https://netflix.com",
      "process_name": "netflix.exe"
    }
  ],
  "monitoring_interval": 10
}
```

## Related Documentation

- [UNIFIED_V2_GUIDE.md](UNIFIED_V2_GUIDE.md) - **V2 usage guide and architecture**
- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture guide with examples
- [README.md](README.md) - User-facing documentation
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Migration summary
