package com.semseytech.rtsdevicesuitepro.restore.engine

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class ArchiveReader(private val archiveFile: File) {

    fun extractTo(targetDir: File, onProgress: (Float, String) -> Unit) {
        val fileName = archiveFile.name.lowercase()
        android.util.Log.d("ArchiveReader", "Extracting $fileName to ${targetDir.absolutePath}")
        try {
            when {
                fileName.endsWith(".7z") -> extract7z(targetDir, onProgress)
                fileName.endsWith(".zip") -> extractArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), targetDir, onProgress)
                fileName.endsWith(".tar") -> extractArchive(TarArchiveInputStream(FileInputStream(archiveFile)), targetDir, onProgress)
                fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> 
                    extractArchive(TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, onProgress)
                fileName.endsWith(".tar.bz2") -> 
                    extractArchive(TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, onProgress)
                fileName.endsWith(".tar.xz") -> 
                    extractArchive(TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, onProgress)
                else -> {
                    android.util.Log.e("ArchiveReader", "Unsupported extension for $fileName")
                    // Try ZIP as fallback if it's a generic file but might be a ZIP
                    extractArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), targetDir, onProgress)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ArchiveReader", "Extraction failed", e)
            throw e
        }
    }

    fun extractSelective(targetDir: File, wantedPaths: Set<String>, onProgress: (Float, String) -> Unit) {
        val fileName = archiveFile.name.lowercase()
        android.util.Log.d("ArchiveReader", "Selectively extracting from $fileName to ${targetDir.absolutePath}")
        try {
            when {
                fileName.endsWith(".7z") -> extractSelective7z(targetDir, wantedPaths, onProgress)
                fileName.endsWith(".zip") -> extractSelectiveArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), targetDir, wantedPaths, onProgress)
                fileName.endsWith(".tar") -> extractSelectiveArchive(TarArchiveInputStream(FileInputStream(archiveFile)), targetDir, wantedPaths, onProgress)
                fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> 
                    extractSelectiveArchive(TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, wantedPaths, onProgress)
                fileName.endsWith(".tar.bz2") -> 
                    extractSelectiveArchive(TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, wantedPaths, onProgress)
                fileName.endsWith(".tar.xz") -> 
                    extractSelectiveArchive(TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), targetDir, wantedPaths, onProgress)
                else -> extractSelectiveArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), targetDir, wantedPaths, onProgress)
            }
        } catch (e: Exception) {
            android.util.Log.e("ArchiveReader", "Selective extraction failed", e)
            throw e
        }
    }

    fun extractFile(internalPath: String, targetFile: File) {
        val fileName = archiveFile.name.lowercase()
        try {
            when {
                fileName.endsWith(".7z") -> extractFile7z(internalPath, targetFile)
                fileName.endsWith(".zip") -> extractFileArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), internalPath, targetFile)
                fileName.endsWith(".tar") -> extractFileArchive(TarArchiveInputStream(FileInputStream(archiveFile)), internalPath, targetFile)
                fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") -> 
                    extractFileArchive(TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), internalPath, targetFile)
                fileName.endsWith(".tar.bz2") -> 
                    extractFileArchive(TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), internalPath, targetFile)
                fileName.endsWith(".tar.xz") -> 
                    extractFileArchive(TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))), internalPath, targetFile)
                else -> extractFileArchive(ZipArchiveInputStream(FileInputStream(archiveFile)), internalPath, targetFile)
            }
        } catch (e: Exception) {
            android.util.Log.e("ArchiveReader", "Extraction of $internalPath failed", e)
        }
    }

    private fun extractFile7z(internalPath: String, targetFile: File) {
        SevenZFile(archiveFile).use { sevenZFile ->
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                if (entry.name == internalPath) {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8192)
                        var n: Int
                        while (sevenZFile.read(buffer).also { n = it } != -1) {
                            output.write(buffer, 0, n)
                        }
                    }
                    return
                }
                entry = sevenZFile.nextEntry
            }
        }
    }

    private fun extractFileArchive(ais: ArchiveInputStream<*>, internalPath: String, targetFile: File) {
        ais.use { input ->
            var entry: ArchiveEntry? = input.nextEntry
            while (entry != null) {
                if (entry.name == internalPath) {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                    return
                }
                entry = input.nextEntry
            }
        }
    }

    private fun extract7z(targetDir: File, onProgress: (Float, String) -> Unit) {
        SevenZFile(archiveFile).use { sevenZFile ->
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var n: Int
                        while (sevenZFile.read(buffer).also { n = it } != -1) {
                            output.write(buffer, 0, n)
                        }
                    }
                }
                onProgress(0f, "Extracting: ${entry.name}")
                entry = sevenZFile.nextEntry
            }
        }
    }

    private fun extractArchive(ais: ArchiveInputStream<*>, targetDir: File, onProgress: (Float, String) -> Unit) {
        ais.use { input ->
            var entry: ArchiveEntry? = input.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                onProgress(0f, "Extracting: ${entry.name}")
                entry = input.nextEntry
            }
        }
    }

    private fun extractSelective7z(targetDir: File, wantedPaths: Set<String>, onProgress: (Float, String) -> Unit) {
        SevenZFile(archiveFile).use { sevenZFile ->
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                if (wantedPaths.any { entry!!.name == it || entry!!.name.startsWith("$it/") }) {
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            val buffer = ByteArray(8192)
                            var n: Int
                            while (sevenZFile.read(buffer).also { n = it } != -1) {
                                output.write(buffer, 0, n)
                            }
                        }
                    }
                    onProgress(0f, "Extracted: ${entry.name}")
                }
                entry = sevenZFile.nextEntry
            }
        }
    }

    private fun extractSelectiveArchive(ais: ArchiveInputStream<*>, targetDir: File, wantedPaths: Set<String>, onProgress: (Float, String) -> Unit) {
        ais.use { input ->
            var entry: ArchiveEntry? = input.nextEntry
            while (entry != null) {
                if (wantedPaths.any { entry!!.name == it || entry!!.name.startsWith("$it/") }) {
                    val file = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    onProgress(0f, "Extracted: ${entry.name}")
                }
                entry = input.nextEntry
            }
        }
    }
}
