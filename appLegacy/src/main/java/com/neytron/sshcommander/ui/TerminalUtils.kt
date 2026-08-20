package com.neytron.sshcommander.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object TerminalUtils {
    /**
     * Converts raw SSH output with ANSI escape codes into a Compose AnnotatedString.
     * Supports basic 8/16 colors and bold text.
     */
    fun parseAnsi(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            // Baseline style for the entire string
            pushStyle(SpanStyle(color = defaultColor))
            
            val parts = text.split("\u001B[")
            
            // First part before any escape code
            append(parts[0])

            var currentColor = defaultColor
            var currentWeight = FontWeight.Normal

            for (i in 1 until parts.size) {
                val part = parts[i]
                // Regex to find color codes like "1;32m" or "0m" or "31m"
                val match = Regex("^([0-9;]*)m").find(part)
                
                if (match != null) {
                    val codes = match.groupValues[1].split(";").filter { it.isNotEmpty() }.map { it.toInt() }
                    val content = part.substring(match.range.last + 1)
                    
                    codes.forEach { code ->
                        when (code) {
                            0 -> { // Reset to default
                                currentColor = defaultColor
                                currentWeight = FontWeight.Normal
                            }
                            1 -> currentWeight = FontWeight.Bold
                            22 -> currentWeight = FontWeight.Normal 
                            30 -> currentColor = Color.Black
                            31 -> currentColor = Color.Red
                            32 -> currentColor = Color(0xFF00FF00) // Green
                            33 -> currentColor = Color.Yellow
                            34 -> currentColor = Color.Blue
                            35 -> currentColor = Color.Magenta
                            36 -> currentColor = Color.Cyan
                            37 -> currentColor = Color.White
                            39 -> currentColor = defaultColor // Default foreground
                            90 -> currentColor = Color.DarkGray
                            91 -> currentColor = Color(0xFFFF5555) // Bright Red
                            92 -> currentColor = Color(0xFF55FF55) // Bright Green
                            93 -> currentColor = Color(0xFFFFFF55) // Bright Yellow
                            94 -> currentColor = Color(0xFF5555FF) // Bright Blue
                            95 -> currentColor = Color(0xFFFF55FF) // Bright Magenta
                            96 -> currentColor = Color(0xFF55FFFF) // Bright Cyan
                            97 -> currentColor = Color.White
                        }
                    }
                    
                    pushStyle(SpanStyle(color = currentColor, fontWeight = currentWeight))
                    append(content)
                    pop()
                } else {
                    // It's an ANSI code but not a color code (like cursor movement K, J, H, etc.)
                    // We strip the code sequence but keep the following text.
                    // The sequence ends with a letter.
                    val endOfCode = part.indexOfFirst { it.isLetter() }
                    if (endOfCode != -1) {
                        append(part.substring(endOfCode + 1))
                    } else {
                        // Fallback: if no letter found, just append raw (rare)
                        append(part)
                    }
                }
            }
            pop() // Pop baseline
        }
    }
}
