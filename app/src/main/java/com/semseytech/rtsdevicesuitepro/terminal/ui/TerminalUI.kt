package com.semseytech.rtsdevicesuitepro.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.terminal.core.TerminalSession
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun TerminalUI(session: TerminalSession) {
    var output by remember { mutableStateOf(listOf<String>()) }
    var input by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(session) {
        session.outputFlow.collect { text ->
            // Simple logic to handle carriage return \r (basic)
            if (text.startsWith("\r")) {
                output = output.dropLast(1) + text.substring(1).split("\n")
            } else {
                output = output + text.split("\n")
            }
            
            if (output.size > 500) output = output.takeLast(500) // History limit
            
            scope.launch {
                if (output.isNotEmpty()) {
                    scrollState.animateScrollToItem(output.size - 1)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
            state = scrollState,
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(output) { line ->
                TerminalLine(line)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color.Green),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotEmpty()) {
                        session.write(input + "\n")
                        input = ""
                    }
                })
            )
        }
    }
}

@Composable
fun TerminalLine(text: String) {
    // Basic ANSI color support
    val parts = remember(text) { parseAnsi(text) }
    
    Row(modifier = Modifier.fillMaxWidth()) {
        parts.forEach { part ->
            Text(
                text = part.text,
                color = part.color,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}

data class AnsiPart(val text: String, val color: Color)

fun parseAnsi(text: String): List<AnsiPart> {
    val result = mutableListOf<AnsiPart>()
    val regex = Regex("\u001b\\[([0-9;]*)m")
    var lastIndex = 0
    var currentColor = Color.White
    
    regex.findAll(text).forEach { match ->
        val before = text.substring(lastIndex, match.range.first)
        if (before.isNotEmpty()) result.add(AnsiPart(before, currentColor))
        
        val code = match.groupValues[1]
        currentColor = when (code) {
            "0" -> Color.White
            "31" -> Color.Red
            "32" -> Color.Green
            "33" -> Color.Yellow
            "34" -> Color.Blue
            "35" -> Color.Magenta
            "36" -> Color.Cyan
            "1;31" -> Color.Red // Bold Red
            "1;32" -> Color.Green // Bold Green
            "1;34" -> Color(0xFF5C5CFF) // Light Blue
            "1;36" -> Color.Cyan // Bold Cyan
            else -> currentColor
        }
        lastIndex = match.range.last + 1
    }
    
    val remaining = text.substring(lastIndex)
    if (remaining.isNotEmpty()) result.add(AnsiPart(remaining, currentColor))
    
    return if (result.isEmpty()) listOf(AnsiPart(text, Color.White)) else result
}
