package com.semseytech.rtsdevicesuitepro.editor

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.event.ContentChangeEvent
// import io.github.rosemoe.sora.lang.html.HTMLLanguage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    path: String,
    viewModel: TextEditorViewModel,
    onBack: () -> Unit
) {
    val theme = LocalTheme.current
    val file = remember(path) { File(path) }
    val extension = file.extension.lowercase()

    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }

    LaunchedEffect(path) {
        viewModel.loadFile(path)
    }

    DisposableEffect(Unit) {
        onDispose {
            editorInstance?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(file.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(path, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (viewModel.isDirty) {
                        TextButton(onClick = { 
                            editorInstance?.let { viewModel.content = it.text.toString() }
                            viewModel.saveFile() 
                        }) {
                            Text("SAVE", color = theme.accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.startColor)
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        setTextSize(14f)
                        typefaceText = Typeface.MONOSPACE
                        isLineNumberEnabled = true
                        isWordwrap = false
                        
                        colorScheme.apply {
                            setColor(EditorColorScheme.TEXT_NORMAL, Color.White.toArgb())
                            setColor(EditorColorScheme.LINE_NUMBER, Color(0xFF4A4A4A).toArgb())
                            setColor(EditorColorScheme.LINE_NUMBER_PANEL, Color(0xFF1A1A1A).toArgb())
                            setColor(EditorColorScheme.WHOLE_BACKGROUND, Color(0xFF0A0A0A).toArgb())
                            setColor(EditorColorScheme.SELECTION_INSERT, theme.accentColor.toArgb())
                            setColor(EditorColorScheme.SELECTION_HANDLE, theme.accentColor.toArgb())
                        }

                        getComponent(EditorAutoCompletion::class.java).isEnabled = true
                        props.deleteMultiSpaces = 4
                        
                        // TODO: Migrate to TextMateLanguage for HTML/XML support
                        // The legacy language-html module is not available for sora-editor 0.23.6
                        /*
                        if (extension == "html" || extension == "htm" || extension == "xml") {
                            setEditorLanguage(HTMLLanguage())
                        }
                        */
                        
                        subscribeAlways(ContentChangeEvent::class.java) { _ ->
                            if (!viewModel.isLoading) {
                                viewModel.isDirty = true
                            }
                        }

                        editorInstance = this
                    }
                },
                update = { editor ->
                    if (!viewModel.isLoading && editor.text.toString() != viewModel.content) {
                        editor.setText(viewModel.content)
                    }
                }
            )
            
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    color = theme.accentColor
                )
            }
        }
    }
}
