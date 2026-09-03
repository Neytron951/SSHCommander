@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title SSH Commander - build Windows (MSI + EXE)
cd /d "%~dp0"
echo ============================================================
echo   SSH Commander - Windows Installer Builder (MSI + EXE)
echo.
echo   MSI installs to Program Files (all users)
echo   and creates Start Menu shortcut.
echo   Admin rights required.
echo ============================================================
echo.

REM ------------------------------------------------------------
REM  1. Output package name
REM ------------------------------------------------------------
set "PACKAGE_NAME="
set /p "PACKAGE_NAME=Output name [Enter = SSH Commander]: "
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSH Commander"
if /I "!PACKAGE_NAME:~-4!"==".exe" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
if /I "!PACKAGE_NAME:~-4!"==".msi" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSH Commander"

REM ------------------------------------------------------------
REM  2. Package version (without validation)
REM ------------------------------------------------------------
set "PACKAGE_VERSION="
set /p "PACKAGE_VERSION=Version [Enter = 1.9.2]: "
if "!PACKAGE_VERSION!"=="" set "PACKAGE_VERSION=1.9.2"

echo.
echo Building: %PACKAGE_NAME% v%PACKAGE_VERSION%
echo.

REM ------------------------------------------------------------
REM  3. Run jpackage build
REM ------------------------------------------------------------
echo [1/2] Running jpackage: MSI + EXE...
call "%~dp0gradlew.bat" :desktopAppWindows:packageMsi :desktopAppWindows:packageExe "-PappName=%PACKAGE_NAME%" -PappVersion=%PACKAGE_VERSION% --console=plain

if errorlevel 1 (
    echo.
    echo [ERROR] Build failed. Check output above.
    pause
    exit /b 1
)

REM ------------------------------------------------------------
REM  4. Copy installers to dist\
REM ------------------------------------------------------------
set "SRC=%~dp0desktopAppWindows\build\compose\binaries\main"
set "OUT=%~dp0dist"

if not exist "%OUT%" mkdir "%OUT%"

echo.
echo [2/2] Copying installers to "%OUT%"...

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\msi\%PACKAGE_NAME%-*.msi" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\msi\%%f" "%OUT%\%PACKAGE_NAME%.msi" >nul
    echo   [OK] MSI: "%OUT%\%PACKAGE_NAME%.msi"
)
if not defined FOUND echo   [ERROR] MSI not found in "%SRC%\msi\"

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\exe\%PACKAGE_NAME%-*.exe" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\exe\%%f" "%OUT%\%PACKAGE_NAME%.exe" >nul
    echo   [OK] EXE: "%OUT%\%PACKAGE_NAME%.exe"
)
if not defined FOUND echo   [ERROR] EXE not found in "%SRC%\exe\"

echo.
echo ============================================================
echo   Done! Installers are in: %OUT%
echo.
echo   - %PACKAGE_NAME%.msi  - MSI installer
echo   - %PACKAGE_NAME%.exe  - EXE installer (same as MSI)
echo ============================================================

pause
endlocal
