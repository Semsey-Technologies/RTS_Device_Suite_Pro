package com.semseytech.rtsdevicesuitepro.viewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import com.semseytech.rtsdevicesuitepro.backup.model.*
import com.semseytech.rtsdevicesuitepro.restore.engine.ArchiveReader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ViewerInterface(val ctx: Context, val onBack: () -> Unit, val onReload: () -> Unit, val onExport: () -> Unit, val onImport: () -> Unit) {
    private val gson = Gson()
    private val viewerDir = File(ctx.filesDir, "viewer_data")
    private val dataDir = File(viewerDir, "data")

    @JavascriptInterface
    fun exitViewer() {
        (ctx as? android.app.Activity)?.runOnUiThread {
            onBack()
        }
    }

    @JavascriptInterface
    fun importBackup() {
        (ctx as? android.app.Activity)?.runOnUiThread {
            onImport()
        }
    }

    @JavascriptInterface
    fun deleteItem(category: String, identifier: String) {
        performDelete(category, identifier)
        onReload()
    }

    @JavascriptInterface
    fun deleteItems(category: String, identifiersJson: String) {
        try {
            val type = object : TypeToken<List<String>>() {}.type
            val ids: List<String> = gson.fromJson(identifiersJson, type)
            ids.forEach { performDelete(category, it) }
            onReload()
        } catch (e: Exception) {
            android.util.Log.e("ViewerInterface", "Error deleting items", e)
        }
    }

    private fun performDelete(category: String, identifier: String) {
        try {
            when (category.lowercase()) {
                "sms", "messages" -> {
                    val indexFile = File(dataDir, "index.json")
                    if (indexFile.exists()) {
                        val type = object : TypeToken<MutableList<com.semseytech.rtsdevicesuitepro.sms.logic.SmsExtractor.SmsThread>>() {}.type
                        val threads: MutableList<com.semseytech.rtsdevicesuitepro.sms.logic.SmsExtractor.SmsThread> = gson.fromJson(indexFile.readText(), type)
                        threads.removeAll { it.thread_id == identifier }
                        indexFile.writeText(gson.toJson(threads))
                        
                        File(dataDir, "threads/$identifier.json").delete()
                        File(dataDir, "mms/$identifier").deleteRecursively()
                    }
                }
                "calls" -> {
                    val file = File(dataDir, "calls.json")
                    if (file.exists()) {
                        val type = object : TypeToken<MutableList<BackupItem.CallLogEntry>>() {}.type
                        val items: MutableList<BackupItem.CallLogEntry> = gson.fromJson(file.readText(), type)
                        items.removeAll { it.id == identifier }
                        file.writeText(gson.toJson(items))
                    }
                }
                "contacts" -> {
                    val file = File(dataDir, "contacts.json")
                    if (file.exists()) {
                        val type = object : TypeToken<MutableList<BackupItem.Contact>>() {}.type
                        val items: MutableList<BackupItem.Contact> = gson.fromJson(file.readText(), type)
                        items.removeAll { it.id == identifier }
                        file.writeText(gson.toJson(items))
                    }
                }
                else -> {
                    // Handle generic files from manifest
                    val manifestFile = File(viewerDir, "manifest.json")
                    if (manifestFile.exists()) {
                        val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
                        val entry = manifest.entries.find { it.identifier == identifier }
                        entry?.let {
                            File(viewerDir, it.filePath ?: "").delete()
                            val newEntries = manifest.entries.filter { it.identifier != identifier }
                            manifestFile.writeText(gson.toJson(manifest.copy(entries = newEntries)))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ViewerInterface", "Error in performDelete", e)
        }
    }

    @JavascriptInterface
    fun deleteCategory(category: String) {
        performDeleteCategory(category)
        onReload()
    }

    @JavascriptInterface
    fun deleteCategories(categoriesJson: String) {
        try {
            val type = object : TypeToken<List<String>>() {}.type
            val categories: List<String> = gson.fromJson(categoriesJson, type)
            categories.forEach { performDeleteCategory(it) }
            onReload()
        } catch (e: Exception) {
            android.util.Log.e("ViewerInterface", "Error deleting categories", e)
        }
    }

    private fun performDeleteCategory(category: String) {
        try {
            when (category.lowercase()) {
                "sms", "messages" -> {
                    File(dataDir, "index.json").delete()
                    File(dataDir, "threads").deleteRecursively()
                    File(dataDir, "mms").deleteRecursively()
                }
                "calls" -> File(dataDir, "calls.json").delete()
                "contacts" -> File(dataDir, "contacts.json").delete()
                "images", "pictures" -> deleteManifestCategory("Pictures")
                "videos" -> deleteManifestCategory("Videos")
                "files" -> deleteManifestCategory("UserFile")
                "apks" -> deleteManifestCategory("Apk")
            }
        } catch (e: Exception) {
            android.util.Log.e("ViewerInterface", "Error in performDeleteCategory", e)
        }
    }

    private fun deleteManifestCategory(category: String) {
        val manifestFile = File(viewerDir, "manifest.json")
        if (manifestFile.exists()) {
            val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
            val toDelete = if (category == "UserFile" || category == "Apk") {
                manifest.entries.filter { it.itemType == category }
            } else {
                manifest.entries.filter { it.category == category }
            }
            
            toDelete.forEach { File(viewerDir, it.filePath ?: "").delete() }
            val newEntries = manifest.entries - toDelete.toSet()
            manifestFile.writeText(gson.toJson(manifest.copy(entries = newEntries)))
        }
    }

    @JavascriptInterface
    fun exportArchive() {
        (ctx as? android.app.Activity)?.runOnUiThread {
            onExport()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBackupsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale
    val scope = rememberCoroutineScope()

    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var exportStatus by remember { mutableStateOf("") }
    
    var importProgress by remember { mutableStateOf<Float?>(null) }
    var importStatus by remember { mutableStateOf("") }
    var showImportOptions by remember { mutableStateOf<Uri?>(null) }
    var importMode by remember { mutableStateOf(ViewerImporter.ImportMode.ADD_TO_EXISTING) }
    var showCategorySelection by remember { mutableStateOf<List<String>?>(null) }

    var webView by remember { mutableStateOf<WebView?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                exportProgress = 0.01f
                exportStatus = "Initializing export..."
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ViewerExporter.exportViewer(context, outputStream) { progress, status ->
                                scope.launch {
                                    exportProgress = progress
                                    exportStatus = status
                                }
                            }
                        } ?: false
                    } catch (e: Exception) {
                        android.util.Log.e("ViewBackupsScreen", "Export failed", e)
                        false
                    }
                }
                
                if (!success) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                }
                exportProgress = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            showImportOptions = it
        }
    }

    if (showImportOptions != null) {
        AlertDialog(
            onDismissRequest = { showImportOptions = null },
            title = { Text("Import Backup", color = currentTheme.accentColor) },
            text = {
                Column {
                    Text("Select how you want to import this backup:", color = currentTheme.textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = importMode == ViewerImporter.ImportMode.ADD_TO_EXISTING,
                            onClick = { importMode = ViewerImporter.ImportMode.ADD_TO_EXISTING },
                            colors = RadioButtonDefaults.colors(selectedColor = currentTheme.accentColor)
                        )
                        Text("Add to existing data", color = currentTheme.textColor, modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = importMode == ViewerImporter.ImportMode.REPLACE_CATEGORIES,
                            onClick = { importMode = ViewerImporter.ImportMode.REPLACE_CATEGORIES },
                            colors = RadioButtonDefaults.colors(selectedColor = currentTheme.accentColor)
                        )
                        Text("Replace selected categories", color = currentTheme.textColor, modifier = Modifier.padding(start = 8.dp))
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = importMode == ViewerImporter.ImportMode.REPLACE_EVERYTHING,
                            onClick = { importMode = ViewerImporter.ImportMode.REPLACE_EVERYTHING },
                            colors = RadioButtonDefaults.colors(selectedColor = currentTheme.accentColor)
                        )
                        Text("Replace everything", color = currentTheme.textColor, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = showImportOptions ?: return@TextButton
                    // Don't null it yet if we need it for category selection
                    if (importMode != ViewerImporter.ImportMode.REPLACE_CATEGORIES) {
                        showImportOptions = null
                    }
                    
                    if (importMode == ViewerImporter.ImportMode.REPLACE_CATEGORIES) {
                        scope.launch {
                            importStatus = "Analyzing backup..."
                            // Quick scan for categories
                            val categories = withContext(Dispatchers.IO) {
                                try {
                                    val tempFile = File(context.cacheDir, "scan_temp.zip")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        tempFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                    val manifestFile = File(context.cacheDir, "scan_manifest.json")
                                    ArchiveReader(tempFile).extractFile("manifest.json", manifestFile)
                                    if (!manifestFile.exists()) ArchiveReader(tempFile).extractFile("backup.json", manifestFile)
                                    val manifest = Gson().fromJson(manifestFile.readText(), BackupManifest::class.java)
                                    tempFile.delete()
                                    manifestFile.delete()
                                    manifest.categories
                                } catch (e: Exception) { emptyList<String>() }
                            }
                            showCategorySelection = categories
                            if (categories.isEmpty()) {
                                showImportOptions = null
                                Toast.makeText(context, "No categories found in backup", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        scope.launch {
                            importProgress = 0.01f
                            importStatus = "Starting import..."
                            val success = ViewerImporter.importBackup(context, uri, importMode) { progress, status ->
                                importProgress = progress
                                importStatus = status
                            }
                            
                            if (success) {
                                Toast.makeText(context, "Backup imported successfully", Toast.LENGTH_SHORT).show()
                                webView?.reload()
                            } else {
                                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                            }
                            importProgress = null
                        }
                    }
                }) {
                    Text("CONTINUE", color = currentTheme.accentColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportOptions = null }) {
                    Text("CANCEL", color = currentTheme.textColor.copy(alpha = 0.7f))
                }
            },
            containerColor = currentTheme.startColor
        )
    }

    showCategorySelection?.let { categories ->
        ImportCategoryDialog(
            categories = categories,
            onDismiss = { showCategorySelection = null },
            onConfirm = { selected ->
                val uri = showImportOptions ?: return@ImportCategoryDialog // Safety check
                showCategorySelection = null
                scope.launch {
                    importProgress = 0.01f
                    importStatus = "Starting selective import..."
                    val success = ViewerImporter.importBackup(context, uri, importMode, selected) { progress, status ->
                        importProgress = progress
                        importStatus = status
                    }
                    if (success) {
                        Toast.makeText(context, "Categories replaced successfully", Toast.LENGTH_SHORT).show()
                        webView?.reload()
                    } else {
                        Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                    }
                    importProgress = null
                }
            },
            theme = currentTheme
        )
    }

    if (exportProgress != null || importProgress != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (exportProgress != null) "Exporting Backup" else "Importing Backup", color = currentTheme.accentColor) },
            text = {
                Column {
                    Text(if (exportProgress != null) exportStatus else importStatus, color = currentTheme.textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { (exportProgress ?: importProgress) ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = currentTheme.accentColor,
                        trackColor = currentTheme.accentColor.copy(alpha = 0.2f)
                    )
                }
            },
            confirmButton = {},
            containerColor = currentTheme.startColor
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        currentTheme.startColor,
                        currentTheme.endColor
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
            .drawBehind {
                if (currentTheme.hasGridOverlay) {
                    val gridSize = (40.dp * scale).toPx()
                    for (y in 0..size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                    for (x in 0..size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = currentTheme.accentColor.copy(alpha = 0.05f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain("appassets.androidplatform.net")
                    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                    .addPathHandler("/data/", WebViewAssetLoader.InternalStoragePathHandler(ctx, File(ctx.filesDir, "viewer_data/data")))
                    .addPathHandler("/files/", WebViewAssetLoader.InternalStoragePathHandler(ctx, File(ctx.filesDir, "viewer_data/files")))
                    .addPathHandler("/internal/", WebViewAssetLoader.InternalStoragePathHandler(ctx, File(ctx.filesDir, "viewer_data")))
                    .build()

                WebView(ctx).apply {
                    webView = this
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(0)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.databaseEnabled = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                    addJavascriptInterface(ViewerInterface(ctx, onBack, {
                        post { reload() }
                    }, {
                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        createDocumentLauncher.launch("RTS_Viewer_Export_$timestamp.zip")
                    }, {
                        importLauncher.launch("*/*")
                    }), "Android")
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            android.util.Log.d("WebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest
                        ): WebResourceResponse? {
                            return assetLoader.shouldInterceptRequest(request.url)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("ViewBackups", "Page loaded: $url")
                        }
                    }
                    
                    // Pass theme colors to the viewer via URL query parameters
                    val accentHex = java.net.URLEncoder.encode(String.format("#%06X", (0xFFFFFF and currentTheme.accentColor.toArgb())), "UTF-8")
                    val bgHex = java.net.URLEncoder.encode(String.format("#%06X", (0xFFFFFF and currentTheme.startColor.toArgb())), "UTF-8")
                    val textHex = java.net.URLEncoder.encode(String.format("#%06X", (0xFFFFFF and currentTheme.textColor.toArgb())), "UTF-8")
                    
                    loadUrl("https://appassets.androidplatform.net/assets/viewer/index.html?accent=$accentHex&bg=$bgHex&text=$textHex")
                }
            }
        )

    }
}
