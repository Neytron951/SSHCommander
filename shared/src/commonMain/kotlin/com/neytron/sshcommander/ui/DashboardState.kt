package com.neytron.sshcommander.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.data.MonitorWidget
import kotlin.math.roundToInt

class DashboardDragState(
    val columnCount: Int = 12
) {
    var activeId by mutableStateOf<String?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
    var isResizing by mutableStateOf(false)

    // Grid measurements in pixels, updated during composition
    private var _colPx = 0f
    private var _rowPx = 0f

    fun updateGrid(colPx: Float, rowPx: Float) {
        _colPx = colPx
        _rowPx = rowPx
    }

    val colPx get() = _colPx
    val rowPx get() = _rowPx

    fun onDragStart(id: String, isResize: Boolean = false) {
        activeId = id
        isResizing = isResize
        dragOffset = Offset.Zero
    }

    fun onDrag(amount: Offset) {
        dragOffset += amount
    }

    fun onDragEnd(currentWidget: MonitorWidget?, onUpdate: (MonitorWidget) -> Unit) {
        val id = activeId ?: return
        val widget = currentWidget ?: return
        
        if (isResizing) {
            val nw = (widget.w + dragOffset.x / _colPx).roundToInt().coerceIn(1, columnCount - widget.x)
            val nh = (widget.h + dragOffset.y / _rowPx).roundToInt().coerceAtLeast(1)
            onUpdate(widget.copy(w = nw, h = nh))
        } else {
            val nx = (widget.x + dragOffset.x / _colPx).roundToInt().coerceIn(0, columnCount - widget.w)
            val ny = (widget.y + dragOffset.y / _rowPx).roundToInt().coerceAtLeast(0)
            onUpdate(widget.copy(x = nx, y = ny))
        }
        
        resetState()
    }

    fun onDragCancel() {
        resetState()
    }

    private fun resetState() {
        activeId = null
        dragOffset = Offset.Zero
        isResizing = false
    }
}

@Composable
fun rememberDashboardDragState(columnCount: Int = 8) = remember {
    DashboardDragState(columnCount)
}
