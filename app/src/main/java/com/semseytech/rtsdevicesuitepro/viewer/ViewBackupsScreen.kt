package com.semseytech.rtsdevicesuitepro.viewer

import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.compose.ui.graphics.toArgb
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import com.semseytech.rtsdevicesuitepro.ui.theme.ThemeManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewBackupsScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    val scale = ThemeManager.uiScale

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "View Backups",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.textColor
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = currentTheme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
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
                        setBackgroundColor(0) // Make WebView background transparent
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        webChromeClient = WebChromeClient()
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
                        val accentHex = String.format("#%06X", (0xFFFFFF and currentTheme.accentColor.toArgb()))
                        val bgHex = String.format("#%06X", (0xFFFFFF and currentTheme.startColor.toArgb()))
                        val textHex = String.format("#%06X", (0xFFFFFF and currentTheme.textColor.toArgb()))
                        
                        loadUrl("https://appassets.androidplatform.net/assets/viewer/index.html?accent=$accentHex&bg=$bgHex&text=$textHex")
                    }
                }
            )
        }
    }
}
