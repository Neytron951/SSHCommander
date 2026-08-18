package com.neytron.sshcommander.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.RemoteFile

/** What kind of preview can be shown for a file, based on its extension. */
internal enum class PreviewKind { Text, Image, Info, Blocked }

/**
 * Classifies a remote file name into a preview category:
 *  - [PreviewKind.Text]    → readable as UTF-8 text
 *  - [PreviewKind.Image]   → decodable image
 *  - [PreviewKind.Blocked] → binary/archive/executable (no preview)
 *  - [PreviewKind.Info]    → everything else (metadata only)
 */
internal fun previewKindOf(name: String): PreviewKind {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "txt", "json", "log", "md", "markdown", "yml", "yaml", "xml", "csv", "conf",
        "ini", "properties", "env", "sh", "bash", "zsh", "py", "js", "ts", "tsx", "jsx",
        "java", "kt", "kts", "c", "cpp", "h", "hpp", "css", "html", "htm", "sql",
        "toml", "gradle", "rb", "go", "php", "pl", "r", "bat", "cmd", "ps1", "vim",
        "gitignore", "dockerfile", "svg" -> PreviewKind.Text

        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> PreviewKind.Image

        "exe", "msi", "msix", "rar", "zip", "7z", "gz", "tgz", "tar", "bz2", "xz",
        "apk", "aab", "jar", "iso", "dll", "so", "dylib", "bin", "deb", "rpm", "dmg",
        "cab", "class", "o", "a", "lib", "whl", "war" -> PreviewKind.Blocked

        else -> PreviewKind.Info
    }
}

/**
 * Decodes raw image bytes into a composable bitmap, or null when the bytes are
 * not a decodable image. Platform-specific (Skia on desktop, BitmapFactory on
 * Android) — see the actual implementations.
 */
internal expect fun decodePreviewImage(bytes: ByteArray): ImageBitmap?

/**
 * Basic in-memory preview dialog for SFTP files — no download to disk.
 *  - text-like files: content shown in a monospace scrollable pane
 *  - images: decoded and rendered scaled to fit
 *  - media/binaries we cannot render: file metadata (type, size, date…)
 *  - blocked types (EXE/RAR/ZIP/MSI…): message + metadata
 */
@Composable
internal fun FilePreviewDialog(
    readRemoteFile: suspend (path: String, maxBytes: Int) -> ByteArray?,
    file: RemoteFile,
    onDismiss: () -> Unit
) {
    val kind = remember(file.name) { previewKindOf(file.name) }
    val fullPath = file.path
    var loading by remember(file.path) { mutableStateOf(true) }
    var readError by remember(file.path) { mutableStateOf(false) }
    var textContent by remember(file.path) { mutableStateOf<String?>(null) }
    var imageBytes by remember(file.path) { mutableStateOf<ByteArray?>(null) }
    val image = imageBytes?.let { remember(it) { decodePreviewImage(it) } }

    LaunchedEffect(file.path) {
        loading = true
        readError = false
        textContent = null
        imageBytes = null
        when (kind) {
            PreviewKind.Text -> {
                // Cap text reads so huge logs don't blow up memory.
                val bytes = readRemoteFile(fullPath, 512 * 1024)
                if (bytes == null) readError = true else textContent = bytes.toString(Charsets.UTF_8)
            }
            PreviewKind.Image -> {
                val bytes = readRemoteFile(fullPath, 20 * 1024 * 1024)
                if (bytes == null) readError = true else imageBytes = bytes
            }
            else -> Unit
        }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            when {
                loading -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                kind == PreviewKind.Text -> {
                    if (textContent != null) {
                        Box(
                            Modifier
                                .widthIn(min = 280.dp, max = 520.dp)
                                .heightIn(max = 380.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = textContent!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    } else {
                        Text(if (readError) AppStrings.sftpErrorPrefix else AppStrings.previewUnavailable)
                    }
                }

                kind == PreviewKind.Image -> {
                    if (image != null) {
                        Box(
                            Modifier
                                .widthIn(max = 520.dp)
                                .heightIn(max = 380.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = image,
                                contentDescription = file.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text(if (readError) AppStrings.sftpErrorPrefix else AppStrings.previewUnavailable)
                    }
                }

                else -> Column {
                    if (kind == PreviewKind.Blocked) {
                        Text(
                            text = AppStrings.previewUnavailable,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    InfoRow(AppStrings.fileType, fileTypeLabel(file, kind))
                    InfoRow(AppStrings.fileSize, formatSize(file.size))
                    InfoRow(AppStrings.modified, formatDate(file.modifiedTime))
                    InfoRow(AppStrings.permissions, file.permissions)
                    InfoRow("PATH", fullPath)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.dismiss) }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun fileTypeLabel(file: RemoteFile, kind: PreviewKind): String {
    if (file.isDirectory) return AppStrings.folder
    val ext = file.name.substringAfterLast('.', "").uppercase()
    return when {
        ext.isNotBlank() -> "$ext file"
        kind == PreviewKind.Text -> "Text file"
        kind == PreviewKind.Image -> "Image"
        else -> "File"
    }
}
