package com.semseytech.rtsdevicesuitepro.archive.ui

object ArchiveExplanations {
    fun getExplanation(key: String): String {
        return when (key) {
            "Archive Format" -> "Choose the container format for your files. ZIP is most compatible, while 7z offers better compression."
            "Compression Level" -> "Determines how much effort is put into shrinking files. 'Ultra' takes longer but results in smaller files."
            "Compression Method" -> "The algorithm used to compress data. LZMA2 is standard for 7z; Deflate is standard for ZIP."
            "Dictionary Size" -> "The amount of memory used during compression. Larger sizes can improve compression but require more RAM."
            "Word Size" -> "Affects how the compressor finds identical sequences of data. Larger values can improve compression of similar files."
            "Solid Block Size" -> "Combines multiple files into a single stream. Improves compression ratio but makes it slower to extract individual files."
            "Number of CPU Threads" -> "Controls how many processor cores are used. More threads speed up compression on modern devices."
            "Split to Volumes" -> "Divides the archive into multiple smaller files, useful for sharing via email or storage with size limits."
            "Path Mode" -> "Controls how folder structures are stored inside the archive. 'Relative paths' is usually the best choice."
            "Create SFX Archive" -> "Creates a self-extracting file that doesn't require an archive manager to open."
            "Compress Shared Files" -> "Allows the compressor to open files that are currently being used by other programs."
            "Delete Files After Compression" -> "Automatically removes the original files once the archive is successfully created."
            "Encryption Method" -> "The type of security used to protect your files. AES-256 is highly secure and industry standard."
            "Encrypt File Names" -> "Hides the names of the files inside the archive so they can only be seen after entering the password."
            else -> "No explanation available."
        }
    }
}
