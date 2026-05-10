package com.semseytech.rtsdevicesuitepro.viewer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import com.semseytech.rtsdevicesuitepro.backup.model.BackupManifest
import com.semseytech.rtsdevicesuitepro.restore.engine.ArchiveReader
import com.semseytech.rtsdevicesuitepro.sms.logic.SmsExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ViewerImporter {
    private const val TAG = "ViewerImporter"
    private val gson = Gson()

    enum class ImportMode {
        ADD_TO_EXISTING,
        REPLACE_CATEGORIES,
        REPLACE_EVERYTHING
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        mode: ImportMode,
        selectedCategories: Set<String> = emptySet(),
        onProgress: (Float, String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.zip")
        val extractDir = File(context.cacheDir, "import_extract_${System.currentTimeMillis()}").apply { mkdirs() }
        val viewerDir = File(context.filesDir, "viewer_data")
        val dataDir = File(viewerDir, "data")

        try {
            onProgress(0.05f, "Downloading archive...")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            onProgress(0.15f, "Reading manifest...")
            val reader = ArchiveReader(tempFile)
            val manifestFile = File(extractDir, "manifest.json")
            reader.extractFile("manifest.json", manifestFile)
            
            if (!manifestFile.exists()) {
                reader.extractFile("backup.json", manifestFile)
            }

            if (!manifestFile.exists()) {
                Log.e(TAG, "No manifest found in archive")
                return@withContext false
            }

            val manifest = gson.fromJson(manifestFile.readText(), BackupManifest::class.java)
            val categoriesToImport = if (mode == ImportMode.REPLACE_CATEGORIES) selectedCategories else manifest.categories.toSet()

            onProgress(0.3f, "Extracting data...")
            // We extract everything to temp first to handle merging
            reader.extractTo(extractDir) { _, _ -> }

            if (mode == ImportMode.REPLACE_EVERYTHING) {
                viewerDir.deleteRecursively()
                viewerDir.mkdirs()
                dataDir.mkdirs()
            }

            // Process Data Files (SMS, Contacts, Calls, Settings)
            onProgress(0.5f, "Merging data...")
            val importDataDir = File(extractDir, "data")
            if (importDataDir.exists()) {
                mergeDataFiles(importDataDir, dataDir, categoriesToImport, mode)
            }

            // Process Manifest Entries (Files, APKs)
            onProgress(0.7f, "Importing files...")
            val currentManifestFile = File(viewerDir, "manifest.json")
            val currentManifest = if (currentManifestFile.exists() && mode != ImportMode.REPLACE_EVERYTHING) {
                gson.fromJson(currentManifestFile.readText(), BackupManifest::class.java)
            } else {
                manifest.copy(entries = emptyList())
            }

            val newEntries = mutableListOf<com.semseytech.rtsdevicesuitepro.backup.model.ManifestEntry>()
            if (mode == ImportMode.ADD_TO_EXISTING) {
                newEntries.addAll(currentManifest.entries)
            } else if (mode == ImportMode.REPLACE_CATEGORIES) {
                newEntries.addAll(currentManifest.entries.filter { it.category !in selectedCategories })
            }

            manifest.entries.forEach { entry ->
                if (entry.category in categoriesToImport || entry.itemType in categoriesToImport) {
                    val srcFile = File(extractDir, entry.filePath ?: "")
                    if (srcFile.exists()) {
                        val destFile = File(viewerDir, entry.filePath ?: "")
                        destFile.parentFile?.mkdirs()
                        srcFile.copyTo(destFile, overwrite = true)
                        
                        // Add to manifest if not already there (by identifier)
                        if (newEntries.none { it.identifier == entry.identifier }) {
                            newEntries.add(entry)
                        }
                    }
                }
            }

            currentManifestFile.writeText(gson.toJson(currentManifest.copy(entries = newEntries)))

            onProgress(0.95f, "Cleaning up...")
            tempFile.delete()
            extractDir.deleteRecursively()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            false
        }
    }

    private fun mergeDataFiles(srcDir: File, destDir: File, categories: Set<String>, mode: ImportMode) {
        destDir.mkdirs()
        
        // 1. SMS/MMS
        if (categories.contains("SMS") || categories.contains("sms_thread")) {
            val srcIndexFile = File(srcDir, "index.json")
            if (srcIndexFile.exists()) {
                val destIndexFile = File(destDir, "index.json")
                val type = object : TypeToken<MutableList<SmsExtractor.SmsThread>>() {}.type
                val srcThreads: MutableList<SmsExtractor.SmsThread> = gson.fromJson(srcIndexFile.readText(), type)
                
                val mergedThreads = if (destIndexFile.exists() && mode == ImportMode.ADD_TO_EXISTING) {
                    val destThreads: MutableList<SmsExtractor.SmsThread> = gson.fromJson(destIndexFile.readText(), type)
                    (destThreads + srcThreads).distinctBy { it.thread_id }.toMutableList()
                } else {
                    srcThreads
                }
                destIndexFile.writeText(gson.toJson(mergedThreads))

                // Copy thread files and mms
                File(srcDir, "threads").listFiles()?.forEach { 
                    it.copyTo(File(destDir, "threads/${it.name}"), overwrite = true)
                }
                val srcMms = File(srcDir, "mms")
                if (srcMms.exists()) {
                    srcMms.copyRecursively(File(destDir, "mms"), overwrite = true)
                }
            }
        }

        // 2. Generic JSON (Calls, Contacts, Settings)
        mergeJsonList<BackupItem.CallLogEntry>(srcDir, destDir, "calls.json", categories.contains("Call Log") || categories.contains("calls"), mode)
        mergeJsonList<BackupItem.Contact>(srcDir, destDir, "contacts.json", categories.contains("Contact") || categories.contains("contacts"), mode)
        mergeJsonList<BackupItem.SystemSetting>(srcDir, destDir, "settings.json", categories.contains("Setting") || categories.contains("settings"), mode)
    }

    private inline fun <reified T : BackupItem> mergeJsonList(srcDir: File, destDir: File, fileName: String, shouldProcess: Boolean, mode: ImportMode) {
        if (!shouldProcess) return
        val srcFile = File(srcDir, fileName)
        if (!srcFile.exists()) return

        val destFile = File(destDir, fileName)
        val type = object : TypeToken<MutableList<T>>() {}.type
        val srcItems: MutableList<T> = gson.fromJson(srcFile.readText(), type)

        val mergedItems = if (destFile.exists() && mode == ImportMode.ADD_TO_EXISTING) {
            val destItems: MutableList<T> = gson.fromJson(destFile.readText(), type)
            (destItems + srcItems).distinctBy { it.id }.toMutableList()
        } else {
            srcItems
        }
        destFile.writeText(gson.toJson(mergedItems))
    }
}
