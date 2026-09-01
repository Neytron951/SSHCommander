package com.neytron.sshcommander.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * A minimal VT100/ANSI terminal emulator that maintains a character grid
 * instead of a growing string.
 *
 * Full-screen programs (nano, vim, htop) paint their UI by moving the cursor
 * and rewriting lines via ANSI escape sequences. A naive "append all bytes"
 * terminal turns that into garbage, so we interpret the stream against a
 * grid buffer (with a large scrollback) and render the visible screen.
 */
object TerminalDimensions {
    const val ROWS = 1000   // large scrollback
    const val COLS = 500    // max width to avoid truncation
}

internal class TerminalCell {
    var char: Char = ' '
    var fg: Int = -1   // -1 = default foreground
    var bold: Boolean = false
}

internal class ScreenBuffer(var rows: Int, var cols: Int) {
    val cells = Array(rows) { Array(cols) { TerminalCell() } }

    // Cursor rendering: reverse-video block. Space under the cursor becomes a
    // solid block; a real character stays visible with inverted colors.
    private val cursorTextColor = Color.Black

    var cursorRow = 0
    var cursorCol = 0

    var topMargin = 0
    var bottomMargin = rows - 1

    var fg = -1
    var bold = false

    private var savedRow = 0
    private var savedCol = 0

    fun clear() {
        for (r in 0 until rows) for (c in 0 until cols) cells[r][c] = TerminalCell()
        cursorRow = 0
        cursorCol = 0
        topMargin = 0
        bottomMargin = rows - 1
    }

    fun putChar(ch: Char) {
        when (ch) {
            '\n' -> {
                if (cursorRow == bottomMargin) {
                    scrollUp(1, topMargin, bottomMargin)
                } else if (cursorRow + 1 >= rows) {
                    scrollUp(1, 0, rows - 1)
                    cursorRow = rows - 1
                } else {
                    cursorRow++
                }
            }
            '\r' -> cursorCol = 0
            '\b', '\u0008' -> cursorCol = (cursorCol - 1).coerceAtLeast(0)
            '\t' -> cursorCol = (((cursorCol / 8) + 1) * 8).coerceAtMost(cols - 1)
            '\u0007' -> {} // BEL
            else -> {
                val cell = cells[cursorRow][cursorCol]
                cell.char = ch
                cell.fg = fg
                cell.bold = bold
                cursorCol++
                if (cursorCol >= cols) {
                    cursorCol = 0
                    if (cursorRow + 1 >= rows) {
                        scrollUp(cursorRow + 1 - rows + 1)
                        cursorRow = rows - 1
                    } else {
                        cursorRow++
                    }
                }
            }
        }
    }

    fun appendText(text: String) {
        text.forEach { putChar(it) }
    }

    fun scrollUp(n: Int, top: Int = topMargin, bottom: Int = bottomMargin) {
        val count = n.coerceIn(1, (bottom - top + 1).coerceAtLeast(1))
        for (r in top until bottom - count + 1) {
            for (c in 0 until cols) cells[r][c] = cells[r + count][c]
        }
        for (r in (bottom - count + 1)..bottom) {
            for (c in 0 until cols) cells[r][c] = TerminalCell()
        }
    }

    fun scrollDown(n: Int, top: Int = topMargin, bottom: Int = bottomMargin) {
        val count = n.coerceIn(1, (bottom - top + 1).coerceAtLeast(1))
        for (r in bottom downTo top + count) {
            for (c in 0 until cols) cells[r][c] = cells[r - count][c]
        }
        for (r in top until top + count) {
            for (c in 0 until cols) cells[r][c] = TerminalCell()
        }
    }

    fun cursorUp(n: Int) { cursorRow = (cursorRow - n.coerceAtLeast(1)).coerceAtLeast(0) }
    fun cursorDown(n: Int) { cursorRow = (cursorRow + n.coerceAtLeast(1)).coerceAtMost(rows - 1) }
    fun cursorForward(n: Int) { cursorCol = (cursorCol + n.coerceAtLeast(1)).coerceAtMost(cols - 1) }
    fun cursorBack(n: Int) { cursorCol = (cursorCol - n.coerceAtLeast(1)).coerceAtLeast(0) }
    fun cursorPosition(row: Int, col: Int) {
        cursorRow = (row - 1).coerceIn(0, rows - 1)
        cursorCol = (col - 1).coerceIn(0, cols - 1)
    }
    fun cursorColumn(col: Int) { cursorCol = (col - 1).coerceIn(0, cols - 1) }
    fun verticalPosition(row: Int) { cursorRow = (row - 1).coerceIn(0, rows - 1) }
    fun nextLine(n: Int) { cursorDown(n); cursorCol = 0 }
    fun prevLine(n: Int) { cursorUp(n); cursorCol = 0 }

    fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                for (c in cursorCol until cols) cells[cursorRow][c] = TerminalCell()
                for (r in cursorRow + 1 until rows) for (c in 0 until cols) cells[r][c] = TerminalCell()
            }
            1 -> {
                for (c in 0..cursorCol) cells[cursorRow][c] = TerminalCell()
                for (r in 0 until cursorRow) for (c in 0 until cols) cells[r][c] = TerminalCell()
            }
            else -> clear()
        }
    }

    fun eraseLine(mode: Int) {
        when (mode) {
            0 -> for (c in cursorCol until cols) cells[cursorRow][c] = TerminalCell()
            1 -> for (c in 0..cursorCol) cells[cursorRow][c] = TerminalCell()
            else -> for (c in 0 until cols) cells[cursorRow][c] = TerminalCell()
        }
    }

    fun eraseChars(n: Int) {
        for (i in 0 until n) { val c = cursorCol + i; if (c < cols) cells[cursorRow][c] = TerminalCell() }
    }

    fun deleteChars(n: Int) {
        val count = n.coerceIn(1, cols - cursorCol)
        for (c in cursorCol until cols - count) cells[cursorRow][c] = cells[cursorRow][c + count]
        for (c in (cols - count) until cols) cells[cursorRow][c] = TerminalCell()
    }

    fun insertChars(n: Int) {
        val count = n.coerceIn(1, cols - cursorCol)
        for (c in (cols - 1) downTo (cursorCol + count)) cells[cursorRow][c] = cells[cursorRow][c - count]
        for (c in cursorCol until (cursorCol + count).coerceAtMost(cols)) cells[cursorRow][c] = TerminalCell()
    }

    fun deleteLines(n: Int) {
        val count = n.coerceIn(1, bottomMargin - cursorRow + 1)
        for (r in cursorRow until bottomMargin - count + 1) {
            for (c in 0 until cols) cells[r][c] = cells[r + count][c]
        }
        for (r in (bottomMargin - count + 1)..bottomMargin) {
            for (c in 0 until cols) cells[r][c] = TerminalCell()
        }
    }

    fun insertLines(n: Int) {
        val count = n.coerceIn(1, bottomMargin - cursorRow + 1)
        for (r in bottomMargin downTo cursorRow + count) {
            for (c in 0 until cols) cells[r][c] = cells[r - count][c]
        }
        for (r in cursorRow until cursorRow + count) {
            for (c in 0 until cols) cells[r][c] = TerminalCell()
        }
    }

    fun saveCursor() { savedRow = cursorRow; savedCol = cursorCol }
    fun restoreCursor() { cursorRow = savedRow; cursorCol = savedCol }

    fun applySgr(codes: List<Int>) {
        if (codes.isEmpty()) { fg = -1; bold = false; return }
        for (code in codes) {
            when (code) {
                0 -> { fg = -1; bold = false }
                1 -> bold = true
                22 -> bold = false
                in 30..37 -> fg = code
                39 -> fg = -1
                in 90..97 -> fg = code
            }
        }
    }

    fun render(defaultColor: Color): AnnotatedString {
        // feed() runs on a background thread while render() is called from the
        // UI thread, so clamp defensively against any out-of-range transient
        // state (would otherwise surface as "Index 400 out of bounds...").
        val curRow = cursorRow.coerceIn(0, rows - 1)
        val curCol = cursorCol.coerceIn(0, cols - 1)
        var lastRow = rows - 1
        while (lastRow >= 0 && !cells[lastRow].any { it.char != ' ' }) lastRow--
        // Never trim the cursor row away, even if it's blank.
        val renderLastRow = maxOf(lastRow, curRow)

        return buildAnnotatedString {
            for (r in 0..renderLastRow) {
                var lastCol = -1
                for (c in 0 until cols) if (cells[r][c].char != ' ') lastCol = c
                if (r == curRow) lastCol = maxOf(lastCol, curCol)

                var currentFg = Int.MIN_VALUE
                var currentBold = false
                var hasStyle = false

                for (c in 0..lastCol) {
                    val cell = cells[r][c]
                    val isCursorCell = r == curRow && c == cursorCol
                    if (isCursorCell) {
                        if (hasStyle) { pop(); hasStyle = false }
                        // Reverse video: background = the cell's text color.
                        val cursorBg = if (cell.fg == -1) defaultColor else colorFor(cell.fg, defaultColor)
                        if (cell.char == ' ') {
                            // Solid block cursor over a blank cell.
                            pushStyle(SpanStyle(color = cursorBg))
                            append('█')
                        } else {
                            // Real character: keep it visible with inverted colors.
                            pushStyle(SpanStyle(color = cursorTextColor, background = cursorBg))
                            append(cell.char)
                        }
                        pop()
                        currentFg = Int.MIN_VALUE
                        currentBold = false
                        continue
                    }
                    if (!hasStyle || cell.fg != currentFg || cell.bold != currentBold) {
                        if (hasStyle) pop()
                        val color = if (cell.fg == -1) defaultColor else colorFor(cell.fg, defaultColor)
                        pushStyle(SpanStyle(color = color, fontWeight = if (cell.bold) FontWeight.Bold else FontWeight.Normal))
                        currentFg = cell.fg
                        currentBold = cell.bold
                        hasStyle = true
                    }
                    append(cell.char)
                }
                if (hasStyle) pop()
                if (r < renderLastRow) append('\n')
            }
        }
    }

    private fun colorFor(code: Int, defaultColor: Color): Color = when (code) {
        30 -> Color.Black
        31 -> Color.Red
        32 -> Color(0xFF00FF00)
        33 -> Color.Yellow
        34 -> Color.Blue
        35 -> Color.Magenta
        36 -> Color.Cyan
        37 -> Color.White
        90 -> Color.DarkGray
        91 -> Color(0xFFFF5555)
        92 -> Color(0xFF55FF55)
        93 -> Color(0xFFFFFF55)
        94 -> Color(0xFF5555FF)
        95 -> Color(0xFFFF55FF)
        96 -> Color(0xFF55FFFF)
        97 -> Color.White
        else -> defaultColor
    }
}

class TerminalScreen(
    initialRows: Int = 30, // Default to a reasonable PTY height
    initialCols: Int = 100
) {
    private var ptyRows = initialRows
    private var ptyCols = initialCols
    
    private val mainBuffer = ScreenBuffer(TerminalDimensions.ROWS, TerminalDimensions.COLS)
    private val altBuffer = ScreenBuffer(TerminalDimensions.ROWS, TerminalDimensions.COLS)
    
    init {
        mainBuffer.rows = TerminalDimensions.ROWS
        mainBuffer.cols = initialCols
        altBuffer.rows = initialRows
        altBuffer.cols = initialCols
    }
    
    fun resize(newRows: Int, newCols: Int) {
        ptyRows = newRows
        ptyCols = newCols
        
        mainBuffer.cols = newCols.coerceAtMost(TerminalDimensions.COLS)
        altBuffer.rows = newRows.coerceAtMost(TerminalDimensions.ROWS)
        altBuffer.cols = newCols.coerceAtMost(TerminalDimensions.COLS)
        
        // Reset margins to full screen on resize
        mainBuffer.topMargin = 0
        mainBuffer.bottomMargin = mainBuffer.rows - 1
        altBuffer.topMargin = 0
        altBuffer.bottomMargin = altBuffer.rows - 1
    }
    private var useAlt = false

    /** True while a full-screen app (nano/vim/htop) owns the screen via the
     *  alternate buffer. Auto-scrolling must be disabled in this state — the
     *  app redraws the whole screen and scroll jumping looks like the
     *  terminal is being recreated. */
    val isFullScreen: Boolean get() = useAlt

    private val current get() = if (useAlt) altBuffer else mainBuffer

    val cursorRow get() = current.cursorRow
    val cursorCol get() = current.cursorCol

    // Holds bytes/sequences that arrived split across TCP reads.
    private var pending = ""

    /**
     * Feeds raw terminal data. Handles partial escape sequences that may be
     * split across reads.
     */
    fun feed(data: String) {
        val input = pending + data
        pending = ""
        var i = 0
        val n = input.length

        while (i < n) {
            val ch = input[i]
            when {
                ch == '\u001b' -> {
                    if (i + 1 >= n) { pending = input.substring(i); return }
                    val next = input[i + 1]
                    when (next) {
                        '[' -> { // CSI
                            var j = i + 2
                            while (j < n && (input[j] == '?' || input[j] == '>' || input[j] == '<' || input[j] == '=' ||
                                    input[j] == ';' || input[j] in '0'..'9')) j++
                            if (j >= n) { pending = input.substring(i); return }
                            val final = input[j]
                            handleCsi(input.substring(i + 2, j), final)
                            i = j + 1
                        }
                        ']' -> { // OSC — skip until BEL or ST
                            var j = i + 2
                            var terminated = false
                            while (j < n) {
                                if (input[j] == '\u0007') { j++; terminated = true; break }
                                if (input[j] == '\u001b' && j + 1 < n && input[j + 1] == '\\') { j += 2; terminated = true; break }
                                j++
                            }
                            if (!terminated) { pending = input.substring(i); return }
                            i = j
                        }
                        '7' -> { current.saveCursor(); i += 2 }
                        '8' -> { current.restoreCursor(); i += 2 }
                        '(', ')' -> { i += 3 } // character set — skip
                        '=', '>' -> i += 2   // keypad mode
                        'D' -> { current.scrollUp(1, current.topMargin, current.bottomMargin); i += 2 }   // IND
                        'M' -> { current.scrollDown(1, current.topMargin, current.bottomMargin); i += 2 } // RI
                        'E' -> { current.nextLine(1); i += 2 }
                        'c' -> { current.clear(); i += 2 } // RIS
                        else -> i += 2 // unknown — drop
                    }
                }
                else -> {
                    current.putChar(ch)
                    i++
                }
            }
        }
    }

    private fun handleCsi(paramsStr: String, final: Char) {
        val private = paramsStr.startsWith("?")
        val clean = paramsStr.removePrefix("?")
        val parts = clean.split(';').mapNotNull { it.toIntOrNull() }
        val p = parts.getOrElse(0) { 1 }
        val buf = current

        when (final) {
            'A' -> buf.cursorUp(p)
            'B' -> buf.cursorDown(p)
            'C' -> buf.cursorForward(p)
            'D' -> buf.cursorBack(p)
            'E' -> buf.nextLine(p)
            'F' -> buf.prevLine(p)
            'G' -> buf.cursorColumn(p)
            'H', 'f' -> if (parts.size >= 2) buf.cursorPosition(parts[0], parts[1]) else buf.cursorPosition(1, 1)
            'J' -> buf.eraseDisplay(parts.getOrElse(0) { 0 })
            'K' -> buf.eraseLine(parts.getOrElse(0) { 0 })
            'X' -> buf.eraseChars(p)
            'P' -> buf.deleteChars(p)
            '@' -> buf.insertChars(p)
            'L' -> buf.insertLines(p)
            'M' -> buf.deleteLines(p)
            'S' -> buf.scrollUp(p)
            'T' -> buf.scrollDown(p)
            'm' -> buf.applySgr(parts)
            'r' -> {
                val top = parts.getOrElse(0) { 1 }
                val bottom = parts.getOrElse(1) { buf.rows }
                buf.topMargin = (top - 1).coerceIn(0, buf.rows - 1)
                buf.bottomMargin = (bottom - 1).coerceIn(buf.topMargin, buf.rows - 1)
                buf.cursorPosition(1, 1)
            }
            'd' -> buf.verticalPosition(p)
            's' -> buf.saveCursor()
            'u' -> buf.restoreCursor()
            'h' -> if (private && (p == 1049 || p == 1047)) enterAlt()
            'l' -> if (private && (p == 1049 || p == 1047)) leaveAlt()
        }
    }

    private fun enterAlt() {
        if (useAlt) return
        useAlt = true
        altBuffer.clear()
    }

    private fun leaveAlt() {
        if (!useAlt) return
        useAlt = false
    }

    fun clear() {
        mainBuffer.clear()
        altBuffer.clear()
        pending = ""
    }

    fun appendExternal(text: String) {
        current.appendText(text)
    }

    fun render(defaultColor: Color): AnnotatedString = current.render(defaultColor)
}
