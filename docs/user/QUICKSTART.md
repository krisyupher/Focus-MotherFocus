# Quick Start Guide - FocusMotherFocus

## Auto-Close Feature Setup (3 Steps)

### Step 1: Install Playwright
```bash
pip install -r requirements.txt
playwright install chromium
```

### Step 2: Start Chrome with Debugging
**Easy way** - Double-click:
```
start_chrome_debug.bat
```

**Manual way** - Run this command:
```batch
"C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222
```

### Step 3: Run FocusMotherFocus
```bash
python main_v2.py
```

## Quick Test

1. **Add a target**: Type `"Monitor YouTube"` in AI Assistant
2. **Start monitoring**: Type `"Start monitoring"`
3. **Open YouTube** in your Chrome browser
4. **Wait 10 seconds** → Tab closes automatically! ✨

## Expected Output

When Chrome connects successfully:
```
[Main] Playwright browser controller initialized
```

When auto-close triggers:
```
[AutoClose] YouTube has been open for 10.1s - closing tab...
[AutoClose] Successfully closed YouTube
```

## Troubleshooting

### Error: "Browser controller not available"

**Solution**: Chrome needs to be started with `--remote-debugging-port=9222`

Run `start_chrome_debug.bat` or start Chrome manually with the debug flag.

### Error: "ECONNREFUSED ::1:9222"

**Causes**:
1. Chrome isn't running with debug port
2. Chrome is running but closed
3. Port 9222 is blocked

**Solutions**:
1. Run `start_chrome_debug.bat`
2. Make sure Chrome window stays open
3. Check if another program is using port 9222:
   ```batch
   netstat -ano | findstr :9222
   ```

### Auto-close not working

**Checklist**:
- ✅ Ran `start_chrome_debug.bat` (or started Chrome with `--remote-debugging-port=9222`)
- ✅ Chrome window is still open
- ✅ See `[Main] Playwright browser controller initialized` in console
- ✅ Added targets with `"Monitor [website]"`
- ✅ Started monitoring
- ✅ Opened the website in Chrome
- ✅ Waited 10+ seconds

## Without Auto-Close

If you don't want to use auto-close, the app works perfectly without it:

1. **Skip Playwright setup** - Just run `python main_v2.py`
2. You'll see: `[Main] Auto-close feature will be disabled`
3. All other features work normally:
   - ✅ Monitoring websites and apps
   - ✅ Visual and audio alerts
   - ✅ Animated avatar
   - ✅ AI commands

## Configuration

### Change auto-close time

Edit [main_v2.py](main_v2.py#L82):
```python
auto_close_threshold=10.0  # Change to 5, 15, 30, etc.
```

### Disable auto-close

Edit [main_v2.py](main_v2.py#L51):
```python
browser_controller = None  # Disable completely
```

## Complete Documentation

- **Auto-Close Guide**: [docs/AUTO_CLOSE_FEATURE.md](docs/AUTO_CLOSE_FEATURE.md)
- **AI Commands**: [docs/AI_NATURAL_LANGUAGE_GUIDE.md](docs/AI_NATURAL_LANGUAGE_GUIDE.md)
- **Avatar Guide**: [docs/ANIMATED_AVATAR_GUIDE.md](docs/ANIMATED_AVATAR_GUIDE.md)

## Need Help?

1. Check [docs/AUTO_CLOSE_FEATURE.md](docs/AUTO_CLOSE_FEATURE.md) for detailed troubleshooting
2. Make sure Chrome is running with debug port: `start_chrome_debug.bat`
3. Look for error messages in the console

**Enjoy your focused productivity!** 🎯
