# Unified Monitoring Architecture

## Concept

Instead of separate website and application monitoring, we now have **unified monitoring targets** where each target can monitor:
- **Website only** (e.g., Google)
- **Application only** (e.g., Calculator)
- **Both website AND application** (e.g., Netflix)

## Example Use Cases

### 1. Netflix (Hybrid - Website + App)
```
Name: "Netflix"
URL: https://netflix.com
Process: Netflix.exe

Alert triggers when EITHER:
- Netflix website is open in browser, OR
- Netflix app is running
```

### 2. Google (Website Only)
```
Name: "Google"
URL: https://google.com
Process: (none)

Alert triggers when:
- Google website is open in browser
```

### 3. Calculator (App Only)
```
Name: "Calculator"
URL: (none)
Process: calc.exe

Alert triggers when:
- Calculator app is running
```

## Config Format

```json
{
  "targets": [
    {
      "id": "uuid-1",
      "name": "Netflix",
      "url": "https://netflix.com",
      "process_name": "Netflix.exe"
    },
    {
      "id": "uuid-2",
      "name": "Google",
      "url": "https://google.com",
      "process_name": null
    },
    {
      "id": "uuid-3",
      "name": "Calculator",
      "url": null,
      "process_name": "calc.exe"
    }
  ],
  "monitoring_interval": 10
}
```

## GUI Workflow

### Adding a Target

```
┌─────────────────────────────────────┐
│ Add Monitoring Target               │
├─────────────────────────────────────┤
│ Name: [Netflix________________]     │
│                                     │
│ ☑ Monitor Website                  │
│   URL: [netflix.com___________]     │
│                                     │
│ ☑ Monitor Application              │
│   Process: [Netflix.exe________]    │
│                                     │
│ [Add Target]  [Cancel]              │
└─────────────────────────────────────┘
```

### Target List Display

```
┌────────────────────────────────────────┐
│ Monitoring Targets                     │
├────────────────────────────────────────┤
│ ● Netflix                              │
│   🌐 netflix.com                       │
│   📱 Netflix.exe                       │
│   [ALERTING]                           │
├────────────────────────────────────────┤
│ ○ Google                               │
│   🌐 google.com                        │
│                                        │
├────────────────────────────────────────┤
│ ○ Calculator                           │
│   📱 calc.exe                          │
│                                        │
└────────────────────────────────────────┘
```

## Alert Logic

For each target, the monitoring loop checks:

```python
is_active = False

# Check website (if configured)
if target.has_website():
    if http_reachable AND browser_open:
        is_active = True

# Check application (if configured)
if target.has_application():
    if process_running:
        is_active = True

# Trigger/clear alert based on combined status
if is_active and not target.is_alerting:
    trigger_alert(target.name)
elif not is_active and target.is_alerting:
    clear_alert(target.name)
```

## Benefits

1. **Simpler UI**: One list instead of two separate tabs
2. **Unified View**: See all monitoring in one place
3. **Flexibility**: Support website-only, app-only, or hybrid
4. **Logical**: "Netflix" monitors both Netflix.com and Netflix.exe
5. **Less Redundancy**: One alert for "Netflix" regardless of source
