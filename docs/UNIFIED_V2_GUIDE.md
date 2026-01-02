# Unified V2 System Guide

## Overview

The Unified V2 system allows you to monitor **both websites AND desktop applications** using a **single, simple interface**. Just type a name (like "Netflix"), and the system automatically detects whether to monitor the website, the app, or both.

## Key Features

- **Single Input Field**: No more separate checkboxes for website vs app
- **Auto-Detection**: Type "Netflix" → automatically monitors netflix.com + Netflix.exe
- **Smart Resolution**: Built-in knowledge of 40+ popular services
- **Flexible Monitoring**: Can monitor website only, app only, or both
- **Clean Architecture**: Fully refactored with proper separation of concerns

## How to Use

### Running the Application

```bash
python main_v2.py
```

### Adding Targets

Simply type a name in the input field and press "Add" or hit Enter:

**Examples:**
- `Netflix` → Monitors both netflix.com website AND Netflix.exe app
- `Spotify` → Monitors both open.spotify.com AND Spotify.exe
- `Calculator` → Monitors calc.exe (app only, no website)
- `Google` → Monitors google.com (website only, no app)
- `Steam` → Monitors both store.steampowered.com AND steam.exe

### Known Services (Pre-Configured)

The system knows about these popular services:

#### Streaming Services
- Netflix, Spotify, YouTube, Twitch, Hulu, Disney, Prime Video

#### Social Media
- Facebook, Instagram, Twitter, Reddit, TikTok, LinkedIn

#### Productivity
- Slack, Discord, Teams, Zoom

#### Gaming
- Steam, Epic Games, Origin

#### Search Engines
- Google, Bing

#### Email
- Gmail, Outlook

#### Development
- GitHub, Stack Overflow

#### Windows Apps (App Only)
- Notepad, Calculator, Paint, Explorer

### Auto-Generation for Unknown Apps

If you type something the system doesn't recognize, it automatically tries to generate both:
- **Website**: `name.com` (e.g., "myapp" → myapp.com)
- **Process**: `Name.exe` (e.g., "myapp" → Myapp.exe)

### How Alerts Work

Alerts trigger when **EITHER** condition is met:
- Website is online AND open in browser, **OR**
- Application process is running

This is an **OR** condition - you get alerted if either the website or app is active.

## Architecture

### File Structure

```
main_v2.py                                    # Unified composition root
src/
  core/
    entities/
      monitoring_target.py                    # Unified target (website + app)
      monitoring_session_v2.py                # Session managing unified targets
    services/
      target_resolver.py                      # Auto-resolves names to URL/process
    value_objects/
      url.py                                  # Website URL value object
      process_name.py                         # Process name value object
  application/
    use_cases/
      add_target.py                           # Add unified target
      remove_target.py                        # Remove target
      check_targets.py                        # Check both website & app
      start_monitoring_v2.py                  # Start monitoring (V2)
      stop_monitoring_v2.py                   # Stop monitoring (V2)
  infrastructure/
    adapters/
      windows_process_detector.py            # Detect running processes
      windows_browser_detector.py            # Detect browser tabs
      windows_alert_notifier.py              # Windows alerts
    persistence/
      json_config_repository_v2.py           # Persist unified config
  presentation/
    gui_v2.py                                # Unified GUI (single input)
```

### Config File Format

The `config.json` stores unified targets:

```json
{
  "targets": [
    {
      "id": "uuid-abc-123",
      "name": "Netflix",
      "url": "https://netflix.com",
      "process_name": "netflix.exe"
    },
    {
      "id": "uuid-def-456",
      "name": "Calculator",
      "url": null,
      "process_name": "calc.exe"
    },
    {
      "id": "uuid-ghi-789",
      "name": "Google",
      "url": "https://google.com",
      "process_name": null
    }
  ],
  "monitoring_interval": 10,
  "is_active": false
}
```

## Target Types

### 1. Hybrid (Website + App)
**Example**: Netflix
- Monitors: netflix.com AND Netflix.exe
- Alert: Triggers if EITHER website is open OR app is running

### 2. Website Only
**Example**: Google
- Monitors: google.com only
- Alert: Triggers if website is online and open in browser

### 3. App Only
**Example**: Calculator
- Monitors: calc.exe only
- Alert: Triggers if process is running

## Adding Custom Mappings

To add your own custom target mappings, edit `src/core/services/target_resolver.py`:

```python
# In KNOWN_TARGETS dictionary
KNOWN_TARGETS = {
    # ... existing mappings ...
    "myapp": ("myapp.com", "MyApp.exe"),  # Add this line
}
```

Or programmatically:

```python
from src.core.services.target_resolver import TargetResolver

TargetResolver.add_custom_mapping(
    name="MyApp",
    url="myapp.com",
    process="MyApp.exe"
)
```

## Development Workflow

### 1. Making Changes

**Core Logic** (entities, value objects):
- `src/core/entities/monitoring_target.py`
- `src/core/entities/monitoring_session_v2.py`
- `src/core/services/target_resolver.py`

**Business Logic** (use cases):
- `src/application/use_cases/`

**External Services** (adapters):
- `src/infrastructure/adapters/`

**GUI**:
- `src/presentation/gui_v2.py`

### 2. Testing

```bash
# Run all tests
pytest

# Run specific test
pytest tests/core/services/test_target_resolver.py

# Run with coverage
pytest --cov=src --cov-report=html
```

### 3. Building Executable

```bash
build.bat
```

This creates a standalone `.exe` in the `dist/` folder using PyInstaller.

## Comparison: V1 vs V2

| Feature | V1 (Original) | V2 (Unified) |
|---------|---------------|--------------|
| **Input Fields** | Separate website field | Single name field |
| **Entity Model** | Separate Website & Application | Unified MonitoringTarget |
| **Configuration** | Two lists (websites + apps) | Single targets list |
| **GUI** | Two sections | One unified section |
| **Auto-Resolution** | Manual entry required | Automatic detection |
| **Flexibility** | Website OR app | Website, app, OR both |

## Troubleshooting

### Issue: Target not detected correctly

**Solution**: Check `KNOWN_TARGETS` in `target_resolver.py` and add custom mapping if needed.

### Issue: Process name not matching

**Solution**: Process names are case-insensitive and normalized to lowercase with `.exe` extension.
- "Chrome" → "chrome.exe"
- "NOTEPAD.EXE" → "notepad.exe"

### Issue: Website URL not normalized

**Solution**: URLs are automatically normalized:
- "netflix.com" → "https://netflix.com"
- "google" → "https://google.com"

## Migration from V1

If you have an existing V1 config, the system automatically migrates it:

**V1 config:**
```json
{
  "websites": ["https://netflix.com", "https://google.com"],
  "monitoring_interval": 10
}
```

**Becomes V2 config:**
```json
{
  "targets": [
    {"name": "netflix.com", "url": "https://netflix.com", "process_name": null},
    {"name": "google.com", "url": "https://google.com", "process_name": null}
  ],
  "monitoring_interval": 10
}
```

## Contributing

When adding new features:

1. **Add to Core Layer** first (entities, value objects)
2. **Define Interface** in Application Layer
3. **Implement Adapter** in Infrastructure Layer
4. **Update GUI** in Presentation Layer
5. **Wire in main_v2.py**
6. **Write Tests** for each layer

Follow Clean Architecture principles - dependencies always point inward.

## Support

For issues, check:
- [CLAUDE.md](CLAUDE.md) - Architecture overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed design
- [README.md](README.md) - User documentation
