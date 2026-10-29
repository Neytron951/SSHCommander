package com.neytron.sshcommander

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.DesktopSettings
import com.neytron.sshcommander.data.FileSecureStorage
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.data.Protocol
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.sftp.SftpSession
import com.neytron.sshcommander.terminal.TerminalSession
import com.neytron.sshcommander.terminal.AdbSession
import com.neytron.sshcommander.ui.AppDeps
import com.neytron.sshcommander.ui.LocalAppDeps
import com.neytron.sshcommander.ui.theme.ModernAndroidEra
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import com.neytron.sshcommander.ui.theme.Windows9xEra
import com.neytron.sshcommander.ui.theme.Windows10Era
import com.neytron.sshcommander.ui.theme.Windows10DarkEra
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.runtime.CompositionLocalProvider
import com.neytron.sshcommander.data.HeadlessSshExecutor
import com.neytron.sshcommander.terminal.AdbPlatform
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    // Persist servers in the user's home directory so they survive restarts.
    val dataDir = File(System.getProperty("user.home"), ".sshcommander")
    AdbPlatform.init(dataDir)
    val secureStorage = FileSecureStorage(File(dataDir, "secrets.json"))
    val serverRepository: ServerRepository = JsonServerRepository(
        dataDir = dataDir,
        secureStorage = secureStorage
    )

    if (args.isNotEmpty()) {
        runBlocking {
            handleCliArgs(args, serverRepository)
        }
        return
    }

    application {
        val settings: AppSettings = DesktopSettings(dataDir)

    // App icon for the window title bar / taskbar.
    val windowIcon: Painter? = runCatching {
        javaClass.getResourceAsStream("/icon.png")?.use { ImageIO.read(it) }
    }.getOrNull()?.let { BitmapPainter(it.toComposeImageBitmap()) }

    val windowState = rememberWindowState(
        position = androidx.compose.ui.window.WindowPosition.Absolute(100.dp, 100.dp),
        width = 1024.dp,
        height = 740.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "SSH Commander",
        icon = windowIcon,
        state = windowState,
        undecorated = true,
        transparent = false,
        onKeyEvent = {
            if (it.type == KeyEventType.KeyDown && it.key == Key.F11) {
                windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen)
                    WindowPlacement.Floating else WindowPlacement.Fullscreen
                true
            } else false
        }
    ) {
        val themeMode by settings.themeMode.collectAsState(initial = "system")
        val darkTheme = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }

        val uiStyleName by settings.uiStyle.collectAsState(initial = "modern")
        val era = when (uiStyleName) {
            "win9x" -> Windows9xEra
            "win10" -> Windows10Era
            "win10dark" -> Windows10DarkEra
            else -> ModernAndroidEra
        }

        SSHCommanderTheme(darkTheme = darkTheme, era = era) {
            CompositionLocalProvider(
                LocalAppDeps provides AppDeps(
                    repository = serverRepository,
                    settings = settings,
                    biometric = null,
                    authManager = com.neytron.sshcommander.sync.createAuthManager(null, secureStorage),
                    httpClient = HttpClient {
                        install(ContentNegotiation) {
                            json(Json { ignoreUnknownKeys = true })
                        }
                    }
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = if (era is Windows9xEra || era is Windows10Era || era is Windows10DarkEra) RoundedCornerShape(0.dp) else RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        WindowTitleBar(
                            state = windowState,
                            onClose = ::exitApplication
                        )
                        App(
                            terminalSessionFactory = { server, profile, s ->
                                if (server.protocol == Protocol.ADB) {
                                    AdbSession(server)
                                } else {
                                    TerminalSession(server, profile, s, hostKeyStore = null)
                                }
                            },
                            sftpSessionFactory = { server, profile ->
                                SftpSession(server, profile, hostKeyStore = null)
                            },
                            serverRepository = serverRepository,
                            settings = settings,
                            appVersion = "2.2.0",
                            backupManager = ExportImportManager(serverRepository)
                        )
                    }
                }
            }
        }
    }
}
}

suspend fun handleCliArgs(args: Array<String>, serverRepository: ServerRepository): Boolean {
    if (args.isEmpty()) return false
    val executor = HeadlessSshExecutor(serverRepository)
    when (args[0]) {
        "--help", "-h", "help" -> {
            println("SSH Commander CLI")
            println("Usage:")
            println("  ssh-commander --list                                List all configured servers")
            println("  ssh-commander --exec <server> \"<command>\"           Execute SSH command (server can be ID, name, IP, or part of name)")
            println("                                                      Note: Wrap compound commands in quotes (e.g. \"cd dir && ls\")")
            println("  ssh-commander --list-commands                       List saved quick commands")
            println("  ssh-commander --run-command <cmd> <server>          Run a saved quick command on server")
            println("  ssh-commander --add-command \"<name>\" \"<cmd>\"        Save a new quick command")
            println("  ssh-commander --delete-command <id>                 Delete a quick command by ID")
            return true
        }
        "--list", "list" -> {
            val servers = executor.listServers()
            if (servers.isEmpty()) {
                println("No servers configured.")
            } else {
                println("Configured Servers:")
                servers.forEach { s ->
                    println("  [ID: ${s.id}] ${s.name} (${s.host}:${s.port}) - User: ${s.username}")
                }
            }
            return true
        }
        "--exec", "exec" -> {
            if (args.size < 3) {
                println("Error: Missing arguments for exec.")
                println("Usage: ssh-commander --exec <server> \"<command>\"")
                return true
            }
            val serverIdOrName = args[1]
            val command = args.drop(2).joinToString(" ")
            try {
                val result = executor.executeCommand(serverIdOrName, command)
                print(result)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
            return true
        }
        "--list-commands", "list-commands" -> {
            val cmds = executor.listCommands()
            if (cmds.isEmpty()) {
                println("No quick commands saved.")
            } else {
                println("Quick Commands:")
                cmds.forEach { c ->
                    println("  [ID: ${c.id}] ${c.name}: ${c.command}")
                }
            }
            return true
        }
        "--run-command", "run-command" -> {
            if (args.size < 3) {
                println("Error: Missing arguments.")
                println("Usage: ssh-commander --run-command <command_name_or_id> <server> [var=val ...]")
                return true
            }
            val cmdNameOrId = args[1]
            val serverIdOrName = args[2]
            val extraArgs = args.drop(3)
            try {
                val result = executor.runQuickCommand(cmdNameOrId, serverIdOrName, extraArgs)
                print(result)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
            return true
        }
        "--add-command", "add-command" -> {
            if (args.size < 3) {
                println("Error: Missing arguments.")
                println("Usage: ssh-commander --add-command \"<name>\" \"<command>\"")
                return true
            }
            val name = args[1]
            val command = args.drop(2).joinToString(" ")
            try {
                executor.addCommand(name, command)
                println("Quick command '$name' added successfully.")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
            return true
        }
        "--delete-command", "delete-command" -> {
            if (args.size < 2) {
                println("Error: Missing command ID.")
                println("Usage: ssh-commander --delete-command <id>")
                return true
            }
            val id = args[1].toIntOrNull()
            if (id == null) {
                println("Error: Invalid command ID.")
                return true
            }
            try {
                executor.deleteCommand(id)
                println("Quick command ID $id deleted.")
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
            return true
        }
    }
    return false
}
