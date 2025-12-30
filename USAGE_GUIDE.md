# FocusMotherFocus - Unified Monitoring Usage Guide

## Quick Start

### Running the Application

```bash
# Run the unified version (NEW - recommended)
python main_v2.py

# Run the original version (legacy)
python main.py
```

## Adding Monitoring Targets

The unified system lets you monitor websites, applications, or BOTH with a single entry.

### Example 1: Netflix (Hybrid - Website + App)

```
Name: Netflix
☑ Monitor Website
  URL: netflix.com
☑ Monitor Application
  Process: Netflix.exe
```

**Alert triggers when:**
- Netflix.com is open in browser, OR
- Netflix.exe app is running

### Example 2: Google (Website Only)

```
Name: Google
☑ Monitor Website
  URL: google.com
☐ Monitor Application
```

**Alert triggers when:**
- Google.com is open in browser

### Example 3: Calculator (App Only)

```
Name: Calculator
☐ Monitor Website
☑ Monitor Application
  Process: calc.exe
```

**Alert triggers when:**
- Calculator app is running

## Configuration File

The application saves targets to `config.json`:

```json
{
  "targets": [
    {
      "id": "abc-123",
      "name": "Netflix",
      "url": "https://netflix.com",
      "process_name": "netflix.exe"
    },
    {
      "id": "def-456",
      "name": "Google",
      "url": "https://google.com",
      "process_name": null
    }
  ],
  "monitoring_interval": 10
}
```

## Common Use Cases

### 1. Monitor Social Media (Website)
- **Facebook**: facebook.com
- **Twitter**: twitter.com
- **Instagram**: instagram.com

### 2. Monitor Productivity Apps (Application)
- **Slack**: Slack.exe
- **Teams**: Teams.exe
- **Discord**: Discord.exe

### 3. Monitor Entertainment (Hybrid)
- **Spotify**: spotify.com + Spotify.exe
- **YouTube**: youtube.com + (no app)
- **Steam**: store.steampowered.com + Steam.exe

## Alert Behavior

### When Alerts Trigger
- **Sound**: Windows beep (1000 Hz, 500ms)
- **Pop-up**: Alert window with target name
- **Frequency**: Every monitoring interval while active

### When Alerts Clear
- Website: When page is closed in browser
- Application: When process terminates
- Both: When BOTH conditions become false

## Tips

1. **Process Names**: Include .exe extension (e.g., "chrome.exe" not "chrome")
2. **URLs**: Protocol (https://) is auto-added if missing
3. **Interval**: Lower = more frequent checks (minimum 1 second)
4. **Auto-startup**: Enable to launch on Windows boot

## Troubleshooting

### "No targets to monitor" Error
- Add at least one target before starting monitoring
- Ensure at least one checkbox is selected (website OR app)

### Website Not Detected
- Check if browser detector supports your browser (Chrome, Firefox, Edge)
- Verify URL is reachable (open in browser manually first)

### Application Not Detected
- Verify exact process name in Task Manager
- Process names are case-insensitive
- Include .exe extension

### Alerts Don't Stop
- Click "STOP MONITORING" button
- Check if process is still running (Task Manager)
- Check if browser tab is still open

## Keyboard Shortcuts

- **Enter**: Submit target (when in input fields)
- **Alt+F4**: Close application
