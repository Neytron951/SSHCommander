<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" align="left" style="margin-right: 15px;"/>

**SSH COMMANDER**

---

**SSH Commander** — a powerful SSH/SFTP client with terminal, widgets, and biometric authentication.

> **Current version:** 1.4 — SFTP file preview & selection, Terminal+SFTP split view, Windows & Linux support.

![App Screenshot](ScreenShots/screenshot.png)

SSH Commander is a powerful and user-friendly cross-platform client for remote server management via SSH and SFTP — available on **Android**, **Windows**, and **Linux**. Built for system administrators, DevOps engineers, and anyone who works with remote servers.

## Features

### Full-Featured Terminal
- Full VT100/ANSI emulation with color support — nano, vim, htop work flawlessly
- Control key panel: ESC, arrow keys, TAB, Ctrl+X/O/G/W/K
- Key hold with auto-repeat, block cursor, auto-scroll
- Customizable font and text size for comfortable work
- Desktop: zoom terminal text with Ctrl + mouse wheel

### Sessions & Tabs
- **Session tabs** on both Android and Windows — open as many sessions as you need
- **Multiple sessions per server** — start a second (third, …) session for the same server with the "+" button
- **Full session persistence** — minimize the app, switch servers or tabs; every session keeps its state until the app is closed
- Session tabs scroll horizontally, so a large number of open sessions never becomes a problem
- One tap closes a session and releases its connection

### SFTP File Manager
- Browse and navigate server file structure
- Upload and download files, create folders, rename and delete
- File search, show hidden files, multi-select
- Configurable startup folder for quick access
- **File preview without download** — text, JSON and images open instantly; binaries (EXE/RAR/ZIP/MSI) are blocked
- **Selection mechanics:** single click — select, Ctrl+click — multi-select, Shift+click — range, double-click — preview / open folder, right-click — context menu with download
- **Choose the download folder** instead of silently saving to the home directory
- **Manual path entry** with the keyboard (e.g. `/var/www/site`)

### Split View (Terminal + SFTP)
- Split the window 50/50 with a draggable divider — resize panes by dragging the border
- Quick-commands panel is available in split mode too

### Quick Commands & Widgets
- Ready-to-use commands: df -h, free -m, htop, uptime, ps aux, tail -f
- Create custom commands with confirmation and biometric authentication for dangerous actions
- Android home screen widget: server status (online/offline) and quick command execution

### Security
- App lock with fingerprint or Face ID
- Host key change verification (Man-in-the-Middle protection)
- Privacy mode — hide part of IP addresses in the list

### Flexibility & Settings
- Unlimited number of servers with grouping by folders and icons
- Folders can be created, renamed and deleted on both platforms
- Multiple logins per server
- Light and dark theme, terminal appearance customization
- Auto-reconnect on connection drop
- Backup & restore (JSON export/import, compatible between Android and desktop)
- English and Russian language support

## Platforms

- **Android** — full-featured mobile client with home-screen widgets and biometric lock
- **Windows** — desktop client with resizable panes, Terminal+SFTP split view, and MSI/EXE installers.
- **Linux** — desktop client for Linux systems (tested on Debian/Ubuntu), supports DEB and RPM packages.

## Requirements

- **Android 7.0** (API level 24) or higher
- **Windows 10** or higher
- **Linux** (modern distributions like Ubuntu 22.04+, Debian 11+, etc.)

## Installation

### From GitHub Releases
1. Go to [Releases](https://github.com/Neytron951/SSHCommander/releases)
2. Download the latest `.apk` (Android), `.msi`/`.exe` (Windows), or `.deb`/`.rpm` (Linux)
3. Install

### From Source
```bash
git clone https://github.com/Neytron951/SSHCommander.git
cd SSHCommander

# Android app
./gradlew :androidApp:assembleDebug

# Windows desktop app (MSI or EXE installer)
./gradlew :desktopAppWindows:packageMsi
./gradlew :desktopAppWindows:packageExe

# Or build both installers with the interactive script (asks for name/version)
# and puts ready .msi / .exe into the dist\ folder
build-windows.bat

# Windows desktop app — unpacked folder
./gradlew :desktopAppWindows:createDistributable

# Run desktop app for development
./gradlew :desktopAppWindows:run
./gradlew :desktopAppLinux:run

# Build Linux packages (DEB or RPM)
./gradlew :desktopAppLinux:packageDeb
./gradlew :desktopAppLinux:packageRpm

# Or use the build script for Linux
chmod +x build-linux.sh
./build-linux.sh

# Tests
./gradlew :shared:desktopTest
```