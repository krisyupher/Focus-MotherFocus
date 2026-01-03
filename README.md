# FocusMotherFocus 🎯

**AI Productivity Counselor with Avatar** - Complete 4-phase system for behavioral monitoring, voice-based intervention, and smart enforcement.

## Quick Start (Single Command!)

```bash
# Install dependencies (one-time)
pip install -r requirements.txt

# Run FocusMotherFocus
python main_v2.py
```

**That's it!** Click "Start Monitoring" in the GUI and the AI counselor handles everything automatically.

## What It Does

1. **Monitors your activity** - Detects endless scrolling, distractions, inappropriate content
2. **Avatar intervenes** - Speaks to you with voice when patterns detected
3. **Negotiates agreements** - "How much longer do you need?" → You: "10 minutes"
4. **Enforces time limits** - Countdown timer, warnings, grace period

**Note**: Tab auto-close is disabled (Playwright removed to avoid issues). All other features work perfectly!

## Complete System Features

### ✅ Phase 1: Behavioral Analysis
- **Pattern Detection**: Endless scrolling (20+ seconds), adult content, distraction sites
- **Smart Triggering**: Severity-based intervention with cooldown to prevent spam
- **Intelligent Analysis**: Recognizes habitual behaviors

### ✅ Phase 2: Avatar Counselor
- **Voice Interaction**: Speaks using ElevenLabs (or Windows TTS fallback)
- **Natural Dialogue**: Multi-turn conversation for time negotiation
- **Agreement Creation**: Stores commitments with expiration tracking

### ✅ Phase 3: Agreement Enforcement
- **Countdown Timers**: Color-coded visual feedback (green/yellow/red)
- **Smart Warnings**: Notifications 1 minute before expiration
- **Grace Periods**: 30 seconds to wrap up after time's up
- **Automatic Tab Closure**: Enforces agreements via Playwright

### ✅ Phase 4: Service Orchestration
- **Auto-Discovery**: Finds and initializes 9 MCP services automatically
- **Health Monitoring**: Checks service health every 30 seconds
- **Intelligent Fallbacks**: Auto-switches when primary service fails
- **Self-Healing**: Recovers from service failures automatically

## Minimal Interface

```
┌────────────────────────────────────────┐
│       🎯 FocusMotherFocus              │
│   AI Productivity Counselor            │
│                                        │
│   ┌──────────────────────────┐         │
│   │                          │         │
│   │     👤 Avatar            │         │
│   │                          │         │
│   │   "Monitoring your       │         │
│   │    activity..."          │         │
│   │                          │         │
│   └──────────────────────────┘         │
│                                        │
│      [▶ Start Monitoring]              │
│                                        │
│     Ready to start                     │
└────────────────────────────────────────┘
```

**One button. That's all.** Everything else is automatic

**3. Start Monitoring:**
Type: `"Start monitoring"`

### Enable Auto-Close Feature

Automatically close distraction tabs after 10 seconds:

**1. Install Playwright:**
```bash
playwright install chromium
```

**2. Start Chrome with Debugging:**
```batch
start_chrome_debug.bat
```

**3. Run App:**
```bash
python main_v2.py
```

Now distraction sites close automatically! 🎯

📖 **Full setup guide:** [QUICKSTART.md](QUICKSTART.md)

## Project Structure

```
FocusMotherFocus/
├── src/                          # Source code
│   ├── core/                     # Core domain logic
│   │   ├── entities/             # Business entities
│   │   ├── services/             # Domain services
│   │   └── value_objects/        # Value objects
│   ├── application/              # Application layer
│   │   ├── interfaces/           # Port interfaces
│   │   └── use_cases/            # Use case implementations
│   ├── infrastructure/           # Infrastructure layer
│   │   ├── adapters/             # Adapter implementations
│   │   └── persistence/          # Data persistence
│   └── presentation/             # Presentation layer
│       └── gui_v2.py             # Main GUI
├── tests/                        # Unit tests
├── scripts/                      # Test and debug scripts
├── docs/                         # Documentation
├── main_v2.py                    # Application entry point
├── openai_mcp_client.py          # OpenAI MCP client (Playwright integration)
├── mcp_client_config.json        # MCP server configuration
├── requirements.txt              # Python dependencies
├── pytest.ini                    # Pytest configuration
├── build.bat                     # Build script for Windows
├── FocusMonitor.spec             # PyInstaller spec
├── .env                          # Environment variables (API keys)
├── config.json                   # User configuration (monitoring targets)
└── README.md                     # This file
```

## Architecture

This project follows **Clean Architecture** principles with strict layer separation:

- **Core Layer**: Pure business logic (no dependencies)
- **Application Layer**: Use cases and interfaces
- **Infrastructure Layer**: External integrations (Windows APIs, OpenAI, etc.)
- **Presentation Layer**: Tkinter GUI

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed architecture documentation.

## Key Technologies

- **Python 3.13+**
- **Tkinter**: GUI framework
- **OpenCV**: Avatar animation and face detection
- **OpenAI GPT-4o-mini**: Natural language processing
- **Windows APIs**: Process detection, browser detection, alerts
- **MCP (Model Context Protocol)**: AI integration standard

## Documentation

### User Guides
- [AI Natural Language Guide](docs/AI_NATURAL_LANGUAGE_GUIDE.md) - How to use AI commands
- [Animated Avatar Guide](docs/ANIMATED_AVATAR_GUIDE.md) - Avatar features and customization
- [Unified V2 Guide](docs/UNIFIED_V2_GUIDE.md) - Complete V2 system guide
- [Usage Guide](docs/USAGE_GUIDE.md) - General usage instructions

### Technical Documentation
- [Architecture](docs/ARCHITECTURE.md) - System architecture and design patterns
- [Playwright MCP Guide](docs/PLAYWRIGHT_MCP_GUIDE.md) - Browser automation with OpenAI + Playwright
- [Multi-MCP Guide](docs/MULTI_MCP_GUIDE.md) - Browser + Filesystem with multiple MCP servers
- [Distribution](docs/DISTRIBUTION.md) - Building executables

### Troubleshooting
- [AI Command Fix](docs/AI_COMMAND_FIX.md) - Fixing OpenAI API issues
- [Avatar Troubleshooting](docs/AVATAR_TROUBLESHOOTING.md) - Avatar-related issues
- [Camera Debug Guide](docs/CAMERA_DEBUG_GUIDE.md) - Camera debugging

## Building Executable

To build a standalone Windows executable:

```bash
build.bat
```

The executable will be in `dist/FocusMonitor/`

## Testing

Run all tests:

```bash
pytest
```

Run with coverage:

```bash
pytest --cov=src --cov-report=html
```

## Configuration

### Auto-Startup

Enable auto-startup from the GUI or manually configure Windows registry.

See [docs/AUTO_STARTUP.md](docs/AUTO_STARTUP.md) for details.

### Monitoring Interval

The system checks every 1 second by default. This is configured in the start monitoring use case.

## Requirements

- **Platform**: Windows 10/11 (uses Windows-specific APIs)
- **Python**: 3.13+
- **Camera**: Webcam required for avatar features
- **OpenAI Account**: API key with billing enabled

## Cost

With GPT-4o-mini:
- Per command: ~$0.0001 (1/100th of a cent)
- 100 commands: ~$0.01 (1 cent)
- 1000 commands: ~$0.10 (10 cents)

Extremely affordable for daily use.

## Development

### Code Style

Follow Clean Architecture principles:
- No business logic in GUI
- All use cases are independent
- Dependencies point inward (toward core)
- Interfaces define all external dependencies

### Adding New Features

1. Define interface in `src/application/interfaces/`
2. Implement adapter in `src/infrastructure/adapters/`
3. Create use case in `src/application/use_cases/`
4. Wire everything in `main_v2.py`
5. Add tests

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for examples.

## License

[Add your license here]

## Contributing

[Add contribution guidelines if open source]

## Credits

- **Clean Architecture**: Inspired by Robert C. Martin's Clean Architecture
- **MCP Protocol**: Anthropic's Model Context Protocol
- **Zordon Effect**: Inspired by Power Rangers aesthetic

## Support

For issues, questions, or feature requests, please check the documentation in the `docs/` folder.

## Project Status

**Active Development** - Currently on V2 with unified monitoring and AI integration.

Major features implemented:
- ✅ Unified website and application monitoring
- ✅ AI natural language control (OpenAI integration)
- ✅ Animated speaking avatar with Zordon effects
- ✅ Single-window alert updates
- ✅ MCP server for AI assistant integration
- ✅ Auto-startup on system boot
- ✅ Clean Architecture refactoring

## Version History

- **V2**: Unified monitoring + AI + Animated Avatar (Current)
- **V1**: Basic website monitoring (Legacy)
