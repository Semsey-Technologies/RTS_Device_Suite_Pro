package com.semseytech.rtsdevicesuitepro.organizer.worker

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream

object ExtractionUtils {
    fun extract(archiveFile: File, destinationDir: File) {
        if (!destinationDir.exists()) destinationDir.mkdirs()
        
        try {
            FileInputStream(archiveFile).use { fis ->
                val bis = BufferedInputStream(fis)
                val ais: ArchiveInputStream<*> = ArchiveStreamFactory().createArchiveInputStream(bis)
                ais.use { archiveInputStream ->
                    var entry = archiveInputStream.nextEntry
                    while (entry != null) {
                        val outputFile = File(destinationDir, entry.name)
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            outputFile.outputStream().use { fos ->
                                archiveInputStream.copyTo(fos)
                            }
                        }
                        entry = archiveInputStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
