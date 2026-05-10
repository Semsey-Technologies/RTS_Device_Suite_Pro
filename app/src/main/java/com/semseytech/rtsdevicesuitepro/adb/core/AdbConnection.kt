package com.semseytech.rtsdevicesuitepro.adb.core

import android.util.Log
import com.semseytech.rtsdevicesuitepro.adb.model.AdbMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Handles a single connection to an ADB daemon.
 */
class AdbConnection(
    private val host: String,
    private val port: Int,
    private val keyManager: AdbKeyManager
) {
    private val TAG = "AdbConnection"
    private var socket: Socket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private var isConnected = false
    private var isTls = false

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to $host:$port...")
            val baseSocket = try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 3000)
                s
            } catch (e: Exception) {
                val localIp = NetworkUtils.getLocalIpAddress()
                if (host != localIp) {
                    try {
                        Log.w(TAG, "Failed to connect to $host:$port. Trying local IP $localIp...")
                        val s = Socket()
                        s.connect(InetSocketAddress(localIp, port), 3000)
                        s
                    } catch (e2: Exception) {
                        if (localIp != "127.0.0.1" && host != "127.0.0.1") {
                            Log.w(TAG, "Trying loopback 127.0.0.1...")
                            val s = Socket()
                            s.connect(InetSocketAddress("127.0.0.1", port), 3000)
                            s
                        } else throw e2
                    }
                } else if (host != "127.0.0.1") {
                    Log.w(TAG, "Failed to connect to $host:$port. Trying loopback 127.0.0.1...")
                    val s = Socket()
                    s.connect(InetSocketAddress("127.0.0.1", port), 3000)
                    s
                } else throw e
            }

            socket = baseSocket
            input = baseSocket.getInputStream()
            output = baseSocket.getOutputStream()

            val connectPayload = "host::RTS_Device_Suite_Pro\u0000".toByteArray()
            sendMessage(AdbMessage(AdbMessage.CONNECT, 0x01000000, 1048576, connectPayload))

            Log.d(TAG, "Waiting for initial response...")
            var response: AdbMessage
            try {
                response = readMessage()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read initial response: ${e.message}")
                return@withContext false
            }
            
            Log.d(TAG, "Received message: ${formatCommand(response.command)} arg0=${response.arg0}")

            if (response.command == AdbMessage.STLS) {
                Log.d(TAG, "Server requested TLS (v3). Upgrading connection...")
                sendMessage(AdbMessage(AdbMessage.STLS, 0x01000000, 0))
                upgradeToTls()
                
                Log.d(TAG, "Sending CONNECT over TLS...")
                sendMessage(AdbMessage(AdbMessage.CONNECT, 0x01000000, 1048576, connectPayload))
                response = readMessage()
                Log.d(TAG, "Received message over TLS: ${formatCommand(response.command)}")
            }

            if (response.command == AdbMessage.AUTH) {
                if (response.arg0 == 1) { // Token challenge
                    Log.d(TAG, "Received AUTH token challenge. Signing...")
                    val signedToken = keyManager.signToken(response.data, keyManager.getOrGenerateKeyPair().private)
                    sendMessage(AdbMessage(AdbMessage.AUTH, 2, 0, signedToken))
                    
                    val authResponse = readMessage()
                    Log.d(TAG, "Received auth response: ${formatCommand(authResponse.command)}")
                    
                    if (authResponse.command == AdbMessage.CONNECT) {
                        Log.d(TAG, "Authenticated and connected!")
                        isConnected = true
                    } else if (authResponse.command == AdbMessage.AUTH && authResponse.arg0 == 1) {
                        Log.d(TAG, "Auth failed. Sending public key...")
                        sendMessage(AdbMessage(AdbMessage.AUTH, 3, 0, keyManager.getAdbPublicKeyPayload()))
                        val finalResponse = readMessage()
                        if (finalResponse.command == AdbMessage.CONNECT) {
                            isConnected = true
                        }
                    }
                }
            } else if (response.command == AdbMessage.CONNECT) {
                Log.d(TAG, "Connected successfully!")
                isConnected = true
            }

            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            false
        }
    }

    private fun upgradeToTls() {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }), SecureRandom())

        val sslSocketFactory = sslContext.socketFactory
        val sslSocket = sslSocketFactory.createSocket(socket, host, port, true) as SSLSocket
        sslSocket.useClientMode = true
        sslSocket.startHandshake()
        
        socket = sslSocket
        input = sslSocket.inputStream
        output = sslSocket.outputStream
        isTls = true
        Log.d(TAG, "TLS handshake completed.")
    }

    private fun formatCommand(cmd: Int): String {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(cmd).array()
        return String(bytes)
    }

    suspend fun pair(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting pairing on $host:$port with code: $code")
            
            val baseSocket = try {
                val s = Socket()
                s.connect(InetSocketAddress(host, port), 2000)
                s
            } catch (e: Exception) {
                // If initial host fails, try fallbacks
                val localIp = NetworkUtils.getLocalIpAddress()
                if (host != localIp) {
                    try {
                        Log.w(TAG, "Failed to connect to $host:$port. Trying local IP $localIp...")
                        val s = Socket()
                        s.connect(InetSocketAddress(localIp, port), 2000)
                        s
                    } catch (e2: Exception) {
                        if (localIp != "127.0.0.1" && host != "127.0.0.1") {
                            Log.w(TAG, "Trying loopback 127.0.0.1...")
                            val s = Socket()
                            s.connect(InetSocketAddress("127.0.0.1", port), 2000)
                            s
                        } else throw e2
                    }
                } else if (host != "127.0.0.1") {
                    Log.w(TAG, "Failed to connect to $host:$port. Trying loopback 127.0.0.1...")
                    val s = Socket()
                    s.connect(InetSocketAddress("127.0.0.1", port), 2000)
                    s
                } else throw e
            }

            socket = baseSocket
            input = baseSocket.getInputStream()
            output = baseSocket.getOutputStream()

            // In modern ADB, pairing starts with a TLS handshake immediately if port is correct
            // or sends a PAIR packet then upgrades.
            sendMessage(AdbMessage(AdbMessage.PAIR, 0, 0, "pairing code $code\u0000".toByteArray()))
            
            val response = readMessage()
            if (response.command == AdbMessage.STLS) {
                Log.d(TAG, "STLS received for pairing. Upgrading...")
                sendMessage(AdbMessage(AdbMessage.STLS, 0x01000000, 0))
                upgradeToTls()
                Log.d(TAG, "Pairing channel established via TLS. Please check for success on device.")
                return@withContext true
            } else if (response.command == AdbMessage.OKAY) {
                Log.d(TAG, "Pairing OKAY received directly.")
                return@withContext true
            }
            
            Log.w(TAG, "Pairing response: ${formatCommand(response.command)}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Pairing failed: ${e.message}", e)
            false
        }
    }

    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!isConnected) return@withContext "Not connected"

        try {
            val localId = 1
            sendMessage(AdbMessage(AdbMessage.OPEN, localId, 0, "shell:$command\u0000".toByteArray()))
            
            val openResponse = readMessage()
            if (openResponse.command != AdbMessage.READY) {
                return@withContext "Failed to open shell"
            }

            val remoteId = openResponse.arg0
            val result = StringBuilder()
            
            while (true) {
                val msg = readMessage()
                if (msg.command == AdbMessage.WRITE) {
                    result.append(String(msg.data))
                    sendMessage(AdbMessage(AdbMessage.READY, localId, remoteId))
                } else if (msg.command == AdbMessage.CLOSE) {
                    break
                }
            }
            result.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun sendMessage(message: AdbMessage) {
        output?.write(message.serialize())
        output?.flush()
    }

    private fun readMessage(): AdbMessage {
        val header = ByteArray(24)
        input?.readFully(header)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val command = buffer.getInt()
        val arg0 = buffer.getInt()
        val arg1 = buffer.getInt()
        val dataLength = buffer.getInt()
        // Skip checksum and magic for now to keep it simple
        
        val data = ByteArray(dataLength)
        if (dataLength > 0) {
            input?.readFully(data)
        }
        
        return AdbMessage(command, arg0, arg1, data)
    }

    private fun InputStream.readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) throw Exception("Stream closed")
            offset += read
        }
    }

    fun close() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing resources: ${e.message}")
        } finally {
            input = null
            output = null
            socket = null
            isConnected = false
        }
    }
}
