@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title SSHCommander - build Windows (MSI + EXE)
cd /d "%~dp0"

echo ============================================================
echo   SSHCommander - сборка установщиков Windows (MSI + EXE)
echo.
echo   MSI ставит приложение в Program Files (все пользователи)
echo   и создаёт ярлык в меню Пуск - поэтому Windows сможет
echo   находить SSHCommander поиском / в списке приложений.
echo   При установке потребуется подтверждение администратора.
echo ============================================================
echo.

rem ------------------------------------------------------------
rem  1. Имя выходного exe/msi
rem ------------------------------------------------------------
set "PACKAGE_NAME="
set /p "PACKAGE_NAME=Имя выходного exe/msi [Enter = SSHCommander]: "
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSHCommander"
rem убираем возможный суффикс ".exe" / ".msi" из ввода
if /I "!PACKAGE_NAME:~-4!"==".exe" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
if /I "!PACKAGE_NAME:~-4!"==".msi" set "PACKAGE_NAME=!PACKAGE_NAME:~0,-4!"
rem убираем пробелы из имени (чтобы пути были корректными)
set "PACKAGE_NAME=!PACKAGE_NAME: =!"
if "!PACKAGE_NAME!"=="" set "PACKAGE_NAME=SSHCommander"

rem ------------------------------------------------------------
rem  2. Версия пакета (формат MAJOR.MINOR.BUILD, напр. 1.4.0)
rem ------------------------------------------------------------
set "PACKAGE_VERSION="
set /p "PACKAGE_VERSION=Версия пакета [Enter = 1.4.0]: "
if "!PACKAGE_VERSION!"=="" set "PACKAGE_VERSION=1.4.0"
echo !PACKAGE_VERSION!| findstr /r /c:"^[0-9][0-9]*\.[0-9][0-9]*\.[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Версия должна быть в формате MAJOR.MINOR.BUILD, например 1.4.0.
    pause
    exit /b 1
)

echo.
echo Собираю: %PACKAGE_NAME% v%PACKAGE_VERSION%
echo.

rem ------------------------------------------------------------
rem  3. Запуск сборки (jpackage: MSI + EXE)
rem ------------------------------------------------------------
echo [1/2] jpackage: сборка MSI + EXE...
rem вызываем gradlew по абсолютному пути: на части систем переменная
rem NoDefaultCurrentDirectoryInExePath запрещает поиск в текущей папке
call "%~dp0gradlew.bat" :desktopApp:packageMsi :desktopApp:packageExe -PappName=%PACKAGE_NAME% -PappVersion=%PACKAGE_VERSION% --console=plain
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Сборка завершилась с ошибкой. Смотрите вывод выше.
    pause
    exit /b 1
)

rem ------------------------------------------------------------
rem  4. Копирование готовых установщиков в dist\
rem ------------------------------------------------------------
set "SRC=%~dp0desktopApp\build\compose\binaries\main"
set "OUT=%~dp0dist"
if not exist "%OUT%" mkdir "%OUT%"

echo.
echo [2/2] Копирую установщики в "%OUT%"...

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\msi\%PACKAGE_NAME%-*.msi" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\msi\%%f" "%OUT%\%PACKAGE_NAME%.msi" >nul
    echo   MSI:  "%OUT%\%PACKAGE_NAME%.msi"
)
if not defined FOUND echo   [ВНИМАНИЕ] MSI не найден в "%SRC%\msi\"

set "FOUND="
for /f "delims=" %%f in ('dir /b /o-d "%SRC%\exe\%PACKAGE_NAME%-*.exe" 2^>nul') do if not defined FOUND (
    set "FOUND=1"
    copy /y "%SRC%\exe\%%f" "%OUT%\%PACKAGE_NAME%.exe" >nul
    echo   EXE:  "%OUT%\%PACKAGE_NAME%.exe"
)
if not defined FOUND echo   [ВНИМАНИЕ] EXE не найден в "%SRC%\exe\"

echo.
echo ============================================================
echo   Готово! Установщики лежат в папке: %OUT%
echo.
echo   - %PACKAGE_NAME%.msi - обычная установка (рекомендуется):
echo     Program Files + ярлык в меню Пуск, Windows найдёт приложение.
echo   - %PACKAGE_NAME%.exe  - тот же установщик в формате EXE.
echo ============================================================
pause
endlocal
