package com.semseytech.rtsdevicesuitepro.backup.engine

import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ArchiveWriter(
    private val outputFile: File,
    private val format: ArchiveFormat,
    private val compressionLevel: Int = 5 // 0-9
) : AutoCloseable {

    private var archiveOutputStream: ArchiveOutputStream<*>? = null
    private var sevenZOutputFile: SevenZOutputFile? = null
    private var baseOutputStream: OutputStream? = null

    init {
        when (format) {
            ArchiveFormat.SEVEN_Z -> {
                sevenZOutputFile = SevenZOutputFile(outputFile)
                // 7z compression level handling is more complex, defaults for now
            }
            else -> {
                val fos = FileOutputStream(outputFile)
                val bos = BufferedOutputStream(fos)
                baseOutputStream = bos

                val compressedStream = when (format) {
                    ArchiveFormat.TAR_GZ -> GzipCompressorOutputStream(bos)
                    ArchiveFormat.TAR_BZ2 -> BZip2CompressorOutputStream(bos)
                    ArchiveFormat.TAR_XZ -> XZCompressorOutputStream(bos)
                    else -> bos
                }

                archiveOutputStream = when (format) {
                    ArchiveFormat.ZIP -> ZipArchiveOutputStream(compressedStream).apply {
                        setLevel(compressionLevel)
                    }
                    ArchiveFormat.TAR, ArchiveFormat.TAR_GZ, ArchiveFormat.TAR_BZ2, ArchiveFormat.TAR_XZ -> 
                        TarArchiveOutputStream(compressedStream).apply {
                            setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                        }
                    else -> throw IllegalArgumentException("Unsupported format")
                }
            }
        }
    }

    fun addFile(file: File, entryPath: String) {
        if (!file.exists()) return
        
        when (format) {
            ArchiveFormat.SEVEN_Z -> {
                val entry = sevenZOutputFile?.createArchiveEntry(file, entryPath) ?: return
                sevenZOutputFile?.putArchiveEntry(entry)
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var n: Int
                    while (input.read(buffer).also { n = it } != -1) {
                        sevenZOutputFile?.write(buffer, 0, n)
                    }
                }
                sevenZOutputFile?.closeArchiveEntry()
            }
            ArchiveFormat.ZIP -> {
                val zos = archiveOutputStream as ZipArchiveOutputStream
                val entry = zos.createArchiveEntry(file, entryPath)
                zos.putArchiveEntry(entry)
                file.inputStream().use { it.copyTo(zos) }
                zos.closeArchiveEntry()
            }
            else -> { // TAR formats
                val tos = archiveOutputStream as TarArchiveOutputStream
                val entry = tos.createArchiveEntry(file, entryPath)
                tos.putArchiveEntry(entry)
                file.inputStream().use { it.copyTo(tos) }
                tos.closeArchiveEntry()
            }
        }
    }

    fun addData(data: ByteArray, entryPath: String) {
        when (format) {
            ArchiveFormat.SEVEN_Z -> {
                val entry = sevenZOutputFile?.createArchiveEntry(File("dummy"), entryPath) ?: return
                entry.size = data.size.toLong()
                sevenZOutputFile?.putArchiveEntry(entry)
                sevenZOutputFile?.write(data)
                sevenZOutputFile?.closeArchiveEntry()
            }
            ArchiveFormat.ZIP -> {
                val zos = archiveOutputStream as ZipArchiveOutputStream
                val entry = zos.createArchiveEntry(File("dummy"), entryPath)
                entry.size = data.size.toLong()
                zos.putArchiveEntry(entry)
                zos.write(data)
                zos.closeArchiveEntry()
            }
            else -> { // TAR formats
                val tos = archiveOutputStream as TarArchiveOutputStream
                val entry = tos.createArchiveEntry(File("dummy"), entryPath)
                entry.size = data.size.toLong()
                tos.putArchiveEntry(entry)
                tos.write(data)
                tos.closeArchiveEntry()
            }
        }
    }

    override fun close() {
        try {
            archiveOutputStream?.finish()
            archiveOutputStream?.close()
        } catch (e: Exception) {}
        
        try {
            sevenZOutputFile?.finish()
            sevenZOutputFile?.close()
        } catch (e: Exception) {}
        
        try {
            baseOutputStream?.close()
        } catch (e: Exception) {}
    }
}
