package com.semseytech.rtsdevicesuitepro.wipe.engine

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.semseytech.rtsdevicesuitepro.wipe.model.WipeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import kotlin.math.min

class SecureWipeEngine(private val context: Context) {

    private val random = SecureRandom()

    fun wipeFile(
        file: File,
        passes: Int = 1,
        isSimulation: Boolean = false
    ): Flow<WipeProgress> = flow {
        if (!file.exists()) {
            emit(WipeProgress(file.name, 1f, "File not found", 0, passes))
            return@flow
        }

        val length = file.length()
        
        for (pass in 1..passes) {
            emit(WipeProgress(file.name, (pass - 1).toFloat() / passes, "Pass $pass/$passes: Overwriting with ${if (pass == passes) "zeros" else "random data"}...", pass, passes))
            
            if (!isSimulation) {
                try {
                    overwrite(file, length, pass == passes)
                } catch (e: Exception) {
                    emit(WipeProgress(file.name, 1f, "Error during overwrite: ${e.message}", pass, passes))
                    return@flow
                }
            } else {
                // Simulate time based on file size, max 2 seconds per pass for simulation
                val simulateDelay = min(length / 1024 / 10, 2000L) 
                delay(simulateDelay)
            }
        }

        emit(WipeProgress(file.name, 1f, "Deleting file...", passes, passes))
        if (!isSimulation) {
            file.delete()
        } else {
            delay(200)
        }
        
        emit(WipeProgress(file.name, 1f, "Wipe Complete", passes, passes))
    }.flowOn(Dispatchers.IO)

    fun wipeDirectory(
        directory: File,
        passes: Int = 1,
        isSimulation: Boolean = false
    ): Flow<WipeProgress> = flow {
        if (!directory.exists() || !directory.isDirectory) {
            emit(WipeProgress(directory.name, 1f, "Directory not found", 0, passes))
            return@flow
        }

        val files = directory.listFiles() ?: emptyArray()
        val totalFiles = files.size
        
        emit(WipeProgress(directory.name, 0f, "Preparing to wipe $totalFiles files...", 0, passes))

        files.forEachIndexed { index, file ->
            if (file.isDirectory) {
                // Recursive call (simplified for flow emission)
                wipeDirectory(file, passes, isSimulation).collect { progress ->
                    emit(progress.copy(detail = "[${index + 1}/$totalFiles] ${progress.detail}"))
                }
            } else {
                wipeFile(file, passes, isSimulation).collect { progress ->
                    emit(progress.copy(
                        progress = (index.toFloat() + progress.progress) / totalFiles,
                        detail = "[${index + 1}/$totalFiles] ${progress.detail}"
                    ))
                }
            }
        }

        emit(WipeProgress(directory.name, 1f, "Cleaning up directory structure...", passes, passes))
        if (!isSimulation) {
            directory.delete()
        }
        emit(WipeProgress(directory.name, 1f, "Directory Wipe Complete", passes, passes))
    }.flowOn(Dispatchers.IO)

    fun wipeFreeSpace(
        directory: File,
        passes: Int = 1,
        isSimulation: Boolean = false
    ): Flow<WipeProgress> = flow {
        val stat = StatFs(directory.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val fillerFile = File(directory, "rts_wipe_filler_${System.currentTimeMillis()}.tmp")

        emit(WipeProgress("Free Space", 0f, "Target: ${availableBytes / 1024 / 1024} MB", 0, passes))
        
        for (pass in 1..passes) {
            emit(WipeProgress("Free Space", (pass - 1).toFloat() / passes, "Pass $pass/$passes: Filling free space...", pass, passes))
            
            if (isSimulation) {
                for (i in 1..20) {
                    delay(100)
                    emit(WipeProgress("Free Space", (pass - 1).toFloat() / passes + (i / 20f / passes), "Simulating fill: ${availableBytes * i / 20 / 1024 / 1024} MB", pass, passes))
                }
            } else {
                try {
                    FileOutputStream(fillerFile).use { fos ->
                        val buffer = ByteArray(1024 * 1024) // 1MB buffer
                        var written = 0L
                        while (written < availableBytes) {
                            if (pass == passes) {
                                buffer.fill(0)
                            } else {
                                random.nextBytes(buffer)
                            }
                            val toWrite = min(buffer.size.toLong(), availableBytes - written).toInt()
                            fos.write(buffer, 0, toWrite)
                            written += toWrite
                            
                            if (written % (10 * 1024 * 1024) == 0L) { // Update UI every 10MB
                                emit(WipeProgress("Free Space", (pass - 1).toFloat() / passes + (written.toFloat() / availableBytes / passes), "Writing: ${written / 1024 / 1024} / ${availableBytes / 1024 / 1024} MB", pass, passes))
                            }
                        }
                        fos.flush()
                    }
                    fillerFile.delete() // Delete after each pass or just reuse? Let's delete to be safe with disk space
                } catch (e: Exception) {
                    emit(WipeProgress("Free Space", 1f, "Error: ${e.message}", pass, passes))
                    fillerFile.delete()
                    return@flow
                }
            }
        }
        
        emit(WipeProgress("Free Space", 1f, "Free Space Wipe Complete", passes, passes))
    }.flowOn(Dispatchers.IO)

    fun clearAppData(isSimulation: Boolean = false): Flow<WipeProgress> = flow {
        emit(WipeProgress("App Cache", 0f, "Clearing cache...", 1, 1))
        if (!isSimulation) {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } else {
            delay(500)
        }
        
        emit(WipeProgress("App Data", 0.5f, "Clearing internal files...", 1, 1))
        if (!isSimulation) {
            context.filesDir.deleteRecursively()
            // Databases are usually better handled by context.deleteDatabase
            context.databaseList().forEach { dbName ->
                context.deleteDatabase(dbName)
            }
        } else {
            delay(500)
        }
        
        emit(WipeProgress("App Reset", 1f, "App data cleared successfully", 1, 1))
    }.flowOn(Dispatchers.IO)

    private fun overwrite(file: File, length: Long, isLastPass: Boolean) {
        RandomAccessFile(file, "rws").use { raf ->
            val bufferSize = 64 * 1024 // 64KB
            val buffer = ByteArray(bufferSize)
            var offset = 0L
            
            while (offset < length) {
                if (isLastPass) {
                    buffer.fill(0)
                } else {
                    random.nextBytes(buffer)
                }
                
                val toWrite = min(bufferSize.toLong(), length - offset).toInt()
                raf.write(buffer, 0, toWrite)
                offset += toWrite
            }
            raf.setLength(0) // Truncate after overwriting
        }
    }

    fun verifyWipe(file: File): Boolean {
        if (file.exists()) return false
        // Advanced verification would check if disk blocks are actually zeroed, but that's not possible without root/low-level access in Android
        return true
    }
}
