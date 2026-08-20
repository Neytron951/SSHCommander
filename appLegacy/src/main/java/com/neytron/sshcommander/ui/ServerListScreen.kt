package com.neytron.sshcommander.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.data.Workspace
import com.neytron.sshcommander.ui.PrivacyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ServerListScreen(
    viewModel: ServerListViewModel = viewModel(),
    onAddServer: () -> Unit,
    onEditServer: (Int) -> Unit,
    onServerClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ServerRepository(context) }
    val scope = rememberCoroutineScope()
    
    val servers by viewModel.servers.collectAsState(initial = emptyList())
    val workspaces by repository.allWorkspaces.collectAsState(initial = emptyList())
    val statuses by viewModel.serverStatuses.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Servers, 1: Workspaces
    var selectedServer by remember { mutableStateOf<Server?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(servers) {
        viewModel.checkStatuses(servers)
        while (true) {
            delay(60_000)
            viewModel.checkStatuses(servers)
        }
    }

    Scaffold(
        topBar = { 
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(stringResource(R.string.servers), modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Workspaces", modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAddServer) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_server))
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (selectedTab == 1) {
                WorkspaceList(
                    workspaces = workspaces,
                    onOpen = { ws ->
                        ws.items.firstOrNull()?.let { onServerClick(it.serverId) }
                    },
                    onDelete = { id -> scope.launch { repository.deleteWorkspace(id) } }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val adBlockId = "R-M-19743557-1"
                    
                    if (servers.size == 1) {
                        item {
                            ServerCard(
                                server = servers[0],
                                isOnline = statuses[servers[0].id] ?: false,
                                onClick = { onServerClick(servers[0].id) },
                                onLongClick = {
                                    selectedServer = servers[0]
                                    showMenu = true
                                }
                            )
                        }
                        item { NativeAdBanner(blockId = adBlockId) }
                    } else if (servers.size >= 2) {
                        itemsIndexed(servers) { index, server ->
                            ServerCard(
                                server = server,
                                isOnline = statuses[server.id] ?: false,
                                onClick = { onServerClick(server.id) },
                                onLongClick = {
                                    selectedServer = server
                                    showMenu = true
                                }
                            )
                            if (index == 1) {
                                NativeAdBanner(blockId = adBlockId)
                            }
                        }
                    } else {
                        items(servers) { server ->
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
                    }
                }
            }
        }
    }

    if (showMenu && selectedServer != null) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            title = { Text(selectedServer!!.name) },
            text = { Text(stringResource(R.string.choose_action)) },
            confirmButton = {
                TextButton(onClick = {
                    onEditServer(selectedServer!!.id)
                    showMenu = false
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.deleteServer(selectedServer!!)
                    showMenu = false
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
fun WorkspaceList(
    workspaces: List<Workspace>,
    onOpen: (Workspace) -> Unit,
    onDelete: (Int) -> Unit
) {
    if (workspaces.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved workspaces", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(workspaces, key = { it.id }) { ws ->
                Card(
                    onClick = { onOpen(ws) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GroupWork, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ws.name, style = MaterialTheme.typography.titleMedium)
                            Text("${ws.items.size} tabs", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDelete(ws.id) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
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
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val privacyMode by settingsManager.privacyMode.collectAsState(initial = false)

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
