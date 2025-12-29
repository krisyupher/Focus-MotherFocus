# Website Monitor - Focus Alert System

A Python desktop application that continuously monitors websites and generates repeated visual and audio alerts when they are online.

## Features

### Core Functionality
- **Website List Management**: Add, edit, and remove websites from your monitoring list
- **Continuous Monitoring**: Checks website availability at configurable intervals
- **Persistent Alerts**: Generates alerts repeatedly while a website remains online
- **Dual Alert System**:
  - Pop-up alert windows that appear on top
  - Audible beep sounds
- **Real-time Controls**: Start and stop monitoring on demand

### Alert Behavior
- Alerts trigger **immediately** when a website is detected as online (HTTP 200 status)
- Alerts **repeat continuously** at every monitoring interval while the website stays online
- Alerts **stop automatically** when:
  - The website becomes unreachable
  - The website is removed from the monitoring list
  - Monitoring is stopped by the user

## Installation

### Prerequisites
- Python 3.7 or higher
- Windows OS (uses `winsound` for audio alerts)

### Setup Steps

1. **Clone or download** this repository

2. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the application**:
   ```bash
   python index.py
   ```

## Usage Guide

### 1. Add Websites to Monitor
- Click **"➕ Add Website"**
- Enter the website URL (e.g., `google.com` or `https://example.com`)
- The URL will appear in the monitoring list

### 2. Manage Website List
- **Edit**: Select a website and click **"✏️ Edit Selected"**
- **Remove**: Select a website and click **"🗑️ Remove Selected"**

### 3. Configure Monitoring Interval
- Enter the desired interval in seconds (default: 10 seconds)
- Click **"Set Interval"** to apply
- Lower intervals = more frequent checks and alerts

### 4. Start Monitoring
- Click **"▶️ START MONITORING"**
- The application will continuously check all websites
- **Pop-up alerts** and **beep sounds** will trigger for each online website at every interval

### 5. Stop Monitoring
- Click **"⏹️ STOP MONITORING"** to stop all alerts

## Technical Details

### Architecture

```
WebsiteMonitor
├── WebsiteMonitor (Core Logic)
│   └── check_website() - HTTP availability check
│
├── AlertManager (Alert System)
│   ├── show_popup_alert() - Visual pop-up windows
│   └── play_alert_sound() - Audio beep alerts
│
├── MonitoringController (Control Logic)
│   ├── Website list management
│   ├── Monitoring loop (threaded)
│   └── Alert coordination
│
└── WebsiteMonitorGUI (User Interface)
    ├── Website list display
    ├── Management controls
    └── Monitoring controls
```

### How It Works

1. **Website Checking**: Uses the `requests` library to send HTTP GET requests
   - Considers a website "online" if it returns HTTP status code 200
   - Handles redirects automatically
   - Times out after 5 seconds

2. **Monitoring Loop**: Runs in a separate thread to avoid blocking the UI
   - Checks all websites sequentially
   - Triggers alerts for each online website
   - Waits for the configured interval before the next check cycle

3. **Alert System**:
   - **Pop-ups**: Creates tkinter Toplevel windows with `-topmost` attribute
   - **Sounds**: Uses `winsound.Beep()` for system beeps (1000 Hz, 500ms)
   - Auto-dismisses pop-ups after 5 seconds if not acknowledged

4. **Threading**: Alert pop-ups are scheduled on the main thread using `after()` to ensure thread safety with tkinter

## Configuration Options

| Setting | Default | Description |
|---------|---------|-------------|
| Check Interval | 10 seconds | How often to check website status |
| Request Timeout | 5 seconds | Max wait time for website response |
| Pop-up Auto-close | 5 seconds | Time before alert auto-dismisses |
| Alert Frequency | 1000 Hz | Audio beep frequency |
| Alert Duration | 500 ms | Audio beep duration |

## Use Cases

This application is designed for scenarios where you need to be **immediately and continuously notified** when specific websites become available:

- Monitoring website uptime during maintenance
- Detecting when a temporarily offline service comes back online
- Tracking when limited-availability websites become accessible
- Development/testing scenarios requiring immediate awareness of site status

## Limitations

- **Windows Only**: Audio alerts use `winsound` (Windows-specific)
- **HTTP(S) Only**: Only monitors web-accessible resources
- **No Browser Integration**: Does not detect browser activity or tabs
- **Single Instance**: Designed to run as a single application instance

## Troubleshooting

### "Import requests could not be resolved"
```bash
pip install requests
```

### No sound alerts
- Ensure your system volume is not muted
- The application falls back to `MessageBeep` if `Beep` fails

### Pop-ups not appearing
- Check if other windows are set to "always on top"
- Ensure the application has focus permissions

### Monitoring seems slow
- Reduce the check interval
- Check your internet connection
- Some websites may have slow response times

## Future Enhancements

Potential improvements:
- Cross-platform audio support (macOS, Linux)
- Website status history and logging
- Email/SMS notification integration
- Custom alert sounds
- Website list persistence (save/load)
- Multi-threaded website checking for faster parallel checks
- Response time tracking
- Dashboard with visual status indicators

## License

This project is provided as-is for educational and personal use.

## Support

For issues or questions, please refer to the code documentation in [index.py](index.py).
# Focus-MotherFocus
