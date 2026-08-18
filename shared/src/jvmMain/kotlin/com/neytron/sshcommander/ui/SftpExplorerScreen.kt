package com.neytron.sshcommander.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.RemoteFile
import com.neytron.sshcommander.data.ServerLogin
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SftpExplorerScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    onManageLogins: () -> Unit
) {
    val deps = LocalAppDeps.current
    val viewModel: SftpViewModel = viewModel { SftpViewModel(deps.repository) }
    val files = viewModel.remoteFiles
    val currentPath = viewModel.currentPath
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val selectedFiles = viewModel.selectedFiles
    val showHidden = viewModel.showHiddenFiles
    val transferProgress = viewModel.transferProgress
    val isTransferring = viewModel.isTransferring

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isTransitioning by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<RemoteFile?>(null) }
    var fileToPreview by remember { mutableStateOf<RemoteFile?>(null) }

    // Navigation and Action Handlers
    val uploadPicker = rememberUploadPicker { viewModel.uploadFiles(it) }

    var pendingDownload by remember { mutableStateOf<RemoteFile?>(null) }
    val savePicker = rememberSavePicker { target ->
        target?.let { dest ->
            pendingDownload?.let { file ->
                viewModel.downloadFile(file, dest)
            }
        }
    }

    // MULTI-DOWNLOAD: For multiple selection
    val dirPicker = rememberDirectoryPicker { dir ->
        dir?.let { viewModel.downloadSelected(it) }
    }

    val isInSelectionMode = selectedFiles.isNotEmpty()

    // Safety: Prevent accidental exit. The back gesture first navigates up
    // a folder; only at the root folder does it offer to close the session.
    PlatformBackHandler(enabled = true) {
        when {
            isInSelectionMode -> viewModel.selectedFiles.clear()
            isSearchActive -> isSearchActive = false
            currentPath != "/" && currentPath.isNotEmpty() && !isTransitioning -> {
                isTransitioning = true
                viewModel.navigateUp()
            }
            else -> showExitConfirmation = true
        }
    }

    LaunchedEffect(serverId) {
        viewModel.connect(serverId)
    }

    // Release the transition guard once the folder actually changed.
    LaunchedEffect(currentPath) {
        if (isTransitioning) isTransitioning = false
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isEmpty()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            if (isInSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedFiles.size,
                    onClearSelection = { viewModel.selectedFiles.clear() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                    onDownloadSelected = {
                        // For multi-selection, we ask for a directory
                        dirPicker()
                    }
                )
            } else {
                ExplorerTopBar(
                    serverName = viewModel.currentServer?.name ?: AppStrings.loading,
                    currentPath = currentPath,
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    selectedLogin = viewModel.selectedLogin,
                    logins = viewModel.logins,
                    onSelectLogin = { viewModel.selectLogin(it) },
                    onManageLogins = onManageLogins,
                    onSearchQueryChange = { searchQuery = it },
                    onToggleSearch = { isSearchActive = !isSearchActive; if (!isSearchActive) searchQuery = "" },
                    onNavigateBack = { showExitConfirmation = true },
                    onRefresh = { viewModel.loadDirectory(currentPath) },
                    showHidden = showHidden,
                    onToggleHidden = { viewModel.toggleHiddenFiles() },
                    onNewFolder = { showNewFolderDialog = true }
                )
            }
        },
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(onClick = { uploadPicker() }) {
                    Icon(Icons.Default.FileUpload, contentDescription = AppStrings.upload)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column {
                if (isTransferring) {
                    LinearProgressIndicator(
                        progress = { transferProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (currentPath != "/" && currentPath.isNotEmpty() && !isSearchActive) {
                    ListItem(
                        headlineContent = { Text("..", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                        modifier = Modifier.clickable(enabled = !isTransitioning) { viewModel.navigateUp() }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filteredFiles.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(AppStrings.emptyDirectory, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    items(filteredFiles, key = { it.path }) { file ->
                        val isSelected = selectedFiles.any { it.path == file.path }
                        FileItem(
                            file = file,
                            isSelected = isSelected,
                            isInSelectionMode = isInSelectionMode,
                            onClick = {
                                if (!isTransitioning) {
                                    if (isInSelectionMode) viewModel.toggleSelection(file)
                                    else if (file.isDirectory) viewModel.loadDirectory(file.path)
                                }
                            },
                            onLongClick = { if (!isTransitioning) viewModel.toggleSelection(file) },
                            onDoubleClick = {
                                if (!isTransitioning) {
                                    if (file.isDirectory) viewModel.loadDirectory(file.path)
                                    else fileToPreview = file
                                }
                            },
                            onDownload = {
                                pendingDownload = file
                                savePicker(file.name)
                            },
                            onRename = { fileToRename = file },
                            onPreview = { fileToPreview = file },
                            onDelete = { viewModel.deleteFile(file) }
                        )
                    }
                }
            }

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))

            errorMessage?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.errorMessage = null },
                    title = { Text(AppStrings.sessionError) },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.errorMessage = null
                            viewModel.connect(serverId) // Try to reconnect
                        }) {
                            Text(AppStrings.reconnect)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.errorMessage = null }) {
                            Text(AppStrings.dismiss)
                        }
                    }
                )
            }

            if (showNewFolderDialog) {
                TextInputDialog(
                    title = AppStrings.newFolder,
                    initialValue = "",
                    placeholder = AppStrings.folderNamePlaceholder,
                    confirmLabel = AppStrings.create,
                    onDismiss = { showNewFolderDialog = false },
                    onConfirm = { name ->
                        viewModel.createFolder(name)
                        showNewFolderDialog = false
                    }
                )
            }

            fileToRename?.let { file ->
                TextInputDialog(
                    title = AppStrings.rename,
                    initialValue = file.name,
                    confirmLabel = AppStrings.rename,
                    onDismiss = { fileToRename = null },
                    onConfirm = { newName ->
                        viewModel.renameFile(file, newName)
                        fileToRename = null
                    }
                )
            }

            fileToPreview?.let { file ->
                FilePreviewDialog(
                    readRemoteFile = { path, maxBytes -> viewModel.readRemoteFile(path, maxBytes) },
                    file = file,
                    onDismiss = { fileToPreview = null }
                )
            }

            if (showExitConfirmation) {
                AlertDialog(
                    onDismissRequest = { showExitConfirmation = false },
                    title = { Text(AppStrings.closeSessionQ) },
                    text = { Text(AppStrings.exitSftpMsg) },
                    confirmButton = {
                        TextButton(onClick = {
                            showExitConfirmation = false
                            onNavigateBack()
                        }) {
                            Text(AppStrings.exit, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirmation = false }) {
                            Text(AppStrings.stay)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerTopBar(
    serverName: String,
    currentPath: String,
    isSearchActive: Boolean,
    searchQuery: String,
    selectedLogin: ServerLogin?,
    logins: List<ServerLogin>,
    onSelectLogin: (ServerLogin?) -> Unit,
    onManageLogins: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    showHidden: Boolean,
    onToggleHidden: () -> Unit,
    onNewFolder: () -> Unit
) {
    if (isSearchActive) {
        TopAppBar(
            title = {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(AppStrings.searchFiles) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
            },
            navigationIcon = {
                IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Close, null) }
            }
        )
    } else {
        var showLoginMenu by remember { mutableStateOf(false) }
        TopAppBar(
            title = {
                Column {
                    Text(serverName, style = MaterialTheme.typography.titleMedium)
                    Text(currentPath, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selectedLogin != null) {
                        Text(
                            selectedLogin.label.ifBlank { selectedLogin.username },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            },
            actions = {
                IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Search, null) }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
                Box {
                    IconButton(onClick = { showLoginMenu = true }) { Icon(Icons.Default.Person, null) }
                    DropdownMenu(expanded = showLoginMenu, onDismissRequest = { showLoginMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(String.format(AppStrings.mainLoginLabel, "")) },
                            onClick = { onSelectLogin(null); showLoginMenu = false }
                        )
                        logins.forEach { login ->
                            DropdownMenuItem(
                                text = { Text(login.label.ifBlank { login.username }) },
                                trailingIcon = {
                                    if (login.id == selectedLogin?.id) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = { onSelectLogin(login); showLoginMenu = false }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(AppStrings.manageLogins) },
                            onClick = { showLoginMenu = false; onManageLogins() }
                        )
                    }
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(AppStrings.newFolder) },
                            onClick = { showMenu = false; onNewFolder() },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (showHidden) AppStrings.hideHidden else AppStrings.showHidden) },
                            onClick = { onToggleHidden(); showMenu = false },
                            leadingIcon = { Icon(if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDownloadSelected: () -> Unit
) {
    TopAppBar(
        title = { Text(String.format(AppStrings.selectedCount, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) { Icon(Icons.Default.Close, null) }
        },
        actions = {
            IconButton(onClick = onDownloadSelected) {
                Icon(Icons.Default.Download, null)
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItem(
    file: RemoteFile,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier
            .background(backgroundColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onDoubleClick = onDoubleClick),
        leadingContent = {
            Box {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else getFileIcon(file.name),
                    contentDescription = null,
                    tint = if (file.isDirectory) Color(0xFFFFC107) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        },
        headlineContent = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text("${formatFileSize(file.size)} | ${file.permissions}", style = MaterialTheme.typography.bodySmall)
        },
        trailingContent = {
            if (!isInSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!file.isDirectory) {
                        IconButton(onClick = onDownload) { Icon(Icons.Default.Download, null) }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (!file.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text(AppStrings.preview) },
                                    onClick = { showMenu = false; onPreview() },
                                    leadingIcon = { Icon(Icons.Default.Preview, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(AppStrings.rename) },
                                onClick = { showMenu = false; onRename() },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(AppStrings.delete) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
            }
        }
    )
}

private fun getFileIcon(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif" -> Icons.Default.Image
        "mp4", "mkv", "avi" -> Icons.Default.Movie
        "mp3", "wav", "flac" -> Icons.Default.MusicNote
        "pdf", "doc", "txt" -> Icons.Default.Description
        "zip", "rar", "7z" -> Icons.Default.Archive
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB")
    val i = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, i.toDouble()), units[i])
}

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String,
    placeholder: String = "",
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}
