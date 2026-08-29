package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.ExperimentalComposeUiApi
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

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
actual fun getSystemFontFamily(name: String): androidx.compose.ui.text.font.FontFamily {
    return androidx.compose.runtime.remember(name) {
        when (name) {
            "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
            else -> try {
                androidx.compose.ui.text.font.FontFamily(name)
            } catch (e: Exception) {
                androidx.compose.ui.text.font.FontFamily.Monospace
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
