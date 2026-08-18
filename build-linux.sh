#!/bin/bash

# Exit on any error
set -e

APP_NAME=${1:-"SSHCommander"}
APP_VERSION=${2:-"1.4.0"}

echo "============================================================"
echo "  SSHCommander - Building Linux packages (DEB + RPM)"
echo "  Building: $APP_NAME v$APP_VERSION"
echo "============================================================"

# Ensure we are in the project root
cd "$(dirname "$0")"

# Grant execution permissions to gradlew if needed
chmod +x gradlew

echo "[1/2] Running jpackage tasks: packageDeb packageRpm..."
./gradlew :desktopAppLinux:packageDeb :desktopAppLinux:packageRpm \
    -PappName="$APP_NAME" \
    -PappVersion="$APP_VERSION" \
    --console=plain

SRC="desktopAppLinux/build/compose/binaries/main"
OUT="dist"

mkdir -p "$OUT"

echo ""
echo "[2/2] Copying packages to '$OUT'..."

# Copy latest DEB
DEB_FILE=$(ls -t "$SRC/deb/"*.deb 2>/dev/null | head -n 1)
if [ -n "$DEB_FILE" ]; then
    cp "$DEB_FILE" "$OUT/$APP_NAME.deb"
    echo "  DEB:  $OUT/$APP_NAME.deb"
else
    echo "  [WARNING] DEB package not found in $SRC/deb/"
fi

# Copy latest RPM
RPM_FILE=$(ls -t "$SRC/rpm/"*.rpm 2>/dev/null | head -n 1)
if [ -n "$RPM_FILE" ]; then
    cp "$RPM_FILE" "$OUT/$APP_NAME.rpm"
    echo "  RPM:  $OUT/$APP_NAME.rpm"
else
    echo "  [WARNING] RPM package not found in $SRC/rpm/"
fi

echo ""
echo "============================================================"
echo "  Done! Packages are in: $OUT"
echo "============================================================"
