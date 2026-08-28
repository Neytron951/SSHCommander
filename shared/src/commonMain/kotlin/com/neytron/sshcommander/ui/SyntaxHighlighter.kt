package com.neytron.sshcommander.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SyntaxHighlighter(val extension: String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlight(text.text, extension),
            OffsetMapping.Identity
        )
    }

    private fun highlight(text: String, ext: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            // Define common colors
            val keywordColor = Color(0xFFD55E00) // Orange
            val stringColor = Color(0xFF009E73)  // Green
            val commentColor = Color(0xFF999999) // Gray
            val numberColor = Color(0xFF0072B2)  // Blue
            val functionColor = Color(0xFFCC79A7) // Pink

            val patterns = mutableListOf<Pair<Regex, SpanStyle>>()

            // Strings (Common)
            patterns.add(Regex("\"\"\"[\\s\\S]*?\"\"\"|\"\"[\\s\\S]*?\"\"|\"[^\"]*\"|'[^']*'") to SpanStyle(color = stringColor))
            
            // Comments
            when (ext) {
                "json" -> {} // No standard comments in JSON
                else -> patterns.add(Regex("#.*") to SpanStyle(color = commentColor))
            }

            // Numbers
            patterns.add(Regex("\\b\\d+\\b") to SpanStyle(color = numberColor))

            // Keywords by language
            val keywords = when (ext) {
                "sh", "bash" -> listOf("if", "then", "else", "fi", "for", "in", "do", "done", "exit", "return", "sudo", "echo")
                "py" -> listOf("def", "class", "if", "else", "elif", "for", "while", "import", "from", "as", "return", "try", "except", "with")
                "yml", "yaml", "conf" -> listOf("true", "false", "yes", "no", "null")
                else -> emptyList()
            }

            if (keywords.isNotEmpty()) {
                patterns.add(Regex("\\b(${keywords.joinToString("|")})\\b") to SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold))
            }

            // Shell specific variables
            if (ext == "sh" || ext == "bash") {
                patterns.add(Regex("\\$\\w+|\\\$\\{[^}]+\\}") to SpanStyle(color = functionColor))
            }

            // Apply patterns
            patterns.forEach { (regex, style) ->
                regex.findAll(text).forEach { match ->
                    addStyle(style, match.range.first, match.range.last + 1)
                }
            }
        }
    }
}
