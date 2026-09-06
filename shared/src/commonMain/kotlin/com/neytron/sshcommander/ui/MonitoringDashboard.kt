package com.neytron.sshcommander.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.MonitorWidget
import com.neytron.sshcommander.data.WidgetType
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun MonitoringDashboard(
    terminalSession: TerminalController?
) {
    if (terminalSession == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Session required") }
        return
    }

    val remoteWidgets by terminalSession.monitorWidgets.collectAsState()
    val results by terminalSession.widgetResults.collectAsState()
    val history by terminalSession.widgetHistory.collectAsState()
    val loadingMap by terminalSession.widgetLoading.collectAsState()
    val stats by terminalSession.sysStats.collectAsState()
    val isConnected by terminalSession.isConnected.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<MonitorWidget?>(null) }

    val density = LocalDensity.current
    val dragState = rememberDashboardDragState(12)

    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val mw = maxWidth
        val colWidth = mw / 12
        val rowHeight = 80.dp
        val colPx = with(density) { colWidth.toPx() }
        val rowPx = with(density) { rowHeight.toPx() }

        SideEffect { dragState.updateGrid(colPx, rowPx) }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(AppStrings.dashboard, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                if (isConnected) Surface(Modifier.size(8.dp), CircleShape, Color(0xFF4CAF50)) {}
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    isEditMode = !isEditMode
                    dragState.activeId = null
                }) {
                    Icon(if (isEditMode) Icons.Default.Check else Icons.Default.Settings, null, tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp)); Text(AppStrings.add)
                }
            }
            Spacer(Modifier.height(24.dp))

            val totalRows = (remoteWidgets.maxOfOrNull { it.y + it.h } ?: 8).coerceAtLeast(10)
            Box(Modifier.fillMaxWidth().height(rowHeight * totalRows)) {
                if (isEditMode) {
                    Canvas(Modifier.fillMaxSize().alpha(0.05f)) {
                        for (i in 1..11) drawLine(Color.Gray, androidx.compose.ui.geometry.Offset(i * colPx, 0f), androidx.compose.ui.geometry.Offset(i * colPx, size.height), 1.dp.toPx())
                        for (i in 1..totalRows) drawLine(Color.Gray, androidx.compose.ui.geometry.Offset(0f, i * rowPx), androidx.compose.ui.geometry.Offset(size.width, i * rowPx), 1.dp.toPx())
                    }
                    dragState.activeId?.let { id ->
                        remoteWidgets.find { it.id == id }?.let { w ->
                            val gx: Int; val gy: Int; val gw: Int; val gh: Int
                            if (dragState.isResizing) {
                                gx = w.x; gy = w.y
                                gw = (w.w + dragState.dragOffset.x / colPx).roundToInt().coerceIn(1, 12 - w.x)
                                gh = (w.h + dragState.dragOffset.y / rowPx).roundToInt().coerceIn(1, 15)
                            } else {
                                gx = (w.x + dragState.dragOffset.x / colPx).roundToInt().coerceIn(0, 12 - w.w)
                                gy = (w.y + dragState.dragOffset.y / rowPx).roundToInt().coerceAtLeast(0)
                                gw = w.w; gh = w.h
                            }
                            Box(Modifier.offset { IntOffset((gx * colPx).roundToInt(), (gy * rowPx).roundToInt()) }.width(colWidth * gw).height(rowHeight * gh).padding(2.dp).background(MaterialTheme.colorScheme.primary.copy(0.15f), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
                        }
                    }
                }

                remoteWidgets.forEach { widget ->
                    key(widget.id) {
                        val isActive = dragState.activeId == widget.id
                        val isR = dragState.isResizing && isActive
                        val isD = !dragState.isResizing && isActive
                        val rawV = results[widget.id]
                        val hist = history[widget.id] ?: emptyList()
                        val isL = loadingMap[widget.id] ?: (rawV == null)

                        // Apply constraints to prevent data-driven overflow
                        val wx = widget.x.coerceIn(0, 12 - widget.w.coerceIn(1, 12))
                        val ww = widget.w.coerceIn(1, 12 - wx)

                        Box(
                            Modifier.offset {
                                IntOffset(
                                    ((wx * colPx) + (if (isD) dragState.dragOffset.x else 0f)).roundToInt(),
                                    ((widget.y * rowPx) + (if (isD) dragState.dragOffset.y else 0f)).roundToInt()
                                )
                            }
                                .width(colWidth * ww + (if (isR) (dragState.dragOffset.x / density.density).dp else 0.dp))
                                .height(rowHeight * widget.h + (if (isR) (dragState.dragOffset.y / density.density).dp else 0.dp))
                                .padding(2.dp).alpha(if (isActive) 0.6f else 1f)
                        ) {
                            WidgetCard(
                                widget = widget,
                                value = rawV ?: "",
                                history = hist,
                                isLoading = isL,
                                isEditMode = isEditMode,
                                onEdit = { editingWidget = widget },
                                onDelete = { terminalSession.deleteWidget(widget.id) },
                                onDragStart = {
                                    dragState.onDragStart(widget.id, false)
                                },
                                onDrag = { dragState.onDrag(it) },
                                onDragEnd = {
                                    val latest = remoteWidgets.find { it.id == widget.id } ?: widget
                                    val latestWx = latest.x.coerceIn(0, 12 - latest.w.coerceIn(1, 12))
                                    val latestWw = latest.w.coerceIn(1, 12 - latestWx)
                                    val latestConstrained = latest.copy(x = latestWx, w = latestWw)
                                    
                                    dragState.onDragEnd(latestConstrained) {
                                        terminalSession.updateWidget(it)
                                    }
                                },
                                onDragCancel = {
                                    dragState.onDragCancel()
                                },
                                onResizeStart = {
                                    dragState.onDragStart(widget.id, true)
                                },
                                onResizeEnd = {
                                    val latest = remoteWidgets.find { it.id == widget.id } ?: widget
                                    val latestWx = latest.x.coerceIn(0, 12 - latest.w.coerceIn(1, 12))
                                    val latestWw = latest.w.coerceIn(1, 12 - latestWx)
                                    val latestConstrained = latest.copy(x = latestWx, w = latestWw)

                                    dragState.onDragEnd(latestConstrained) {
                                        terminalSession.updateWidget(it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(AppStrings.systemLogs, style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) ) {
                Text(text = stats.rawLogs.ifBlank { "No logs captured yet..." }, modifier = Modifier.padding(12.dp), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.height(100.dp))
        }
    }

    if (showAddDialog || editingWidget != null) {
        WidgetEditDialog(widget = editingWidget, onDismiss = { showAddDialog = false; editingWidget = null },
            onSave = { title, cmd, type, w, h, fs, color, align, vAlign ->
                if (editingWidget != null) terminalSession.updateWidget(editingWidget!!.copy(title = title, command = cmd, type = type, w = w, h = h, fontSize = fs, colorHex = color, textAlign = align, textVerticalAlign = vAlign))
                else terminalSession.addWidget(title, cmd, type, 0, (remoteWidgets.maxOfOrNull { it.y + it.h } ?: 0), w, h, fs, color, align, vAlign)
                showAddDialog = false; editingWidget = null
            }
        )
    }
}

@Composable
private fun WidgetCard(
    widget: MonitorWidget, value: String, history: List<Float>, isLoading: Boolean, isEditMode: Boolean,
    onEdit: () -> Unit, onDelete: () -> Unit,
    onDragStart: () -> Unit, onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit, onDragCancel: () -> Unit,
    onResizeStart: () -> Unit, onResizeEnd: () -> Unit
) {
    val displayValue = remember(value, widget.type) {
        if (value.isEmpty() || value == "...") return@remember "..."
        when (widget.type) {
            WidgetType.BYTES -> formatBytes(value.toLongOrNull() ?: 0L)
            WidgetType.PERCENTAGE -> { val f = value.replace(',', '.').toFloatOrNull() ?: 0f; if (value.contains(".")) "%.1f%%".format(f) else "${f.toInt()}%" }
            else -> value
        }
    }
    val progress = remember(value, widget.type) { if (widget.type == WidgetType.PERCENTAGE) (value.replace(',', '.').toFloatOrNull() ?: 0f) / 100f else 0f }

    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEditMode) 8.dp else 2.dp),
        border = BorderStroke(1.dp, if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Box(Modifier.fillMaxSize()) {
            if (isEditMode) {
                Box(Modifier.fillMaxSize().pointerInput(widget.id) {
                    detectDragGestures(
                        onDragStart = { _ -> onDragStart() },
                        onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() }
                    )
                })
            }
            Column(Modifier.padding(8.dp).fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (isLoading && value.isEmpty()) CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.5.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(widget.title, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(2.dp))
                if (widget.w >= 4 && widget.type == WidgetType.CHART) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = widget.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = when(widget.textAlign) {
                                "left" -> TextAlign.Left
                                "right" -> TextAlign.Right
                                else -> TextAlign.Center
                            },
                            modifier = Modifier.weight(1f).wrapContentHeight(
                                when(widget.textVerticalAlign) {
                                    "top" -> Alignment.Top
                                    "bottom" -> Alignment.Bottom
                                    else -> Alignment.CenterVertically
                                }
                            )
                        )
                        Spacer(Modifier.width(12.dp)); ChartView(history, widget.colorHex, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    if (isLoading && value.isEmpty()) CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(4.dp))
                    else Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = when(widget.textVerticalAlign) {
                        "top" -> Alignment.TopCenter
                        "bottom" -> Alignment.BottomCenter
                        else -> Alignment.Center
                    }) {
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = widget.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = when(widget.textAlign) {
                                "left" -> TextAlign.Left
                                "right" -> TextAlign.Right
                                else -> TextAlign.Center
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (widget.type == WidgetType.PERCENTAGE) {
                        Spacer(Modifier.height(4.dp)); val color = widget.colorHex?.let { Color(parseHex(it)) } ?: Color(0xFF4CAF50)
                        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = color, trackColor = color.copy(0.2f))
                    } else if (widget.type == WidgetType.CHART && history.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp)); ChartView(history, widget.colorHex, modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
            }
            if (isEditMode) {
                Box(Modifier.size(32.dp).align(Alignment.BottomEnd).pointerInput(widget.id) {
                    detectDragGestures(
                        onDragStart = { _ -> onResizeStart() },
                        onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount) },
                        onDragEnd = { onResizeEnd() },
                        onDragCancel = { onDragCancel() }
                    )
                }) {
                    Icon(Icons.Default.DragHandle, null, modifier = Modifier.size(20.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ChartView(history: List<Float>, colorHex: String?, modifier: Modifier) {
    val color = colorHex?.let { Color(parseHex(it)) } ?: Color(0xFF4CAF50)
    if (history.isEmpty()) return
    Canvas(modifier) {
        val path = Path()
        val maxVal = (history.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
        val minVal = (history.minOrNull() ?: 0f)
        val range = (maxVal - minVal).coerceAtLeast(0.001f)
        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
        history.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minVal) / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetEditDialog(widget: MonitorWidget?, onDismiss: () -> Unit, onSave: (String, String, WidgetType, Int, Int, Float, String?, String, String) -> Unit) {
    var title by remember { mutableStateOf(widget?.title ?: "") }
    var command by remember { mutableStateOf(widget?.command ?: "") }
    var type by remember { mutableStateOf(widget?.type ?: WidgetType.TEXT) }
    var w by remember { mutableIntStateOf(widget?.w ?: 3) }
    var h by remember { mutableIntStateOf(widget?.h ?: 2) }
    var fSize by remember { mutableFloatStateOf(widget?.fontSize ?: 16f) }
    var colorHex by remember { mutableStateOf(widget?.colorHex ?: "#4CAF50") }
    var textAlign by remember { mutableStateOf(widget?.textAlign ?: "center") }
    var textVerticalAlign by remember { mutableStateOf(widget?.textVerticalAlign ?: "center") }
    val presetListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (widget == null) AppStrings.addWidget else AppStrings.editWidget) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(AppStrings.presets, style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    state = presetListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val delta = event.changes.first().scrollDelta
                                        val scrollAmount = (delta.x + delta.y) * 40f
                                        scope.launch {
                                            presetListState.scrollBy(scrollAmount)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    lazyRowItems(MonitorWidget.PRESETS) { preset ->
                        AssistChip(onClick = { 
                            title = preset.title; command = preset.command; type = preset.type
                            w = preset.w; h = preset.h; fSize = preset.fontSize; textAlign = preset.textAlign
                            textVerticalAlign = preset.textVerticalAlign
                            preset.color?.let { colorHex = it } 
                        }, label = { Text(preset.title) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(AppStrings.widgetTitle) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text(AppStrings.widgetCommand) }, modifier = Modifier.fillMaxWidth())
                Text(AppStrings.displayType, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { 
                    WidgetType.entries.forEach { t -> 
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.name) }) 
                    } 
                }

                Text("Horizontal Alignment", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("left", "center", "right").forEach { align ->
                        FilterChip(
                            selected = textAlign == align,
                            onClick = { textAlign = align },
                            label = { 
                                Icon(
                                    when(align) {
                                        "left" -> Icons.AutoMirrored.Filled.FormatAlignLeft
                                        "right" -> Icons.AutoMirrored.Filled.FormatAlignRight
                                        else -> Icons.Default.FormatAlignCenter
                                    },
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Text("Vertical Alignment", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("top", "center", "bottom").forEach { vAlign ->
                        FilterChip(
                            selected = textVerticalAlign == vAlign,
                            onClick = { textVerticalAlign = vAlign },
                            label = { 
                                Icon(
                                    when(vAlign) {
                                        "top" -> Icons.Default.VerticalAlignTop
                                        "bottom" -> Icons.Default.VerticalAlignBottom
                                        else -> Icons.Default.VerticalAlignCenter
                                    },
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Text("Font Size: ${fSize.toInt()}sp", style = MaterialTheme.typography.labelMedium)
                Slider(value = fSize, onValueChange = { fSize = it }, valueRange = 8f..32f, steps = 24)
                Text("Size (Width x Height)", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { Text("Width: $w", style = MaterialTheme.typography.bodySmall); Slider(value = w.toFloat(), onValueChange = { w = it.roundToInt() }, valueRange = 1f..12f, steps = 11) }
                    Column(Modifier.weight(1f)) { Text("Height: $h", style = MaterialTheme.typography.bodySmall); Slider(value = h.toFloat(), onValueChange = { h = it.roundToInt() }, valueRange = 1f..10f, steps = 9) }
                }
                if (type == WidgetType.PERCENTAGE || type == WidgetType.CHART) OutlinedTextField(value = colorHex, onValueChange = { colorHex = it }, label = { Text(AppStrings.indicatorColor) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(title, command, type, w, h, fSize, colorHex, textAlign, textVerticalAlign) }) { Text(AppStrings.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppStrings.cancel) } }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, 4)
    return "%.1f %s".format(bytes / 1024.0.pow(i.toDouble()), units[i])
}

private fun parseHex(hex: String): Long {
    return try { hex.removePrefix("#").toLong(16) or 0xFF000000L } catch (e: Exception) { 0xFF4CAF50 }
}
