# CLAUDE.md - Unified Monitoring Version

This file provides guidance to Claude Code when working with the **unified monitoring** version of FocusMotherFocus.

## Project Overview

**Unified Website & Application Monitor** - A Python desktop application that monitors websites and/or desktop applications with a single interface. Each monitoring target can track:
- **Website only** (HTTP-reachable + open in browser)
- **Application only** (process running)
- **Both website AND application** (e.g., Netflix.com + Netflix.exe)

Built with Clean Architecture principles.

**Platform**: Windows-only (uses winsound, pywinauto, win32 APIs, psutil)

## Quick Start

### Running the Application
```bash
# Unified version (NEW - recommended)
python main_v2.py

# Original version (legacy - separate lists)
python main.py
```

### Adding a Target
```python
# Example: Netflix (monitors BOTH website and app)
add_target.execute(
    name="Netflix",
    url_string="netflix.com",
    process_name_string="Netflix.exe"
)
```

## Architecture

**Unified Architecture** - Single `MonitoringTarget` entity combines website + application monitoring.

### Core Components

**Entities:**
- `MonitoringTarget` - [src/core/entities/monitoring_target.py](src/core/entities/monitoring_target.py)
  - Has optional `url` and `process_name`
  - Alerts when EITHER condition is met
- `MonitoringSessionV2` - [src/core/entities/monitoring_session_v2.py](src/core/entities/monitoring_session_v2.py)
  - Manages collection of unified targets

**Use Cases:**
- `AddTargetUseCase` - [src/application/use_cases/add_target.py](src/application/use_cases/add_target.py)
- `RemoveTargetUseCase` - [src/application/use_cases/remove_target.py](src/application/use_cases/remove_target.py)
- `CheckTargetsUseCase` - [src/application/use_cases/check_targets.py](src/application/use_cases/check_targets.py)

**Infrastructure:**
- `WindowsProcessDetector` - [src/infrastructure/adapters/windows_process_detector.py](src/infrastructure/adapters/windows_process_detector.py)
- `JsonConfigRepositoryV2` - [src/infrastructure/persistence/json_config_repository_v2.py](src/infrastructure/persistence/json_config_repository_v2.py)

**GUI:**
- `UnifiedMonitorGUI` - [src/presentation/gui_v2.py](src/presentation/gui_v2.py)

## Key Business Logic

### Alert Logic
Alerts trigger when **EITHER** condition is true:
1. Website is reachable AND open in browser
2. Application process is running

### Examples

**Netflix (Hybrid):**
- Has URL: `https://netflix.com`
- Has Process: `Netflix.exe`
- Alert ON: Netflix.com open OR Netflix.exe running
- Alert OFF: Both closed/not running

**Google (Website Only):**
- Has URL: `https://google.com`
- No process
- Alert ON: Google.com open in browser
- Alert OFF: Google.com closed

**Calculator (App Only):**
- No URL
- Has Process: `calc.exe`
- Alert ON: calc.exe running
- Alert OFF: calc.exe not running

## Configuration Format

```json
{
  "targets": [
    {
      "id": "uuid-123",
      "name": "Netflix",
      "url": "https://netflix.com",
      "process_name": "Netflix.exe"
    },
    {
      "id": "uuid-456",
      "name": "Google",
      "url": "https://google.com",
      "process_name": null
    },
    {
      "id": "uuid-789",
      "name": "Calculator",
      "url": null,
      "process_name": "calc.exe"
    }
  ],
  "monitoring_interval": 10
}
```

## Development Commands

### Testing
```bash
# Run all tests
pytest

# Test specific layer
pytest tests/core/
pytest tests/application/
pytest tests/infrastructure/
```

### Building
```bash
# Build standalone .exe
build.bat
```

### Python Syntax Check
```bash
python -m py_compile main_v2.py
python -m py_compile src/presentation/gui_v2.py
```

## Common Patterns

### Adding Targets Programmatically
```python
from src.core.value_objects.url import URL
from src.core.value_objects.process_name import ProcessName

# Website only
session.add_target("Google", url=URL("google.com"))

# App only
session.add_target("Calculator", process_name=ProcessName("calc.exe"))

# Both (hybrid)
session.add_target(
    "Netflix",
    url=URL("netflix.com"),
    process_name=ProcessName("Netflix.exe")
)
```

### Checking Target Status
```python
for target in session.get_all_targets():
    print(f"{target.name}:")
    print(f"  Website: {target.has_website()}")
    print(f"  App: {target.has_application()}")
    print(f"  Hybrid: {target.is_hybrid()}")
    print(f"  Alerting: {target.is_alerting}")
```

## Migration from Original Version

The unified version automatically migrates old config format:

**Old (v1):**
```json
{
  "websites": ["https://google.com"],
  "applications": [{"process_name": "calc.exe", "display_name": "Calculator"}]
}
```

**New (v2):**
```json
{
  "targets": [
    {"name": "Google.Com", "url": "https://google.com", "process_name": null},
    {"name": "Calculator", "url": null, "process_name": "calc.exe"}
  ]
}
```

## Related Documentation

- [UNIFIED_MONITORING.md](UNIFIED_MONITORING.md) - Architecture explanation
- [USAGE_GUIDE.md](USAGE_GUIDE.md) - User guide
- [CLAUDE.md](CLAUDE.md) - Original version documentation

## Important Notes

- **Windows-only**: Uses `winsound`, `pywinauto`, `win32`, and `psutil`
- **Thread Safety**: Alerts scheduled on main thread using tkinter's `after()`
- **Immutability**: Value objects (`URL`, `ProcessName`) are frozen dataclasses
- **Clean Architecture**: Strict layer separation, dependency inversion throughout
- **Backward Compatible**: Can load old config format and migrate automatically
