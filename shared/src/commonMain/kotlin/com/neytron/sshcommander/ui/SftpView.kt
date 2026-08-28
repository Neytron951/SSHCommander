package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
        ?: remember { mutableStateOf(false) })
    val error by (controller?.error?.collectAsState()
        ?: remember { mutableStateOf<String?>(null) })
    val currentPath by (controller?.currentPath?.collectAsState()
        ?: remember { mutableStateOf("/") })
    val files by (controller?.files?.collectAsState()
        ?: remember { mutableStateOf(emptyList<RemoteFile>()) })

    val scope = rememberCoroutineScope()
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    
    var pathDraft by remember { mutableStateOf("") }
    var pathEditing by remember { mutableStateOf(false) }
    LaunchedEffect(currentPath) {
        if (!pathEditing) pathDraft = currentPath
    }

    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var anchorPath by remember { mutableStateOf<String?>(null) }
    var previewFile by remember { mutableStateOf<RemoteFile?>(null) }
    var editingFile by remember { mutableStateOf<RemoteFile?>(null) }

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

    // Navigation and Action Handlers
    val uploadPicker = rememberUploadPicker { scope.launch { sftp.upload(it, currentPath) } }

    var pendingDownload by remember { mutableStateOf<RemoteFile?>(null) }
    val savePicker = rememberSavePicker { target ->
        target?.let { dest ->
            pendingDownload?.let { file ->
                scope.launch {
                    val ok = sftp.download(currentPath, file, dest)
                    status = if (ok) "Downloaded ${file.name}" else "Download failed"
                }
            }
        }
    }

    // MULTI-DOWNLOAD: For multiple selection
    val dirPicker = rememberDirectoryPicker { dir ->
        dir?.let { dest ->
            scope.launch {
                val targets = files.filter { it.path in selectedPaths && !it.isDirectory }
                var ok = 0
                targets.forEach { if (sftp.downloadToDir(currentPath, it, dest)) ok++ }
                status = "Downloaded $ok/${targets.size}"
                selectedPaths = emptySet()
            }
        }
    }

    fun joinRemote(parent: String, name: String): String =
        if (parent == "/") "/$name"
        else if (parent.endsWith("/")) "$parent$name"
        else "$parent/$name"

    fun handleSingleTap(file: RemoteFile, index: Int, ctrl: Boolean, shift: Boolean) {
        when {
            ctrl -> {
                selectedPaths = if (file.path in selectedPaths) selectedPaths - file.path else selectedPaths + file.path
                anchorPath = file.path
            }
            shift -> {
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
        pendingDownload = file
        savePicker(file.name)
    }

    fun downloadSelected() {
        val targets = files.filter { it.path in selectedPaths && !it.isDirectory }
        if (targets.isEmpty()) return
        dirPicker()
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

    Column(
        modifier = modifier.fillMaxSize()
            .platformDragAndDrop { filePaths ->
                scope.launch {
                    try {
                        status = "Uploading..."
                        var uploaded = 0
                        filePaths.forEach { path ->
                            if (sftp.upload(path, currentPath)) uploaded++
                        }
                        status = if (uploaded > 0) "Uploaded $uploaded files" else "Upload failed"
                        // Force refresh the list after DAD with a tiny safety delay
                        kotlinx.coroutines.delay(500)
                        sftp.listDirectory(currentPath)
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                }
            }
    ) {
        // Unified Toolbar
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
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                }
                IconButton(onClick = { scope.launch { sftp.listDirectory() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }

                // Breadcrumbs & Path Input
                Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    if (pathEditing) {
                        OutlinedTextField(
                            value = pathDraft,
                            onValueChange = { pathDraft = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { focused ->
                                pathEditing = focused.isFocused
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                val p = pathDraft.trim().ifEmpty { "/" }
                                scope.launch {
                                    status = null
                                    sftp.goTo(p)
                                }
                                pathEditing = false
                            })
                        )
                    } else {
                        Breadcrumbs(
                            path = currentPath,
                            onClick = { sftp.goTo(it) },
                            onManualEdit = { pathEditing = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (selectedPaths.isNotEmpty()) {
                    // Selection Actions (Replacing Global Actions)
                    Text(
                        text = "${selectedPaths.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { downloadSelected() }) {
                        Icon(Icons.Default.Download, contentDescription = AppStrings.download)
                    }
                    IconButton(onClick = { deleteSelected() }) {
                        Icon(Icons.Default.Delete, contentDescription = AppStrings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { selectedPaths = emptySet(); anchorPath = null }) {
                        Icon(Icons.Default.Close, contentDescription = AppStrings.clearSelection)
                    }
                } else {
                    // Global Actions
                    IconButton(onClick = { showNewFolder = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New folder")
                    }
                    IconButton(onClick = { 
                        uploadPicker()
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Upload")
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
                            onEdit = { editingFile = file },
                            onDownload = { downloadFile(file) },
                            onDelete = { deleteFile(file) },
                            onCopyPath = { copyPath(file) }
                        )
                    }
                }
            }
        }
    }

    if (editingFile != null) {
        Dialog(onDismissRequest = { editingFile = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(0.9f)
            ) {
                RemoteTextEditor(
                    remotePath = editingFile!!.path,
                    controller = sftp,
                    onClose = { 
                        editingFile = null
                        scope.launch { sftp.listDirectory() }
                    }
                )
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
private fun Breadcrumbs(
    path: String,
    onClick: (String) -> Unit,
    onManualEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = remember(path) {
        val list = mutableListOf<Pair<String, String>>()
        list.add("/" to "/")
        var current = ""
        path.split('/').filter { it.isNotEmpty() }.forEach {
            current += "/$it"
            list.add(it to current)
        }
        list
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .clickable { onManualEdit() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, (name, fullPath) ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = if (index == segments.size - 1) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClick(fullPath) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            if (index < segments.size - 1 && name != "/") {
                Text("/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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

@Composable
private fun SftpRow(
    file: RemoteFile,
    isSelected: Boolean,
    onSingleTap: (ctrl: Boolean, shift: Boolean) -> Unit,
    onDoubleTap: () -> Unit,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onCopyPath: () -> Unit
) {
    var menuAnchor by remember { mutableStateOf<Offset?>(null) }

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
                    tint = if (file.isDirectory) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                text = { Text(AppStrings.editText) },
                                onClick = { menuAnchor = null; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
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

// ---- platform hooks for file dialogs ----

internal expect fun uploadFile(onFileSelected: (String) -> Unit)
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
