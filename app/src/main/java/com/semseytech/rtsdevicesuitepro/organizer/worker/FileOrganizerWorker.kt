package com.semseytech.rtsdevicesuitepro.organizer.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerDatabase
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerRepository
import com.semseytech.rtsdevicesuitepro.organizer.model.OrganizerRule
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "FileOrganizerWorker"

class FileOrganizerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting organization work...")
        val database = OrganizerDatabase.getDatabase(applicationContext)
        val repository = OrganizerRepository(database.organizerDao())
        val rules = repository.allRules.first()

        if (rules.isEmpty()) {
            Log.d(TAG, "No rules found to process")
        }

        rules.forEach { rule ->
            if (rule.isEnabled) {
                Log.d(TAG, "Processing rule: ${rule.name} (${rule.sourcePath} -> ${rule.targetPath})")
                processRule(rule)
            } else {
                Log.d(TAG, "Rule ${rule.name} is disabled, skipping")
            }
        }
        
        return Result.success()
    }

    private fun processRule(rule: OrganizerRule) {
        val sourceDir = File(rule.sourcePath)
        val targetDir = File(rule.targetPath)

        if (!sourceDir.exists()) {
            Log.e(TAG, "Source directory does not exist: ${rule.sourcePath}")
            return
        }
        if (!sourceDir.isDirectory) {
            Log.e(TAG, "Source path is not a directory: ${rule.sourcePath}")
            return
        }
        
        if (!targetDir.exists()) {
            Log.d(TAG, "Creating target directory: ${rule.targetPath}")
            targetDir.mkdirs()
        }

        val files = sourceDir.listFiles()
        Log.d(TAG, "Found ${files?.size ?: 0} items in source directory")

        files?.forEach { file ->
            if (shouldMove(file, rule)) {
                Log.d(TAG, "Moving file: ${file.name}")
                moveItem(file, targetDir, rule)
            }
        }
    }

    private fun shouldMove(file: File, rule: OrganizerRule): Boolean {
        if (file.isDirectory) {
            if (rule.options.ignoreSubfolders) return false
            if (rule.options.moveEntireFolderIfContainsMatch) {
                // Check if any file inside matches
                return file.walkTopDown().any { it.isFile && isTypeMatch(it, rule.fileTypes) }
            }
            return false
        }
        
        return isTypeMatch(file, rule.fileTypes)
    }

    private fun isTypeMatch(file: File, types: List<String>): Boolean {
        val extension = file.extension.lowercase()
        return types.any { it.lowercase() == extension || isCategoryMatch(extension, it) }
    }

    private fun isCategoryMatch(extension: String, category: String): Boolean {
        return when (category.lowercase()) {
            "audio" -> listOf("mp3", "wav", "flac", "m4a", "ogg").contains(extension)
            "video" -> listOf("mp4", "mkv", "avi", "mov").contains(extension)
            "image" -> listOf("jpg", "jpeg", "png", "gif", "webp").contains(extension)
            "document" -> listOf("pdf", "doc", "docx", "txt").contains(extension)
            "archive" -> listOf("zip", "rar", "7z", "tar", "gz").contains(extension)
            else -> false
        }
    }

    private fun moveItem(file: File, targetDir: File, rule: OrganizerRule) {
        val destFile = File(targetDir, file.name)
        
        // Handle archives
        if (isArchive(file) && rule.options.archiveOptions.autoExtract) {
            extractAndHandle(file, targetDir, rule)
            return
        }

        // Standard move
        try {
            if (file.renameTo(destFile)) {
                // Success
            } else {
                copyAndDelete(file, destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isArchive(file: File): Boolean {
        return listOf("zip", "rar", "7z").contains(file.extension.lowercase())
    }

    private fun extractAndHandle(file: File, targetDir: File, rule: OrganizerRule) {
        val extractionDir = if (rule.options.archiveOptions.treatAsSingleUnit) {
            File(targetDir, file.nameWithoutExtension)
        } else {
            targetDir
        }
        
        if (!extractionDir.exists()) extractionDir.mkdirs()
        
        ExtractionUtils.extract(file, extractionDir)
        
        if (rule.options.archiveOptions.deleteAfterExtraction) {
            file.delete()
        }
    }

    @Throws(IOException::class)
    private fun copyAndDelete(source: File, dest: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        source.delete()
    }
}
