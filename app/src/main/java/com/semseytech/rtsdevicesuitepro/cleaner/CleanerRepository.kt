package com.semseytech.rtsdevicesuitepro.cleaner

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

class CleanerRepository(private val context: Context) {

    suspend fun performAutoClean(
        categories: List<String> = listOf("temp", "dupes", "empty_folders"),
        onProgress: ((String, Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        Log.d("CleanerRepository", "Starting Auto Clean for categories: $categories")
        val externalStorage = Environment.getExternalStorageDirectory()
        
        var totalCleaned = 0L
        val fileMap = if (categories.contains("dupes")) mutableMapOf<String, File>() else null
        val tempPatterns = listOf(".tmp", ".temp", ".cache", "thumbnails", ".log")

        // Optimization: Single walk for all categories
        externalStorage.walkTopDown().onEnter { 
            // Skip hidden directories like .android to save time
            val name = it.name
            !name.startsWith(".") || name == ".temp" || name == ".cache"
        }.forEach { file ->
            yield() // Be cooperative to avoid blocking thread too long
            onProgress?.invoke("Scanning: ${file.name}", -1)
            
            if (file.isFile) {
                // Check Temp
                if (categories.contains("temp") && tempPatterns.any { file.name.contains(it, ignoreCase = true) || file.path.contains(it, ignoreCase = true) }) {
                    val size = file.length()
                    if (file.delete()) totalCleaned += size
                    return@forEach
                }

                // Check Dupes
                if (fileMap != null) {
                    val size = file.length()
                    if (size > 0) {
                        val hash = FileHasher.calculateMD5(file)
                        if (hash != null) {
                            val key = "${hash}_${size}"
                            if (fileMap.containsKey(key)) {
                                if (file.delete()) totalCleaned += size
                            } else {
                                fileMap[key] = file
                            }
                        }
                    }
                }
            } else if (file.isDirectory && categories.contains("empty_folders")) {
                // Note: Empty folders check is tricky during top-down walk
                // Better to handle in a second pass or bottom-up
            }
        }

        // Secondary pass for empty folders if needed
        if (categories.contains("empty_folders")) {
            externalStorage.walkBottomUp().onEnter { !it.name.startsWith(".") }.forEach { file ->
                yield()
                if (file.isDirectory) {
                    onProgress?.invoke("Checking folder: ${file.name}", -1)
                    val children = file.list()
                    if (children != null && children.isEmpty()) {
                        file.delete()
                    }
                }
            }
        }

        Log.d("CleanerRepository", "Auto Clean Finished. Total reclaimed: $totalCleaned bytes")
    }
}
