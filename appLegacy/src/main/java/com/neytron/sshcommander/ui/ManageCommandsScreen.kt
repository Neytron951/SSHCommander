package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.AppDatabase
import com.neytron.sshcommander.data.CustomCommand
import com.neytron.sshcommander.data.ServerDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageCommandsViewModel(private val dao: ServerDao) : ViewModel() {
    val commands: StateFlow<List<CustomCommand>> = dao.getAllCustomCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCommand(command: CustomCommand) = viewModelScope.launch {
        dao.insertCustomCommand(command)
    }

    fun updateCommand(command: CustomCommand) = viewModelScope.launch {
        dao.updateCustomCommand(command)
    }

    fun deleteCommand(command: CustomCommand) = viewModelScope.launch {
        dao.deleteCustomCommand(command)
    }

    fun moveUp(index: Int, list: List<CustomCommand>) = viewModelScope.launch {
        if (index > 0) {
            val current = list[index]
            val previous = list[index - 1]
            dao.updateCustomCommand(current.copy(orderIndex = previous.orderIndex))
            dao.updateCustomCommand(previous.copy(orderIndex = current.orderIndex))
        }
    }

    fun moveDown(index: Int, list: List<CustomCommand>) = viewModelScope.launch {
        if (index < list.size - 1) {
            val current = list[index]
            val next = list[index + 1]
            dao.updateCustomCommand(current.copy(orderIndex = next.orderIndex))
            dao.updateCustomCommand(next.copy(orderIndex = current.orderIndex))
        }
    }

    class Factory(private val dao: ServerDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManageCommandsViewModel(dao) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCommandsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).serverDao() }
    val viewModel: ManageCommandsViewModel = viewModel(factory = ManageCommandsViewModel.Factory(dao))
    
    val commands by viewModel.commands.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = commands.mapNotNull { it.categoryName }.distinct().sorted()
    val filteredCommands = commands.filter { cmd ->
        (selectedCategory == null || cmd.categoryName == selectedCategory) &&
        (searchQuery.isEmpty() || cmd.name.contains(searchQuery, ignoreCase = true) || cmd.command.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.manage_commands)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
                
                // Search and Categories
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search commands...") },
                    singleLine = true,
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Delete, null) } }
                )
                
                if (categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All") }
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_server))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredCommands) { index, command ->
                CommandItem(
                    command = command,
                    onEdit = { commandToEdit = command },
                    onDelete = { viewModel.deleteCommand(command) },
                    onMoveUp = { viewModel.moveUp(index, commands) },
                    onMoveDown = { viewModel.moveDown(index, commands) }
                )
            }
        }
    }

    if (showAddDialog) {
        val primaryColor = MaterialTheme.colorScheme.primary
        AddCommandDialog(
            initialColorHex = String.format("#%06X", 0xFFFFFF and primaryColor.toArgb()),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                viewModel.addCommand(
                    CustomCommand(
                        name = name,
                        command = cmd,
                        categoryName = cat.ifBlank { null },
                        iconName = "default",
                        colorHex = colorHex,
                        orderIndex = commands.size,
                        isDangerous = isDangerous
                    )
                )
                showAddDialog = false
            }
        )
    }

    if (commandToEdit != null) {
        AddCommandDialog(
            initialName = commandToEdit!!.name,
            initialCommand = commandToEdit!!.command,
            initialCategory = commandToEdit!!.categoryName ?: "",
            initialIsDangerous = commandToEdit!!.isDangerous,
            initialColorHex = commandToEdit!!.colorHex,
            onDismiss = { commandToEdit = null },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                viewModel.updateCommand(
                    commandToEdit!!.copy(
                        name = name,
                        command = cmd,
                        categoryName = cat.ifBlank { null },
                        isDangerous = isDangerous,
                        colorHex = colorHex
                    )
                )
                commandToEdit = null
            }
        )
    }
}

@Composable
fun CommandItem(
    command: CustomCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(try { Color(command.colorHex.toColorInt()) } catch(e: Exception) { Color.Gray })
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(command.name, style = MaterialTheme.typography.titleMedium)
                Text(command.command, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                if (command.isDangerous) {
                    Text(stringResource(R.string.dangerous), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onMoveUp) { Icon(Icons.Default.KeyboardArrowUp, null) }
            IconButton(onClick = onMoveDown) { Icon(Icons.Default.KeyboardArrowDown, null) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun AddCommandDialog(
    initialName: String = "",
    initialCommand: String = "",
    initialCategory: String = "",
    initialIsDangerous: Boolean = false,
    initialColorHex: String = "#6200EE",
    onDismiss: () -> Unit, 
    onConfirm: (String, String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var cmd by remember { mutableStateOf(initialCommand) }
    var category by remember { mutableStateOf(initialCategory) }
    var isDangerous by remember { mutableStateOf(initialIsDangerous) }
    var colorHex by remember { mutableStateOf(initialColorHex) }

    val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", 
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", 
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) stringResource(R.string.new_command) else stringResource(R.string.edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text(stringResource(R.string.server_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category, 
                    onValueChange = { category = it }, 
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cmd, 
                    onValueChange = { cmd = it }, 
                    label = { Text(stringResource(R.string.commands)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(stringResource(R.string.cmd_placeholder_example)) }
                )
                
                Text(stringResource(R.string.button_color), style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(presetColors) { colorStr ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorStr.toColorInt()))
                                .border(
                                    width = if (colorHex.uppercase() == colorStr.uppercase()) 3.dp else 1.dp,
                                    color = if (colorHex.uppercase() == colorStr.uppercase()) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = colorStr }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDangerous, onCheckedChange = { isDangerous = it })
                    Text(stringResource(R.string.requires_bio))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, cmd, category, isDangerous, colorHex) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
