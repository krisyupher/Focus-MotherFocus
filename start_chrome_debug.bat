@echo off
echo ========================================
echo Starting Chrome with Remote Debugging
echo ========================================
echo.
echo This allows FocusMotherFocus to automatically close distraction tabs.
echo.
echo Debugging port: 9222
echo.

REM Try common Chrome installation paths
set CHROME_PATH=""

REM Path 1: Program Files (System-wide install)
if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    set CHROME_PATH="C:\Program Files\Google\Chrome\Application\chrome.exe"
    goto :found
)

REM Path 2: Program Files (x86)
if exist "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" (
    set CHROME_PATH="C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
    goto :found
)

REM Path 3: Local AppData (User install)
if exist "%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe" (
    set CHROME_PATH="%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"
    goto :found
)

REM Chrome not found
echo ERROR: Chrome not found in common locations!
echo.
echo Please manually run Chrome with:
echo chrome.exe --remote-debugging-port=9222
echo.
pause
exit /b 1

:found
echo Found Chrome at: %CHROME_PATH%
echo.
echo Starting Chrome with remote debugging enabled...
echo.

REM Start Chrome with remote debugging
start "" %CHROME_PATH% --remote-debugging-port=9222

echo.
echo ========================================
echo Chrome Started Successfully!
echo ========================================
echo.
echo You can now run project checks and demos with:
echo    python -m scripts.run_all_checks
echo.
echo Or run a specific demo in scripts/, e.g.:
echo    python scripts\main_mcp_demo.py
echo.
echo To automatically organize files (archive old demos/config), run:
echo    python scripts/organize_repo.py --archive
echo.
echo IMPORTANT: Keep this Chrome instance running
echo for auto-close to work!
echo.

REM New: hint for Windows-MCP usage
echo.
echo Optional integration: Windows-MCP (MCPGet) can provide active-window, running apps, screenshots and UI tree.
echo See: https://github.com/CursorTouch/Windows-MCP
REM Try to detect MCPGet on PATH
where MCPGet.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo MCPGet found on PATH. You can run: python main_mcp_demo.py
) else (
    echo MCPGet not found. Download/build Windows-MCP and add MCPGet.exe to PATH, or pass path to WindowsMCP in code.
)

REM New: detect mcp_server_notify on PATH
where mcp_server_notify.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo mcp_server_notify found on PATH. You can run: python main_notify_demo.py
) else (
    echo mcp_server_notify not found. Install mcp_server_notify or add it to PATH to enable desktop notifications.
)

REM New: detect browser-tools-mcp on PATH (Browser-Tools-MCP)
where browser-tools-mcp.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo browser-tools-mcp found on PATH. You can run: python main_browser_mcp_demo.py
) else (
    echo browser-tools-mcp not found. Install/browser-tools-mcp or add it to PATH to enable browser tab/console/network captures.
)

REM New: detect memory server binary on PATH
where memory_server.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo memory_server found on PATH. You can run: python main_memory_demo.py or python main_memory_kb_demo.py
) else (
    echo memory_server not found. If you run a Memory MCP HTTP server, ensure it's reachable at the API URL in mcp_client_config.json.
)

REM New: detect filesystem server binary on PATH
where filesystem_server.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo filesystem_server found on PATH. You can run: python main_filesystem_demo.py
) else (
    echo filesystem_server not found. If you run a Filesystem MCP server, ensure it's reachable at the API URL in mcp_client_config.json.
)

REM New: detect elevenlabs_mcp on PATH
where elevenlabs_mcp.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo elevenlabs_mcp found on PATH. You can run: python main_eleven_demo.py
) else (
    echo elevenlabs_mcp not found. Install or add elevenlabs_mcp to PATH to enable TTS/voice cloning.
)

REM New: detect heygen_mcp on PATH
where heygen_mcp.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo heygen_mcp found on PATH. You can run: python main_heygen_demo.py
) else (
    echo heygen_mcp not found. Install or add heygen_mcp to PATH to enable AI avatar video generation.
)

REM New: detect mcp-webcam on PATH
where mcp-webcam.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo mcp-webcam found on PATH. You can run: python main_webcam_demo.py
) else (
    echo mcp-webcam not found. Install or add mcp-webcam to PATH, or configure api_url in mcp_client_config.json to enable webcam captures.
)

REM New: detect notifymemaybe on PATH
where notifymemaybe.exe >nul 2>nul
if %ERRORLEVEL% == 0 (
    echo notifymemaybe found on PATH. You can run: python main_notifymaybe_demo.py
) else (
    echo notifymemaybe not found. Install or add notifymemaybe to PATH to enable interactive prompts.
)
echo.

REM Tip: run the central runner instead of invoking many scripts directly
echo.
pause
