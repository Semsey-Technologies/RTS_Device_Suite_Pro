package com.semseytech.rtsdevicesuitepro.recovery

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.semseytech.rtsdevicesuitepro.storage.analyzer.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class RecoveryRepository(private val context: Context) {

    private val TAG = "RecoveryRepo"

    fun performDeepScan(): Flow<RecoveryScanUpdate> = flow {
        emit(RecoveryScanUpdate.Progress(0.01f, "Initializing deep scan..."))

        val recoverableItems = mutableListOf<RecoverableItem>()

        // 1. Scan MediaStore Trash (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            emit(RecoveryScanUpdate.Progress(0.1f, "Scanning MediaStore Trash..."))
            scanMediaStoreTrash(recoverableItems)
        }

        // 2. Scan Thumbnails
        emit(RecoveryScanUpdate.Progress(0.3f, "Scanning Thumbnails..."))
        scanThumbnails(recoverableItems)

        // 3. Scan Common Cache Directories
        emit(RecoveryScanUpdate.Progress(0.5f, "Scanning Cache Residue..."))
        scanCacheResidue(recoverableItems)

        // 4. Scan Android/media (often contains app residue)
        emit(RecoveryScanUpdate.Progress(0.7f, "Scanning App Media Residue..."))
        scanAppMediaResidue(recoverableItems)

        // 5. Scan Hidden Files in Public Directories
        emit(RecoveryScanUpdate.Progress(0.85f, "Scanning for Hidden Data..."))
        scanHiddenFiles(recoverableItems)

        emit(RecoveryScanUpdate.Finished(recoverableItems))
    }.flowOn(Dispatchers.IO)

    private fun scanMediaStoreTrash(list: MutableList<RecoverableItem>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            "is_trashed"
        )

        val bundle = android.os.Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                bundle,
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
                    val name = it.getString(nameIndex) ?: "Trashed File"
                    val path = it.getString(dataIndex) ?: ""
                    val size = it.getLong(sizeIndex)
                    val mime = it.getString(mimeIndex)
                    val date = it.getLong(dateIndex) * 1000

                    val category = determineCategory(path, mime)
                    val uri = getUriForFile(id, category)
                    
                    list.add(RecoverableItem(
                        name = name,
                        path = path,
                        size = size,
                        category = category,
                        lastModified = date,
                        dateDeleted = date, // Trashed date approx
                        mimeType = mime,
                        uri = uri,
                        recoverabilityScore = 0.9f
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Trash scan failed", e)
        }
    }

    private fun scanThumbnails(list: MutableList<RecoverableItem>) {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val thumbnailsDir = File(dcim, ".thumbnails")
        if (thumbnailsDir.exists()) {
            thumbnailsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    list.add(createRecoverableFromFile(file, "System Thumbnail", 0.7f))
                }
            }
        }
        
        // Also check Pictures/.thumbnails if it exists
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val pictThumbnails = File(pictures, ".thumbnails")
        if (pictThumbnails.exists()) {
            pictThumbnails.listFiles()?.forEach { file ->
                if (file.isFile) {
                    list.add(createRecoverableFromFile(file, "System Thumbnail", 0.7f))
                }
            }
        }
    }

    private fun scanCacheResidue(list: MutableList<RecoverableItem>) {
        val externalCache = context.externalCacheDir?.parentFile
        if (externalCache != null && externalCache.exists()) {
            externalCache.listFiles()?.forEach { appDir ->
                val cache = File(appDir, "cache")
                if (cache.exists()) {
                    recursiveFastScan(cache, list, "App Cache Residue", 0.5f, depth = 2)
                }
            }
        }
    }

    private fun scanAppMediaResidue(list: MutableList<RecoverableItem>) {
        val androidMedia = File(Environment.getExternalStorageDirectory(), "Android/media")
        if (androidMedia.exists()) {
            androidMedia.listFiles()?.forEach { appDir ->
                recursiveFastScan(appDir, list, "App Media Residue", 0.6f, depth = 3)
            }
        }
    }

    private fun scanHiddenFiles(list: MutableList<RecoverableItem>) {
        val publicDirs = arrayOf(
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_MUSIC
        )

        for (dirType in publicDirs) {
            val dir = Environment.getExternalStoragePublicDirectory(dirType)
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith(".") && file.isFile) {
                    list.add(createRecoverableFromFile(file, "Hidden Data", 0.8f))
                }
            }
        }
    }

    private fun recursiveFastScan(dir: File, list: MutableList<RecoverableItem>, source: String, score: Float, depth: Int) {
        if (depth <= 0) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                recursiveFastScan(file, list, source, score, depth - 1)
            } else if (file.length() > 1024) { // Ignore tiny files
                list.add(createRecoverableFromFile(file, source, score))
            }
        }
    }

    private fun createRecoverableFromFile(file: File, source: String, score: Float): RecoverableItem {
        val category = determineCategory(file.path, null)
        return RecoverableItem(
            name = file.name,
            path = file.path,
            size = file.length(),
            category = category,
            lastModified = file.lastModified(),
            mimeType = getMimeType(file.path),
            sourceApp = source,
            recoverabilityScore = score
        )
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

    private fun getUriForFile(id: Long, category: FileCategory): Uri {
        val baseUri = when (category) {
            FileCategory.IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            FileCategory.VIDEOS -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            FileCategory.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
        return ContentUris.withAppendedId(baseUri, id)
    }

    suspend fun recoverItem(item: RecoverableItem, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Use DocumentFile to create a new file in the picked directory
            val pickedDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, destinationUri)
            val newFile = pickedDir?.createFile(item.mimeType ?: "*/*", item.name)
            
            if (newFile != null) {
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    if (item.uri != null) {
                        context.contentResolver.openInputStream(item.uri)?.use { input ->
                            input.copyTo(output)
                        }
                    } else {
                        val file = File(item.path)
                        if (file.exists()) {
                            FileInputStream(file).use { input ->
                                input.copyTo(output)
                            }
                        } else return@withContext false
                    }
                }
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed", e)
            false
        }
    }

    suspend fun permanentlyDelete(item: RecoverableItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.path)
            if (file.exists() && file.canWrite()) {
                // Secure wipe: overwrite with zeros before deleting
                val size = file.length()
                FileOutputStream(file).use { out ->
                    val buffer = ByteArray(1024 * 64)
                    var written = 0L
                    while (written < size) {
                        val toWrite = minOf(buffer.size.toLong(), size - written).toInt()
                        out.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                    out.flush()
                }
            }
            
            // Now delete
            if (item.uri != null) {
                context.contentResolver.delete(item.uri, null, null) > 0
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Secure delete failed", e)
            false
        }
    }

    fun openItem(item: RecoverableItem) {
        try {
            val intent: Intent
            if (item.uri != null) {
                // For MediaStore files
                intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.uri, item.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                // For direct file system items
                val file = File(item.path)
                if (!file.exists()) {
                    Log.e(TAG, "File does not exist: ${item.path}")
                    return
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, 
                    "${context.packageName}.fileprovider", 
                    file
                )
                intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, item.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Open failed", e)
        }
    }
}

sealed class RecoveryScanUpdate {
    data class Progress(val progress: Float, val status: String) : RecoveryScanUpdate()
    data class Finished(val items: List<RecoverableItem>) : RecoveryScanUpdate()
}
