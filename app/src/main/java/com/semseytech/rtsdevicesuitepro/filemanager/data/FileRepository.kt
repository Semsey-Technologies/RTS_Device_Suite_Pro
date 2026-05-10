package com.semseytech.rtsdevicesuitepro.filemanager.data

import android.util.Log
import jcifs.CIFSContext
import jcifs.context.BaseContext
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import jcifs.config.PropertyConfiguration
import com.semseytech.rtsdevicesuitepro.filemanager.ExplorerFileItem

class FileRepository(private val smbDao: SmbDao, private val favoriteDao: FavoriteDao) {
    private val TAG = "FileRepository"

    val smbConnections = smbDao.getAllConnections()
    val favorites = favoriteDao.getAllFavorites()

    suspend fun saveFavorite(favorite: FavoriteLocation) = favoriteDao.insertFavorite(favorite)
    suspend fun deleteFavorite(favorite: FavoriteLocation) = favoriteDao.deleteFavorite(favorite)
    suspend fun deleteFavoriteByPath(path: String) = favoriteDao.deleteFavoriteByPath(path)

    suspend fun saveSmbConnection(connection: SmbConnection) = smbDao.insertConnection(connection)

    suspend fun updateSmbConnection(connection: SmbConnection) = smbDao.updateConnection(connection)

    suspend fun deleteSmbConnection(connection: SmbConnection) = smbDao.deleteConnection(connection)

    suspend fun getSmbConnection(host: String) = smbDao.getConnectionByHost(host)

    suspend fun deleteSmbFile(host: String, user: String, pass: String, path: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        
        // Safe construction of SmbFile for deletion
        val hostPart = host.trimEnd('/')
        val pathParts = path.trim('/').split('/')
        if (pathParts.isEmpty()) return@withContext
        
        var current = SmbFile("smb://$hostPart/", authContext)
        for (i in pathParts.indices) {
            val name = pathParts[i]
            // If it's not the last part, it's definitely a directory
            // If it is the last part, we check if it was intended to be a directory
            val isLast = i == pathParts.size - 1
            val suffix = if (!isLast || path.endsWith("/")) "/" else ""
            current = SmbFile(current, name + suffix)
        }
        
        recursiveDeleteSmb(current)
    }

    private fun recursiveDeleteSmb(smbFile: SmbFile) {
        if (smbFile.isDirectory) {
            smbFile.listFiles().forEach { child ->
                recursiveDeleteSmb(child)
            }
        }
        smbFile.delete()
    }

    suspend fun renameSmbFile(host: String, user: String, pass: String, oldPath: String, newName: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val oldFile = SmbFile("smb://$host/${oldPath.removePrefix("/")}", authContext)
        val parent = oldFile.parent
        val newFile = SmbFile(parent + newName, authContext)
        oldFile.renameTo(newFile)
    }

    suspend fun createSmbFolder(host: String, user: String, pass: String, parentPath: String, name: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val url = if (parentPath.startsWith("smb://")) parentPath else "smb://$host/${parentPath.trim('/')}/"
        val parentDir = SmbFile(if (url.endsWith("/")) url else "$url/", authContext)
        val newDir = SmbFile(parentDir, name + "/")
        if (!newDir.exists()) {
            newDir.mkdir()
        }
    }

    suspend fun createSmbFile(host: String, user: String, pass: String, parentPath: String, name: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val url = if (parentPath.startsWith("smb://")) parentPath else "smb://$host/${parentPath.trim('/')}/"
        val parentDir = SmbFile(if (url.endsWith("/")) url else "$url/", authContext)
        val newFile = SmbFile(parentDir, name)
        if (!newFile.exists()) {
            newFile.createNewFile()
        }
    }

    suspend fun copyFileLocalToSmb(localFile: java.io.File, smbHost: String, user: String, pass: String, smbDestPath: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        
        // Use the SmbFile(SmbFile parent, String child) constructor for safe encoding of names
        val hostPart = smbHost.trimEnd('/')
        val pathPart = smbDestPath.trim('/')
        val basePath = if (pathPart.isEmpty()) "smb://$hostPart/" else "smb://$hostPart/$pathPart/"
        
        val parentDir = SmbFile(basePath, authContext)
        val smbFile = SmbFile(parentDir, localFile.name + if (localFile.isDirectory) "/" else "")
        
        recursiveCopyLocalToSmb(localFile, smbFile)
    }

    private fun recursiveCopyLocalToSmb(localFile: java.io.File, smbDest: SmbFile) {
        if (localFile.isDirectory) {
            if (!smbDest.exists()) {
                smbDest.mkdir()
            }
            localFile.listFiles()?.forEach { child ->
                val childSmb = SmbFile(smbDest, child.name + if (child.isDirectory) "/" else "")
                recursiveCopyLocalToSmb(child, childSmb)
            }
        } else {
            localFile.inputStream().use { input ->
                smbDest.openOutputStream().use { output ->
                    val buffer = ByteArray(1024 * 64) // Smaller buffer for compatibility
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
        }
    }

    suspend fun copyFileSmbToLocal(smbUrl: String, user: String, pass: String, localDestDir: java.io.File) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        // Ensure trailing slash for directories to avoid "Parameter is incorrect"
        val correctedUrl = if (!smbUrl.contains("?") && !smbUrl.endsWith("/")) {
            val tempFile = SmbFile(smbUrl, authContext)
            if (try { tempFile.isDirectory } catch (e: Exception) { false }) "$smbUrl/" else smbUrl
        } else smbUrl
        
        val smbFile = SmbFile(correctedUrl, authContext)
        recursiveCopySmbToLocal(smbFile, localDestDir)
    }

    private fun recursiveCopySmbToLocal(smbFile: SmbFile, localDestDir: java.io.File) {
        val name = smbFile.name.removeSuffix("/")
        if (name.isEmpty()) return // Should not happen with well-formed URLs
        
        val localFile = java.io.File(localDestDir, name)
        
        if (try { smbFile.isDirectory } catch (e: Exception) { false }) {
            if (!localFile.exists()) {
                val created = localFile.mkdirs()
                if (!created && !localFile.exists()) {
                    Log.e(TAG, "Failed to create local directory: ${localFile.absolutePath}")
                }
            }
            try {
                val children = smbFile.listFiles()
                children?.forEach { child ->
                    recursiveCopySmbToLocal(child, localFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error listing children of ${smbFile.url}: ${e.message}")
            }
        } else {
            try {
                smbFile.openInputStream().use { input ->
                    localFile.outputStream().use { output ->
                        val buffer = ByteArray(1024 * 64) // Smaller buffer for compatibility
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying file ${smbFile.url}: ${e.message}")
                throw e
            }
        }
    }

    private fun getAuthContext(user: String, pass: String): CIFSContext {
        val props = Properties()
        props.setProperty("jcifs.smb.client.enableSMB2", "true")
        props.setProperty("jcifs.smb.client.disableSMB1", "false")
        props.setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
        props.setProperty("jcifs.smb.lmCompatibility", "3")
        props.setProperty("jcifs.smb.client.signingPreferred", "true")
        
        // Default buffer sizes are often more compatible
        // props.setProperty("jcifs.smb.client.rcv_buf_size", "1048576") 
        // props.setProperty("jcifs.smb.client.snd_buf_size", "1048576")
        // props.setProperty("jcifs.smb.client.transaction_buf_size", "1048576")
        // Large Read/Write can cause "Invalid Parameter" on some older SMB2 implementations
        props.setProperty("jcifs.smb.client.useLargeReadWrite", "false")
        
        val config = PropertyConfiguration(props)
        val baseContext = BaseContext(config)
        
        return if (user.isNotEmpty()) {
            val domain: String?
            val username: String
            
            when {
                user.contains("\\") -> {
                    val parts = user.split("\\", limit = 2)
                    domain = parts[0]
                    username = parts[1]
                }
                user.contains("@") -> {
                    val parts = user.split("@", limit = 2)
                    domain = parts[1]
                    username = parts[0]
                }
                else -> {
                    domain = null // Reverting to null as it was in the initial version
                    username = user
                }
            }
            baseContext.withCredentials(jcifs.smb.NtlmPasswordAuthenticator(domain, username, pass))
        } else {
            baseContext.withAnonymousCredentials()
        }
    }

    suspend fun listSmbFiles(host: String, user: String, pass: String, path: String = ""): List<ExplorerFileItem> = withContext(Dispatchers.IO) {
        try {
            val authContext = getAuthContext(user, pass)

            // Ensure path ends with slash for SMB directories
            val cleanPath = path.trim('/')
            val url = if (cleanPath.isEmpty()) "smb://$host/" else "smb://$host/$cleanPath/"
            Log.d(TAG, "Listing SMB files: $url")
            
            val smbFile = SmbFile(url, authContext)
            
            val files = smbFile.listFiles().map { file ->
                val isDir = file.isDirectory
                // Use a more robust path construction for the UI
                val fileName = file.name
                val itemPath = if (url.endsWith("/")) "$url$fileName" else "$url/$fileName"
                
                ExplorerFileItem(
                    name = fileName.removeSuffix("/"),
                    path = itemPath,
                    isDirectory = isDir,
                    size = if (isDir) 0 else file.length(),
                    lastModified = file.lastModified()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            
            Log.d(TAG, "Successfully listed ${files.size} SMB files")
            files
        } catch (e: Exception) {
            Log.e(TAG, "SMB List Error: ${e.message}", e)
            throw e
        }
    }
}
