package com.semseytech.rtsdevicesuitepro.terminal.emulator

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

class TerminalEmulator {
    private val maxLines = 1000
    val lines = mutableStateListOf<AnnotatedString>()
    
    private var currentLine = StringBuilder()
    private var currentColor = Color.White
    private var currentBackground = Color.Transparent
    private var isBold = false

    init {
        lines.add(buildAnnotatedString { append("") })
    }

    fun appendText(text: String) {
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (char == '\u001b' && i + 1 < text.length && text[i+1] == '[') {
                val end = text.indexOf('m', i)
                if (end != -1) {
                    val code = text.substring(i + 2, end)
                    parseAnsiCode(code)
                    i = end + 1
                    continue
                }
            }

            when (char) {
                '\n' -> flushLine()
                '\r' -> {
                    // Simple carriage return: clear current line buffer
                    currentLine.setLength(0)
                }
                '\b' -> {
                    if (currentLine.isNotEmpty()) {
                        currentLine.setLength(currentLine.length - 1)
                    }
                }
                else -> {
                    currentLine.append(char)
                }
            }
            i++
        }
        updateLastLine()
    }

    private fun parseAnsiCode(code: String) {
        val parts = code.split(';')
        for (part in parts) {
            when (part) {
                "0" -> {
                    currentColor = Color.White
                    currentBackground = Color.Transparent
                    isBold = false
                }
                "1" -> isBold = true
                "30" -> currentColor = Color.Black
                "31" -> currentColor = Color(0xFFEF4444) // Improved Red
                "32" -> currentColor = Color(0xFF22C55E) // Improved Green
                "33" -> currentColor = Color(0xFFEAB308) // Improved Yellow
                "34" -> currentColor = Color(0xFF3B82F6) // Improved Blue
                "35" -> currentColor = Color(0xFFA855F7) // Improved Magenta
                "36" -> currentColor = Color(0xFF06B6D4) // Improved Cyan
                "37" -> currentColor = Color.White
                "90" -> currentColor = Color.Gray
                "40" -> currentBackground = Color.Black
                "41" -> currentBackground = Color.Red
                "42" -> currentBackground = Color.Green
                // Add more as needed
            }
        }
    }

    fun clear() {
        lines.clear()
        currentLine.setLength(0)
        lines.add(buildAnnotatedString { append("") })
    }

    private fun flushLine() {
        updateLastLine()
        if (lines.size >= maxLines) {
            lines.removeAt(0)
        }
        lines.add(buildAnnotatedString { append("") })
        currentLine = StringBuilder()
    }

    private fun updateLastLine() {
        if (lines.isNotEmpty()) {
            val index = lines.size - 1
            lines[index] = buildAnnotatedString {
                withStyle(SpanStyle(
                    color = currentColor,
                    background = currentBackground,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                )) {
                    append(currentLine.toString())
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.withStyle(style: SpanStyle, block: AnnotatedString.Builder.() -> Unit) {
    val start = length
    block()
    addStyle(style, start, length)
}
