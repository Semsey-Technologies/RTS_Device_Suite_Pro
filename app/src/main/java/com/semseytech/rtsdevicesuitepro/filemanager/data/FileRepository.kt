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

class FileRepository(private val smbDao: SmbDao) {
    private val TAG = "FileRepository"

    val smbConnections = smbDao.getAllConnections()

    suspend fun saveSmbConnection(connection: SmbConnection) = smbDao.insertConnection(connection)

    suspend fun updateSmbConnection(connection: SmbConnection) = smbDao.updateConnection(connection)

    suspend fun deleteSmbConnection(connection: SmbConnection) = smbDao.deleteConnection(connection)

    suspend fun getSmbConnection(host: String) = smbDao.getConnectionByHost(host)

    suspend fun deleteSmbFile(host: String, user: String, pass: String, path: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val smbFile = SmbFile("smb://$host/${path.removePrefix("/")}", authContext)
        smbFile.delete()
    }

    suspend fun renameSmbFile(host: String, user: String, pass: String, oldPath: String, newName: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val oldFile = SmbFile("smb://$host/${oldPath.removePrefix("/")}", authContext)
        val parent = oldFile.parent
        val newFile = SmbFile(parent + newName, authContext)
        oldFile.renameTo(newFile)
    }

    suspend fun copyFileLocalToSmb(localFile: java.io.File, smbHost: String, user: String, pass: String, smbDestPath: String) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val destUrl = "smb://$smbHost/${smbDestPath.removePrefix("/")}${if (smbDestPath.endsWith("/")) "" else "/"}${localFile.name}"
        val smbFile = SmbFile(destUrl, authContext)
        
        localFile.inputStream().use { input ->
            // Use getOutputStream() which is safer for some server configurations
            smbFile.openOutputStream().use { output ->
                val buffer = ByteArray(1024 * 1024) // 1MB buffer for performance
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
        }
    }

    suspend fun copyFileSmbToLocal(smbUrl: String, user: String, pass: String, localDestDir: java.io.File) = withContext(Dispatchers.IO) {
        val authContext = getAuthContext(user, pass)
        val smbFile = SmbFile(smbUrl, authContext)
        val localFile = java.io.File(localDestDir, smbFile.name)
        
        smbFile.openInputStream().use { input ->
            localFile.outputStream().use { output ->
                val buffer = ByteArray(1024 * 1024) // 1MB buffer for performance
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
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
        
        // Add stability properties for transfers
        props.setProperty("jcifs.smb.client.rcv_buf_size", "1048576") // 1MB
        props.setProperty("jcifs.smb.client.snd_buf_size", "1048576") // 1MB
        props.setProperty("jcifs.smb.client.transaction_buf_size", "1048576") // 1MB
        // Enable "Large Read/Write" for performance on modern servers
        props.setProperty("jcifs.smb.client.useLargeReadWrite", "true")
        
        val config = PropertyConfiguration(props)
        val baseContext = BaseContext(config)
        
        return if (user.isNotEmpty()) {
            val parts = if (user.contains("\\")) {
                user.split("\\", limit = 2)
            } else if (user.contains("@")) {
                val atParts = user.split("@", limit = 2)
                listOf(atParts[1], atParts[0])
            } else {
                listOf(null, user)
            }
            baseContext.withCredentials(jcifs.smb.NtlmPasswordAuthenticator(parts[0], if (parts.size > 1) parts[1] else parts[0]!!, pass))
        } else {
            baseContext.withAnonymousCredentials()
        }
    }

    suspend fun listSmbFiles(host: String, user: String, pass: String, path: String = ""): List<ExplorerFileItem> = withContext(Dispatchers.IO) {
        try {
            val authContext = getAuthContext(user, pass)

            // Ensure path ends with slash for SMB
            val formattedPath = if (path.isEmpty() || path == "/") "" else if (path.endsWith("/")) path else "$path/"
            val url = "smb://$host/$formattedPath"
            Log.d(TAG, "Attempting SMB connection to: $url with user: $user")
            
            val smbFile = SmbFile(url, authContext)
            
            // Explicitly trigger a connection check
            smbFile.exists() 
            
            val files = smbFile.listFiles().map { file ->
                ExplorerFileItem(
                    name = file.name.removeSuffix("/"),
                    path = "smb://$host/${path.removePrefix("/")}/${file.name.removeSuffix("/")}".replace("//", "/").replace("smb:/", "smb://"),
                    isDirectory = file.isDirectory,
                    size = if (file.isDirectory) 0 else file.length(),
                    lastModified = file.lastModified()
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            
            Log.d(TAG, "Successfully listed ${files.size} SMB files")
            files
        } catch (e: Exception) {
            Log.e(TAG, "SMB Error: ${e.message}", e)
            throw e // Re-throw to handle in ViewModel
        }
    }
}
