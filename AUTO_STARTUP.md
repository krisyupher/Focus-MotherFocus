# Auto-Startup Feature

## Overview

The application can now **start automatically when your computer turns on**. This feature uses the Windows Registry to add the application to startup programs.

## How to Enable

1. **Run the application**: `python main.py`
2. **Find the checkbox**: At the bottom of the Monitoring Controls section
3. **Check the box**: "Start automatically when computer turns on"
4. **Confirmation**: You'll see a popup confirming the startup command

## How to Disable

1. **Uncheck the box**: Simply uncheck "Start automatically when computer turns on"
2. **Done**: The application will no longer start on boot

## How It Works

### Windows Registry Integration

The auto-startup feature uses the **Windows Registry** to register the application:

**Registry Location:**
```
HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run
```

**Registry Key:**
```
FocusMotherFocus
```

**Registry Value:**
```
"C:\Path\To\Python\python.exe" "C:\Path\To\Focus\main.py"
```

### Clean Architecture Implementation

Following Clean Architecture principles, the auto-startup feature is implemented with:

1. **Interface (Application Layer)**:
   - [`IStartupManager`](src/application/interfaces/startup_manager.py)
   - Abstract contract for startup management
   - Platform-agnostic interface

2. **Adapter (Infrastructure Layer)**:
   - [`WindowsStartupManager`](src/infrastructure/adapters/windows_startup_manager.py)
   - Windows-specific implementation
   - Uses `winreg` module for Registry access

3. **Presentation (GUI Layer)**:
   - Checkbox in [`WebsiteMonitorGUI`](src/presentation/gui.py)
   - Depends on `IStartupManager` interface (not concrete implementation)
   - Clean dependency injection

4. **Composition Root**:
   - [`main.py`](main.py) wires everything together
   - Creates `WindowsStartupManager` instance
   - Injects into GUI via constructor

### Dependency Flow

```
GUI (Presentation)
    ↓ depends on
IStartupManager (Interface)
    ↑ implemented by
WindowsStartupManager (Infrastructure)
```

This design allows:
- ✅ Easy testing (mock the interface)
- ✅ Platform independence (create LinuxStartupManager, MacStartupManager)
- ✅ No coupling between GUI and Windows-specific code

## Features

### Auto-Detection
- The checkbox reflects the current state
- If already enabled externally, checkbox shows checked
- If disabled, checkbox shows unchecked

### Validation
- Shows confirmation message when enabled
- Displays the actual startup command
- Error handling if Registry access fails

### Permission Handling
- Normally works without admin rights (HKEY_CURRENT_USER)
- If fails, suggests running as administrator
- Checkbox reverts if operation fails

## Testing

### Manual Test

1. **Enable auto-startup**:
   - Check the checkbox
   - Verify confirmation message
   - Note the startup command

2. **Verify Registry entry**:
   - Press `Win + R`
   - Type: `regedit`
   - Navigate to: `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`
   - Look for: `FocusMotherFocus`
   - Verify value matches your Python path

3. **Test auto-start**:
   - **Restart your computer**
   - Application should start automatically after login
   - GUI window should appear

4. **Disable auto-startup**:
   - Uncheck the checkbox
   - Restart computer
   - Application should NOT start

### Programmatic Test

```python
from src.infrastructure.adapters.windows_startup_manager import WindowsStartupManager

# Create manager
manager = WindowsStartupManager()

# Check current state
print(f"Auto-startup enabled: {manager.is_enabled()}")

# Enable
if manager.enable():
    print("Successfully enabled")
    print(f"Command: {manager.get_startup_command()}")

# Check state again
print(f"Auto-startup enabled: {manager.is_enabled()}")

# Disable
if manager.disable():
    print("Successfully disabled")

# Check state
print(f"Auto-startup enabled: {manager.is_enabled()}")
```

## Troubleshooting

### "Failed to enable auto-startup"

**Cause**: Registry access denied

**Solutions**:
1. Run as administrator
2. Check antivirus/security software
3. Verify Registry permissions

### "Application doesn't start on boot"

**Possible Causes**:

1. **Python not in PATH**:
   - Registry entry uses full Python path
   - If Python moved, update the entry

2. **Working directory issue**:
   - The script might need absolute paths
   - Check main.py uses absolute paths for config.json

3. **Antivirus blocking**:
   - Some security software blocks Registry startup entries
   - Add exception for the application

4. **User account**:
   - Registry entry is user-specific
   - Only works for the user who enabled it

### "Checkbox doesn't reflect actual state"

**Cause**: External modification of Registry

**Solution**:
- Close and reopen the application
- GUI checks Registry state on startup

## Security Considerations

### Registry Access
- Uses `HKEY_CURRENT_USER` (user-level, no admin needed)
- Does NOT use `HKEY_LOCAL_MACHINE` (system-level)
- Changes only affect current user

### Command Execution
- Full path to Python executable
- Full path to main.py
- Both paths are absolute (not relative)
- No shell interpretation

### Privacy
- Only modifies standard Windows startup location
- No hidden or obfuscated entries
- Can be viewed/removed via Task Manager → Startup tab

## Alternative Methods

### Task Manager

You can also manage startup via Windows Task Manager:

1. Open Task Manager (`Ctrl + Shift + Esc`)
2. Go to "Startup" tab
3. Find "FocusMotherFocus"
4. Right-click → Enable/Disable

### Task Scheduler (Advanced)

For more control (delayed start, specific conditions):

1. Use Windows Task Scheduler
2. Create a new task
3. Trigger: At log on
4. Action: Start a program
5. Program: `python.exe`
6. Arguments: `"C:\Path\To\main.py"`

## Platform Support

### Windows
✅ **Fully Supported** - Uses Registry

### macOS
❌ **Not Implemented** - Would need:
- LaunchAgents plist file
- `~/Library/LaunchAgents/com.focus.monitor.plist`

### Linux
❌ **Not Implemented** - Would need:
- `.desktop` file
- `~/.config/autostart/focus-monitor.desktop`

## Future Enhancements

Potential improvements:

1. **Cross-Platform Support**:
   - Implement `MacStartupManager`
   - Implement `LinuxStartupManager`
   - Auto-detect platform in main.py

2. **Startup Options**:
   - Delayed start (wait X seconds after login)
   - Start minimized to system tray
   - Start with monitoring already active

3. **Advanced Configuration**:
   - Run only on specific days/times
   - Conditional startup (if VPN connected, etc.)
   - Multiple startup profiles

## Summary

✅ **Auto-startup is now fully functional!**

The application will:
- ✅ Add itself to Windows startup programs via Registry
- ✅ Start automatically when you log in to Windows
- ✅ Provide a simple checkbox to enable/disable
- ✅ Follow Clean Architecture principles
- ✅ Be platform-independent (infrastructure layer)

Simply check the box, restart your computer, and the application will be ready and waiting! 🚀
