#!/bin/bash

# Останавливаемся при любой ошибке
set -e

echo "============================================================"
echo "  Установка SSH Commander для Arch Linux (Hyprland)"
echo "============================================================"

# Переходим в корень проекта
cd "$(dirname "$0")"

# Даем права на выполнение Gradle
chmod +x gradlew

echo "[1/2] Собираю приложение..."
./gradlew :desktopAppLinux:createDistributable --console=plain

# Путь к собранному приложению
APP_BASE_DIR="desktopAppLinux/build/compose/binaries/main/app"

# Ищем любую папку внутри app, где есть подпапка bin (игнорируя регистр и пробелы)
APP_DIR=$(find "$APP_BASE_DIR" -maxdepth 1 -mindepth 1 -type d | head -n 1)

if [ -z "$APP_DIR" ] || [ ! -d "$APP_DIR" ]; then
    echo "Ошибка: Не удалось найти папку с приложением в $APP_BASE_DIR"
    exit 1
fi

# Ищем исполняемый файл внутри папки bin
BINARY_PATH=$(find "$APP_DIR/bin" -maxdepth 1 -type f -executable | head -n 1)

if [ -z "$BINARY_PATH" ]; then
    echo "Ошибка: Не удалось найти исполняемый файл в $APP_DIR/bin"
    exit 1
fi

# Получаем абсолютные пути
BINARY_ABS_PATH="$(realpath "$BINARY_PATH")"
ICON_PATH="$(realpath "desktopAppLinux/src/main/resources/icon.png")"
DESKTOP_DIR="$HOME/.local/share/applications"
DESKTOP_FILE="$DESKTOP_DIR/sshcommander.desktop"

# Создаем папку, если её нет
mkdir -p "$DESKTOP_DIR"

echo "[2/2] Создаю ярлык в $DESKTOP_FILE..."

cat <<EOF > "$DESKTOP_FILE"
[Desktop Entry]
Name=SSH Commander
Exec=env GDK_BACKEND=x11 _JAVA_AWT_WM_NONREPARENTING=1 "$BINARY_ABS_PATH"
Icon=$ICON_PATH
Terminal=false
Type=Application
Categories=Network;Development;
Comment=SSH/SFTP client for Linux
EOF

chmod +x "$DESKTOP_FILE"

echo ""
echo "============================================================"
echo "  Готово! Проверь свое меню приложений (rofi/wofi/etc)."
echo "  Путь к бинарнику: $BINARY_ABS_PATH"
echo ""
echo "  ПРИМЕЧАНИЕ: Если приложение не появилось в меню, попробуй"
echo "  скопировать ярлык в системную папку (потребуется sudo):"
echo "  sudo cp $DESKTOP_FILE /usr/share/applications/"
echo "============================================================"
