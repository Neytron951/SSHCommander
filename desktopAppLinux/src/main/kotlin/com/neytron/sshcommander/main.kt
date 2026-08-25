package com.neytron.sshcommander

import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.DesktopSettings
import com.neytron.sshcommander.data.FileSecureStorage
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.sftp.SftpSession
import com.neytron.sshcommander.terminal.TerminalSession
import java.io.File
import javax.imageio.ImageIO

fun main() = application {
    // Persist servers in the user's home directory so they survive restarts.
    val dataDir = File(System.getProperty("user.home"), ".sshcommander")
    val serverRepository: ServerRepository = JsonServerRepository(
        dataDir = dataDir,
        secureStorage = FileSecureStorage(File(dataDir, "secrets.json"))
    )
    val settings: AppSettings = DesktopSettings(dataDir)

    // App icon for the window title bar / taskbar.
    val windowIcon: Painter? = runCatching {
        javaClass.getResourceAsStream("/icon.png")?.use { ImageIO.read(it) }
    }.getOrNull()?.let { BitmapPainter(it.toComposeImageBitmap()) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "SSH Commander",
        icon = windowIcon,
        state = rememberWindowState(width = 1000.dp, height = 700.dp)
    ) {
        App(
            terminalSessionFactory = { server, profile ->
                TerminalSession(server, profile, hostKeyStore = null)
            },
            sftpSessionFactory = { server, profile ->
                SftpSession(server, profile, hostKeyStore = null)
            },
            serverRepository = serverRepository,
            settings = settings,
            appVersion = "1.6",
            backupManager = ExportImportManager(serverRepository)
        )
    }
}
