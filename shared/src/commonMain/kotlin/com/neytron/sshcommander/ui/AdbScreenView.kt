package com.neytron.sshcommander.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.terminal.AdbPlatform
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdbScreenView(controller: TerminalController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isStreaming by remember { mutableStateOf(true) }
    var fps by remember { mutableIntStateOf(10) } // Smooth steady target FPS
    var statusText by remember { mutableStateOf("Connecting to screen stream...") }
    var isLoadingFrame by remember { mutableStateOf(false) }
    var showScrcpyDialog by remember { mutableStateOf(false) }

    // Producer-Consumer pipeline with Jitter Buffer (Frame Queue)
    LaunchedEffect(controller, isStreaming, fps) {
        val frameChannel = Channel<ImageBitmap>(capacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        // Producer: fetches frames off-thread as fast as device can supply
        val producerJob = launch(Dispatchers.IO) {
            while (isActive && isStreaming) {
                val bmp = controller.captureScreenFrame()
                if (bmp != null) {
                    frameChannel.send(bmp)
                } else {
                    delay(100)
                }
            }
        }

        // Consumer: plays frames from the channel at a steady, perfectly-paced interval
        val consumerJob = launch(Dispatchers.Default) {
            val frameIntervalMs = (1000L / fps).coerceAtLeast(16L)
            var lastBmp: ImageBitmap? = null

            while (isActive && isStreaming) {
                val nextBmp = frameChannel.tryReceive().getOrNull()
                if (nextBmp != null) {
                    lastBmp = nextBmp
                    withContext(Dispatchers.Main) {
                        imageBitmap = nextBmp
                        isLoadingFrame = false
                        statusText = "Streaming active (Paced Buffer)"
                    }
                }
                delay(frameIntervalMs)
            }
        }

        producerJob.join()
        consumerJob.join()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top control bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { isStreaming = !isStreaming }) {
                    Icon(if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isStreaming) "Pause" else "Start")
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        isLoadingFrame = true
                        val bmp = controller.captureScreenFrame()
                        isLoadingFrame = false
                        if (bmp != null) imageBitmap = bmp
                    }
                }) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh")
                }
                FilledTonalButton(onClick = {
                    val res = AdbPlatform.launchScrcpy("localhost", 5555)
                    if (res.isFailure) {
                        showScrcpyDialog = true
                    } else {
                        platformToast("scrcpy 60 FPS mirror launched!")
                    }
                }) {
                    Icon(Icons.Default.Tv, null)
                    Spacer(Modifier.width(4.dp))
                    Text("HD 60 FPS (scrcpy)")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("FPS: $fps", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = fps.toFloat(),
                    onValueChange = { fps = it.toInt().coerceIn(1, 30) },
                    valueRange = 1f..30f,
                    steps = 29,
                    modifier = Modifier.width(100.dp)
                )
            }
        }

        // Status text / progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLoadingFrame) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Screen display area
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = imageBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = "Device Screen Mirror",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Waiting for screen frame...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showScrcpyDialog) {
        AlertDialog(
            onDismissRequest = { showScrcpyDialog = false },
            title = { Text("60 FPS HD Screen Mirroring") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Android Studio и scrcpy используют аппаратное H.264 видеокодирование через GPU устройства для получения суперплавных 60 FPS.")
                    Text("Для запуска 60 FPS трансляции установите утилиту 'scrcpy' на ваш ПК:")
                    Text("• Windows: winget install Genymobile.scrcpy", style = MaterialTheme.typography.bodySmall)
                    Text("• macOS: brew install scrcpy", style = MaterialTheme.typography.bodySmall)
                    Text("• Linux: sudo apt install scrcpy", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    platformOpenUrl("https://github.com/Genymobile/scrcpy")
                    showScrcpyDialog = false
                }) {
                    Text("Скачать scrcpy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScrcpyDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}
