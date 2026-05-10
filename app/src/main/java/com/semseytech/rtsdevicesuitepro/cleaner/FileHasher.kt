package com.semseytech.rtsdevicesuitepro.cleaner

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileHasher {
    fun calculateMD5(file: File): String? {
        if (!file.exists() || !file.isFile) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            val md5Bytes = digest.digest()
            val hexString = StringBuilder()
            for (b in md5Bytes) {
                hexString.append(String.format("%02x", b))
            }
            hexString.toString()
        } catch (e: Exception) {
            null
        }
    }
}
