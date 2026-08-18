@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title SSHCommander - build Windows (MSI + EXE)
cd /d "%~dp0"

echo ============================================================
echo   SSHCommander - СЃР±РѕСЂРєР° СѓСЃС‚Р°РЅРѕРІС‰РёРєРѕРІ Windows (MSI + EXE)
echo.
echo   MSI СЃС‚Р°РІРёС‚ РїСЂРёР»РѕР¶РµРЅРёРµ РІ Program Files (РІСЃРµ РїРѕР»СЊР·РѕРІР°С‚РµР»Рё)
echo   Рё СЃРѕР·РґР°С‘С‚ СЏСЂР»С‹Рє РІ РјРµРЅСЋ РџСѓСЃРє - РїРѕСЌС‚РѕРјСѓ Windows СЃРјРѕР¶РµС‚
echo   РЅР°С…РѕРґРёС‚СЊ SSHCommander РїРѕРёСЃРєРѕРј / РІ СЃРїРёСЃРєРµ РїСЂРёР»РѕР¶РµРЅРёР№.
echo   РџСЂРё СѓСЃС‚Р°РЅРѕРІРєРµ РїРѕС‚СЂРµР±СѓРµС‚СЃСЏ РїРѕРґС‚РІРµСЂР¶РґРµРЅРёРµ Р°РґРјРёРЅРёСЃС‚СЂР°С‚РѕСЂР°.
echo ============================================================
echo.

rem ------------------------------------------------------------
rem  1. РРјСЏ РІС‹С…РѕРґРЅРѕРіРѕ exe/msi
rem ------------------------------------------------------------
set "PACKAGE_NAME="
set /p "PACKAGE_NAME=РРјСЏ РІС‹С…РѕРґРЅРѕРіРѕ exe/msi [Enter = SSHCommander]: "
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSHCommander"
rem СѓР±РёСЂР°РµРј РІРѕР·РјРѕР¶РЅС‹Р№ СЃСѓС„С„РёРєСЃ ".exe" / ".msi" РёР· РІРІРѕРґР°
if /I "!PACKAGE_NAME:~-4!"==".exe" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
if /I "!PACKAGE_NAME:~-4!"==".msi" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
rem СѓР±РёСЂР°РµРј РїСЂРѕР±РµР»С‹ РёР· РёРјРµРЅРё (С‡С‚РѕР±С‹ РїСѓС‚Рё Р±С‹Р»Рё РєРѕСЂСЂРµРєС‚РЅС‹РјРё)
set "PACKAGE_NAME=!PACKAGE_NAME: =!"
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSHCommander"

rem ------------------------------------------------------------
rem  2. Р’РµСЂСЃРёСЏ РїР°РєРµС‚Р° (С„РѕСЂРјР°С‚ MAJOR.MINOR.BUILD, РЅР°РїСЂ. 1.4.0)
rem ------------------------------------------------------------
set "PACKAGE_VERSION="
set /p "PACKAGE_VERSION=Р’РµСЂСЃРёСЏ РїР°РєРµС‚Р° [Enter = 1.4.0]: "
if "!PACKAGE_VERSION!"=="" set "PACKAGE_VERSION=1.4.0"
echo !PACKAGE_VERSION!| findstr /r /c:"^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo.
    echo [РћРЁРР‘РљРђ] Р’РµСЂСЃРёСЏ РґРѕР»Р¶РЅР° Р±С‹С‚СЊ РІ С„РѕСЂРјР°С‚Рµ MAJOR.MINOR.BUILD, РЅР°РїСЂРёРјРµСЂ 1.4.0.
    pause
    exit /b 1
)

echo.
echo РЎРѕР±РёСЂР°СЋ: %PACKAGE_NAME% v%PACKAGE_VERSION%
echo.

rem ------------------------------------------------------------
rem  3. Р—Р°РїСѓСЃРє СЃР±РѕСЂРєРё (jpackage: MSI + EXE)
rem ------------------------------------------------------------
echo [1/2] jpackage: СЃР±РѕСЂРєР° MSI + EXE...
rem РІС‹Р·С‹РІР°РµРј gradlew РїРѕ Р°Р±СЃРѕР»СЋС‚РЅРѕРјСѓ РїСѓС‚Рё: РЅР° С‡Р°СЃС‚Рё СЃРёСЃС‚РµРј РїРµСЂРµРјРµРЅРЅР°СЏ
rem NoDefaultCurrentDirectoryInExePath Р·Р°РїСЂРµС‰Р°РµС‚ РїРѕРёСЃРє РІ С‚РµРєСѓС‰РµР№ РїР°РїРєРµ
call "%~dp0gradlew.bat" :desktopAppWindows:packageMsi :desktopAppWindows:packageExe -PappName=%PACKAGE_NAME% -PappVersion=%PACKAGE_VERSION% --console=plain
if errorlevel 1 (
    echo.
    echo [РћРЁРР‘РљРђ] РЎР±РѕСЂРєР° Р·Р°РІРµСЂС€РёР»Р°СЃСЊ СЃ РѕС€РёР±РєРѕР№. РЎРјРѕС‚СЂРёС‚Рµ РІС‹РІРѕРґ РІС‹С€Рµ.
    pause
    exit /b 1
)

rem ------------------------------------------------------------
rem  4. РљРѕРїРёСЂРѕРІР°РЅРёРµ РіРѕС‚РѕРІС‹С… СѓСЃС‚Р°РЅРѕРІС‰РёРєРѕРІ РІ dist\
rem ------------------------------------------------------------
set "SRC=%~dp0desktopApp\build\compose\binaries\main"
set "OUT=%~dp0dist"
if not exist "%OUT%" mkdir "%OUT%"

echo.
echo [2/2] РљРѕРїРёСЂСѓСЋ СѓСЃС‚Р°РЅРѕРІС‰РёРєРё РІ "%OUT%"...

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\msi\%PACKAGE_NAME%-*.msi" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\msi\%%f" "%OUT%\%PACKAGE_NAME%.msi" >nul
    echo   MSI:  "%OUT%\%PACKAGE_NAME%.msi"
)
if not defined FOUND echo   [Р’РќРРњРђРќРР•] MSI РЅРµ РЅР°Р№РґРµРЅ РІ "%SRC%\msi\"

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\exe\%PACKAGE_NAME%-*.exe" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\exe\%%f" "%OUT%\%PACKAGE_NAME%.exe" >nul
    echo   EXE:  "%OUT%\%PACKAGE_NAME%.exe"
)
if not defined FOUND echo   [Р’РќРРњРђРќРР•] EXE РЅРµ РЅР°Р№РґРµРЅ РІ "%SRC%\exe\"

echo.
echo ============================================================
echo   Р“РѕС‚РѕРІРѕ! РЈСЃС‚Р°РЅРѕРІС‰РёРєРё Р»РµР¶Р°С‚ РІ РїР°РїРєРµ: %OUT%
echo.
echo   - %PACKAGE_NAME%.msi - РѕР±С‹С‡РЅР°СЏ СѓСЃС‚Р°РЅРѕРІРєР° (СЂРµРєРѕРјРµРЅРґСѓРµС‚СЃСЏ):
echo     Program Files + СЏСЂР»С‹Рє РІ РјРµРЅСЋ РџСѓСЃРє, Windows РЅР°Р№РґС‘С‚ РїСЂРёР»РѕР¶РµРЅРёРµ.
echo   - %PACKAGE_NAME%.exe  - С‚РѕС‚ Р¶Рµ СѓСЃС‚Р°РЅРѕРІС‰РёРє РІ С„РѕСЂРјР°С‚Рµ EXE.
echo ============================================================
pause
endlocal

