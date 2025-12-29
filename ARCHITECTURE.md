# Clean Architecture Implementation Guide

## Architecture Overview

This application follows **Clean Architecture** with strict layer separation and dependency inversion. The original monolithic `index.py` (~600 lines) has been refactored into a layered architecture with clear boundaries.

## Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│                    (src/presentation)                    │
│                 - GUI (WebsiteMonitorGUI)                │
└─────────────────────┬───────────────────────────────────┘
                      │ depends on
┌─────────────────────▼───────────────────────────────────┐
│                   Application Layer                      │
│                   (src/application)                      │
│         - Use Cases (AddWebsite, StartMonitoring)        │
│         - Interfaces (IHttpChecker, IAlertNotifier)      │
└─────────────────────┬───────────────────────────────────┘
                      │ depends on
┌─────────────────────▼───────────────────────────────────┐
│                      Core Layer                          │
│                      (src/core)                          │
│            - Entities (Website, MonitoringSession)       │
│            - Value Objects (URL)                         │
│            - Pure Business Logic (Zero Dependencies)     │
└──────────────────────────────────────────────────────────┘
                      ▲
                      │ implements interfaces
┌─────────────────────┴───────────────────────────────────┐
│                Infrastructure Layer                      │
│                  (src/infrastructure)                    │
│     - Adapters (RequestsHttpChecker, WindowsAlert)       │
│     - Persistence (JsonConfigRepository)                 │
└──────────────────────────────────────────────────────────┘
```

## Project Structure

```
Focus/
├── src/
│   ├── core/                           # Domain Layer
│   │   ├── entities/
│   │   │   ├── __init__.py
│   │   │   ├── website.py              # Website entity
│   │   │   └── monitoring_session.py   # Session aggregate root
│   │   └── value_objects/
│   │       ├── __init__.py
│   │       └── url.py                  # URL value object
│   │
│   ├── application/                    # Application Layer
│   │   ├── interfaces/                 # Ports (abstractions)
│   │   │   ├── __init__.py
│   │   │   ├── http_checker.py
│   │   │   ├── alert_notifier.py
│   │   │   ├── config_repository.py
│   │   │   └── monitoring_scheduler.py
│   │   └── use_cases/                  # Application logic
│   │       ├── __init__.py
│   │       ├── add_website.py
│   │       ├── remove_website.py
│   │       ├── start_monitoring.py
│   │       ├── stop_monitoring.py
│   │       └── check_websites.py
│   │
│   ├── infrastructure/                 # Infrastructure Layer
│   │   ├── adapters/                   # Interface implementations
│   │   │   ├── __init__.py
│   │   │   ├── requests_http_checker.py
│   │   │   ├── windows_alert_notifier.py
│   │   │   └── threaded_scheduler.py
│   │   └── persistence/
│   │       ├── __init__.py
│   │       └── json_config_repository.py
│   │
│   └── presentation/                   # Presentation Layer
│       ├── __init__.py
│       └── gui.py                      # Tkinter GUI
│
├── tests/                              # Test Suite
│   ├── conftest.py                     # Shared fixtures
│   ├── core/
│   │   ├── entities/
│   │   │   ├── test_website.py
│   │   │   └── test_monitoring_session.py
│   │   └── value_objects/
│   │       └── test_url.py
│   ├── application/
│   │   └── use_cases/
│   │       ├── test_add_website.py
│   │       ├── test_remove_website.py
│   │       ├── test_start_monitoring.py
│   │       ├── test_stop_monitoring.py
│   │       └── test_check_websites.py
│   └── infrastructure/
│       ├── test_json_config_repository.py
│       └── test_threaded_scheduler.py
│
├── main.py                             # Composition Root
├── index.py                            # Legacy monolithic version
├── config.json                         # Configuration file
├── requirements.txt
├── pytest.ini
└── README.md
```

## Layer Responsibilities

### 1. Core Layer (src/core/)

**Responsibility**: Domain logic and business rules

**Rules**:
- Zero external dependencies
- Contains only pure Python
- No imports from other layers
- Defines the ubiquitous language

**Components**:

#### Entities
- `Website`: Represents a monitored website
  - Tracks online/offline status
  - Records last check time
  - Business logic for status updates

- `MonitoringSession`: Aggregate root
  - Manages collection of websites
  - Enforces monitoring rules
  - Controls session lifecycle

#### Value Objects
- `URL`: Immutable URL representation
  - Self-validating
  - Normalizes URLs (adds protocol)
  - Provides equality and hashing

### 2. Application Layer (src/application/)

**Responsibility**: Application-specific business logic

**Rules**:
- Depends only on Core layer
- Defines interfaces (ports) for external services
- Orchestrates domain entities
- Contains use cases

**Components**:

#### Interfaces (Ports)
- `IHttpChecker`: Check website availability
- `IAlertNotifier`: Send alerts to users
- `IConfigRepository`: Persist configuration
- `IMonitoringScheduler`: Schedule periodic tasks

#### Use Cases
- `AddWebsiteUseCase`: Add website to monitoring
- `RemoveWebsiteUseCase`: Remove website from monitoring
- `StartMonitoringUseCase`: Start monitoring session
- `StopMonitoringUseCase`: Stop monitoring session
- `CheckWebsitesUseCase`: Check all websites and send alerts

### 3. Infrastructure Layer (src/infrastructure/)

**Responsibility**: External service implementations

**Rules**:
- Implements application interfaces
- Depends on Core and Application layers
- Contains all external dependencies
- Swappable implementations

**Components**:

#### Adapters
- `RequestsHttpChecker`: HTTP checking via requests library
- `WindowsAlertNotifier`: Windows pop-up and sound alerts
- `ThreadedScheduler`: Threading-based periodic scheduler

#### Persistence
- `JsonConfigRepository`: JSON file configuration storage

### 4. Presentation Layer (src/presentation/)

**Responsibility**: User interface

**Rules**:
- Depends on Application and Core layers
- Delegates all logic to use cases
- Thin layer - no business logic
- Framework-specific code

**Components**:
- `WebsiteMonitorGUI`: Tkinter GUI application
  - Receives use cases via dependency injection
  - Handles user interactions
  - Updates UI based on domain state

## Dependency Rules

### The Dependency Rule

**Dependencies point inward only**:
1. Core → Nothing
2. Application → Core
3. Infrastructure → Application + Core
4. Presentation → Application + Core

### Dependency Inversion

High-level modules don't depend on low-level modules. Both depend on abstractions.

Example:
```python
# Use case depends on interface (abstraction)
class StartMonitoringUseCase:
    def __init__(self, scheduler: IMonitoringScheduler):
        self.scheduler = scheduler

# Infrastructure provides concrete implementation
class ThreadedScheduler(IMonitoringScheduler):
    def start(self, interval, callback):
        # Threading implementation
```

## Composition Root (main.py)

The composition root wires all dependencies together:

```python
def create_application() -> WebsiteMonitorGUI:
    # 1. Create infrastructure adapters
    config_repository = JsonConfigRepository()
    http_checker = RequestsHttpChecker()
    alert_notifier = WindowsAlertNotifier()
    scheduler = ThreadedScheduler()

    # 2. Load/create domain objects
    session = config_repository.load_session()

    # 3. Create use cases with dependencies
    add_website_use_case = AddWebsiteUseCase(
        session=session,
        config_repository=config_repository
    )
    # ... other use cases

    # 4. Create GUI with use cases
    gui = WebsiteMonitorGUI(
        session=session,
        add_website_use_case=add_website_use_case,
        # ... other use cases
    )

    return gui
```

## Testing Strategy

### Core Layer Tests
- Pure unit tests
- No mocks needed
- Test business logic in isolation
- Fast execution

### Application Layer Tests
- Use mocks for interfaces
- Test use case orchestration
- Verify correct domain interactions
- Test error handling

### Infrastructure Layer Tests
- Test adapter implementations
- May use real dependencies (files, etc.)
- Test edge cases and failures
- Integration test characteristics

### Example Test

```python
def test_add_website_success(empty_session, mock_config_repository):
    use_case = AddWebsiteUseCase(
        session=empty_session,
        config_repository=mock_config_repository
    )

    website = use_case.execute("google.com")

    assert website.url == URL("https://google.com")
    assert empty_session.get_website_count() == 1
    mock_config_repository.save_session.assert_called_once()
```

## Key Design Patterns

### 1. Repository Pattern
`IConfigRepository` abstracts persistence:
```python
class IConfigRepository(ABC):
    @abstractmethod
    def save_session(self, session: MonitoringSession) -> bool:
        pass

    @abstractmethod
    def load_session(self) -> MonitoringSession:
        pass
```

### 2. Dependency Injection
Use cases receive dependencies through constructor:
```python
@dataclass
class AddWebsiteUseCase:
    session: MonitoringSession
    config_repository: IConfigRepository
```

### 3. Aggregate Root
`MonitoringSession` controls all website operations:
```python
session = MonitoringSession()
session.add_website(url)  # Enforces business rules
session.start()           # Validates state
```

### 4. Value Object
`URL` ensures validity:
```python
url = URL("google.com")  # Normalized to https://google.com
url.value = "other"      # Error: immutable!
```

## Migration from Legacy

### Before (index.py)
- ~600 lines in one file
- Mixed concerns
- Hard to test
- Tight coupling
- No clear boundaries

### After (Clean Architecture)
- 20+ focused modules
- Clear separation of concerns
- 90%+ test coverage
- Loose coupling via interfaces
- Explicit dependencies

## Benefits Achieved

1. **Testability**: Each layer tested independently
2. **Maintainability**: Changes localized to specific layers
3. **Flexibility**: Easy to swap implementations
4. **Scalability**: Can add features without modifying existing code
5. **Clarity**: Business logic clearly separated from technical details
6. **Reusability**: Core logic independent of framework

## Example: Adding Email Notifications

To add email alerts:

1. Create interface in `application/interfaces/`:
```python
class IEmailNotifier(ABC):
    @abstractmethod
    def send_email(self, url: URL, message: str) -> None:
        pass
```

2. Implement in `infrastructure/adapters/`:
```python
class SmtpEmailNotifier(IEmailNotifier):
    def send_email(self, url: URL, message: str) -> None:
        # SMTP implementation
        pass
```

3. Update use case:
```python
@dataclass
class CheckWebsitesUseCase:
    email_notifier: IEmailNotifier  # Add dependency

    def execute(self):
        if is_online:
            self.email_notifier.send_email(url, "Site is online!")
```

4. Wire in composition root:
```python
email_notifier = SmtpEmailNotifier(config)
check_use_case = CheckWebsitesUseCase(
    ...,
    email_notifier=email_notifier
)
```

**No changes to core layer or existing code!**

## Running the Application

```bash
# Install dependencies
pip install -r requirements.txt

# Run tests
pytest

# Run application
python main.py
```

## Best Practices

1. **Keep Core Pure**: No external dependencies in core layer
2. **Define Interfaces First**: Think about contracts before implementations
3. **Inject Dependencies**: Use constructor injection
4. **Test at Boundaries**: Test use cases and adapters thoroughly
5. **Immutable Value Objects**: Use frozen dataclasses
6. **Single Responsibility**: Each class has one reason to change
7. **Explicit over Implicit**: Clear dependencies and boundaries
