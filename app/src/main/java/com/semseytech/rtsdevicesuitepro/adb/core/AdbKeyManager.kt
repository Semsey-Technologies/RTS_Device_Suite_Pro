package com.semseytech.rtsdevicesuitepro.adb.core

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher

/**
 * Manages RSA keys for ADB authentication.
 */
class AdbKeyManager(private val context: Context) {

    private val keyFile = File(context.filesDir, "adb_key")
    private val pubKeyFile = File(context.filesDir, "adb_key.pub")

    fun getOrGenerateKeyPair(): KeyPair {
        return if (keyFile.exists() && pubKeyFile.exists()) {
            loadKeyPair()
        } else {
            generateKeyPair()
        }
    }

    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        val keyPair = generator.generateKeyPair()
        
        saveKeyPair(keyPair)
        return keyPair
    }

    private fun saveKeyPair(keyPair: KeyPair) {
        keyFile.writeBytes(keyPair.private.encoded)
        pubKeyFile.writeBytes(keyPair.public.encoded)
    }

    private fun loadKeyPair(): KeyPair {
        // Simple loading, in a real app use KeyStore for better security
        // For this ADB module, we'll keep it simple for portability
        val privateKeyBytes = keyFile.readBytes()
        val publicKeyBytes = pubKeyFile.readBytes()
        
        val keyFactory = java.security.KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
        val publicKey = keyFactory.generatePublic(java.security.spec.X509EncodedKeySpec(publicKeyBytes))
        
        return KeyPair(publicKey, privateKey)
    }

    fun getAdbPublicKeyPayload(): ByteArray {
        val keyPair = getOrGenerateKeyPair()
        val pubKey = keyPair.public as RSAPublicKey
        val encoded = Base64.encodeToString(pubKey.encoded, Base64.NO_WRAP)
        return "$encoded rts_device_suite_pro@android\u0000".toByteArray()
    }

    fun signToken(token: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        return cipher.doFinal(token)
    }
}
