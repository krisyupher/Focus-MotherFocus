# FocusMotherFocus - Project Structure

**Last Updated**: January 3, 2026
**Status**: Organized and Production-Ready

---

## 📁 Directory Overview

```
FocusMotherFocus/
├── 🚀 main_v2.py                    # MAIN ENTRY POINT - Run this!
├── 📦 requirements.txt               # Python dependencies
├── ⚙️  pytest.ini                    # Test configuration
├── 🔨 build.bat                      # Build executable
├── 📝 README.md                      # Project overview
├── 🔒 .gitignore                     # Git ignore rules
│
├── 📚 docs/                          # Documentation
│   ├── user/                         # User guides
│   ├── technical/                    # Technical documentation
│   ├── features/                     # Feature guides
│   ├── phases/                       # Phase completion summaries
│   ├── fixes/                        # Bug fix documentation
│   └── archive/                      # Legacy documentation
│
├── 🎯 src/                           # Source code (Clean Architecture)
│   ├── core/                         # Domain layer
│   ├── application/                  # Use cases layer
│   ├── infrastructure/               # External services
│   ├── presentation/                 # UI layer
│   └── mcp_servers/                  # MCP server implementations
│
├── 💡 examples/                      # Demo applications
│   ├── basic/                        # Basic MCP demos
│   ├── features/                     # Feature demos
│   └── mcp/                          # Advanced MCP examples
│
├── 🧪 tests/                         # Test suites (87 tests)
│   ├── core/
│   ├── application/
│   └── infrastructure/
│
├── ⚙️  config/                       # Configuration files
│   ├── config.json                   # App configuration
│   └── mcp_client_config.json        # MCP configuration
│
└── 🛠️  scripts/                      # Utility scripts
    └── organize_repo.py
```

---

## 🚀 Entry Points

### Main Application
**File**: [main_v2.py](main_v2.py)
**Purpose**: Primary entry point for production use
**Run**: `python main_v2.py`

Launches the Avatar Counselor GUI with all 4 phases integrated:
- Phase 1: Behavioral Analysis
- Phase 2: Avatar Counselor & Negotiation
- Phase 3: Agreement Enforcement
- Phase 4: MCP Service Orchestration

---

## 📚 Documentation Structure

### [`docs/user/`](docs/user/) - User Guides
Quick start and usage guides for end users.

| File | Description |
|------|-------------|
| `QUICKSTART.md` | Quick installation and first run |
| `QUICKSTART_PHASES.md` | Phase-by-phase quick guide |
| `READY_TO_RUN.md` | Complete setup verification |
| `FINAL_SETUP.md` | Production deployment guide |

### [`docs/technical/`](docs/technical/) - Technical Documentation
Architecture and developer documentation.

| File | Description |
|------|-------------|
| `CLAUDE.md` | Developer guidelines for Claude Code |
| `SYSTEM_STATUS.md` | Complete system status report |
| `IMPLEMENTATION_STATUS.md` | Implementation progress tracker |

### [`docs/features/`](docs/features/) - Feature Guides
Detailed feature documentation.

| File | Description |
|------|-------------|
| `ALL_PHASES_COMPLETE.md` | Overview of all 4 phases |
| `AVATAR_GUI_GUIDE.md` | Avatar GUI usage guide |
| `AVATAR_GUI_SUMMARY.md` | Avatar features summary |

### [`docs/phases/`](docs/phases/) - Phase Summaries
Completion summaries for each development phase.

| File | Description |
|------|-------------|
| `PHASE1_COMPLETION_SUMMARY.md` | Behavioral Analysis |
| `PHASE2_COMPLETION_SUMMARY.md` | Avatar Counselor |
| `PHASE3_COMPLETION_SUMMARY.md` | Agreement Enforcement |
| `PHASE4_COMPLETION_SUMMARY.md` | MCP Orchestration |

### [`docs/fixes/`](docs/fixes/) - Bug Fixes
Documentation of major bug fixes.

| File | Description |
|------|-------------|
| `FACEBOOK_ALERT_FIX.md` | Facebook detection fix (Jan 3, 2026) |

### [`docs/archive/`](docs/archive/) - Legacy Documentation
Outdated or superseded documentation (kept for reference).

---

## 🎯 Source Code Structure

The `src/` directory follows **Clean Architecture** principles with strict layer separation.

### [`src/core/`](src/core/) - Domain Layer
**Dependencies**: None (pure Python only)

```
src/core/
├── entities/
│   ├── agreement.py              # Agreement entity (Phase 2/3)
│   ├── monitoring_session.py     # V1 session (legacy)
│   ├── monitoring_session_v2.py  # V2 session (unified)
│   ├── monitoring_target.py      # Unified monitoring target
│   ├── target_resolver.py        # Auto-resolve targets
│   └── website.py                # Website entity (V1)
│
└── value_objects/
    ├── process_name.py           # Process name value object
    └── url.py                    # URL value object
```

**Key Entities**:
- `Agreement`: Time-based agreements between user and counselor
- `MonitoringTarget`: Unified target (website + process)
- `TargetResolver`: Maps simple names to monitoring configs

### [`src/application/`](src/application/) - Use Cases Layer
**Dependencies**: Core layer only

```
src/application/
├── interfaces/                   # Port definitions (abstractions)
│   ├── i_alert_notifier.py
│   ├── i_behavioral_analyzer.py
│   ├── i_browser_controller.py
│   ├── i_browser_detector.py
│   ├── i_config_repository.py
│   ├── i_http_checker.py
│   ├── i_mcp_service_registry.py
│   ├── i_monitoring_scheduler.py
│   ├── i_process_detector.py
│   └── i_startup_manager.py
│
└── use_cases/                    # Business logic
    ├── add_target.py             # Add monitoring target (V2)
    ├── add_website.py            # Add website (V1)
    ├── check_targets.py          # Check all targets (V2)
    ├── check_websites.py         # Check websites (V1)
    ├── enforce_agreement.py      # Phase 3: Enforce agreements
    ├── negotiate_agreement.py    # Phase 2: Negotiate agreements
    ├── orchestrate_mcp_services.py # Phase 4: MCP orchestration
    ├── remove_target.py          # Remove target (V2)
    ├── remove_website.py         # Remove website (V1)
    ├── start_monitoring_v2.py    # Start monitoring (V2)
    ├── stop_monitoring_v2.py     # Stop monitoring (V2)
    ├── track_agreements.py       # Phase 3: Track agreement compliance
    └── trigger_intervention.py   # Phase 1: Trigger interventions
```

**Key Use Cases**:
- **Phase 1**: `trigger_intervention.py` - Behavioral analysis
- **Phase 2**: `negotiate_agreement.py` - Counselor negotiation
- **Phase 3**: `enforce_agreement.py`, `track_agreements.py` - Enforcement
- **Phase 4**: `orchestrate_mcp_services.py` - Service orchestration

### [`src/infrastructure/`](src/infrastructure/) - Infrastructure Layer
**Dependencies**: Application & Core layers

```
src/infrastructure/
├── adapters/                     # Service implementations
│   ├── counselor_orchestrator.py        # Phase 2: Counselor workflow
│   ├── counselor_voice_service.py       # Phase 2: Voice synthesis
│   ├── enforcement_notifier.py          # Phase 3: Enforcement notifications
│   ├── mcp_behavioral_analyzer.py       # Phase 1: Behavioral analysis
│   ├── mcp_service_factory.py           # Phase 4: Service factory
│   ├── mcp_service_registry.py          # Phase 4: Service registry
│   ├── playwright_browser_controller.py # Tab control (optional)
│   ├── requests_http_checker.py         # HTTP checking
│   ├── threaded_scheduler.py            # Periodic monitoring
│   ├── windows_alert_notifier.py        # Windows alerts
│   ├── windows_browser_detector.py      # Browser tab detection
│   ├── windows_process_detector.py      # Process detection
│   └── windows_startup_manager.py       # Auto-startup
│
└── persistence/                  # Data storage
    ├── json_config_repository.py         # V1 config storage
    └── json_config_repository_v2.py      # V2 config storage
```

### [`src/presentation/`](src/presentation/) - UI Layer
**Dependencies**: All other layers

```
src/presentation/
├── avatar_counselor_gui.py       # Main Avatar GUI (Phase 1-4)
├── avatar_counselor_window.py    # Fullscreen counselor window
├── countdown_timer_widget.py     # Countdown timer widget (Phase 3)
├── gui.py                        # V1 GUI (legacy)
└── gui_v2.py                     # V2 Unified GUI
```

**Active GUI**: `avatar_counselor_gui.py` - Minimal interface with avatar display.

### [`src/mcp_servers/`](src/mcp_servers/) - MCP Server Implementations
Python wrappers for MCP (Model Context Protocol) servers.

| File | Purpose | Status |
|------|---------|--------|
| `browser_tools_mcp.py` | Browser automation | ✅ Active |
| `webcam_mcp.py` | Camera access | ✅ Active |
| `elevenlabs_mcp.py` | Voice synthesis | ✅ Active |
| `memory_mcp.py` | Event storage | ✅ Active |
| `heygen_mcp.py` | Avatar generation | ⚠️ Prepared |
| `windows_mcp.py` | Windows TTS | ✅ Active |
| `filesystem_mcp.py` | File operations | ⚠️ Available |
| `mcp_server_notify.py` | Notifications | ✅ Active |
| `notifymemaybe.py` | Interactive notifications | ⚠️ Available |

---

## 💡 Examples Structure

### [`examples/basic/`](examples/basic/) - Basic MCP Demos
Simple demonstrations of individual MCP servers.

| File | Demonstrates |
|------|-------------|
| `browser_mcp_demo.py` | Browser automation |
| `webcam_demo.py` | Camera capture |
| `eleven_demo.py` | ElevenLabs voice |
| `heygen_demo.py` | HeyGen avatar |
| `memory_demo.py` | Memory storage |
| `filesystem_demo.py` | File operations |
| `notify_demo.py` | Basic notifications |
| `notifymaybe_demo.py` | Interactive notifications |

### [`examples/features/`](examples/features/) - Feature Demos
Demonstrations of integrated features.

| File | Demonstrates |
|------|-------------|
| `behavioral_demo.py` | Phase 1: Behavioral analysis |
| `avatar_counselor_demo.py` | Phase 2: Avatar counselor |
| `enforcement_demo.py` | Phase 3: Agreement enforcement |
| `avatar_gui.py` | Minimal avatar GUI |

### [`examples/mcp/`](examples/mcp/) - Advanced MCP
Advanced MCP orchestration examples.

| File | Demonstrates |
|------|-------------|
| `mcp_demo.py` | Basic MCP usage |
| `mcp_orchestration_demo.py` | Phase 4: Service orchestration |
| `openai_mcp_client.py` | OpenAI MCP client |
| `openai_multi_mcp_client.py` | Multi-MCP client |

---

## 🧪 Tests Structure

```
tests/
├── core/
│   ├── entities/
│   │   ├── test_agreement.py
│   │   ├── test_monitoring_session.py
│   │   └── test_website.py
│   └── value_objects/
│       └── test_url.py
│
├── application/
│   ├── interfaces/
│   │   └── test_i_behavioral_analyzer.py
│   └── use_cases/
│       ├── test_add_website.py
│       ├── test_check_websites.py
│       ├── test_enforce_agreement.py
│       ├── test_negotiate_agreement.py
│       ├── test_orchestrate_mcp_services.py
│       ├── test_track_agreements.py
│       └── test_trigger_intervention.py
│
├── infrastructure/
│   └── adapters/
│       ├── test_mcp_behavioral_analyzer.py
│       └── test_mcp_service_registry.py
│
└── test_facebook_detection.py      # Integration test
```

**Total**: 87 tests, all passing ✅

---

## ⚙️ Configuration

### [`config/config.json`](config/config.json)
Application runtime configuration (user-specific, git-ignored).

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
  "monitoring_interval": 5
}
```

### [`config/mcp_client_config.json`](config/mcp_client_config.json)
MCP server connection configuration (contains API keys, git-ignored).

```json
{
  "mcpServers": {
    "elevenlabs": {
      "command": "node",
      "args": ["path/to/server.js"],
      "env": {
        "ELEVENLABS_API_KEY": "..."
      }
    }
  }
}
```

---

## 🛠️ Scripts

### [`scripts/organize_repo.py`](scripts/organize_repo.py)
Repository organization utilities (used for project restructuring).

---

## 📦 Dependencies

See [requirements.txt](requirements.txt) for complete list.

**Core Dependencies**:
- `requests` - HTTP checking
- `psutil` - Process monitoring
- `pywinauto` - Windows automation
- `opencv-python` - Image processing
- `pillow` - Image handling
- `pyttsx3` - Text-to-speech
- `pygame` - Audio playback

**Development**:
- `pytest` - Testing framework
- `pytest-cov` - Coverage reporting
- `pytest-mock` - Mocking

**Packaging**:
- `pyinstaller` - Executable building

---

## 🚀 Quick Navigation

### I want to...
- **Run the application** → [main_v2.py](main_v2.py)
- **Get started quickly** → [docs/user/READY_TO_RUN.md](docs/user/READY_TO_RUN.md)
- **Understand the architecture** → [docs/technical/CLAUDE.md](docs/technical/CLAUDE.md)
- **See phase progress** → [docs/phases/](docs/phases/)
- **Run tests** → `pytest` in root directory
- **Add a new feature** → Follow Clean Architecture in [docs/technical/CLAUDE.md](docs/technical/CLAUDE.md)
- **Debug an issue** → Check [docs/fixes/](docs/fixes/) first
- **Try examples** → [examples/](examples/)
- **Build executable** → `build.bat` or `pyinstaller FocusMonitor.spec`

---

## 📝 Key Files Reference

| File | Purpose | Layer |
|------|---------|-------|
| `main_v2.py` | Main entry point | Entry |
| `src/presentation/avatar_counselor_gui.py` | Main GUI | Presentation |
| `src/application/use_cases/orchestrate_mcp_services.py` | MCP orchestration | Application |
| `src/infrastructure/adapters/mcp_behavioral_analyzer.py` | Behavioral analysis | Infrastructure |
| `src/core/entities/agreement.py` | Agreement entity | Core |
| `config/config.json` | Runtime configuration | Config |
| `tests/` | Test suite (87 tests) | Tests |

---

## 🔄 Version History

- **V2.0** (Jan 3, 2026) - All 4 phases complete, project reorganized
- **V1.5** (Jan 2, 2026) - Phase 4 complete (MCP Orchestration)
- **V1.4** (Jan 2, 2026) - Phase 3 complete (Agreement Enforcement)
- **V1.3** (Jan 2, 2026) - Phase 2 complete (Avatar Counselor)
- **V1.2** (Jan 1, 2026) - Phase 1 complete (Behavioral Analysis)
- **V1.0** (Dec 29, 2025) - Initial refactored architecture

---

## 📞 Support

For help:
1. Check [docs/user/READY_TO_RUN.md](docs/user/READY_TO_RUN.md)
2. Review [docs/fixes/](docs/fixes/) for known issues
3. See examples in [examples/](examples/)
4. Read technical docs in [docs/technical/](docs/technical/)

---

*Project organized and documented on January 3, 2026*
