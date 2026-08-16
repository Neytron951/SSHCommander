<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" align="left" style="margin-right: 15px;"/>

**SSH COMMANDER**

---

**SSH Commander** — a powerful SSH/SFTP client with terminal, widgets, and biometric authentication.

> **Current version:** 1.3 — sessions & tabs, folder management, desktop UI polish

![App Screenshot](ScreenShots/screenshot.png)

SSH Commander is a powerful and user-friendly cross-platform client for remote server management via SSH and SFTP — available on **Android** and **Windows**. Built for system administrators, DevOps engineers, and anyone who works with remote servers.

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
- **Windows** — desktop client with resizable panes, menu bar, and import/export

## Requirements

- **Android 7.0** (API level 24) or higher
- **Windows 10** or higher (for the desktop version)

## Installation

### From GitHub Releases
1. Go to [Releases](https://github.com/Neytron951/SSHCommander/releases)
2. Download the latest `.apk` for Android or `.msi` / `.exe` for Windows
3. Install (allow installation from unknown sources on Android)

### From Source
```bash
git clone https://github.com/Neytron951/SSHCommander.git
cd SSHCommander

# Android app
./gradlew :androidApp:assembleDebug

# Windows desktop app (MSI or EXE installer)
./gradlew :desktopApp:packageMsi
./gradlew :desktopApp:packageExe

# Windows desktop app — unpacked folder
./gradlew :desktopApp:createDistributable

# Run desktop app for development
./gradlew :desktopApp:run

# Tests
./gradlew :shared:desktopTest
```