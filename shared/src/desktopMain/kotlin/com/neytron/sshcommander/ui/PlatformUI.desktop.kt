package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import org.jetbrains.skia.Image
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import java.awt.GraphicsEnvironment
import java.awt.datatransfer.DataFlavor
import java.io.File

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop: no system back button.
}

@Composable
actual fun PlatformAdBanner(blockId: String) {
    // Desktop: no ads.
}

@Composable
actual fun isLandscapeLayout(): Boolean = true

actual fun platformToast(message: String) {
    println("[SSH Commander] $message")
}

actual fun platformOpenUrl(url: String) {
    try {
        val os = System.getProperty("os.name").lowercase()
        val rt = Runtime.getRuntime()
        when {
            os.contains("win") -> rt.exec("rundll32 url.dll,FileProtocolHandler $url")
            os.contains("mac") -> rt.exec("open $url")
            else -> rt.exec("xdg-open $url")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
actual fun getSystemFontFamily(name: String): FontFamily {
    return remember(name) {
        when (name) {
            "Monospace" -> FontFamily.Monospace
            else -> try {
                androidx.compose.ui.text.font.FontFamily(name)
            } catch (e: Exception) {
                FontFamily.Monospace
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun Modifier.platformDragAndDrop(onFilesDropped: (List<String>) -> Unit): Modifier = this.dragAndDropTarget(
    shouldStartDragAndDrop = { true },
    target = object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            return try {
                val transferable = event.awtTransferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val data = transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    if (data is List<*>) {
                        val paths = data.filterIsInstance<File>().map { it.absolutePath }
                        if (paths.isNotEmpty()) {
                            onFilesDropped(paths)
                            return true
                        }
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }
    }
)

actual fun getAvailableSystemFonts(): List<String> {
    return try {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .availableFontFamilyNames
            .toList()
            .sorted()
    } catch (e: Exception) {
        listOf("Monospace", "Serif", "SansSerif")
    }
}

@Composable
actual fun mascotPainter(): Painter? {
    val painter = remember {
        runCatching {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("webby.png")
                ?: object {}.javaClass.getResourceAsStream("/webby.png")
                ?: Thread.currentThread().contextClassLoader.getResourceAsStream("shared/src/desktopMain/resources/webby.png")
            stream?.use { input ->
                val bytes = input.readBytes()
                BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
            }
        }.getOrNull()
    }
    return painter
}
