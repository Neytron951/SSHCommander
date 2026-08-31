package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cross-platform string catalog. Mirrors the Android values/strings.xml +
 * values-ru/strings.xml so the ported screens render identical text.
 */
object AppStrings {

    // Backed by a Compose state so composables that read any AppStrings value
    // recompose immediately when the language changes (the desktop UI keeps
    // its whole screen mounted, unlike the phone's per-screen navigation).
    private val languageState = mutableStateOf("en")
    var language: String
        get() = languageState.value
        set(value) {
            languageState.value = value
            onLanguageChanged?.invoke()
        }

    /** Hook so UI can recompose when the language setting changes. */
    var onLanguageChanged: (() -> Unit)? = null

    private val s get() = if (language == "ru") ru else en

    // General
    val appName get() = s.appName
    val servers get() = s.servers
    val addServer get() = s.addServer
    val editServer get() = s.editServer
    val deleteServer get() = s.deleteServer
    val settings get() = s.settings
    val back get() = s.back
    val exit get() = s.exit

    // Theme
    val theme get() = s.theme
    val themeLight get() = s.themeLight
    val themeDark get() = s.themeDark
    val themeSystem get() = s.themeSystem

    // Language
    val languageLabel get() = s.languageLabel
    val langEn get() = s.langEn
    val langRu get() = s.langRu

    // Console font
    val consoleFont get() = s.consoleFont
    val fontDefault get() = s.fontDefault
    val fontMonospace get() = s.fontMonospace
    val fontSansSerif get() = s.fontSansSerif
    val fontSerif get() = s.fontSerif

    // Console font size
    val consoleFontSize get() = s.consoleFontSize
    val sizeSmall get() = s.sizeSmall
    val sizeMedium get() = s.sizeMedium
    val sizeLarge get() = s.sizeLarge

    // Data management
    val dataManagement get() = s.dataManagement
    val exportData get() = s.exportData
    val importData get() = s.importData
    val exportSuccess get() = s.exportSuccess
    val importSuccess get() = s.importSuccess

    // Server fields
    val serverName get() = s.serverName
    val hostIp get() = s.hostIp
    val port get() = s.port
    val username get() = s.username
    val password get() = s.password
    val saveServer get() = s.saveServer
    val save get() = s.save
    val cancel get() = s.cancel
    val pin get() = s.pin
    val unpin get() = s.unpin
    val pinnedServers get() = s.pinnedServers
    val delete get() = s.delete
    val edit get() = s.edit
    val editText get() = s.edit + " (Text)"
    val chooseAction get() = s.chooseAction
    val chooseIcon get() = s.chooseIcon

    // Commands
    val reboot get() = s.reboot
    val checkConnection get() = s.checkConnection
    val loading get() = s.loading
    val commands get() = s.commands
    val manageCommands get() = s.manageCommands
    val runCommand get() = s.runCommand
    val run get() = s.run
    val confirmExecution get() = s.confirmExecution
    val execute get() = s.execute
    val dangerous get() = s.dangerous
    val newCommand get() = s.newCommand
    val requiresBio get() = s.requiresBio
    val confirmRebootMsg get() = s.confirmRebootMsg
    val rebootConfirm get() = s.rebootConfirm

    // About / widget
    val aboutApp get() = s.aboutApp
    val aboutVersion get() = s.aboutVersion
    val license get() = s.license
    val disableAds get() = s.disableAds
    val disableAdsDesc get() = s.disableAdsDesc
    val disableAdsConfirmTitle get() = s.disableAdsConfirmTitle
    val disableAdsConfirmMsg get() = s.disableAdsConfirmMsg
    val disableAdsConfirmFinalTitle get() = s.disableAdsConfirmFinalTitle
    val disableAdsConfirmFinalMsg get() = s.disableAdsConfirmFinalMsg
    val showInWidget get() = s.showInWidget

    /** Short description lines shown on the desktop About dialog. */
    val aboutTextLines: List<String>
        get() = if (language == "ru") {
            listOf(
                "SSH Commander — удобный инструмент для управления серверами по SSH.",
                "Подключайтесь, выполняйте команды и управляйте файлами с Android и Windows."
            )
        } else {
            listOf(
                "SSH Commander is a handy SSH/SFTP client for managing your servers.",
                "Connect, run commands and manage files from Android and Windows."
            )
        }

    // Icons
    val iconGaming get() = s.iconGaming
    val iconWeb get() = s.iconWeb
    val iconDatabase get() = s.iconDatabase
    val iconCloud get() = s.iconCloud
    val iconNas get() = s.iconNas
    val iconVpn get() = s.iconVpn
    val iconDev get() = s.iconDev
    val iconMedia get() = s.iconMedia
    val iconDefault get() = s.iconDefault

    // SFTP
    val sftpExplorer get() = s.sftpExplorer
    val terminal get() = s.terminal
    val emptyDirectory get() = s.emptyDirectory
    val deleteFileConfirm get() = s.deleteFileConfirm

    // ServerControlScreen
    val exitSshSessionMsg get() = s.exitSshSessionMsg
    val confirmExit get() = s.confirmExit
    val historyUp get() = s.historyUp
    val historyDown get() = s.historyDown
    val tabKey get() = s.tabKey
    val ctrlCKey get() = s.ctrlCKey
    val clearKey get() = s.clearKey
    val escKey get() = s.escKey
    val enterKey get() = s.enterKey
    val commandPlaceholder get() = s.commandPlaceholder
    val executeConfirmMsg get() = s.executeConfirmMsg

    // Base commands
    val cmdList get() = s.cmdList
    val cmdTop get() = s.cmdTop
    val cmdDisk get() = s.cmdDisk
    val cmdRam get() = s.cmdRam
    val cmdUptime get() = s.cmdUptime
    val cmdProcesses get() = s.cmdProcesses
    val cmdLogs get() = s.cmdLogs

    // SftpExplorerScreen
    val searchFiles get() = s.searchFiles
    val closeSessionQ get() = s.closeSessionQ
    val stay get() = s.stay
    val upload get() = s.upload
    val sessionError get() = s.sessionError
    val reconnect get() = s.reconnect
    val dismiss get() = s.dismiss
    val exitSftpMsg get() = s.exitSftpMsg
    val hideHidden get() = s.hideHidden
    val showHidden get() = s.showHidden
    val selectedCount get() = s.selectedCount
    val newFolder get() = s.newFolder
    val folderNamePlaceholder get() = s.folderNamePlaceholder
    val create get() = s.create
    val rename get() = s.rename
    val folders get() = s.folders
    val noFolder get() = s.noFolder
    val deleteFolderConfirm get() = s.deleteFolderConfirm

    // SSH errors
    val errTimeout get() = s.errTimeout
    val errAuthFailed get() = s.errAuthFailed
    val errHostUnreachable get() = s.errHostUnreachable
    val errHostKeyMismatch get() = s.errHostKeyMismatch
    val errUnknown get() = s.errUnknown
    val errConnectionLost get() = s.errConnectionLost
    val sshErrorTemplate get() = s.sshErrorTemplate

    // Settings
    val terminalStyle get() = s.terminalStyle
    val backgroundColor get() = s.backgroundColor
    val textColor get() = s.textColor
    val cmdPlaceholderExample get() = s.cmdPlaceholderExample
    val errorPrefix get() = s.errorPrefix
    val sftpErrorPrefix get() = s.sftpErrorPrefix

    // Privacy / quick commands / auto-reconnect
    val privacyMode get() = s.privacyMode
    val privacyModeDesc get() = s.privacyModeDesc
    val quickCommands get() = s.quickCommands
    val autoReconnect get() = s.autoReconnect
    val autoReconnectDesc get() = s.autoReconnectDesc
    val reconnectingMsg get() = s.reconnectingMsg

    // Biometric lock
    val biometricLock get() = s.biometricLock
    val biometricLockDesc get() = s.biometricLockDesc
    val appLocked get() = s.appLocked
    val appLockedSubtitle get() = s.appLockedSubtitle
    val unlock get() = s.unlock
    val biometricNotAvailable get() = s.biometricNotAvailable
    val retry get() = s.retry

    // Onboarding
    val yes get() = s.yes
    val no get() = s.no
    val next get() = s.next
    val skip get() = s.skip
    val finish get() = s.finish
    val welcomeTitle get() = s.welcomeTitle
    val welcomeQuestion get() = s.welcomeQuestion
    val importJsonQuestion get() = s.importJsonQuestion
    val importJsonButton get() = s.importJsonButton
    val tourStep get() = s.tourStep

    // Logins
    val manageLogins get() = s.manageLogins
    val addLogin get() = s.addLogin
    val loginLabel get() = s.loginLabel
    val sftpStartPath get() = s.sftpStartPath
    val sftpStartPathHint get() = s.sftpStartPathHint
    val sftpStartPathHint2 get() = s.sftpStartPathHint2
    val noLogins get() = s.noLogins
    val setDefaultLogin get() = s.setDefaultLogin
    val deleteLoginTitle get() = s.deleteLoginTitle
    val deleteLoginMsg get() = s.deleteLoginMsg
    val selectLogin get() = s.selectLogin
    val mainLoginLabel get() = s.mainLoginLabel
    val addSession get() = s.addSession
    val closeSession get() = s.closeSession
    val sessionTabs get() = s.sessionTabs
    val copyText get() = s.copyText
    val selectAll get() = s.selectAll
    val clearSelection get() = s.clearSelection
    val preview get() = s.preview
    val download get() = s.download
    val copyPath get() = s.copyPath
    val open get() = s.open
    val fileInfo get() = s.fileInfo
    val previewUnavailable get() = s.previewUnavailable
    val fileType get() = s.fileType
    val fileSize get() = s.fileSize
    val modified get() = s.modified
    val permissions get() = s.permissions
    val folder get() = s.folder

    // SSH Keys
    val manageKeys get() = s.manageKeys
    val sshKeys get() = s.sshKeys
    val scriptMarket get() = s.scriptMarket
    val addKey get() = s.addKey
    val generateKey get() = s.generateKey
    val keyName get() = s.keyName
    val keyType get() = s.keyType
    val keyBits get() = s.keyBits
    val passphrase get() = s.passphrase
    val copyPublicKey get() = s.copyPublicKey
    val publicKeyCopied get() = s.publicKeyCopied
    val deleteKeyConfirm get() = s.deleteKeyConfirm

    // Identity / Unified Auth
    val identities get() = s.identities
    val authMethod get() = s.authMethod
    val usePassword get() = s.usePassword
    val useSshKey get() = s.useSshKey
    val autoProvisionDesc get() = s.autoProvisionDesc
    val provisioningWarning get() = s.provisioningWarning
    val importKeyContent get() = s.importKeyContent
    val generateNewKey get() = s.generateNewKey
    val selectExistingKey get() = s.selectExistingKey
    val provisionSuccess get() = s.provisionSuccess

    data class Strings(
        val appName: String, val servers: String, val addServer: String, val editServer: String,
        val deleteServer: String, val settings: String, val back: String, val exit: String,
        val theme: String, val themeLight: String, val themeDark: String, val themeSystem: String,
        val languageLabel: String, val langEn: String, val langRu: String,
        val consoleFont: String, val fontDefault: String, val fontMonospace: String,
        val fontSansSerif: String, val fontSerif: String,
        val consoleFontSize: String, val sizeSmall: String, val sizeMedium: String, val sizeLarge: String,
        val dataManagement: String, val exportData: String, val importData: String,
        val exportSuccess: String, val importSuccess: String,
        val serverName: String, val hostIp: String, val port: String, val username: String,
        val password: String, val saveServer: String, val save: String, val cancel: String,
        val pin: String, val unpin: String, val pinnedServers: String,
        val delete: String, val edit: String, val chooseAction: String, val chooseIcon: String,
        val reboot: String, val checkConnection: String, val loading: String, val commands: String,
        val manageCommands: String, val runCommand: String, val run: String,
        val confirmExecution: String, val execute: String, val dangerous: String,
        val newCommand: String, val requiresBio: String, val confirmRebootMsg: String,
        val rebootConfirm: String,
        val aboutApp: String, val aboutVersion: String,
        val license: String, val disableAds: String, val disableAdsDesc: String,
        val disableAdsConfirmTitle: String, val disableAdsConfirmMsg: String,
        val disableAdsConfirmFinalTitle: String, val disableAdsConfirmFinalMsg: String,
        val showInWidget: String,
        val iconGaming: String, val iconWeb: String, val iconDatabase: String, val iconCloud: String,
        val iconNas: String, val iconVpn: String, val iconDev: String, val iconMedia: String,
        val iconDefault: String,
        val sftpExplorer: String, val terminal: String, val emptyDirectory: String,
        val deleteFileConfirm: String,
        val exitSshSessionMsg: String, val confirmExit: String, val historyUp: String,
        val historyDown: String, val tabKey: String, val ctrlCKey: String, val clearKey: String,
        val escKey: String, val enterKey: String, val commandPlaceholder: String,
        val executeConfirmMsg: String,
        val cmdList: String, val cmdTop: String, val cmdDisk: String, val cmdRam: String,
        val cmdUptime: String, val cmdProcesses: String, val cmdLogs: String,
        val searchFiles: String, val closeSessionQ: String, val stay: String, val upload: String,
        val sessionError: String, val reconnect: String, val dismiss: String,
        val exitSftpMsg: String, val hideHidden: String, val showHidden: String,
        val selectedCount: String, val newFolder: String, val folderNamePlaceholder: String,
        val create: String, val rename: String,
        val folders: String, val noFolder: String, val deleteFolderConfirm: String,
        val errTimeout: String, val errAuthFailed: String, val errHostUnreachable: String,
        val errHostKeyMismatch: String, val errUnknown: String, val errConnectionLost: String,
        val sshErrorTemplate: String,
        val terminalStyle: String, val backgroundColor: String, val textColor: String,
        val cmdPlaceholderExample: String, val errorPrefix: String, val sftpErrorPrefix: String,
        val privacyMode: String, val privacyModeDesc: String, val quickCommands: String,
        val autoReconnect: String, val autoReconnectDesc: String, val reconnectingMsg: String,
        val biometricLock: String, val biometricLockDesc: String, val appLocked: String,
        val appLockedSubtitle: String, val unlock: String, val biometricNotAvailable: String,
        val retry: String,
        val yes: String, val no: String, val next: String, val skip: String, val finish: String,
        val welcomeTitle: String, val welcomeQuestion: String,
        val importJsonQuestion: String, val importJsonButton: String,
        val tourStep: String,
        val manageLogins: String, val addLogin: String, val loginLabel: String,
        val sftpStartPath: String, val sftpStartPathHint: String, val sftpStartPathHint2: String,
        val noLogins: String, val setDefaultLogin: String, val deleteLoginTitle: String,
        val deleteLoginMsg: String, val selectLogin: String, val mainLoginLabel: String,
        val addSession: String, val closeSession: String, val sessionTabs: String,
        val copyText: String, val selectAll: String, val clearSelection: String,
        val preview: String, val download: String, val copyPath: String, val open: String,
        val fileInfo: String, val previewUnavailable: String, val fileType: String,
        val fileSize: String, val modified: String, val permissions: String, val folder: String,
        val manageKeys: String, val sshKeys: String, val addKey: String, val generateKey: String,
            val keyName: String, val keyType: String, val keyBits: String, val passphrase: String,
        val copyPublicKey: String, val publicKeyCopied: String, val deleteKeyConfirm: String,
        val scriptMarket: String,
        val identities: String, val authMethod: String, val usePassword: String,
        val useSshKey: String, val autoProvisionDesc: String, val provisioningWarning: String,
        val importKeyContent: String, val generateNewKey: String, val selectExistingKey: String,
        val provisionSuccess: String
    )

    val en = Strings(
        appName = "SSH Commander", servers = "Servers", addServer = "Add Server",
        editServer = "Edit Server", deleteServer = "Delete Server", settings = "Settings",
        back = "Back", exit = "Exit",
        theme = "Theme", themeLight = "Light", themeDark = "Dark", themeSystem = "System",
        languageLabel = "Language", langEn = "English", langRu = "Русский",
        consoleFont = "Console Font Family", fontDefault = "Default",
        fontMonospace = "Monospace", fontSansSerif = "Sans Serif", fontSerif = "Serif",
        consoleFontSize = "Console Font Size", sizeSmall = "Small (12sp)",
        sizeMedium = "Medium (14sp)", sizeLarge = "Large (18sp)",
        dataManagement = "Data Management", exportData = "Export JSON",
        importData = "Import JSON", exportSuccess = "Data exported successfully",
        importSuccess = "Data imported. Please restart.",
        serverName = "Name", hostIp = "Host / IP", port = "Port", username = "Username",
        password = "Password", saveServer = "Save Server", save = "Save", cancel = "Cancel",
        pin = "Pin to top", unpin = "Unpin", pinnedServers = "Pinned",
        delete = "Delete", edit = "Edit", chooseAction = "Choose Action",
        chooseIcon = "Choose Icon",
        reboot = "Reboot", checkConnection = "Check Connection", loading = "Loading...",
        commands = "Commands", manageCommands = "Manage Commands",
        runCommand = "Run Command", run = "Run", confirmExecution = "Confirm Execution",
        execute = "Execute", dangerous = "Dangerous", newCommand = "New Custom Command",
        requiresBio = "Requires biometric confirmation",
        confirmRebootMsg = "Are you sure? Biometric verification may be required.",
        rebootConfirm = "Reboot Confirmation",
        aboutApp = "About", aboutVersion = "Version: %1\$s",
        license = "License", disableAds = "Disable Ads",
        disableAdsDesc = "Turning off ads will slightly reduce app support.",
        disableAdsConfirmTitle = "Disable Ads?",
        disableAdsConfirmMsg = "Ads help keep the app free. Are you sure you want to disable them?",
        disableAdsConfirmFinalTitle = "Last chance",
        disableAdsConfirmFinalMsg = "Are you really sure you want to disable ads? Double-check before you proceed.",
        showInWidget = "Show in Widget",
        iconGaming = "Gaming", iconWeb = "Web", iconDatabase = "Database", iconCloud = "Cloud",
        iconNas = "NAS", iconVpn = "VPN", iconDev = "Development", iconMedia = "Media",
        iconDefault = "Default",
        sftpExplorer = "SFTP Explorer", terminal = "Terminal",
        emptyDirectory = "Empty Directory", deleteFileConfirm = "Delete this file?",
        exitSshSessionMsg = "This will close the active SSH session. Continue?",
        confirmExit = "Confirm Exit", historyUp = "History Up", historyDown = "History Down",
        tabKey = "TAB", ctrlCKey = "CTRL+C", clearKey = "CLEAR", escKey = "ESC",
        enterKey = "ENTER", commandPlaceholder = "Command...",
        executeConfirmMsg = "Execute: %1\$s?",
        cmdList = "List", cmdTop = "Top", cmdDisk = "Disk", cmdRam = "RAM",
        cmdUptime = "Uptime", cmdProcesses = "Processes", cmdLogs = "Logs",
        searchFiles = "Search files...", closeSessionQ = "Close Session?", stay = "Stay",
        upload = "Upload", sessionError = "Session Error", reconnect = "Reconnect",
        dismiss = "Dismiss", exitSftpMsg = "Exiting will close the active SFTP connection.",
        hideHidden = "Hide hidden files", showHidden = "Show hidden files",
        selectedCount = "%1\$d selected", newFolder = "New Folder",
        folderNamePlaceholder = "Folder name", create = "Create", rename = "Rename",
        folders = "Folders", noFolder = "No folder",
        deleteFolderConfirm = "Delete folder \"%1\$s\"? Its servers will stay.",
        errTimeout = "Connection timed out. Check your network or server status.",
        errAuthFailed = "Authentication failed. Check your username, password, or SSH keys.",
        errHostUnreachable = "Host unreachable. Ensure the IP/Host and Port are correct.",
        errHostKeyMismatch = "SECURITY WARNING: Host identification has changed! Possible Man-in-the-Middle attack.",
        errUnknown = "An unexpected error occurred.",
        errConnectionLost = "Connection lost. Please try again.",
        sshErrorTemplate = "\n[Error: %1\$s]\n",
        terminalStyle = "Terminal Style", backgroundColor = "Background Color",
        textColor = "Text Color", cmdPlaceholderExample = "ls -la\ncd /var/www\n...",
        errorPrefix = "Error: %1\$s", sftpErrorPrefix = "SFTP Error: %1\$s",
        privacyMode = "Privacy Mode",
        privacyModeDesc = "Hide part of IP addresses when displaying them",
        quickCommands = "Quick Commands",
        autoReconnect = "Auto-reconnect",
        autoReconnectDesc = "Reconnect automatically when the connection drops",
        reconnectingMsg = "Connection lost. Reconnecting in %1\$d s…",
        biometricLock = "Biometric lock",
        biometricLockDesc = "Require fingerprint to unlock the app",
        appLocked = "App locked",
        appLockedSubtitle = "Unlock with your fingerprint to continue",
        unlock = "Unlock", biometricNotAvailable = "Biometrics are not available on this device",
        retry = "Try again",
        yes = "Yes", no = "No", next = "Next", skip = "Skip", finish = "Finish",
        welcomeTitle = "Welcome to SSH Commander!",
        welcomeQuestion = "Would you like to learn how to use SSH Commander?",
        importJsonQuestion = "Do you have a data backup (JSON file)? Import it now?",
        importJsonButton = "Import",
        tourStep = "Step %1\$d of %2\$d",
        manageLogins = "Logins", addLogin = "Add Login",
        loginLabel = "Label (e.g. root, deploy)", sftpStartPath = "SFTP Start Folder",
        sftpStartPathHint = "SFTP start: %1\$s",
        sftpStartPathHint2 = "Where SFTP opens first. Empty = home folder (or last visited).",
        noLogins = "No additional logins yet. Tap + to add one.",
        setDefaultLogin = "Set as default login", deleteLoginTitle = "Delete login?",
        deleteLoginMsg = "Delete login \"%1\$s\"?", selectLogin = "Select login",
        mainLoginLabel = "Main login (%1\$s)",
        addSession = "New Session", closeSession = "Close Session", sessionTabs = "Sessions",
        copyText = "Copy", selectAll = "Select All", clearSelection = "Clear selection",
        preview = "Preview", download = "Download", copyPath = "Copy path", open = "Open",
        fileInfo = "File info", previewUnavailable = "Preview is not available for this file type",
        fileType = "Type", fileSize = "Size", modified = "Modified", permissions = "Permissions",
        folder = "Folder",
        manageKeys = "SSH Keys", sshKeys = "SSH Keys", addKey = "Add Key", generateKey = "Generate Key",
        keyName = "Key Name", keyType = "Key Type", keyBits = "Bits", passphrase = "Passphrase (optional)",
        copyPublicKey = "Copy Public Key", publicKeyCopied = "Public key copied to clipboard",
        deleteKeyConfirm = "Delete this SSH key?",
        scriptMarket = "Script Market",
        identities = "Identities & Access", authMethod = "Authentication Method",
        usePassword = "Use Password", useSshKey = "Use SSH Key",
        autoProvisionDesc = "Auto-provision on server",
        provisioningWarning = "Requires an active sudo session on this server",
        importKeyContent = "Import Key Content", generateNewKey = "Generate New Key",
        selectExistingKey = "Select Existing Key",
        provisionSuccess = "Identity provisioned successfully!"
    )

    val ru = Strings(
        appName = "SSH Commander", servers = "Серверы", addServer = "Добавить сервер",
        editServer = "Изменить сервер", deleteServer = "Удалить сервер", settings = "Настройки",
        back = "Назад", exit = "Выход",
        theme = "Тема", themeLight = "Светлая", themeDark = "Тёмная",
        themeSystem = "Системная",
        languageLabel = "Язык", langEn = "English", langRu = "Русский",
        consoleFont = "Шрифт консоли", fontDefault = "По умолчанию",
        fontMonospace = "Monospace", fontSansSerif = "Sans Serif", fontSerif = "Serif",
        consoleFontSize = "Размер шрифта", sizeSmall = "Маленький (12sp)",
        sizeMedium = "Средний (14sp)", sizeLarge = "Большой (18sp)",
        dataManagement = "Управление данными", exportData = "Экспорт JSON",
        importData = "Импорт JSON", exportSuccess = "Данные экспортированы",
        importSuccess = "Данные импортированы. Перезапустите.",
        serverName = "Название", hostIp = "Хост / IP", port = "Порт", username = "Логин",
        password = "Пароль", saveServer = "Сохранить сервер", save = "Сохранить",
        cancel = "Отмена", 
        pin = "Закрепить", unpin = "Открепить", pinnedServers = "Закрепленные",
        delete = "Удалить", edit = "Изменить",
        chooseAction = "Выберите действие", chooseIcon = "Выбрать иконку",
        reboot = "Перезагрузить", checkConnection = "Проверить соединение",
        loading = "Загрузка...", commands = "Команды",
        manageCommands = "Управление командами", runCommand = "Выполнить команду",
        run = "Запуск", confirmExecution = "Подтверждение выполнения",
        execute = "Выполнить", dangerous = "Опасно", newCommand = "Новая команда",
        requiresBio = "Требуется биометрия",
        confirmRebootMsg = "Вы уверены? Может потребоваться биометрия.",
        rebootConfirm = "Подтверждение",
        aboutApp = "О приложении", aboutVersion = "Версия: %1\$s",
        license = "Лицензия", disableAds = "Отключить рекламу",
        disableAdsDesc = "Отключение рекламы снижает поддержку разработки приложения.",
        disableAdsConfirmTitle = "Отключить рекламу?",
        disableAdsConfirmMsg = "Реклама помогает приложению оставаться бесплатным. Вы уверены, что хотите её отключить?",
        disableAdsConfirmFinalTitle = "Последнее предупреждение",
        disableAdsConfirmFinalMsg = "Вы точно-точно уверены? Подумайте еще раз, прежде чем подтвердить.",
        showInWidget = "Показывать в виджете",
        iconGaming = "Игровой", iconWeb = "Веб-сервер", iconDatabase = "База данных",
        iconCloud = "Облако", iconNas = "NAS", iconVpn = "VPN",
        iconDev = "Разработка", iconMedia = "Медиа-сервер", iconDefault = "По умолчанию",
        sftpExplorer = "Проводник SFTP", terminal = "Терминал",
        emptyDirectory = "Папка пуста", deleteFileConfirm = "Удалить этот файл?",
        exitSshSessionMsg = "Это закроет активную SSH-сессию. Продолжить?",
        confirmExit = "Подтвердите выход", historyUp = "История вверх",
        historyDown = "История вниз", tabKey = "TAB", ctrlCKey = "CTRL+C",
        clearKey = "ОЧИСТИТЬ", escKey = "ESC", enterKey = "ENTER",
        commandPlaceholder = "Команда...", executeConfirmMsg = "Выполнить: %1\$s?",
        cmdList = "Список", cmdTop = "Топ", cmdDisk = "Диск", cmdRam = "Память",
        cmdUptime = "Аптайм", cmdProcesses = "Процессы", cmdLogs = "Логи",
        searchFiles = "Поиск файлов...", closeSessionQ = "Закрыть сессию?", stay = "Остаться",
        upload = "Загрузить", sessionError = "Ошибка сессии", reconnect = "Переподключиться",
        dismiss = "Закрыть", exitSftpMsg = "Выход закроет активное SFTP-соединение.",
        hideHidden = "Скрыть скрытые файлы", showHidden = "Показать скрытые файлы",
        selectedCount = "Выбрано: %1\$d", newFolder = "Новая папка",
        folderNamePlaceholder = "Имя папки", create = "Создать", rename = "Переименовать",
        folders = "Папки", noFolder = "Без папки",
        deleteFolderConfirm = "Удалить папку \"%1\$s\"? Её серверы останутся.",
        errTimeout = "Время ожидания истекло. Проверьте сеть или статус сервера.",
        errAuthFailed = "Ошибка аутентификации. Проверьте имя пользователя, пароль или ключи.",
        errHostUnreachable = "Узел недоступен. Проверьте IP/хост и порт.",
        errHostKeyMismatch = "ВНИМАНИЕ: Идентификация узла изменилась! Возможна атака Man-in-the-Middle.",
        errUnknown = "Произошла непредвиденная ошибка.",
        errConnectionLost = "Соединение потеряно. Попробуйте еще раз.",
        sshErrorTemplate = "\n[Ошибка: %1\$s]\n",
        terminalStyle = "Стиль терминала", backgroundColor = "Цвет фона",
        textColor = "Цвет текста", cmdPlaceholderExample = "ls -la\ncd /var/www\n...",
        errorPrefix = "Ошибка: %1\$s", sftpErrorPrefix = "Ошибка SFTP: %1\$s",
        privacyMode = "Режим анонимности",
        privacyModeDesc = "Скрывать часть IP-адресов при отображении",
        quickCommands = "Быстрые команды",
        autoReconnect = "Автопереподключение",
        autoReconnectDesc = "Переподключаться автоматически при обрыве соединения",
        reconnectingMsg = "Соединение потеряно. Переподключение через %1\$d с…",
        biometricLock = "Биометрическая блокировка",
        biometricLockDesc = "Требовать отпечаток пальца для входа в приложение",
        appLocked = "Приложение заблокировано",
        appLockedSubtitle = "Разблокируйте отпечатком пальца, чтобы продолжить",
        unlock = "Разблокировать",
        biometricNotAvailable = "Биометрия недоступна на этом устройстве",
        retry = "Попробовать снова",
        yes = "Да", no = "Нет", next = "Далее", skip = "Пропустить", finish = "Готово",
        welcomeTitle = "Добро пожаловать в SSH Commander!",
        welcomeQuestion = "Хотите узнать, как пользоваться SSH Commander?",
        importJsonQuestion = "У вас есть резервная копия данных (файл JSON)? Импортировать её сейчас?",
        importJsonButton = "Импортировать",
        tourStep = "Шаг %1\$d из %2\$d",
        manageLogins = "Logins", addLogin = "Добавить логин",
        loginLabel = "Метка (напр. root, deploy)", sftpStartPath = "Стартовая папка SFTP",
        sftpStartPathHint = "Старт SFTP: %1\$s",
        sftpStartPathHint2 = "Папка, которая открывается в SFTP первой. Пусто = домашняя папка (или последняя посещённая).",
        noLogins = "Дополнительных логинов нет. Нажмите +, чтобы добавить.",
        setDefaultLogin = "Сделать логином по умолчанию", deleteLoginTitle = "Удалить логин?",
        deleteLoginMsg = "Удалить логин \"%1\$s\"?", selectLogin = "Выбрать логин",
        mainLoginLabel = "Основной логин (%1\$s)",
        addSession = "Новая сессия", closeSession = "Закрыть сессию", sessionTabs = "Сессии",
        copyText = "Копировать", selectAll = "Выбрать всё", clearSelection = "Снять выделение",
        preview = "Предпросмотр", download = "Скачать", copyPath = "Копировать путь",
        open = "Открыть", fileInfo = "Информация о файле",
        previewUnavailable = "Предпросмотр недоступен для этого типа файлов",
        fileType = "Тип", fileSize = "Размер", modified = "Изменён",
        permissions = "Права доступа", folder = "Папка",
        manageKeys = "SSH Ключи", sshKeys = "SSH Ключи", addKey = "Добавить ключ", generateKey = "Создать ключ",
        keyName = "Название ключа", keyType = "Тип ключа", keyBits = "Бит", passphrase = "Пароль (необязательно)",
        copyPublicKey = "Скопировать Pub Key", publicKeyCopied = "Публичный ключ скопирован",
        deleteKeyConfirm = "Удалить этот SSH ключ?",
        scriptMarket = "Маркет скриптов",
        identities = "Личности и Доступ", authMethod = "Способ входа",
        usePassword = "Использовать пароль", useSshKey = "Использовать SSH ключ",
        autoProvisionDesc = "Развернуть на сервере автоматически",
        provisioningWarning = "Требуется активная sudo-сессия",
        importKeyContent = "Вставить содержимое ключа", generateNewKey = "Создать новый ключ",
        selectExistingKey = "Выбрать из списка",
        provisionSuccess = "Личность успешно развернута на сервере!"
    )
}
