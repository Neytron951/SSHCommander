package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.TerminalScreen
import com.neytron.sshcommander.terminal.TerminalController
import kotlin.math.abs
import kotlinx.coroutines.flow.StateFlow

/**
 * Renders a [TerminalScreen] emulator into a scrollable console.
 * Fully cross-platform: receives the emulator and its revision flow.
 * When [controller] is supplied, keystrokes are forwarded to the SSH session.
 */
@Composable
fun TerminalView(
    terminalScreen: TerminalScreen,
    terminalRevision: StateFlow<Int>,
    isLoading: StateFlow<Boolean>,
    controller: TerminalController? = null,
    bgColor: Color = Color(0xFF0D1117),
    textColor: Color = Color(0xFFC9D1D9),
    fontSizeSp: Float = 13f,
    onFontSizeChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val revision by terminalRevision.collectAsState()
    val loading by isLoading.collectAsState()
    val scrollState = rememberScrollState()
    val internalFocusRequester = remember { FocusRequester() }
    // An optional externally-owned FocusRequester lets callers re-focus the
    // terminal after e.g. clicking a command button (which steals focus).
    val effectiveFocusRequester = focusRequester ?: internalFocusRequester
    val interactionSource = remember { MutableInteractionSource() }
    // Caps Lock isn't exposed by the common KeyEvent API, so we track it
    // ourselves by observing the Caps Lock key itself.
    val capsLock = remember { mutableStateOf(false) }

    val parsedOutput: AnnotatedString = remember(revision, textColor) {
        terminalScreen.render(textColor)
    }

    // ---- Text selection (mouse drag on desktop) ----
    val clipboard = LocalClipboardManager.current
    var selectionStart by remember { mutableStateOf<Int?>(null) }
    var selectionEnd by remember { mutableStateOf<Int?>(null) }
    var contextMenuAnchor by remember { mutableStateOf<Offset?>(null) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentLayout by rememberUpdatedState(layoutResult)
    // Double-click detection: last primary-click timestamp + char offset.
    var lastClickUptime by remember { mutableStateOf(0L) }
    var lastClickOffset by remember { mutableStateOf(0) }

    val hasSelection = selectionStart != null && selectionEnd != null &&
        selectionStart != selectionEnd

    fun copySelection() {
        val a = selectionStart ?: return
        val b = selectionEnd ?: return
        if (a == b) return
        val lo = minOf(a, b).coerceIn(0, parsedOutput.length)
        val hi = maxOf(a, b).coerceIn(0, parsedOutput.length)
        if (lo == hi) return
        clipboard.setText(AnnotatedString(parsedOutput.text.substring(lo, hi)))
    }

    val selectionHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val displayText = remember(revision, textColor, selectionStart, selectionEnd, selectionHighlight) {
        buildAnnotatedString {
            append(parsedOutput)
            val a = selectionStart?.coerceIn(0, parsedOutput.length) ?: -1
            val b = selectionEnd?.coerceIn(0, parsedOutput.length) ?: -1
            if (a >= 0 && b >= 0 && a != b) {
                addStyle(SpanStyle(background = selectionHighlight), minOf(a, b), maxOf(a, b))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(onFontSizeChange) {
                // Ctrl + mouse wheel → zoom the terminal font (desktop). Uses the
                // Initial pass so we can consume the scroll before the inner
                // verticalScroll modifier would turn it into page scrolling.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Scroll &&
                            event.keyboardModifiers.isCtrlPressed
                        ) {
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (delta != 0f) {
                                val step = if (delta > 0) 1f else -1f
                                onFontSizeChange((fontSizeSp + step).coerceIn(6f, 40f))
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                // Right-click → context menu (Copy / Select All / Clear).
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull()
                        if (event.type == PointerEventType.Press &&
                            change != null &&
                            event.buttons.isSecondaryPressed
                        ) {
                            contextMenuAnchor = change.position
                            change.consume()
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Click anywhere in the terminal → keyboard goes to the terminal.
                effectiveFocusRequester.requestFocus()
            }
            .focusRequester(effectiveFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (controller != null && event.type == KeyEventType.KeyDown) {
                    when {
                        // Keep track of Caps Lock ourselves (not in common API).
                        event.key == Key.CapsLock -> {
                            capsLock.value = !capsLock.value
                            true
                        }
                        // Modifier-only keys (Shift/Ctrl/Alt/Caps...) carry no
                        // character of their own; AWT reports CHAR_UNDEFINED
                        // (0xFFFF) for them and forwarding it would put a stray
                        // space/glyph into the terminal.
                        event.key in modifierKeys -> false
                        // Ctrl+C copies when there is an active text selection,
                        // otherwise it keeps its terminal meaning (SIGINT). This
                        // is what native terminal apps do — selection wins over
                        // the interrupt signal.
                        event.isCtrlPressed && event.key == Key.C -> {
                            if (hasSelection) {
                                copySelection()
                                selectionStart = null
                                selectionEnd = null
                            } else {
                                controller.sendCtrlKey('c')
                            }
                            true
                        }
                        // Ctrl+V or Ctrl+Shift+V pastes from the local clipboard.
                        event.isCtrlPressed && event.key == Key.V -> {
                            clipboard.getText()?.text?.let { text ->
                                // Remove trailing newline so it doesn't execute immediately
                                val cleanText = when {
                                    text.endsWith("\r\n") -> text.dropLast(2)
                                    text.endsWith("\n") || text.endsWith("\r") -> text.dropLast(1)
                                    else -> text
                                }
                                controller.sendInput(cleanText)
                            }
                            true
                        }
                        // Ctrl+letter → control character (Ctrl+X exits nano,
                        // Ctrl+O saves, Ctrl+G cancels...).
                        event.isCtrlPressed && event.key.toLetter() != null -> {
                            controller.sendCtrlKey(event.key.toLetter()!!)
                            true
                        }
                        event.key == Key.Enter -> { controller.sendEnter(); true }
                        event.key == Key.Backspace -> { controller.sendBackspace(); true }
                        event.key == Key.DirectionUp -> { controller.sendArrowUp(); true }
                        event.key == Key.DirectionDown -> { controller.sendArrowDown(); true }
                        event.key == Key.DirectionLeft -> { controller.sendArrowLeft(); true }
                        event.key == Key.DirectionRight -> { controller.sendArrowRight(); true }
                        event.key == Key.Escape -> { controller.sendEscape(); true }
                        event.key == Key.Tab -> { controller.sendInput("\t"); true }
                        event.key == Key.Delete -> { controller.sendInput("\u001b[3~"); true }
                        event.key == Key.MoveHome -> { controller.sendInput("\u001b[H"); true }
                        event.key == Key.MoveEnd -> { controller.sendInput("\u001b[F"); true }
                        event.key == Key.PageUp -> { controller.sendInput("\u001b[5~"); true }
                        event.key == Key.PageDown -> { controller.sendInput("\u001b[6~"); true }
                        else -> {
                            val cp = event.utf16CodePoint
                            // 0xFFFF = AWT CHAR_UNDEFINED (modifier keys).
                            if (cp != 0 && cp != 0xFFFF && !Char(cp).isISOControl()) {
                                controller.sendInput(Char(cp).toString())
                                true
                            } else {
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(10.dp)
        ) {
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.2f).sp,
                color = textColor,
                onTextLayout = { layoutResult = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // Mouse drag → text selection (single click positions the
                        // caret-ish point, double click selects a whole word).
                        awaitEachGesture {
                            val pressEvent = awaitPointerEvent()
                            val down = pressEvent.changes.firstOrNull() ?: return@awaitEachGesture
                            if (pressEvent.type != PointerEventType.Press) return@awaitEachGesture
                            if (down.type != PointerType.Mouse) return@awaitEachGesture
                            if (!pressEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                            val layout = currentLayout ?: return@awaitEachGesture
                            val offset = layout.getOffsetForPosition(down.position)
                            val isDoubleClick = down.uptimeMillis - lastClickUptime < 500 &&
                                abs(offset - lastClickOffset) <= 3
                            lastClickUptime = down.uptimeMillis
                            lastClickOffset = offset
                            if (isDoubleClick) {
                                val word = wordRange(parsedOutput.text, offset)
                                if (word.isEmpty()) {
                                    selectionStart = null
                                    selectionEnd = null
                                } else {
                                    selectionStart = word.first
                                    selectionEnd = word.last + 1
                                }
                            } else {
                                selectionStart = offset
                                selectionEnd = offset
                            }
                            effectiveFocusRequester.requestFocus()
                            val dragged = drag(down.id) { change ->
                                currentLayout?.let { l ->
                                    selectionEnd = l.getOffsetForPosition(change.position)
                                    change.consume()
                                }
                            }
                            // Plain click with no drag: collapse the selection.
                            if (dragged && selectionStart != null && selectionEnd == selectionStart) {
                                selectionStart = null
                                selectionEnd = null
                            }
                        }
                    }
            )
        }

        LaunchedEffect(revision) {
            if (!terminalScreen.isFullScreen) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        LaunchedEffect(terminalScreen) {
            effectiveFocusRequester.requestFocus()
        }

        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = textColor,
                trackColor = Color.Transparent
            )
        }

        contextMenuAnchor?.let { anchor ->
            // Anchor the DropdownMenu through a zero-size Box: applying offset
            // directly to DropdownMenu conflicts with its internal Popup
            // positioning and renders the menu crooked / away from the cursor.
            Box(
                modifier = Modifier.offset {
                    IntOffset(anchor.x.toInt(), anchor.y.toInt())
                }
            ) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { contextMenuAnchor = null }
            ) {
                DropdownMenuItem(
                    text = { Text(AppStrings.copyText) },
                    onClick = {
                        copySelection()
                        selectionStart = null
                        selectionEnd = null
                        contextMenuAnchor = null
                    },
                    enabled = hasSelection
                )
                DropdownMenuItem(
                    text = { Text(AppStrings.selectAll) },
                    onClick = {
                        selectionStart = 0
                        selectionEnd = parsedOutput.length
                        contextMenuAnchor = null
                    }
                )
                DropdownMenuItem(
                    text = { Text(AppStrings.clearSelection) },
                    onClick = {
                        selectionStart = null
                        selectionEnd = null
                        contextMenuAnchor = null
                    },
                    enabled = hasSelection
                )
            }
            }
        }
    }
}

/**
 * Returns the whitespace-delimited word covering [offset] (the character
 * offset the user double-clicked), or an empty range if it hit whitespace.
 */
private fun wordRange(text: String, offset: Int): IntRange {
    val clamped = offset.coerceIn(0, text.length)
    var s = clamped
    while (s > 0 && !text[s - 1].isWhitespace()) s--
    var e = clamped
    while (e < text.length && !text[e].isWhitespace()) e++
    return s until e
}

/** Returns the lowercase letter for a physical A-Z key, or null. */
private fun Key.toLetter(): Char? = when (this) {
    Key.A -> 'a'
    Key.B -> 'b'
    Key.C -> 'c'
    Key.D -> 'd'
    Key.E -> 'e'
    Key.F -> 'f'
    Key.G -> 'g'
    Key.H -> 'h'
    Key.I -> 'i'
    Key.J -> 'j'
    Key.K -> 'k'
    Key.L -> 'l'
    Key.M -> 'm'
    Key.N -> 'n'
    Key.O -> 'o'
    Key.P -> 'p'
    Key.Q -> 'q'
    Key.R -> 'r'
    Key.S -> 's'
    Key.T -> 't'
    Key.U -> 'u'
    Key.V -> 'v'
    Key.W -> 'w'
    Key.X -> 'x'
    Key.Y -> 'y'
    Key.Z -> 'z'
    else -> null
}

/** Keys that only modify other keys and must never be sent as text. */
private val modifierKeys = setOf(
    Key.ShiftLeft, Key.ShiftRight,
    Key.CtrlLeft, Key.CtrlRight,
    Key.AltLeft, Key.AltRight,
    Key.MetaLeft, Key.MetaRight,
    Key.CapsLock, Key.NumLock, Key.ScrollLock, Key.Function, Key.Unknown
)
