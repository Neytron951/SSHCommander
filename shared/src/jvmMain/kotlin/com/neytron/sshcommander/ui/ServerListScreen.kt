package com.neytron.sshcommander.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.Server
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerListScreen(
    onAddServer: () -> Unit,
    onEditServer: (Int) -> Unit,
    onServerClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val deps = LocalAppDeps.current
    val viewModel: ServerListViewModel = viewModel { ServerListViewModel(deps.repository, deps.settings) }

    val servers by viewModel.servers.collectAsState(initial = emptyList())
    val folders by deps.repository.allFolders.collectAsState(initial = emptyList())
    val statuses by viewModel.serverStatuses.collectAsState()

    var selectedServer by remember { mutableStateOf<Server?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(servers) {
        viewModel.checkStatuses(servers)
        // Periodic refresh while the screen is visible. Delayed start keeps
        // the initial check snappy; the 60s cadence is gentle on the battery.
        while (true) {
            delay(60_000)
            viewModel.checkStatuses(servers)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.appName) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = AppStrings.settings)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.addServer)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val adBlockId = "R-M-19743557-1"

            // Build a grouped list: unfiled servers first, then one group per folder.
            // Keep track of how many server cards we've emitted for ad placement.
            var adAfterIndex = if (servers.size >= 2) 2 else if (servers.size == 1) 1 else -1
            var rendered = 0

            fun emitServer(server: Server) {
                rendered++
                item(key = "server-${server.id}") {
                    ServerCard(
                        server = server,
                        isOnline = statuses[server.id] ?: false,
                        onClick = { onServerClick(server.id) },
                        onLongClick = {
                            selectedServer = server
                            showMenu = true
                        }
                    )
                }
                if (adAfterIndex > 0 && rendered == adAfterIndex) {
                    item { PlatformAdBanner(blockId = adBlockId) }
                }
            }

            servers.filter { it.folderId == null }.forEach { emitServer(it) }

            folders.forEach { folder ->
                val inFolder = servers.filter { it.folderId == folder.id }
                if (inFolder.isNotEmpty()) {
                    item(key = "folder-${folder.id}") {
                        FolderListHeader(name = folder.name, count = inFolder.size)
                    }
                    inFolder.forEach { emitServer(it) }
                }
            }
        }
    }

    if (showMenu && selectedServer != null) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text(selectedServer!!.name) },
            text = { Text(AppStrings.chooseAction) },
            confirmButton = {
                TextButton(onClick = {
                    onEditServer(selectedServer!!.id)
                    showMenu = false
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.edit)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteServer(selectedServer!!)
                    showMenu = false
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/** Simple section header shown above a folder's servers on the phone list. */
@Composable
private fun FolderListHeader(name: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerCard(
    server: Server,
    isOnline: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val deps = LocalAppDeps.current
    val privacyMode by deps.settings.privacyMode.collectAsState(initial = false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = IconUtils.getIcon(server.iconName),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (privacyMode) PrivacyUtils.maskHost(server.host) else server.host,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = if (isOnline) Color.Green else Color.Red
            ) {}
        }
    }
}
