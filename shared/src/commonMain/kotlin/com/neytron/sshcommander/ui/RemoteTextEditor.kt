package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    var content by remember { mutableStateOf("Loading...") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(remotePath) {
        content = controller.readRemoteText(remotePath) ?: "Error loading file"
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(remotePath.substringAfterLast('/')) },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = {
                            scope.launch {
                                isSaving = true
                                if (controller.writeRemoteText(remotePath, content)) {
                                    onClose()
                                }
                                isSaving = false
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
    ) { padding ->
        TextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
    }
}
