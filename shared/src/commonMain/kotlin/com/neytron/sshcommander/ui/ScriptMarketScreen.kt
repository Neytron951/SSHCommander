package com.neytron.sshcommander.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.MarketScript
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScriptMarketScreen(
    onNavigateBack: () -> Unit,
    onExecuteScript: (String) -> Unit // Passes the final command to terminal
) {
    val deps = LocalAppDeps.current
    val viewModel: ScriptMarketViewModel = viewModel { 
        ScriptMarketViewModel(deps.repository, com.neytron.sshcommander.data.ScriptMarketService(deps.httpClient ?: io.ktor.client.HttpClient())) 
    }
    
    val filteredScripts by viewModel.filteredScripts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    var selectedScript by remember { mutableStateOf<MarketScript?>(null) }
    var scriptToFill by remember { mutableStateOf<MarketScript?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("ScriptMarket", fontWeight = FontWeight.Bold)
                        if (viewModel.isLoading) {
                            Text("Loading...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("Total: $totalCount | Visible: ${filteredScripts.size}", 
                                 style = MaterialTheme.typography.labelSmall, 
                                 color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadScripts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search and Filters
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Search scripts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = viewModel.selectedCategory == null,
                            onClick = { viewModel.selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = viewModel.selectedCategory == category,
                            onClick = { viewModel.selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${viewModel.error}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadScripts() }) { Text("Retry") }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredScripts) { script ->
                        ScriptCard(script = script, onClick = { selectedScript = script })
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedScript?.let { script ->
        var showSuccess by remember { mutableStateOf(false) }
        
        ScriptDetailDialog(
            script = script,
            isInstalling = viewModel.isInstalling == script.id,
            isInstalled = viewModel.isInstalled(script.id) || showSuccess,
            onDismiss = { selectedScript = null },
            onInstall = { 
                coroutineScope.launch {
                    val success = viewModel.installScript(script)
                    if (success) {
                        showSuccess = true
                        platformToast("Script added to your commands panel!")
                        kotlinx.coroutines.delay(1000)
                        selectedScript = null
                    }
                }
            },
            onRun = { scriptToFill = script; selectedScript = null }
        )
    }

    // Variable Fill Dialog
    scriptToFill?.let { script ->
        ScriptVariableDialog(
            script = script,
            onDismiss = { scriptToFill = null },
            onExecute = { finalCmd ->
                onExecuteScript(finalCmd)
                scriptToFill = null
            }
        )
    }
}

@Composable
fun ScriptCard(script: MarketScript, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(script.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(script.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) { Text(script.category) }
                if (script.isDangerous) {
                    Badge(containerColor = MaterialTheme.colorScheme.errorContainer) { Text("Dangerous") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScriptDetailDialog(
    script: MarketScript,
    isInstalling: Boolean,
    isInstalled: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onRun: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(script.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isInstalled && !isInstalling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "This script is already in your commands panel.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Text(script.description)
                
                Text("Compatibility:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                FlowRow(mainAxisSpacing = 4.dp, crossAxisSpacing = 4.dp) {
                    script.compatibleOs.forEach { os ->
                        AssistChip(onClick = {}, label = { Text(os) })
                    }
                }
                
                Text("Author: ${script.author}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onInstall, 
                    enabled = !isInstalling && !isInstalled,
                    colors = if (isInstalled) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary) 
                             else ButtonDefaults.outlinedButtonColors()
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (isInstalled) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Default.Download, null)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(if (isInstalled) "Installed" else "Install")
                }
                Button(onClick = onRun, enabled = !isInstalling) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Run")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isInstalling) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    mainAxisSpacing: androidx.compose.ui.unit.Dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(mainAxisSpacing),
        verticalArrangement = Arrangement.spacedBy(crossAxisSpacing),
        content = content
    )
}

@Composable
fun ScriptVariableDialog(
    script: MarketScript,
    onDismiss: () -> Unit,
    onExecute: (String) -> Unit
) {
    val vars = remember(script) { script.extractVariables() }
    val values = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Script Variables") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (vars.isEmpty()) {
                    Text("This script will be executed directly as it has no variables.")
                } else {
                    vars.forEach { v ->
                        OutlinedTextField(
                            value = values[v] ?: "",
                            onValueChange = { values[v] = it },
                            label = { Text(v.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onExecute(script.buildFinalCommand(values)) },
                enabled = vars.all { (values[it]?.isNotBlank() == true) }
            ) {
                Text("Execute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
