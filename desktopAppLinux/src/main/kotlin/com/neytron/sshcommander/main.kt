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
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.sftp.SftpSession
import com.neytron.sshcommander.terminal.TerminalSession
import com.neytron.sshcommander.ui.AppDeps
import com.neytron.sshcommander.ui.LocalAppDeps
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.runtime.CompositionLocalProvider

fun main() = application {
    // Persist servers in the user's home directory so they survive restarts.
    val dataDir = File(System.getProperty("user.home"), ".sshcommander")
    val secureStorage = FileSecureStorage(File(dataDir, "secrets.json"))
    val serverRepository: ServerRepository = JsonServerRepository(
        dataDir = dataDir,
        secureStorage = secureStorage
    )
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

        SSHCommanderTheme(darkTheme = darkTheme) {
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
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(Modifier.fillMaxSize()) {
                        WindowTitleBar(
                            state = windowState,
                            onClose = ::exitApplication
                        )
                        App(
                            terminalSessionFactory = { server, profile, s ->
                                TerminalSession(server, profile, s, hostKeyStore = null)
                            },
                            sftpSessionFactory = { server, profile ->
                                SftpSession(server, profile, hostKeyStore = null)
                            },
                            serverRepository = serverRepository,
                            settings = settings,
                            appVersion = "1.9.2",
                            backupManager = ExportImportManager(serverRepository)
                        )
                    }
                }
            }
        }
    }
}
