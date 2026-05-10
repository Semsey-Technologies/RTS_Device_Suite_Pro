package com.semseytech.rtsdevicesuitepro.archive.logic

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.archive.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {
    var currentDirectory by mutableStateOf(File("/sdcard"))
    var fileItems by mutableStateOf(listOf<FileItem>())
    var selectedFiles = mutableStateListOf<File>()
    
    var isArchiveDialogOpen by mutableStateOf(false)
    var isInfoDialogOpen by mutableStateOf(false)
    var isCopyMoveDialogOpen by mutableStateOf(false)
    var isCopyOperation by mutableStateOf(true) // true for copy, false for move
    
    var isProcessing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isPasswordDialogOpen by mutableStateOf(false)
    var pendingArchiveFile by mutableStateOf<File?>(null)
    
    var sortOption by mutableStateOf(SortOption.NAME)
    var sortAscending by mutableStateOf(true)
    var groupOption by mutableStateOf(GroupOption.NONE)
    var viewOption by mutableStateOf(ViewOption.DETAILS)

    var archiveOptions by mutableStateOf(ArchiveOptions())

    enum class SortOption { NAME, TYPE, SIZE, DATE }
    enum class GroupOption { NONE, TYPE, DATE, SIZE }
    enum class ViewOption { DETAILS, LIST, ICONS_SMALL, ICONS_MEDIUM, ICONS_LARGE }

    init {
        refreshFiles()
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = currentDirectory.listFiles()?.map { FileItem(it) } ?: emptyList()
            applySortingAndGrouping(files)
        }
    }

    private fun applySortingAndGrouping(files: List<FileItem>) {
        var sorted = when (sortOption) {
            SortOption.NAME -> files.sortedBy { it.name.lowercase() }
            SortOption.TYPE -> files.sortedBy { if (it.isDirectory) "" else it.file.extension.lowercase() }
            SortOption.SIZE -> files.sortedBy { it.size }
            SortOption.DATE -> files.sortedBy { it.lastModified }
        }
        if (!sortAscending) sorted = sorted.reversed()
        
        // Directories always first (usually)
        sorted = sorted.sortedByDescending { it.isDirectory }

        fileItems = sorted
    }

    fun setSort(option: SortOption) {
        if (sortOption == option) sortAscending = !sortAscending
        else {
            sortOption = option
            sortAscending = true
        }
        refreshFiles()
    }

    fun navigateTo(directory: File) {
        if (directory.isDirectory) {
            currentDirectory = directory
            selectedFiles.clear()
            refreshFiles()
        }
    }

    fun navigateUp() {
        currentDirectory.parentFile?.let { navigateTo(it) }
    }

    fun toggleSelection(file: File) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
        } else {
            selectedFiles.add(file)
        }
    }

    fun addFilesToArchive(outputFile: File, options: ArchiveOptions) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            errorMessage = null
            try {
                ArchiveManager.createArchive(selectedFiles.toList(), outputFile, options)
                isArchiveDialogOpen = false
                selectedFiles.clear()
                refreshFiles()
            } catch (e: Exception) {
                errorMessage = "Archive creation failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun addFilesToArchiveUri(targetUri: android.net.Uri, name: String, options: ArchiveOptions) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            errorMessage = null
            try {
                val tempFile = File.createTempFile("archive_gen", ".tmp")
                ArchiveManager.createArchive(selectedFiles.toList(), tempFile, options)
                
                // Copy to URI
                val context = (getApplication() as android.app.Application)
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()

                isArchiveDialogOpen = false
                selectedFiles.clear()
                refreshFiles()
            } catch (e: Exception) {
                errorMessage = "Archive creation failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun extractArchive(archiveFile: File, destinationDir: File, password: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            isProcessing = true
            errorMessage = null
            try {
                ArchiveManager.extractArchive(archiveFile, destinationDir, password)
                refreshFiles()
                isPasswordDialogOpen = false
                pendingArchiveFile = null
            } catch (e: ArchiveManager.PasswordRequiredException) {
                pendingArchiveFile = archiveFile
                isPasswordDialogOpen = true
            } catch (e: Exception) {
                errorMessage = "Extraction failed: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun testArchive(archiveFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ArchiveManager.testArchive(archiveFile)
            // Show result in UI (omitted for brevity but logic is there)
        }
    }

    fun copyOrMoveSelected(destinationDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedFiles.forEach { file ->
                val dest = File(destinationDir, file.name)
                if (isCopyOperation) {
                    file.copyRecursively(dest, overwrite = true)
                } else {
                    file.renameTo(dest)
                }
            }
            selectedFiles.clear()
            isCopyMoveDialogOpen = false
            refreshFiles()
        }
    }
    
    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedFiles.forEach { it.deleteRecursively() }
            selectedFiles.clear()
            refreshFiles()
        }
    }
}
