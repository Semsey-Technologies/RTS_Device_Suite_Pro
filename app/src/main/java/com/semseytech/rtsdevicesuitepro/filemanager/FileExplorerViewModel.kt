package com.semseytech.rtsdevicesuitepro.filemanager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.filemanager.data.AppDatabase
import com.semseytech.rtsdevicesuitepro.filemanager.data.FileRepository
import com.semseytech.rtsdevicesuitepro.filemanager.data.SmbConnection
import com.semseytech.rtsdevicesuitepro.filemanager.data.FavoriteLocation
import com.semseytech.rtsdevicesuitepro.net.NetworkScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = NetworkScanner(application)
    private val database = AppDatabase.getDatabase(application)
    private val repository = FileRepository(database.smbDao(), database.favoriteDao())

    private val _uiState = MutableStateFlow(FileExplorerState())
    val uiState: StateFlow<FileExplorerState> = _uiState.asStateFlow()

    private val _clipboard = MutableStateFlow<ClipboardItem?>(null)
    val clipboard = _clipboard.asStateFlow()

    val savedConnections = repository.smbConnections

    init {
        val internalStorage = Environment.getExternalStorageDirectory().absolutePath
        loadPath(internalStorage, isLeftPane = true)
        loadPath(internalStorage, isLeftPane = false)
        
        viewModelScope.launch {
            repository.favorites.collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
    }

    fun loadPath(path: String, isLeftPane: Boolean, addToHistory: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            updatePane(isLeftPane) { it.copy(isScanning = true, errorMessage = null) }
            
            try {
                val files = if (path.startsWith("smb://")) {
                    val uri = Uri.parse(path)
                    val host = uri.host ?: ""
                    val savedConn = repository.getSmbConnection(host)
                    repository.listSmbFiles(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "")
                } else {
                    val folder = File(path)
                    if (folder.exists() && folder.isDirectory) {
                        val allFiles = folder.listFiles() ?: emptyArray()
                        val filteredList = if (_uiState.value.isSystemAccessEnabled) {
                            allFiles.toList()
                        } else {
                            allFiles.filter { !it.name.startsWith(".") }
                        }

                        filteredList.map {
                            ExplorerFileItem(
                                name = it.name,
                                path = it.absolutePath,
                                isDirectory = it.isDirectory,
                                size = it.length(),
                                lastModified = it.lastModified()
                            )
                        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    } else emptyList()
                }

                updatePane(isLeftPane) { current ->
                    val newHistory = if (addToHistory) {
                        current.history.take(current.historyIndex + 1) + path
                    } else current.history
                    
                    current.copy(
                        items = files,
                        currentPath = path,
                        isScanning = false,
                        history = newHistory,
                        historyIndex = if (addToHistory) newHistory.lastIndex else current.historyIndex
                    )
                }
            } catch (e: Exception) {
                updatePane(isLeftPane) { it.copy(isScanning = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun navigateBack(isLeftPane: Boolean) {
        val pane = if (isLeftPane) _uiState.value.leftPane else _uiState.value.rightPane
        if (pane.historyIndex > 0) {
            val newIndex = pane.historyIndex - 1
            val path = pane.history[newIndex]
            updatePane(isLeftPane) { it.copy(historyIndex = newIndex) }
            loadPath(path, isLeftPane, addToHistory = false)
        }
    }

    fun navigateForward(isLeftPane: Boolean) {
        val pane = if (isLeftPane) _uiState.value.leftPane else _uiState.value.rightPane
        if (pane.historyIndex < pane.history.size - 1) {
            val newIndex = pane.historyIndex + 1
            val path = pane.history[newIndex]
            updatePane(isLeftPane) { it.copy(historyIndex = newIndex) }
            loadPath(path, isLeftPane, addToHistory = false)
        }
    }

    fun toggleSplitScreen() {
        _uiState.update { it.copy(isSplitScreen = !it.isSplitScreen) }
    }

    fun setSystemAccess(enabled: Boolean) {
        _uiState.update { it.copy(isSystemAccessEnabled = enabled) }
        refreshPanes()
    }

    private fun updatePane(isLeftPane: Boolean, update: (PaneState) -> PaneState) {
        _uiState.update { state ->
            if (isLeftPane) state.copy(leftPane = update(state.leftPane))
            else state.copy(rightPane = update(state.rightPane))
        }
    }

    fun startSmbScan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningNetwork = true, networkNodes = emptyList()) }
            val scannedDevices = scanner.scanLocalNetwork()
            val nodes = scannedDevices.map { NetworkNode(it.name, it.ip, "SMB") }
            _uiState.update { it.copy(isScanningNetwork = false, networkNodes = nodes) }
        }
    }

    fun connectToNetwork(type: String, host: String, user: String, pass: String, save: Boolean, onConnected: (String) -> Unit) {
        viewModelScope.launch {
            if (type == "SMB") {
                if (save) repository.saveSmbConnection(SmbConnection(host, host, user, pass))
                onConnected("smb://$host/")
            }
        }
    }

    fun renameConnection(connection: SmbConnection, newName: String) {
        viewModelScope.launch { repository.updateSmbConnection(connection.copy(name = newName)) }
    }

    fun removeConnection(connection: SmbConnection) {
        viewModelScope.launch { repository.deleteSmbConnection(connection) }
    }

    fun openTerminal(context: Context, path: String) {
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            com.semseytech.rtsdevicesuitepro.terminal.core.IntegrationHooks.openTerminalAt(context, file)
        }
    }

    fun deleteFile(item: ExplorerFileItem, isLeftPane: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item.path.startsWith("smb://")) {
                    val uri = Uri.parse(item.path)
                    val host = uri.host ?: ""
                    val savedConn = repository.getSmbConnection(host)
                    repository.deleteSmbFile(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "")
                } else {
                    File(item.path).delete()
                }
                refreshPanes()
            } catch (e: Exception) {
                updatePane(isLeftPane) { it.copy(errorMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    fun renameFile(item: ExplorerFileItem, newName: String, isLeftPane: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item.path.startsWith("smb://")) {
                    val uri = Uri.parse(item.path)
                    val host = uri.host ?: ""
                    val savedConn = repository.getSmbConnection(host)
                    repository.renameSmbFile(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "", newName)
                } else {
                    val file = File(item.path)
                    file.renameTo(File(file.parentFile, newName))
                }
                refreshPanes()
            } catch (e: Exception) {
                updatePane(isLeftPane) { it.copy(errorMessage = "Rename failed: ${e.message}") }
            }
        }
    }

    fun createFolder(parentPath: String, name: String, isLeftPane: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (parentPath.startsWith("smb://")) {
                    val uri = Uri.parse(parentPath)
                    val host = uri.host ?: ""
                    val savedConn = repository.getSmbConnection(host)
                    repository.createSmbFolder(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "", name)
                } else {
                    val folder = File(parentPath, name)
                    if (!folder.exists() && !folder.mkdirs()) {
                        throw Exception("Could not create directory. Check permissions.")
                    }
                }
                refreshPanes()
            } catch (e: Exception) {
                updatePane(isLeftPane) { it.copy(errorMessage = "Create folder failed: ${e.message}") }
            }
        }
    }

    fun createFile(parentPath: String, name: String, isLeftPane: Boolean, onCreated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val absolutePath: String
                if (parentPath.startsWith("smb://")) {
                    val uri = Uri.parse(parentPath)
                    val host = uri.host ?: ""
                    val savedConn = repository.getSmbConnection(host)
                    repository.createSmbFile(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "", name)
                    absolutePath = if (parentPath.endsWith("/")) "$parentPath$name" else "$parentPath/$name"
                } else {
                    val file = File(parentPath, name)
                    if (!file.exists() && !file.createNewFile()) {
                        throw Exception("Could not create file. Check permissions.")
                    }
                    absolutePath = file.absolutePath
                }
                refreshPanes()
                viewModelScope.launch(Dispatchers.Main) {
                    onCreated(absolutePath)
                }
            } catch (e: Exception) {
                updatePane(isLeftPane) { it.copy(errorMessage = "Create file failed: ${e.message}") }
            }
        }
    }

    suspend fun generateUniqueName(parentPath: String, baseName: String, extension: String? = null): String {
        var name = if (extension != null) "$baseName.$extension" else baseName
        var counter = 1
        
        while (checkExists(parentPath, name)) {
            counter++
            name = if (extension != null) "$baseName ($counter).$extension" else "$baseName ($counter)"
        }
        return name
    }

    private suspend fun checkExists(parentPath: String, name: String): Boolean = withContext(Dispatchers.IO) {
        if (parentPath.startsWith("smb://")) {
            try {
                val uri = Uri.parse(parentPath)
                val host = uri.host ?: ""
                val savedConn = repository.getSmbConnection(host)
                val items = repository.listSmbFiles(host, savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "")
                items.any { it.name == name }
            } catch (e: Exception) {
                false
            }
        } else {
            File(parentPath, name).exists()
        }
    }

    private fun refreshPanes() {
        loadPath(_uiState.value.leftPane.currentPath, isLeftPane = true, addToHistory = false)
        loadPath(_uiState.value.rightPane.currentPath, isLeftPane = false, addToHistory = false)
    }

    fun copyToClipboard(item: ExplorerFileItem, isMove: Boolean) {
        _clipboard.value = ClipboardItem(item, isMove)
    }

    fun addFavorite(path: String, name: String) {
        viewModelScope.launch {
            val type = if (path.startsWith("smb://")) "SMB" else "LOCAL"
            repository.saveFavorite(FavoriteLocation(name = name, path = path, type = type))
        }
    }

    fun renameFavorite(favorite: FavoriteLocation, newName: String) {
        viewModelScope.launch {
            repository.saveFavorite(favorite.copy(name = newName))
        }
    }

    fun removeFavorite(favorite: FavoriteLocation) {
        viewModelScope.launch {
            repository.deleteFavorite(favorite)
        }
    }

    fun removeFavoriteByPath(path: String) {
        viewModelScope.launch {
            repository.deleteFavoriteByPath(path)
        }
    }

    fun paste(destPath: String, isLeftPane: Boolean) {
        val clip = _clipboard.value ?: return
        performTransfer(clip.item, destPath, clip.isMove, isLeftPane)
    }

    fun openWith(context: Context, item: ExplorerFileItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item.path.startsWith("smb://")) {
                    // SMB files need to be downloaded first to open with external apps
                    // For now, only local files support "Open With" easily
                } else {
                    val file = File(item.path)
                    val extension = MimeTypeMap.getFileExtensionFromUrl(item.path)
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
                    
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    val chooser = Intent.createChooser(intent, "Open with...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                Log.e("FileExplorerVM", "Open With failed", e)
            }
        }
    }

    fun performTransfer(source: ExplorerFileItem, destPath: String, isMove: Boolean, targetIsLeftPane: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updatePane(targetIsLeftPane) { it.copy(isScanning = true) }
                val sourceIsSmb = source.path.startsWith("smb://")
                val destIsSmb = destPath.startsWith("smb://")

                when {
                    !sourceIsSmb && !destIsSmb -> {
                        val srcFile = File(source.path)
                        val destFile = File(destPath, srcFile.name)
                        srcFile.copyRecursively(destFile, overwrite = true)
                        if (isMove) srcFile.deleteRecursively()
                    }
                    !sourceIsSmb && destIsSmb -> {
                        val uri = Uri.parse(destPath)
                        val savedConn = repository.getSmbConnection(uri.host ?: "")
                        repository.copyFileLocalToSmb(File(source.path), uri.host ?: "", savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "")
                        if (isMove) File(source.path).delete()
                    }
                    sourceIsSmb && !destIsSmb -> {
                        val uri = Uri.parse(source.path)
                        val savedConn = repository.getSmbConnection(uri.host ?: "")
                        repository.copyFileSmbToLocal(source.path, savedConn?.user ?: "", savedConn?.pass ?: "", File(destPath))
                        if (isMove) repository.deleteSmbFile(uri.host ?: "", savedConn?.user ?: "", savedConn?.pass ?: "", uri.path ?: "")
                    }
                }
                _clipboard.value = null
                refreshPanes()
            } catch (e: Exception) {
                updatePane(targetIsLeftPane) { it.copy(isScanning = false, errorMessage = "Transfer failed: ${e.message}") }
            }
        }
    }
}
