package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.sftp.SftpController
import kotlinx.coroutines.launch

@Composable
fun RemoteTextEditor(
    remotePath: String,
    controller: SftpController,
    onClose: () -> Unit
) {
    var textValue by remember { mutableStateOf(TextFieldValue("Loading...")) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    
    val scope = rememberCoroutineScope()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val extension = remotePath.substringAfterLast('.', "")
    val highlighter = remember(extension) { SyntaxHighlighter(extension) }

    LaunchedEffect(remotePath) {
        val remoteText = controller.readRemoteText(remotePath) ?: "Error loading file"
        textValue = TextFieldValue(remoteText)
        isLoaded = true
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(remotePath.substringAfterLast('/'), maxLines = 1) },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                isSaving = true
                                if (controller.writeRemoteText(remotePath, textValue.text)) {
                                    onClose()
                                }
                                isSaving = false
                            }
                        }, enabled = isLoaded) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        },
        bottomBar = {
            EditorToolbar(
                onSymbolClick = { symbol ->
                    val newText = textValue.text.substring(0, textValue.selection.start) + 
                                 symbol + 
                                 textValue.text.substring(textValue.selection.end)
                    val newSelection = TextRange(textValue.selection.start + symbol.length)
                    
                    undoStack.add(textValue.text)
                    textValue = textValue.copy(text = newText, selection = newSelection)
                    redoStack.clear()
                },
                onUndo = {
                    if (undoStack.isNotEmpty()) {
                        redoStack.add(textValue.text)
                        val last = undoStack.removeAt(undoStack.size - 1)
                        textValue = textValue.copy(text = last, selection = TextRange(last.length))
                    }
                },
                onRedo = {
                    if (redoStack.isNotEmpty()) {
                        undoStack.add(textValue.text)
                        val next = redoStack.removeAt(redoStack.size - 1)
                        textValue = textValue.copy(text = next, selection = TextRange(next.length))
                    }
                },
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Line Numbers
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(verticalScrollState)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                val lines = textValue.text.count { it == '\n' } + 1
                for (i in 1..lines) {
                    Text(
                        text = i.toString(),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Text Editor
            BasicTextField(
                value = textValue,
                onValueChange = { newValue ->
                    if (newValue.text != textValue.text) {
                        if (undoStack.isEmpty() || undoStack.last() != textValue.text) {
                            undoStack.add(textValue.text)
                            if (undoStack.size > 50) undoStack.removeAt(0)
                        }
                        redoStack.clear()
                    }
                    textValue = newValue
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(8.dp),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = highlighter
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    onSymbolClick: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, null, modifier = Modifier.size(20.dp))
            }
            
            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

            val symbols = listOf("\t", "{", "}", "[", "]", "(", ")", ";", "=", "\"", "'", "<", ">", "|", "/", "\\", "$")
            symbols.forEach { symbol ->
                TextButton(
                    onClick = { onSymbolClick(symbol) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.width(36.dp)
                ) {
                    Text(
                        text = if (symbol == "\t") "TAB" else symbol,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
