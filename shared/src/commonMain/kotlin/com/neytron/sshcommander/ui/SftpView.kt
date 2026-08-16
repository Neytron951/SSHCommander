package com.neytron.sshcommander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.sftp.SftpController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Desktop-oriented SFTP file browser. Shows the remote directory listing with
 * toolbar actions: navigate up, refresh, create folder, upload, download,
 * delete. Double-click a folder to enter it.
 */
@Composable
fun SftpView(controller: SftpController?) {
    val isLoading by (controller?.isLoading?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(false) })
    val error by (controller?.error?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf<String?>(null) })
    val currentPath by (controller?.currentPath?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf("/") })
    val files by (controller?.files?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList<RemoteFile>()) })

    val scope = rememberCoroutineScope()
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val sftp = controller

    if (sftp == null) {
        SftpPlaceholder()
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Toolbar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
            IconButton(onClick = { sftp.goUp() }, enabled = currentPath != "/") {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Up")
            }
            IconButton(onClick = { sftp.listDirectory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            IconButton(onClick = { showNewFolder = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
            }
            IconButton(onClick = { uploadFile { path -> scope.launch { if (sftp.upload(path, currentPath)) status = "Uploaded" else status = "Upload failed" } } }) {
                Icon(Icons.Default.FileUpload, contentDescription = "Upload")
            }
        }
        }

        // Status / error line
        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        if (status != null) {
            Text(
                text = status ?: "",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // File list
        Box(Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Name", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Size", Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Modified", Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(files, key = { it.path }) { file ->
                        SftpRow(
                            file = file,
                            onOpen = {
                                if (file.isDirectory) sftp.goTo(file.path)
                                else scope.launch {
                                    status = null
                                    val ok = sftp.download(currentPath, file, System.getProperty("user.home", "."))
                                    status = if (ok) "Downloaded to ~/${file.name}" else "Download failed"
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    val ok = sftp.delete(currentPath, file)
                                    status = if (ok) "Deleted ${file.name}" else "Delete failed"
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewFolder) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        scope.launch {
                            val ok = sftp.makeDirectory(currentPath, newFolderName.trim())
                            status = if (ok) "Created $newFolderName" else "Failed to create folder"
                        }
                    }
                    showNewFolder = false
                    newFolderName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showNewFolder = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SftpRow(file: RemoteFile, onOpen: () -> Unit, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (file.isDirectory) "—" else formatSize(file.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = formatDate(file.modifiedTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        }
    }
}

@Composable
private fun SftpPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "SFTP — подключите сервер, чтобы просматривать файлы",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---- platform hooks for file dialogs ----

/**
 * Opens a native file picker and invokes [onFileSelected] with the chosen path.
 * Desktop implementation uses a Swing JFileChooser; Android will use SAF.
 */
internal expect fun uploadFile(onFileSelected: (String) -> Unit)

internal fun formatSize(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

internal fun formatDate(millis: Long): String {
    return try {
        val date = java.util.Date(millis)
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        fmt.format(date)
    } catch (e: Exception) {
        ""
    }
}
