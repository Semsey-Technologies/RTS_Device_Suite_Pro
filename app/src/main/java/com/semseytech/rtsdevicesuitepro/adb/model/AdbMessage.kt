package com.semseytech.rtsdevicesuitepro.adb.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents a standard ADB protocol message.
 */
data class AdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val data: ByteArray = byteArrayOf()
) {
    companion object {
        const val CONNECT = 0x4e584e43 // CNXN
        const val AUTH = 0x48545541 // AUTH
        const val OPEN = 0x4e45504f // OPEN
        const val READY = 0x59414b4f // OKAY
        const val WRITE = 0x45545257 // WRTE
        const val CLOSE = 0x45534c43 // CLSE
        const val OKAY = 0x59414b4f // OKAY
        const val FAIL = 0x4c494146 // FAIL
        const val STLS = 0x534c5453 // STLS
        const val PAIR = 0x52494150 // PAIR

        fun parse(header: ByteArray, data: ByteArray): AdbMessage {
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val command = buffer.getInt()
            val arg0 = buffer.getInt()
            val arg1 = buffer.getInt()
            return AdbMessage(command, arg0, arg1, data)
        }
    }

    fun serialize(): ByteArray {
        val buffer = ByteBuffer.allocate(24 + data.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(command)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(data.size)
        buffer.putInt(checksum())
        buffer.putInt(command xor -0x1)
        buffer.put(data)
        return buffer.array()
    }

    private fun checksum(): Int {
        var sum = 0
        for (b in data) {
            sum += b.toInt() and 0xFF
        }
        return sum
    }
}
