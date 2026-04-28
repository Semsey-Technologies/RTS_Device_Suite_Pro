package com.semseytech.rtsdevicesuitepro.storage.analyzer

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class StorageAnalyzerRepository(private val context: Context) {

    private val TAG = "RTS PROOF"

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        val categoryMap = mutableMapOf<FileCategory, MutableCategoryInfo>()
        FileCategory.values().forEach { categoryMap[it] = MutableCategoryInfo() }

        val largestFiles = mutableListOf<FileInfo>()

        // 1. Scan MediaStore
        scanMediaStore(categoryMap, largestFiles)

        // 2. Full Storage Scan (Recursive)
        if (Environment.isExternalStorageManager()) {
            recursiveScan(path, categoryMap, largestFiles)
        }

        // 3. Scan Installed Apps
        scanInstalledApps(categoryMap, largestFiles)

        StorageStats(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            categoryStats = categoryMap.mapValues { 
                val info = it.value.toCategoryInfo()
                info.copy(files = info.files.distinctBy { file -> file.path })
            },
            largestFiles = largestFiles.distinctBy { it.path }.sortedByDescending { it.size }.take(50),
            isScanning = false
        )
    }

    private fun recursiveScan(
        directory: File,
        categoryMap: MutableMap<FileCategory, MutableCategoryInfo>,
        largestFiles: MutableList<FileInfo>
    ) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                recursiveScan(file, categoryMap, largestFiles)
            } else {
                val category = determineCategory(file.path, null)
                val info = FileInfo(
                    name = file.name,
                    path = file.path,
                    size = file.length(),
                    category = category,
                    lastModified = file.lastModified(),
                    mimeType = getMimeType(file.path)
                )
                
                categoryMap[category]?.let {
                    it.count++
                    it.totalSize += file.length()
                    it.files.add(info)
                }
                largestFiles.add(info)
            }
        }
    }

    private fun scanMediaStore(
        categoryMap: MutableMap<FileCategory, MutableCategoryInfo>,
        largestFiles: MutableList<FileInfo>
    ) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val cursor = context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateIndex = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val path = it.getString(dataIndex) ?: ""
                val size = it.getLong(sizeIndex)
                val mimeType = it.getString(mimeIndex)
                val lastMod = it.getLong(dateIndex) * 1000

                val category = determineCategory(path, mimeType)
                val uri = getUriForFile(id, category)
                val info = FileInfo(name, path, size, category, lastMod, mimeType, uri)

                categoryMap[category]?.let { catInfo ->
                    catInfo.count++
                    catInfo.totalSize += size
                    catInfo.files.add(info)
                }

                largestFiles.add(info)
            }
        }
    }

    private fun getUriForFile(id: Long, category: FileCategory): Uri {
        val baseUri = when (category) {
            FileCategory.IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            FileCategory.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            FileCategory.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
        return ContentUris.withAppendedId(baseUri, id)
    }

    fun deleteFile(fileInfo: FileInfo): Boolean {
        // If it's an installed app (stored as package name in path), trigger uninstallation
        if (fileInfo.category == FileCategory.APKS && !fileInfo.path.startsWith("/")) {
            return uninstallApp(fileInfo.path)
        }
        val sizeBefore = fileInfo.size
        var deleted = false
        var apiUsed = "NONE"
        var mediaStoreRows = 0

        try {
            val file = File(fileInfo.path)
            
            // 1. Try MediaStore Deletion (For Media files)
            if (fileInfo.uri != null) {
                apiUsed = "MEDIASTORE_CONTENT_RESOLVER"
                mediaStoreRows = context.contentResolver.delete(fileInfo.uri, null, null)
                deleted = mediaStoreRows > 0
            }

            // 2. Try Direct File API (If MANAGE_EXTERNAL_STORAGE is granted or it's an app-private file)
            if (!deleted && file.exists()) {
                apiUsed = "DIRECT_FILE_IO"
                deleted = file.delete()
            }

            val existsAfter = file.exists()
            
            // PROOF MODE LOGGING - Requirement Part 1.5
            Log.d("RTS PROOF", """
                [DELETE OPERATION]
                PATH: ${fileInfo.path}
                SIZE_BEFORE: $sizeBefore bytes
                API USED: $apiUsed
                MEDIASTORE ROWS: $mediaStoreRows
                SUCCESS: $deleted
                FILE EXISTS AFTER: $existsAfter
            """.trimIndent())

            return deleted
        } catch (e: Exception) {
            Log.e("RTS_PROOF", "[DELETE_FAILED] PATH: ${fileInfo.path}", e)
            return false
        }
    }

    suspend fun moveFile(source: FileInfo, destinationFolder: String): Boolean = withContext(Dispatchers.IO) {
        val success = copyFile(source, destinationFolder)
        if (success) {
            val deleted = deleteFile(source)
            Log.d("RTS PROOF", "OP: MOVE | SRC: ${source.path} | DEST: $destinationFolder | DELETE SRC: $deleted")
            true
        } else false
    }

    suspend fun copyFile(source: FileInfo, destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val srcFile = File(source.path)
            val destDir = File(destPath)
            if (!destDir.exists()) destDir.mkdirs()
            val destFile = File(destDir, srcFile.name)
            
            FileInputStream(srcFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    val inChannel = input.channel
                    val outChannel = output.channel
                    inChannel.transferTo(0, inChannel.size(), outChannel)
                }
            }
            Log.d("RTS PROOF", "OP: COPY | SRC: ${source.path} | DEST: ${destFile.absolutePath} | SUCCESS: TRUE")
            true
        } catch (e: Exception) {
            Log.e("RTS_PROOF", "OP: COPY | FAILED | SRC: ${source.path}", e)
            false
        }
    }

    fun renameFile(fileInfo: FileInfo, newName: String): Boolean {
        try {
            val oldFile = File(fileInfo.path)
            val newFile = File(oldFile.parent, newName)
            val success = oldFile.renameTo(newFile)
            Log.d("RTS PROOF", "OP: RENAME | FROM: ${fileInfo.path} | TO: $newName | RESULT: $success")
            return success
        } catch (e: Exception) {
            Log.e("RTS_PROOF", "OP: RENAME | FAILED | PATH: ${fileInfo.path}", e)
            return false
        }
    }

    fun openFile(fileInfo: FileInfo) {
        try {
            val file = File(fileInfo.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, fileInfo.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "OPEN FAILED", e)
        }
    }

    fun shareFile(fileInfo: FileInfo) {
        try {
            val file = File(fileInfo.path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = fileInfo.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share File").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "SHARE FAILED", e)
        }
    }

    private fun uninstallApp(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "UNINSTALL FAILED: $packageName", e)
            false
        }
    }

    private fun scanInstalledApps(
        categoryMap: MutableMap<FileCategory, MutableCategoryInfo>,
        largestFiles: MutableList<FileInfo>
    ) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            // Filter: Only include user-installed apps (exclude system apps)
            val isSystemApp = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) continue

            try {
                val file = File(app.sourceDir)
                if (file.exists()) {
                    val size = file.length()
                    val label = pm.getApplicationLabel(app).toString()
                    val info = FileInfo(
                        name = label,
                        path = app.packageName, // Store package name in path for APKS
                        size = size,
                        category = FileCategory.APKS,
                        lastModified = file.lastModified(),
                        mimeType = "application/vnd.android.package-archive"
                    )
                    categoryMap[FileCategory.APKS]?.let {
                        it.count++
                        it.totalSize += size
                        it.files.add(info)
                    }
                    largestFiles.add(info)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun determineCategory(path: String, mimeType: String?): FileCategory {
        val extension = path.substringAfterLast('.', "").lowercase()
        val mime = mimeType ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        
        if (mime != null) {
            when {
                mime.startsWith("image/") -> return FileCategory.IMAGES
                mime.startsWith("video/") -> return FileCategory.VIDEOS
                mime.startsWith("audio/") -> return FileCategory.AUDIO
                mime.contains("pdf") || mime.contains("msword") || 
                    mime.contains("vnd.openxmlformats-officedocument") ||
                    mime.contains("text/") -> return FileCategory.DOCUMENTS
            }
        }

        return when (extension) {
            "jpg", "jpeg", "png", "webp", "gif" -> FileCategory.IMAGES
            "mp4", "mkv", "mov", "avi" -> FileCategory.VIDEOS
            "mp3", "wav", "flac", "ogg" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx" -> FileCategory.DOCUMENTS
            "zip", "rar", "7z", "tar", "gz" -> FileCategory.ARCHIVES
            "apk" -> FileCategory.APKS
            else -> FileCategory.OTHERS
        }
    }

    private fun getMimeType(path: String): String? {
        val extension = path.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private class MutableCategoryInfo {
        var count: Int = 0
        var totalSize: Long = 0
        val files = mutableListOf<FileInfo>()
        fun toCategoryInfo() = CategoryInfo(count, totalSize, files.toList())
    }
}
