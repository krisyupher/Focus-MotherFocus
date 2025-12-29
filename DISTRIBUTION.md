# Focus MotherFocus - Distribution Guide

## Overview

This document explains how to share the **Focus MotherFocus** application with others. The application has been packaged as a standalone executable that requires **no Python installation**.

## Finding the Executable

The distributable executable is located at:

```
dist\FocusMotherFocus.exe
```

This is a **single, standalone file** that contains everything needed to run the application.

## System Requirements

**For Recipients:**
- Windows 10 or later
- No Python installation required
- No additional dependencies required
- Approximately 25-30 MB disk space

## How to Share

### Method 1: Direct File Copy

1. Navigate to the `dist` folder in this project
2. Copy `FocusMotherFocus.exe`
3. Share via:
   - USB drive
   - Email (if under size limit)
   - Cloud storage (Google Drive, Dropbox, OneDrive)
   - Network share
   - Messaging apps

### Method 2: Create a ZIP File

```bash
# From the project root
cd dist
tar -a -c -f FocusMotherFocus.zip FocusMotherFocus.exe
```

Then share `FocusMotherFocus.zip`.

### Method 3: Cloud Storage Link

1. Upload `dist\FocusMotherFocus.exe` to cloud storage
2. Generate a shareable link
3. Send the link to recipients

## Instructions for Recipients

### First-Time Setup

1. **Save the file** to a location of your choice (e.g., `C:\Program Files\FocusMotherFocus\` or Desktop)

2. **Run the executable** by double-clicking `FocusMotherFocus.exe`

3. **Windows SmartScreen warning** (may appear on first run):
   - Click "More info"
   - Click "Run anyway"
   - This is normal for unsigned executables

4. **First launch** will:
   - Create `config.json` in the same folder as the executable
   - Show the main application window
   - Start with an empty website list

### Daily Use

1. **Add websites** to monitor:
   - Enter domain name (e.g., `facebook.com`, `google.com`)
   - Click "Add Website"

2. **Configure monitoring**:
   - Set "Check Interval" (seconds between checks)
   - Default: 10 seconds

3. **Start monitoring**:
   - Click "Start Monitoring"
   - Application will check websites and show alerts when they're online AND open in your browser

4. **Enable auto-startup** (optional):
   - Check "Start automatically when computer turns on"
   - Application will start when Windows boots

### How It Works

The application monitors your specified websites and shows **continuous alerts** (pop-up + sound) when:
1. The website is reachable (HTTP check)
2. AND the website is open in a browser tab (window title detection)

Alerts **automatically stop** when you close the browser tab.

## Troubleshooting for Recipients

### "Windows protected your PC" message

**Cause**: Windows SmartScreen blocks unsigned executables

**Solution**:
1. Click "More info"
2. Click "Run anyway"

This happens because the executable is not digitally signed. It's safe to run.

### Application doesn't start

**Possible causes and solutions**:

1. **Antivirus blocking**:
   - Add `FocusMonitor.exe` to antivirus exceptions
   - Temporarily disable antivirus and try again

2. **Missing administrator rights**:
   - Right-click `FocusMonitor.exe`
   - Select "Run as administrator"

3. **Corrupted download**:
   - Re-download the file
   - Verify file size (should be ~25-30 MB)

### Alerts don't work

1. **Check browser is supported**:
   - Google Chrome
   - Microsoft Edge
   - Brave
   - Firefox
   - Opera

2. **Verify website is in the list**:
   - Check the "Monitored Websites" section

3. **Ensure monitoring is active**:
   - Click "Start Monitoring" button

### Auto-startup doesn't work

1. **Check Task Manager**:
   - Press `Ctrl + Shift + Esc`
   - Go to "Startup" tab
   - Look for "FocusMotherFocus"
   - If disabled, enable it

2. **Verify Registry entry**:
   - Press `Win + R`
   - Type `regedit`
   - Navigate to: `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`
   - Look for "FocusMotherFocus" entry

3. **Re-enable via application**:
   - Uncheck the auto-startup checkbox
   - Check it again
   - Restart computer to test

### Config file issues

The application stores settings in `config.json` located in the same folder as `FocusMotherFocus.exe`.

**Reset configuration**:
1. Close the application
2. Delete `config.json`
3. Restart the application
4. Config will be recreated with defaults

## Configuration File Location

When you run `FocusMotherFocus.exe`, it creates a `config.json` file in the **same folder** as the executable.

**Example structure**:
```
C:\Program Files\FocusMotherFocus\
├── FocusMotherFocus.exe
└── config.json  (created automatically)
```

**Important**: If you move `FocusMotherFocus.exe` to a different folder, you'll need to reconfigure your website list (or copy `config.json` too).

## Security Considerations

### For You (Distributor)

- The executable is **not digitally signed**, so Windows SmartScreen will show warnings
- Recipients need to click "Run anyway" on first launch
- This is normal for free, unsigned executables

### For Recipients

- The application is safe and contains no malware
- It only monitors websites you explicitly add to the list
- It accesses:
  - Windows Registry (for auto-startup feature)
  - Network (to check website availability)
  - Window titles (to detect browser tabs)

- It does NOT:
  - Send data to external servers
  - Track your browsing history
  - Access personal files
  - Run hidden background processes

## Advanced: Creating a Signed Executable (Optional)

To avoid Windows SmartScreen warnings, you can sign the executable with a code signing certificate.

**Requirements**:
- Code signing certificate from a Certificate Authority (e.g., DigiCert, Sectigo)
- Cost: ~$100-300/year

**Process**:
1. Purchase code signing certificate
2. Install certificate on your machine
3. Use `signtool` to sign the executable:

```bash
signtool sign /a /t http://timestamp.digicert.com /fd SHA256 dist\FocusMotherFocus.exe
```

This removes the SmartScreen warning for recipients.

## Version Updates

When you release a new version:

1. **Rebuild the executable**:
   ```bash
   build.bat
   ```

2. **Rename with version** (optional):
   ```
   FocusMotherFocus_v1.0.exe
   FocusMotherFocus_v1.1.exe
   ```

3. **Share the new version**

Recipients can simply replace the old `.exe` with the new one. The `config.json` file will be preserved.

## Uninstallation (For Recipients)

There is no formal installer, so uninstallation is simple:

1. **Disable auto-startup** (if enabled):
   - Run the application
   - Uncheck "Start automatically when computer turns on"

2. **Close the application**

3. **Delete files**:
   - Delete `FocusMotherFocus.exe`
   - Delete `config.json` (if you want to remove settings)

4. **Optional - Remove from Registry manually**:
   - Press `Win + R`
   - Type `regedit`
   - Navigate to: `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`
   - Delete "FocusMotherFocus" entry (if exists)

## File Sizes

Expected file sizes:

- `FocusMotherFocus.exe`: ~25-30 MB (single-file bundle)
- `config.json`: < 1 KB (text file)

If the executable is significantly smaller (<10 MB) or larger (>50 MB), the build may have failed.

## Rebuilding the Executable

If you need to rebuild after making code changes:

1. **Run the build script**:
   ```bash
   build.bat
   ```

2. **Verify success**:
   - Check `dist\FocusMotherFocus.exe` exists
   - Check file size (~25-30 MB)

3. **Test before distributing**:
   ```bash
   dist\FocusMotherFocus.exe
   ```

4. **Share the updated version**

## Support

If recipients encounter issues:

1. **Check this document** - Most common issues are covered
2. **Verify system requirements** - Windows 10+ required
3. **Test on your machine** - Reproduce the issue if possible
4. **Check antivirus logs** - Often the culprit for startup issues

## Summary

**To distribute Focus MotherFocus:**
1. Find the executable at `dist\FocusMotherFocus.exe`
2. Copy and share the single file
3. Recipients run it - no installation needed
4. First run creates config automatically

**Recipients need:**
- Windows 10+
- Nothing else - fully standalone

**Common recipient actions:**
- Click "More info" → "Run anyway" on first launch
- Add websites to monitor
- Click "Start Monitoring"
- Optionally enable auto-startup

That's it! The application is ready to share and use.
