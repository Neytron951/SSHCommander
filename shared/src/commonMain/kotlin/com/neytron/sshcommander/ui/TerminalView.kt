package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.TerminalScreen
import com.neytron.sshcommander.terminal.TerminalController
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
                            // Prefer the physical key (works even with a non-Latin
                            // layout: physical "L" types 'l', exactly what a
                            // terminal user expects when typing "ls -la").
                            val physical = event.key.toPhysicalChar(event.isShiftPressed, capsLock.value)
                            if (physical != null) {
                                controller.sendInput(physical.toString())
                                true
                            } else {
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
                text = parsedOutput,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.2f).sp,
                color = textColor,
                modifier = Modifier.fillMaxWidth()
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
    }
}

/**
 * Maps a physical US QWERTY key to its character, so the terminal keeps
 * working regardless of the OS keyboard layout (e.g. Russian layout still
 * types Latin "l" when the physical L key is pressed).
 *
 * Letters respect Shift and Caps Lock (uppercase = Shift XOR CapsLock);
 * symbol keys only care about Shift.
 */
private fun Key.toPhysicalChar(shift: Boolean, capsLock: Boolean): Char? {
    val letter = this.toLetter()
    if (letter != null) {
        return if (shift xor capsLock) letter.uppercaseChar() else letter
    }
    return when (this) {
        Key.Zero -> if (shift) ')' else '0'
        Key.One -> if (shift) '!' else '1'
        Key.Two -> if (shift) '@' else '2'
        Key.Three -> if (shift) '#' else '3'
        Key.Four -> if (shift) '$' else '4'
        Key.Five -> if (shift) '%' else '5'
        Key.Six -> if (shift) '^' else '6'
        Key.Seven -> if (shift) '&' else '7'
        Key.Eight -> if (shift) '*' else '8'
        Key.Nine -> if (shift) '(' else '9'
        Key.Spacebar -> ' '
        Key.Minus -> if (shift) '_' else '-'
        Key.Equals -> if (shift) '+' else '='
        Key.LeftBracket -> if (shift) '{' else '['
        Key.RightBracket -> if (shift) '}' else ']'
        Key.Backslash -> if (shift) '|' else '\\'
        Key.Semicolon -> if (shift) ':' else ';'
        Key.Apostrophe -> if (shift) '"' else '\''
        Key.Comma -> if (shift) '<' else ','
        Key.Period -> if (shift) '>' else '.'
        Key.Slash -> if (shift) '?' else '/'
        else -> null
    }
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
