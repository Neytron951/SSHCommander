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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.CustomCommand
import com.neytron.sshcommander.data.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageCommandsViewModel(private val repository: ServerRepository) : ViewModel() {
    val commands: StateFlow<List<CustomCommand>> = repository.getAllCustomCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCommand(command: CustomCommand) = viewModelScope.launch {
        repository.insertCustomCommand(command)
    }

    fun updateCommand(command: CustomCommand) = viewModelScope.launch {
        repository.updateCustomCommand(command)
    }

    fun deleteCommand(command: CustomCommand) = viewModelScope.launch {
        repository.deleteCustomCommand(command)
    }

    fun moveUp(index: Int, list: List<CustomCommand>) = viewModelScope.launch {
        if (index > 0) {
            val current = list[index]
            val previous = list[index - 1]
            repository.updateCustomCommand(current.copy(orderIndex = previous.orderIndex))
            repository.updateCustomCommand(previous.copy(orderIndex = current.orderIndex))
        }
    }

    fun moveDown(index: Int, list: List<CustomCommand>) = viewModelScope.launch {
        if (index < list.size - 1) {
            val current = list[index]
            val next = list[index + 1]
            repository.updateCustomCommand(current.copy(orderIndex = next.orderIndex))
            repository.updateCustomCommand(next.copy(orderIndex = current.orderIndex))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCommandsScreen(
    onNavigateBack: () -> Unit
) {
    val deps = LocalAppDeps.current
    val viewModel: ManageCommandsViewModel = viewModel { ManageCommandsViewModel(deps.repository) }

    val commands by viewModel.commands.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.manageCommands) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.addServer)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(commands) { index, command ->
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
        val initialColorHex = String.format(
            "#%06X",
            ((primaryColor.red * 255).toInt() shl 16) or
                ((primaryColor.green * 255).toInt() shl 8) or
                (primaryColor.blue * 255).toInt()
        )
        AddCommandDialog(
            initialColorHex = initialColorHex,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cmd, isDangerous, colorHex ->
                viewModel.addCommand(
                    CustomCommand(
                        name = name,
                        command = cmd,
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
            initialIsDangerous = commandToEdit!!.isDangerous,
            initialColorHex = commandToEdit!!.colorHex,
            onDismiss = { commandToEdit = null },
            onConfirm = { name, cmd, isDangerous, colorHex ->
                viewModel.updateCommand(
                    commandToEdit!!.copy(
                        name = name,
                        command = cmd,
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
                    .background(parseHexColor(command.colorHex))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(command.name, style = MaterialTheme.typography.titleMedium)
                Text(command.command, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                if (command.isDangerous) {
                    Text(AppStrings.dangerous, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
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
    initialIsDangerous: Boolean = false,
    initialColorHex: String = "#6200EE",
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var cmd by remember { mutableStateOf(initialCommand) }
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
        title = { Text(if (initialName.isEmpty()) AppStrings.newCommand else AppStrings.edit) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.serverName) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cmd,
                    onValueChange = { cmd = it },
                    label = { Text(AppStrings.commands) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text(AppStrings.cmdPlaceholderExample) }
                )

                Text(buttonColorLabel, style = MaterialTheme.typography.labelLarge)
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
                                .background(parseHexColor(colorStr))
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
                    Text(AppStrings.requiresBio)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, cmd, isDangerous, colorHex) }) { Text(AppStrings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

private val buttonColorLabel: String
    get() = if (AppStrings.language == "ru") "Цвет кнопки" else "Button Color"
