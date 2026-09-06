package com.neytron.sshcommander

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.DesktopSettings
import com.neytron.sshcommander.data.DpapiSecureStorage
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.sftp.SftpSession
import com.neytron.sshcommander.terminal.TerminalSession
import com.neytron.sshcommander.ui.AppDeps
import com.neytron.sshcommander.ui.LocalAppDeps
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.runtime.CompositionLocalProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun main() = application {
    val dataDir = File(System.getProperty("user.home"), ".sshcommander")
    val serverRepository: ServerRepository = JsonServerRepository(
        dataDir = dataDir,
        secureStorage = DpapiSecureStorage(File(dataDir, "secrets.dpapi"))
    )
    val settings: AppSettings = DesktopSettings(dataDir)

    val windowIcon: Painter? = runCatching {
        javaClass.getResourceAsStream("/icon.png")?.use { ImageIO.read(it) }
    }.getOrNull()?.let { BitmapPainter(it.toComposeImageBitmap()) }

    var isWindowVisible by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(width = 1024.dp, height = 740.dp)

    if (windowIcon != null) {
        Tray(
            icon = windowIcon,
            tooltip = "SSH Commander",
            onAction = { isWindowVisible = true },
            menu = {
                Item("Open", onClick = { isWindowVisible = true })
                Item("Exit", onClick = ::exitApplication)
            }
        )
    }

    Window(
        onCloseRequest = {
            if (windowIcon != null) {
                isWindowVisible = false
            } else {
                exitApplication()
            }
        },
        title = "SSH Commander",
        icon = windowIcon,
        state = windowState,
        visible = isWindowVisible,
        undecorated = true,
        transparent = true // Делаем окно прозрачным для поддержки скругления
    ) {
        val themeMode by settings.themeMode.collectAsState(initial = "system")
        val darkTheme = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }

        SSHCommanderTheme(darkTheme = darkTheme) {
            val secureStorage = DpapiSecureStorage(File(dataDir, "secrets.dpapi"))
            CompositionLocalProvider(
                LocalAppDeps provides AppDeps(
                    repository = serverRepository,
                    settings = settings,
                    biometric = null, // Desktop currently has no biometric implementation
                    authManager = com.neytron.sshcommander.sync.createAuthManager(secureStorage = secureStorage),
                    httpClient = io.ktor.client.HttpClient {
                        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                            json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true })
                        }
                    }
                )
            ) {
                // Surface со скруглением краев
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp // Добавляем небольшую тень, так как системная пропадает
                ) {
                    Column(Modifier.fillMaxSize()) {
                        WindowTitleBar(
                            state = windowState,
                            onClose = {
                                if (windowIcon != null) isWindowVisible = false else exitApplication()
                            }
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
