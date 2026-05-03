package com.semseytech.rtsdevicesuitepro.archive.logic

import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveOptions
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.*

object ArchiveManager {

    fun createArchive(files: List<File>, outputFile: File, options: ArchiveOptions) {
        when (options.format) {
            ArchiveFormat.ZIP -> createZip(files, outputFile, options)
            ArchiveFormat.SEVEN_Z -> create7z(files, outputFile, options)
            ArchiveFormat.TAR -> createTar(files, outputFile, options)
            ArchiveFormat.TAR_GZ -> createTarGz(files, outputFile, options)
            ArchiveFormat.TAR_BZ2 -> createTarBz2(files, outputFile, options)
            ArchiveFormat.TAR_XZ -> createTarXz(files, outputFile, options)
        }
    }

    private fun createZip(files: List<File>, outputFile: File, options: ArchiveOptions) {
        ZipArchiveOutputStream(outputFile).use { zos ->
            val level = when (options.level) {
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.STORE -> 0
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.FASTEST -> 1
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.FAST -> 3
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.NORMAL -> 5
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.MAXIMUM -> 7
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionLevel.ULTRA -> 9
            }
            zos.setLevel(level)
            
            val method = when (options.method) {
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionMethod.COPY -> ZipArchiveOutputStream.STORED
                else -> ZipArchiveOutputStream.DEFLATED
            }
            zos.setMethod(method)

            files.forEach { file ->
                addToArchive(zos, file, "", options)
            }
        }
    }

    private fun create7z(files: List<File>, outputFile: File, options: ArchiveOptions) {
        val szos = if (options.password.isNotEmpty()) {
            SevenZOutputFile(outputFile, options.password.toCharArray())
        } else {
            SevenZOutputFile(outputFile)
        }
        
        szos.use { s ->
            val method = when (options.method) {
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionMethod.LZMA -> org.apache.commons.compress.archivers.sevenz.SevenZMethod.LZMA
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionMethod.LZMA2 -> org.apache.commons.compress.archivers.sevenz.SevenZMethod.LZMA2
                com.semseytech.rtsdevicesuitepro.archive.model.CompressionMethod.BZIP2 -> org.apache.commons.compress.archivers.sevenz.SevenZMethod.BZIP2
                else -> org.apache.commons.compress.archivers.sevenz.SevenZMethod.LZMA2
            }
            
            val methods = mutableListOf<org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration>()
            if (options.password.isNotEmpty()) {
                methods.add(org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration(org.apache.commons.compress.archivers.sevenz.SevenZMethod.AES256SHA256))
            }
            methods.add(org.apache.commons.compress.archivers.sevenz.SevenZMethodConfiguration(method))
            s.setContentMethods(methods)

            files.forEach { file ->
                addTo7zArchive(s, file, "", options)
            }
        }
    }

    private fun createTar(files: List<File>, outputFile: File, options: ArchiveOptions) {
        TarArchiveOutputStream(FileOutputStream(outputFile)).use { tos ->
            files.forEach { file ->
                addToArchive(tos, file, "", options)
            }
        }
    }

    private fun createTarGz(files: List<File>, outputFile: File, options: ArchiveOptions) {
        TarArchiveOutputStream(GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))).use { tos ->
            files.forEach { file ->
                addToArchive(tos, file, "", options)
            }
        }
    }

    private fun createTarBz2(files: List<File>, outputFile: File, options: ArchiveOptions) {
        TarArchiveOutputStream(BZip2CompressorOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))).use { tos ->
            files.forEach { file ->
                addToArchive(tos, file, "", options)
            }
        }
    }

    private fun createTarXz(files: List<File>, outputFile: File, options: ArchiveOptions) {
        TarArchiveOutputStream(XZCompressorOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))).use { tos ->
            files.forEach { file ->
                addToArchive(tos, file, "", options)
            }
        }
    }

    private fun addToArchive(aos: ArchiveOutputStream<*>, file: File, base: String, options: ArchiveOptions) {
        val entryName = if (base.isEmpty()) file.name else "$base/${file.name}"
        
        when (aos) {
            is ZipArchiveOutputStream -> {
                val entry = ZipArchiveEntry(file, entryName)
                // We check if the stream is configured for STORED (no compression)
                // This requires us to provide size and CRC up front for ZipArchiveEntry
                if (options.method == com.semseytech.rtsdevicesuitepro.archive.model.CompressionMethod.COPY && !file.isDirectory) {
                    entry.method = ZipArchiveOutputStream.STORED
                    entry.size = file.length()
                    entry.compressedSize = file.length()
                    entry.crc = calculateCrc32(file)
                }
                aos.putArchiveEntry(entry)
            }
            is TarArchiveOutputStream -> {
                val entry = TarArchiveEntry(file, entryName)
                aos.putArchiveEntry(entry)
            }
            else -> throw IllegalArgumentException("Unsupported output stream")
        }

        if (file.isFile) {
            file.inputStream().use { it.copyTo(aos) }
        }
        aos.closeArchiveEntry()

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addToArchive(aos, child, entryName, options)
            }
        }
    }

    private fun calculateCrc32(file: File): Long {
        val crc = java.util.zip.CRC32()
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var len: Int
            while (input.read(buffer).also { len = it } != -1) {
                crc.update(buffer, 0, len)
            }
        }
        return crc.value
    }

    private fun addTo7zArchive(szos: SevenZOutputFile, file: File, base: String, options: ArchiveOptions) {
        val entryName = if (base.isEmpty()) file.name else "$base/${file.name}"
        val entry = szos.createArchiveEntry(file, entryName)
        szos.putArchiveEntry(entry)
        if (file.isFile) {
            val buffer = ByteArray(8192)
            file.inputStream().use { input ->
                var len: Int
                while (input.read(buffer).also { len = it } > 0) {
                    szos.write(buffer, 0, len)
                }
            }
        }
        szos.closeArchiveEntry()

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addTo7zArchive(szos, child, entryName, options)
            }
        }
    }

    fun extractArchive(archiveFile: File, destinationDir: File, password: String? = null) {
        if (!destinationDir.exists()) destinationDir.mkdirs()
        
        val name = archiveFile.name.lowercase()
        when {
            name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".apk") -> extractZip(archiveFile, destinationDir, password)
            name.endsWith(".7z") -> extract7z(archiveFile, destinationDir, password)
            name.endsWith(".tar") -> extractTar(archiveFile, destinationDir)
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> extractTarGz(archiveFile, destinationDir)
            name.endsWith(".tar.bz2") || name.endsWith(".tbz2") -> extractTarBz2(archiveFile, destinationDir)
            name.endsWith(".tar.xz") || name.endsWith(".txz") -> extractTarXz(archiveFile, destinationDir)
            name.endsWith(".gz") -> extractGzipStream(archiveFile, destinationDir)
            name.endsWith(".bz2") -> extractBzip2Stream(archiveFile, destinationDir)
            name.endsWith(".xz") -> extractXzStream(archiveFile, destinationDir)
            else -> throw IllegalArgumentException("Unsupported archive format: ${archiveFile.name}")
        }
    }

    private fun extractZip(archiveFile: File, destinationDir: File, password: String?) {
        ZipFile(archiveFile).use { zipFile ->
            val entries = zipFile.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry is ZipArchiveEntry && (entry.generalPurposeBit.usesEncryption() || entry.method == ZipArchiveOutputStream.STORED && entry.size > 0 && entry.compressedSize > 0)) {
                    // Commons Compress ZipFile doesn't support password protected zips easily.
                    // It requires specific setup or using other libraries.
                    throw PasswordRequiredException("Encrypted ZIP archives are not supported yet")
                }
                val entryName = entry.name.replace('\\', '/')
                val entryFile = File(destinationDir, entryName)
                
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    zipFile.getInputStream(entry).use { input ->
                        entryFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun extract7z(archiveFile: File, destinationDir: File, password: String?) {
        val builder = SevenZFile.builder().setFile(archiveFile)
        if (password != null) {
            builder.setPassword(password.toCharArray())
        }
        
        builder.get().use { szf ->
            var entry = szf.nextEntry
            while (entry != null) {
                val entryName = entry.name.replace('\\', '/')
                val entryFile = File(destinationDir, entryName)
                
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    try {
                        entryFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (szf.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                            }
                        }
                    } catch (e: Exception) {
                        if (e.message?.contains("password", ignoreCase = true) == true || 
                            e.message?.contains("decrypt", ignoreCase = true) == true ||
                            e is IOException && password == null) {
                            throw PasswordRequiredException("7z archive is encrypted")
                        }
                        throw e
                    }
                }
                entry = szf.nextEntry
            }
        }
    }

    class PasswordRequiredException(message: String) : IOException(message)

    private fun extractTar(archiveFile: File, destinationDir: File) {
        TarArchiveInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { tais ->
            extractTarStream(tais, destinationDir)
        }
    }

    private fun extractTarGz(archiveFile: File, destinationDir: File) {
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))).use { tais ->
            extractTarStream(tais, destinationDir)
        }
    }

    private fun extractTarBz2(archiveFile: File, destinationDir: File) {
        TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))).use { tais ->
            extractTarStream(tais, destinationDir)
        }
    }

    private fun extractTarXz(archiveFile: File, destinationDir: File) {
        TarArchiveInputStream(XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile)))).use { tais ->
            extractTarStream(tais, destinationDir)
        }
    }

    private fun extractTarStream(tais: TarArchiveInputStream, destinationDir: File) {
        var entry = tais.nextTarEntry
        while (entry != null) {
            val entryName = entry.name.replace('\\', '/')
            val entryFile = File(destinationDir, entryName)
            
            if (entry.isDirectory) {
                entryFile.mkdirs()
            } else {
                entryFile.parentFile?.mkdirs()
                entryFile.outputStream().use { output ->
                    tais.copyTo(output)
                }
            }
            entry = tais.nextTarEntry
        }
    }

    private fun extractGzipStream(archiveFile: File, destinationDir: File) {
        val outputFile = File(destinationDir, archiveFile.name.removeSuffix(".gz"))
        GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { gzis ->
            outputFile.outputStream().use { gzis.copyTo(it) }
        }
    }

    private fun extractBzip2Stream(archiveFile: File, destinationDir: File) {
        val outputFile = File(destinationDir, archiveFile.name.removeSuffix(".bz2"))
        BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { bzis ->
            outputFile.outputStream().use { bzis.copyTo(it) }
        }
    }

    private fun extractXzStream(archiveFile: File, destinationDir: File) {
        val outputFile = File(destinationDir, archiveFile.name.removeSuffix(".xz"))
        XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { xzis ->
            outputFile.outputStream().use { xzis.copyTo(it) }
        }
    }

    fun testArchive(archiveFile: File): Boolean {
        return try {
            val name = archiveFile.name.lowercase()
            when {
                name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".apk") -> {
                    ZipFile(archiveFile).use { zipFile ->
                        val entries = zipFile.entries
                        while (entries.hasMoreElements()) {
                            zipFile.getInputStream(entries.nextElement()).use { it.skip(Long.MAX_VALUE) }
                        }
                    }
                }
                name.endsWith(".7z") -> {
                    SevenZFile(archiveFile).use { sevenZFile ->
                        while (sevenZFile.nextEntry != null) {
                            val buffer = ByteArray(8192)
                            while (sevenZFile.read(buffer) != -1) { }
                        }
                    }
                }
                name.endsWith(".tar") -> {
                    TarArchiveInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { tais ->
                        while (tais.nextTarEntry != null) { tais.skip(Long.MAX_VALUE) }
                    }
                }
                else -> false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
