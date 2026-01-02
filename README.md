# FocusMotherFocus 🎯

A productivity monitoring system with AI-powered natural language control and speaking animated avatar alerts.

## Features

- **Unified Monitoring**: Monitor both websites AND desktop applications
- **AI Natural Language Control**: Control everything using natural language commands via OpenAI
- **Animated Avatar Alerts**: Zordon-style fullscreen alerts with speaking animated avatar
- **Smart Detection**: Automatically detects browser tabs and running processes
- **Auto-startup**: Configure to run on system boot

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Configure OpenAI API Key

Create a `.env` file in the project root:

```
OPENAI_API_KEY=your_openai_api_key_here
```

### 3. Run the Application

```bash
python main_v2.py
```

### 4. Use Natural Language Commands

Type commands in the AI Assistant section:

- "Monitor Netflix and YouTube"
- "Start monitoring"
- "Show my targets"
- "Remove Facebook"
- "Stop monitoring"

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
├── mcp_server.py                 # MCP server for AI integration
├── openai_mcp_client.py          # OpenAI MCP client
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
- [MCP Integration Guide](docs/MCP_INTEGRATION_GUIDE.md) - MCP server and client setup
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
