package com.semseytech.rtsdevicesuitepro.viewer

import android.content.Context
import android.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ViewerExporter {
    private const val TAG = "ViewerExporter"

    fun exportViewer(
        context: Context,
        outputStream: OutputStream,
        onProgress: (Float, String) -> Unit
    ): Boolean {
        return try {
            val addedEntries = mutableSetOf<String>()
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
                // 1. Export data from viewer_data/data -> root/data/
                onProgress(0.1f, "Packing database files...")
                val dataDir = File(context.filesDir, "viewer_data/data")
                if (dataDir.exists()) {
                    addFileToZip(dataDir, "viewer_payload/data", zos, addedEntries)
                }

                // 2. Export media files -> root/files/
                onProgress(0.3f, "Packing media files...")
                val filesDir = File(context.filesDir, "viewer_data/files")
                if (filesDir.exists()) {
                    addFileToZip(filesDir, "viewer_payload/files", zos, addedEntries)
                }

                // 3. Export static assets from assets/viewer
                onProgress(0.5f, "Packing viewer assets...")
                addAssetsToZip(context, "viewer", zos, addedEntries)

                // 4. Export viewer_data -> root/internal/ (contains manifest.json, data/, files/)
                onProgress(0.8f, "Packing internal records...")
                val viewerDir = File(context.filesDir, "viewer_data")
                if (viewerDir.exists()) {
                    addFileToZip(viewerDir, "viewer_payload/internal", zos, addedEntries)
                }

                // 5. Add PowerShell and Batch scripts to launch viewer
                onProgress(0.9f, "Adding launcher scripts...")
                addLauncherScript(zos, addedEntries)

                // 6. Add README file at the ROOT of the zip
                onProgress(0.92f, "Adding README...")
                addReadme(zos, addedEntries)
                
                onProgress(0.95f, "Finalizing archive...")
            }
            onProgress(1.0f, "Export Complete")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export viewer", e)
            false
        }
    }

    private fun addLauncherScript(zos: ZipOutputStream, addedEntries: MutableSet<String>) {
        val payloadDir = "viewer_payload"
        val psScriptName = "$payloadDir/run_viewer.ps1"
        if (addedEntries.add(psScriptName)) {
            val psScriptContent = """
                # RTS Viewer Local Server Launcher
                ${'$'}port = 5050
                ${'$'}url = "http://localhost:${'$'}port/"
                
                Write-Host "Starting RTS Viewer Server at ${'$'}url" -ForegroundColor Cyan
                Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Yellow
                
                # Open browser
                Start-Process ${'$'}url
                
                # Attempt to load HttpListener
                try {
                    if (-not ([System.Management.Automation.PSTypeName]'System.Net.HttpListener').Type) {
                        Add-Type -AssemblyName System.Net.HttpListener -ErrorAction SilentlyContinue
                    }
                } catch { }

                try {
                    ${'$'}listener = New-Object System.Net.HttpListener
                } catch {
                    Write-Host "Error: Could not create HttpListener. This script requires .NET Framework or .NET Core." -ForegroundColor Red
                    Read-Host "Press Enter to exit"
                    exit
                }

                ${'$'}listener.Prefixes.Add(${'$'}url)
                
                try {
                    ${'$'}listener.Start()
                    while (${'$'}listener.IsListening) {
                        try {
                            ${'$'}context = ${'$'}listener.GetContext()
                            ${'$'}request = ${'$'}context.Request
                            ${'$'}response = ${'$'}context.Response
                            
                            ${'$'}path = ${'$'}request.Url.LocalPath
                            if (${'$'}path -eq "/") { ${'$'}path = "/index.html" }
                            
                            # Handle deletion requests from the web viewer
                            if (${'$'}request.HttpMethod -eq "DELETE") {
                                try {
                                    ${'$'}targetFile = Join-Path ${'$'}PSScriptRoot ${'$'}path.Replace("/", "\")
                                    if (Test-Path ${'$'}targetFile) {
                                        Remove-Item ${'$'}targetFile -Force -Recurse
                                        ${'$'}response.StatusCode = 200
                                        ${'$'}buffer = [System.Text.Encoding]::UTF8.GetBytes("Deleted")
                                    } else {
                                        ${'$'}response.StatusCode = 404
                                        ${'$'}buffer = [System.Text.Encoding]::UTF8.GetBytes("Not Found")
                                    }
                                } catch {
                                    ${'$'}response.StatusCode = 500
                                    ${'$'}buffer = [System.Text.Encoding]::UTF8.GetBytes("Error: ${'$'}_")
                                }
                                ${'$'}response.ContentLength64 = ${'$'}buffer.Length
                                ${'$'}response.OutputStream.Write(${'$'}buffer, 0, ${'$'}buffer.Length)
                                ${'$'}response.Close()
                                continue
                            }

                            ${'$'}filePath = Join-Path ${'$'}PSScriptRoot ${'$'}path.Replace("/", "\")
                            
                            if (Test-Path ${'$'}filePath -PathType Leaf) {
                                ${'$'}content = [System.IO.File]::ReadAllBytes(${'$'}filePath)
                                
                                # Determine Content-Type
                                ${'$'}extension = [System.IO.Path]::GetExtension(${'$'}filePath).ToLower()
                                ${'$'}contentType = switch (${'$'}extension) {
                                    ".html" { "text/html" }
                                    ".js"   { "application/javascript" }
                                    ".css"  { "text/css" }
                                    ".json" { "application/json" }
                                    ".png"  { "image/png" }
                                    ".jpg"  { "image/jpeg" }
                                    ".jpeg" { "image/jpeg" }
                                    ".gif"  { "image/gif" }
                                    ".svg"  { "image/svg+xml" }
                                    default { "application/octet-stream" }
                                }
                                
                                ${'$'}response.ContentType = ${'$'}contentType
                                ${'$'}response.ContentLength64 = ${'$'}content.Length
                                ${'$'}response.OutputStream.Write(${'$'}content, 0, ${'$'}content.Length)
                            } else {
                                ${'$'}response.StatusCode = 404
                                ${'$'}buffer = [System.Text.Encoding]::UTF8.GetBytes("404 Not Found")
                                ${'$'}response.ContentLength64 = ${'$'}buffer.Length
                                ${'$'}response.OutputStream.Write(${'$'}buffer, 0, ${'$'}buffer.Length)
                            }
                            ${'$'}response.Close()
                        } catch {
                            # Handle individual request errors without stopping the server
                        }
                    }
                } catch {
                    Write-Host "Server stopped: ${'$'}_" -ForegroundColor Red
                } finally {
                    if (${'$'}null -ne ${'$'}listener) {
                        ${'$'}listener.Stop()
                    }
                }
            """.trimIndent()

            zos.putNextEntry(ZipEntry(psScriptName))
            zos.write(psScriptContent.toByteArray())
            zos.closeEntry()
        }

        val cmdName = "run_viewer.cmd"
        if (addedEntries.add(cmdName)) {
            val cmdContent = """
                @echo off
                echo Starting RTS Viewer...
                powershell.exe -ExecutionPolicy Bypass -File "%~dp0$payloadDir\run_viewer.ps1"
                if %ERRORLEVEL% neq 0 (
                    echo.
                    echo There was an error starting the viewer. 
                    echo Please ensure PowerShell is installed and you have permission to run scripts.
                    pause
                )
            """.trimIndent()
            zos.putNextEntry(ZipEntry(cmdName))
            zos.write(cmdContent.toByteArray())
            zos.closeEntry()
        }
    }

    private fun addReadme(zos: ZipOutputStream, addedEntries: MutableSet<String>) {
        val readmeName = "README.txt"
        if (!addedEntries.add(readmeName)) return

        val readmeContent = """
            RTS Viewer - Offline Export README
            ==================================

            1. EXTRACTION INSTRUCTIONS (Required)
            -------------------------------------
            To ensure all features (images, videos, messages) work correctly, we recommend using 7-Zip 
            to extract this archive. Some built-in Windows tools may have issues with deep file paths.

            A. Download 7-Zip from: https://www.7-zip.org/
            B. Install 7-Zip on your computer.
            C. Right-click this ZIP file and select: 7-Zip -> Extract to "RTS_Viewer_Export..."
            D. Open the extracted folder.

            2. HOW TO VIEW YOUR DATA
            ------------------------
            Option A: Interactive Viewer (Recommended)
            Double-click the 'run_viewer.cmd' file in this folder.
            This starts a temporary local server and opens your browser to view your data 
            exactly as it appeared in the app.

            Option B: Manual Viewing
            If you prefer not to run the local server, you can still view all your files manually:
            - SMS/Call Logs/Contacts: Found in 'viewer_payload/data/' as JSON files.
            - Photos/Videos/Files: Found in 'viewer_payload/files/'.
            - Internal Records: Found in 'viewer_payload/internal/'.

            Note: Opening 'viewer_payload/index.html' directly in a browser without the local server 
            will likely fail to load images or data due to browser security (CORS) restrictions.

            3. SECURITY & PRIVACY
            ---------------------
            - The local server only runs on your machine (localhost) and is not accessible from the internet.
            - You can stop the server at any time by closing the PowerShell window or pressing Ctrl+C.
        """.trimIndent()

        zos.putNextEntry(ZipEntry(readmeName))
        zos.write(readmeContent.toByteArray())
        zos.closeEntry()
    }

    private fun addAssetsToZip(
        context: Context,
        assetPath: String,
        zos: ZipOutputStream,
        addedEntries: MutableSet<String>
    ) {
        val assets = context.assets.list(assetPath) ?: return
        if (assets.isEmpty()) {
            // It's a file
            try {
                context.assets.open(assetPath).use { input ->
                    // Move assets to viewer_payload/
                    val entryName = if (assetPath.startsWith("viewer/")) {
                        "viewer_payload/" + assetPath.substring("viewer/".length)
                    } else {
                        "viewer_payload/$assetPath"
                    }

                    if (addedEntries.add(entryName)) {
                        zos.putNextEntry(ZipEntry(entryName))
                        input.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            } catch (e: IOException) {
                // Not a file or can't open
            }
        } else {
            // It's a directory
            for (asset in assets) {
                addAssetsToZip(context, "$assetPath/$asset", zos, addedEntries)
            }
        }
    }

    private fun addFileToZip(file: File, zipPath: String, zos: ZipOutputStream, addedEntries: MutableSet<String>) {
        if (file.isDirectory) {
            val files = file.listFiles() ?: return
            if (files.isEmpty()) {
                val entryName = "$zipPath/"
                if (addedEntries.add(entryName)) {
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.closeEntry()
                }
            } else {
                for (f in files) {
                    addFileToZip(f, "$zipPath/${f.name}", zos, addedEntries)
                }
            }
        } else {
            if (addedEntries.add(zipPath)) {
                zos.putNextEntry(ZipEntry(zipPath))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }
}
