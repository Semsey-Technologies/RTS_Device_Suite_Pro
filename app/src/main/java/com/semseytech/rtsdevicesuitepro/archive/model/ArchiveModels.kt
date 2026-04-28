package com.semseytech.rtsdevicesuitepro.archive.model

import java.io.File

enum class ArchiveFormat(val extension: String) {
    ZIP("zip"),
    SEVEN_Z("7z"),
    TAR("tar"),
    GZIP("gz"),
    BZIP2("bz2"),
    XZ("xz")
}

enum class CompressionLevel {
    STORE, FASTEST, FAST, NORMAL, MAXIMUM, ULTRA
}

enum class CompressionMethod {
    LZMA, LZMA2, BZIP2, DEFLATE, DEFLATE64, COPY
}

enum class PathMode {
    FULL_PATHS, RELATIVE_PATHS, NO_PATHS
}

enum class EncryptionMethod {
    ZIP_CRYPTO, AES_128, AES_192, AES_256
}

data class ArchiveOptions(
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val level: CompressionLevel = CompressionLevel.NORMAL,
    val method: CompressionMethod = CompressionMethod.DEFLATE,
    val dictionarySize: Int = 16, // MB
    val wordSize: Int = 32,
    val solidBlockSize: Long = 0, // 0 for None, -1 for Solid
    val threads: Int = 0, // 0 for Auto
    val splitSize: Long = 0, // 0 for no splitting
    val pathMode: PathMode = PathMode.RELATIVE_PATHS,
    val createSfx: Boolean = false,
    val compressShared: Boolean = false,
    val deleteAfter: Boolean = false,
    val password: String = "",
    val encryptionMethod: EncryptionMethod = EncryptionMethod.AES_256,
    val encryptFileNames: Boolean = false
)

data class FileItem(
    val file: File,
    val isDirectory: Boolean = file.isDirectory,
    val name: String = file.name,
    val size: Long = file.length(),
    val lastModified: Long = file.lastModified()
)
