package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.sftp.SftpController
import kotlinx.coroutines.launch

/**
 * Desktop-oriented SFTP file browser. Shows the remote directory listing with
 * toolbar actions: navigate up, refresh, create folder, upload, download,
 * delete. Double-click a folder to enter it.
 */
@Composable
fun SftpView(controller: SftpController?, modifier: Modifier = Modifier) {
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
    // Editable path: keep a draft that syncs with currentPath when it changes
    // externally (navigation buttons), so typing is not overwritten.
    var pathDraft by remember { mutableStateOf("") }
    var pathEditing by remember { mutableStateOf(false) }
    LaunchedEffect(currentPath) {
        if (!pathEditing) pathDraft = currentPath
    }

    // Selection model (path-based so it survives listing refreshes).
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var anchorPath by remember { mutableStateOf<String?>(null) }
    var previewFile by remember { mutableStateOf<RemoteFile?>(null) }

    // Navigating to another folder clears the selection.
    LaunchedEffect(currentPath) {
        selectedPaths = emptySet()
        anchorPath = null
    }

    val clipboard = LocalClipboardManager.current
    val sftp = controller

    if (sftp == null) {
        SftpPlaceholder()
        return
    }

    fun joinRemote(parent: String, name: String): String =
        if (parent == "/") "/$name"
        else if (parent.endsWith("/")) "$parent$name"
        else "$parent/$name"

    fun handleSingleTap(file: RemoteFile, index: Int, ctrl: Boolean, shift: Boolean) {
        when {
            ctrl -> {
                // Ctrl+click — toggle this file in/out of the selection.
                selectedPaths = if (file.path in selectedPaths) selectedPaths - file.path else selectedPaths + file.path
                anchorPath = file.path
            }
            shift -> {
                // Shift+click — select the whole range from the anchor file.
                val anchorIndex = files.indexOfFirst { it.path == anchorPath }
                if (anchorIndex >= 0 && anchorIndex != index) {
                    val from = minOf(anchorIndex, index)
                    val to = maxOf(anchorIndex, index)
                    selectedPaths = files.subList(from, to + 1).map { it.path }.toSet()
                } else {
                    selectedPaths = setOf(file.path)
                    anchorPath = file.path
                }
            }
            else -> {
                selectedPaths = setOf(file.path)
                anchorPath = file.path
            }
        }
    }

    fun handleDoubleTap(file: RemoteFile) {
        if (file.isDirectory) sftp.goTo(file.path)
        else previewFile = file
    }

    fun downloadFile(file: RemoteFile) {
        downloadFolder { dir ->
            if (dir != null) {
                scope.launch {
                    status = null
                    val ok = sftp.download(currentPath, file, dir)
                    status = if (ok) "Downloaded to $dir/${file.name}" else "Download failed"
                }
            }
        }
    }

    fun downloadSelected() {
        val targets = files.filter { it.path in selectedPaths && !it.isDirectory }
        if (targets.isEmpty()) return
        downloadFolder { dir ->
            if (dir != null) {
                scope.launch {
                    var ok = 0
                    targets.forEach { if (sftp.download(currentPath, it, dir)) ok++ }
                    status = if (ok == targets.size) "Downloaded $ok files" else "Downloaded $ok/${targets.size}"
                    if (ok == targets.size) {
                        selectedPaths = emptySet()
                        anchorPath = null
                    }
                }
            }
        }
    }

    fun deleteFile(file: RemoteFile) {
        scope.launch {
            status = null
            val ok = sftp.delete(currentPath, file)
            status = if (ok) "Deleted ${file.name}" else "Delete failed"
            if (ok) {
                selectedPaths = selectedPaths - file.path
                if (anchorPath == file.path) anchorPath = null
            }
        }
    }

    fun deleteSelected() {
        scope.launch {
            val targets = files.filter { it.path in selectedPaths }
            var ok = 0
            targets.forEach { if (sftp.delete(currentPath, it)) ok++ }
            status = "Deleted $ok"
            selectedPaths = emptySet()
            anchorPath = null
        }
    }

    fun copyPath(file: RemoteFile) {
        clipboard.setText(AnnotatedString(joinRemote(currentPath, file.name)))
        status = "Path copied"
    }

    Column(modifier.fillMaxSize()) {
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
            // Editable path — type a path and press Enter to navigate.
            OutlinedTextField(
                value = pathDraft,
                onValueChange = { pathDraft = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                placeholder = { Text("/") },
                modifier = Modifier.weight(1f).onFocusChanged { focused ->
                    pathEditing = focused.isFocused
                    if (!focused.isFocused && pathDraft != currentPath) {
                        pathDraft = currentPath
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val p = pathDraft.trim().ifEmpty { "/" }
                    scope.launch {
                        status = null
                        sftp.goTo(p)
                        pathDraft = p
                    }
                    pathEditing = false
                })
            )
            IconButton(onClick = { showNewFolder = true }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
            }
            IconButton(onClick = { uploadFile { path -> scope.launch { if (sftp.upload(path, currentPath)) status = "Uploaded" else status = "Upload failed" } } }) {
                Icon(Icons.Default.FileUpload, contentDescription = "Upload")
            }
        }
        }

        // Selection toolbar (shown while files are selected)
        if (selectedPaths.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = String.format(AppStrings.selectedCount, selectedPaths.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { downloadSelected() }) {
                        Icon(Icons.Default.Download, contentDescription = AppStrings.download)
                    }
                    IconButton(onClick = { deleteSelected() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = AppStrings.delete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(onClick = {
                        selectedPaths = emptySet()
                        anchorPath = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.clearSelection)
                    }
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
                    itemsIndexed(files, key = { _, f -> f.path }) { index, file ->
                        SftpRow(
                            file = file,
                            isSelected = file.path in selectedPaths,
                            onSingleTap = { ctrl, shift -> handleSingleTap(file, index, ctrl, shift) },
                            onDoubleTap = { handleDoubleTap(file) },
                            onOpen = { if (file.isDirectory) sftp.goTo(file.path) },
                            onPreview = { previewFile = file },
                            onDownload = { downloadFile(file) },
                            onDelete = { deleteFile(file) },
                            onCopyPath = { copyPath(file) }
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

    previewFile?.let { file ->
        FilePreviewDialog(
            readRemoteFile = { path, maxBytes -> sftp.readRemoteFile(path, maxBytes) },
            file = file,
            onDismiss = { previewFile = null }
        )
    }
}

@Composable
private fun SftpRow(
    file: RemoteFile,
    isSelected: Boolean,
    onSingleTap: (ctrl: Boolean, shift: Boolean) -> Unit,
    onDoubleTap: () -> Unit,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCopyPath: () -> Unit
) {
    var menuAnchor by remember { mutableStateOf<Offset?>(null) }

    // Keep the latest callbacks without restarting the gesture coroutine.
    val singleTapState = rememberUpdatedState(onSingleTap)
    val doubleTapState = rememberUpdatedState(onDoubleTap)
    val longPressState = rememberUpdatedState<(Offset) -> Unit>({ menuAnchor = it })
    val secondaryState = rememberUpdatedState<(Offset) -> Unit>({ menuAnchor = it })

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .sftpRowGestures(
                    onSingleTap = singleTapState,
                    onDoubleTap = doubleTapState,
                    onLongPress = longPressState,
                    onSecondary = secondaryState
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
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
            }

            // Context menu anchored at the click position (right-click / long-press).
            menuAnchor?.let { anchor ->
                Box(
                    modifier = Modifier.offset {
                        IntOffset(anchor.x.toInt(), anchor.y.toInt())
                    }
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { menuAnchor = null }
                    ) {
                        if (file.isDirectory) {
                            DropdownMenuItem(
                                text = { Text(AppStrings.open) },
                                onClick = { menuAnchor = null; onOpen() },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(AppStrings.preview) },
                                onClick = { menuAnchor = null; onPreview() },
                                leadingIcon = { Icon(Icons.Default.Preview, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(AppStrings.download) },
                                onClick = { menuAnchor = null; onDownload() },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(AppStrings.copyPath) },
                            onClick = { menuAnchor = null; onCopyPath() },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.delete) },
                            onClick = { menuAnchor = null; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

private const val SFTP_DOUBLE_TAP_MS = 350L
private const val SFTP_LONG_PRESS_MS = 500L

/**
 * Row-level pointer handling:
 *  - single click (primary): selection; Ctrl = toggle, Shift = range
 *  - double click: open (navigate folders / preview files)
 *  - long press / right click: context menu at the cursor
 * The callbacks are passed as [State] so the gesture coroutine always invokes
 * the latest lambdas without needing to restart.
 */
private fun Modifier.sftpRowGestures(
    onSingleTap: State<(ctrl: Boolean, shift: Boolean) -> Unit>,
    onDoubleTap: State<() -> Unit>,
    onLongPress: State<(Offset) -> Unit>,
    onSecondary: State<(Offset) -> Unit>
): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        var downTime = 0L
        var downPos = Offset.Zero
        var downWasPrimary = false
        var lastTapTime = 0L
        var lastTapPos = Offset(-Float.MAX_VALUE, -Float.MAX_VALUE)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull() ?: continue
            when (event.type) {
                PointerEventType.Press -> {
                    if (event.buttons.isSecondaryPressed) {
                        onSecondary.value(change.position)
                        change.consume()
                    } else if (event.buttons.isPrimaryPressed) {
                        downTime = System.currentTimeMillis()
                        downPos = change.position
                        downWasPrimary = true
                    }
                }
                PointerEventType.Release -> {
                    if (downWasPrimary && downTime != 0L) {
                        val duration = System.currentTimeMillis() - downTime
                        downTime = 0L
                        downWasPrimary = false
                        // Ignore drags (e.g. touch scrolling over a row).
                        if ((change.position - downPos).getDistance() > 12.dp.toPx()) continue
                        if (duration >= SFTP_LONG_PRESS_MS) {
                            onLongPress.value(change.position)
                        } else {
                            val now = System.currentTimeMillis()
                            val dx = change.position.x - lastTapPos.x
                            val dy = change.position.y - lastTapPos.y
                            if (now - lastTapTime <= SFTP_DOUBLE_TAP_MS &&
                                dx * dx + dy * dy <= 2500f
                            ) {
                                lastTapTime = 0L
                                lastTapPos = Offset(-Float.MAX_VALUE, -Float.MAX_VALUE)
                                onDoubleTap.value()
                            } else {
                                lastTapTime = now
                                lastTapPos = change.position
                                onSingleTap.value(
                                    event.keyboardModifiers.isCtrlPressed,
                                    event.keyboardModifiers.isShiftPressed
                                )
                            }
                        }
                    }
                }
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

/**
 * Opens a native folder picker and invokes [onFolderSelected] with the chosen
 * directory path (the destination for SFTP downloads). No-op on cancel.
 * Desktop implementation uses a Swing JFileChooser; Android will use SAF.
 */
internal expect fun downloadFolder(onFolderSelected: (String) -> Unit)

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
