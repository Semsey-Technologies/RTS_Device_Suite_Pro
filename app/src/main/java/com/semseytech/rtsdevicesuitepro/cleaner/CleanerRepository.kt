package com.semseytech.rtsdevicesuitepro.cleaner

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

class CleanerRepository(private val context: Context) {

    suspend fun performAutoClean(categories: List<String> = listOf("temp", "dupes", "empty_folders")) {
        Log.d("CleanerRepository", "Starting Auto Clean for categories: $categories")
        val externalStorage = Environment.getExternalStorageDirectory()
        
        var totalCleaned = 0L

        if (categories.contains("temp")) {
            val tempPatterns = listOf(".tmp", ".temp", ".cache", "thumbnails", ".log")
            externalStorage.walkTopDown().forEach { file ->
                if (file.isFile && tempPatterns.any { file.name.contains(it, ignoreCase = true) || file.path.contains(it, ignoreCase = true) }) {
                    val size = file.length()
                    if (file.delete()) {
                        totalCleaned += size
                    }
                }
            }
        }

        if (categories.contains("empty_folders")) {
            externalStorage.walkBottomUp().forEach { file ->
                if (file.isDirectory) {
                    val children = file.list()
                    if (children != null && children.isEmpty()) {
                        file.delete()
                    }
                }
            }
        }

        if (categories.contains("dupes")) {
            val fileMap = mutableMapOf<String, File>()
            externalStorage.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val key = "${file.name}_${file.length()}"
                    if (fileMap.containsKey(key)) {
                        val size = file.length()
                        if (file.delete()) {
                            totalCleaned += size
                        }
                    } else {
                        fileMap[key] = file
                    }
                }
            }
        }

        Log.d("CleanerRepository", "Auto Clean Finished. Total reclaimed: $totalCleaned bytes")
    }
}
