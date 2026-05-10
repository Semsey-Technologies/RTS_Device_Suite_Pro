package com.semseytech.rtsdevicesuitepro.backup.engine

import android.app.Application
import android.os.Environment
import android.util.Log
import com.google.gson.GsonBuilder
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.backup.model.*
import com.semseytech.rtsdevicesuitepro.sms.logic.SmsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupEngine(private val application: Application) {
    private val TAG = "BackupEngine"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun runBackup(
        selectedItems: List<BackupItem>,
        archiveFormat: ArchiveFormat,
        compressionLevel: Int,
        outputFileName: String,
        destination: BackupDestination,
        onProgress: (Float, String) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val workingDir = File(application.cacheDir, "backup_work_${System.currentTimeMillis()}").apply { mkdirs() }
            
            // Initial local destination to build the archive
            val tempArchive = File(application.cacheDir, "$outputFileName${archiveFormat.extension}")
            
            val manifestEntries = mutableListOf<ManifestEntry>()
            val includedCategories = selectedItems.map { 
                when (it.type.lowercase()) {
                    "audio", "music", "ringtones", "notifications", "alarms" -> "Audio"
                    "images", "pictures" -> "Pictures"
                    "videos", "movies" -> "Videos"
                    else -> it.type
                }
            }.distinct()

            ArchiveWriter(tempArchive, archiveFormat, compressionLevel).use { writer ->
                
                // 1. Process SMS/MMS (Special handling)
                val smsItems = selectedItems.filterIsInstance<BackupItem.SmsMessage>()
                if (smsItems.isNotEmpty()) {
                    onProgress(0.1f, "Extracting SMS/MMS...")
                    val extractor = SmsExtractor(application)
                    val threadIds = smsItems.map { it.id }.toSet()
                    val smsEntries = extractor.extractAll(workingDir, threadIds)
                    
                    // Add extracted files to archive
                    val dataDir = File(workingDir, "data")
                    dataDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(workingDir).path
                            writer.addFile(file, relativePath)
                        }
                    }
                    
                    smsEntries.forEach { entry ->
                        manifestEntries.add(ManifestEntry(
                            entry.category, entry.itemName, entry.itemType, entry.identifier, entry.filePath, 
                            date = entry.date, size = entry.size
                        ))
                    }
                }

                // 2. Process other JSON-based data
                processJsonCategory("calls", selectedItems.filterIsInstance<BackupItem.CallLogEntry>(), writer, manifestEntries)
                processJsonCategory("contacts", selectedItems.filterIsInstance<BackupItem.Contact>(), writer, manifestEntries)
                processJsonCategory("settings", selectedItems.filterIsInstance<BackupItem.SystemSetting>(), writer, manifestEntries)

                // 3. Process APKs
                val apks = selectedItems.filterIsInstance<BackupItem.Apk>()
                apks.forEachIndexed { index, apk ->
                    onProgress(0.4f + (index.toFloat() / apks.size * 0.2f), "Backing up APK: ${apk.appName}")
                    apk.sourceDir?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val entryPath = "apks/${apk.packageName}.apk"
                            writer.addFile(file, entryPath)
                            manifestEntries.add(ManifestEntry(
                                "APKs", apk.appName, "Apk", apk.packageName, entryPath, path, 
                                size = file.length(),
                                date = file.lastModified()
                            ))
                        }
                    }
                }

                // 4. Process User Files
                val files = selectedItems.filterIsInstance<BackupItem.UserFile>()
                files.forEachIndexed { index, userFile ->
                    onProgress(0.6f + (index.toFloat() / files.size * 0.3f), "Archiving: ${userFile.fileName}")
                    val file = File(userFile.path)
                    if (file.exists()) {
                        // Normalize folder structure: all audio types (Music, Ringtones, etc.) go under 'audio'
                        val typeLower = userFile.type.lowercase()
                        val folderName = when (typeLower) {
                            "music", "audio", "ringtones", "notifications", "alarms" -> "audio"
                            else -> userFile.type
                        }

                        val entryPath = "files/$folderName/${file.name}"
                        writer.addFile(file, entryPath)
                        
                        // Map type to category for viewer gallery
                        val category = when (typeLower) {
                            "images", "pictures" -> "Pictures"
                            "videos", "movies" -> "Videos"
                            "audio", "music", "ringtones", "notifications", "alarms" -> "Audio"
                            "documents" -> "Documents"
                            else -> userFile.type
                        }
                        
                        manifestEntries.add(ManifestEntry(
                            category, userFile.fileName, "UserFile", userFile.id, entryPath, userFile.path,
                            size = file.length(),
                            date = file.lastModified()
                        ))
                    }
                }

                // 5. Add Viewer (HTML Assets)
                onProgress(0.95f, "Adding SMS Viewer...")
                addViewerAssets(writer)

                // 6. Finalize Manifest
                val manifest = BackupManifest(
                    timestamp = System.currentTimeMillis(),
                    deviceModel = android.os.Build.MODEL,
                    androidVersion = android.os.Build.VERSION.RELEASE,
                    appVersion = "1.0.0",
                    archiveFormat = archiveFormat.name,
                    categories = includedCategories,
                    entries = manifestEntries,
                    smsIndexAvailable = smsItems.isNotEmpty()
                )
                writer.addData(gson.toJson(manifest).toByteArray(), "manifest.json")
                writer.addData(gson.toJson(manifest).toByteArray(), "backup.json") // As requested in Requirement 8
            }

            // Move to final destination
            onProgress(0.98f, "Finalizing storage...")
            val finalFile = moveToDestination(tempArchive, destination, outputFileName, archiveFormat)

            workingDir.deleteRecursively()
            onProgress(1.0f, "Backup Complete")
            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            null
        }
    }

    private fun processJsonCategory(
        catId: String,
        items: List<BackupItem>,
        writer: ArchiveWriter,
        manifestEntries: MutableList<ManifestEntry>
    ) {
        if (items.isEmpty()) return
        val json = gson.toJson(items)
        val entryPath = "data/$catId.json"
        val data = json.toByteArray()
        writer.addData(data, entryPath)
        val latestDate = items.maxOfOrNull { it.date } ?: 0L
        manifestEntries.add(ManifestEntry(
            catId, catId.replaceFirstChar { it.uppercase() }, "JsonData", catId, entryPath, 
            size = data.size.toLong(),
            date = latestDate
        ))
    }

    private fun addViewerAssets(writer: ArchiveWriter) {
        val assets = application.assets
        try {
            copyAssetFolder(assets, "viewer", "viewer", writer)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy viewer assets", e)
        }
    }

    private fun copyAssetFolder(assets: android.content.res.AssetManager, assetPath: String, targetPath: String, writer: ArchiveWriter) {
        val files = assets.list(assetPath) ?: return
        if (files.isEmpty()) {
            assets.open(assetPath).use { input ->
                writer.addData(input.readBytes(), targetPath)
            }
        } else {
            for (file in files) {
                copyAssetFolder(assets, "$assetPath/$file", "$targetPath/$file", writer)
            }
        }
    }

    private fun moveToDestination(
        tempFile: File,
        destination: BackupDestination,
        fileName: String,
        format: ArchiveFormat
    ): File? {
        val finalFileName = "$fileName${format.extension}"
        
        // If a URI is provided (e.g. via SAF), use it regardless of the destination type label
        if (!destination.uri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(destination.uri)
                application.contentResolver.openOutputStream(uri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
                return File(uri.path ?: finalFileName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to URI destination: ${destination.uri}", e)
                // Fall through to default behavior if URI write fails
            }
        }

        return try {
            when (destination.type) {
                BackupDestinationType.INTERNAL -> {
                    val baseDir = application.getExternalFilesDir(null) ?: application.filesDir
                    val dest = File(baseDir, "backups/$finalFileName")
                    dest.parentFile?.mkdirs()
                    moveFile(tempFile, dest)
                }
                BackupDestinationType.SD_CARD -> {
                    val dirs = application.getExternalFilesDirs(null)
                    val sdCardDir = dirs.find { it != null && Environment.isExternalStorageRemovable(it) }
                    if (sdCardDir != null) {
                        val dest = File(sdCardDir, "backups/$finalFileName")
                        dest.parentFile?.mkdirs()
                        moveFile(tempFile, dest)
                    } else {
                        tempFile
                    }
                }
                BackupDestinationType.SAF -> {
                    destination.uri?.let { uriString ->
                        val uri = android.net.Uri.parse(uriString)
                        application.contentResolver.openOutputStream(uri)?.use { output ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                        File(uri.path ?: finalFileName)
                    } ?: tempFile
                }
                BackupDestinationType.WEBDAV -> {
                    val cloudDir = File(application.filesDir, "cloud_sync")
                    cloudDir.mkdirs()
                    val dest = File(cloudDir, finalFileName)
                    moveFile(tempFile, dest)
                }
                else -> {
                    val dest = File(application.filesDir, "backups/$finalFileName")
                    dest.parentFile?.mkdirs()
                    moveFile(tempFile, dest)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to move to destination", e)
            tempFile
        }
    }

    private fun moveFile(source: File, dest: File): File {
        if (source.renameTo(dest)) return dest
        
        // If rename fails (e.g. cross-partition), copy and delete
        source.inputStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        source.delete()
        return dest
    }
}
