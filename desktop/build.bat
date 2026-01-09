@echo off
REM Build script for Focus MotherFocus executable
REM Creates standalone .exe file in dist\ folder

echo ============================================================
echo Focus MotherFocus - Build Script
echo ============================================================
echo.

REM Check if PyInstaller is installed
python -c "import PyInstaller" 2>nul
if errorlevel 1 (
    echo [ERROR] PyInstaller not found!
    echo Installing PyInstaller...
    pip install pyinstaller
    echo.
)

echo [1/4] Cleaning previous builds...
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
echo Done.
echo.

echo [2/4] Building executable with PyInstaller...
pyinstaller --clean FocusMonitor.spec
echo.

if exist dist\FocusMotherFocus.exe (
    echo [3/4] Build successful!
    echo.
    echo [4/4] Executable created:
    echo   Location: %CD%\dist\FocusMotherFocus.exe
    echo   Size:
    dir dist\FocusMotherFocus.exe | find "FocusMotherFocus.exe"
    echo.
    echo ============================================================
    echo SUCCESS! Executable ready to distribute.
    echo ============================================================
    echo.
    echo Next steps:
    echo   1. Test: dist\FocusMotherFocus.exe
    echo   2. Share: Copy dist\FocusMotherFocus.exe to others
    echo   3. First run will create config.json automatically
    echo.
) else (
    echo [ERROR] Build failed!
    echo Check the output above for errors.
    echo.
)

pause
