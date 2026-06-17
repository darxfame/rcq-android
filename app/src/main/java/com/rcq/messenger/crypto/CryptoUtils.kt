@file:OptIn(ExperimentalEncodingApi::class)

package com.rcq.messenger.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64 as KotlinBase64
import kotlin.io.encoding.ExperimentalEncodingApi

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    fun generateKey(): ByteArray {
        val key = ByteArray(KEY_SIZE / 8)
        SecureRandom().nextBytes(key)
        return key
    }

    fun deriveKeyFromPhrase(phrase: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(phrase.toByteArray(Charsets.UTF_8))
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encrypted = cipher.doFinal(data)

        return iv + encrypted
    }

    fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)

        val iv = encryptedData.copyOfRange(0, IV_SIZE)
        val data = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)

        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_SIZE, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(data)
    }

    fun bytesToBase64(bytes: ByteArray): String = KotlinBase64.encode(bytes)
    fun base64ToBytes(base64: String): ByteArray = KotlinBase64.decode(base64)

    fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytesToBase64(bytes)
    }

    fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytesToBase64(hash)
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun String.encodeBase64(): String = KotlinBase64.encode(this.toByteArray())
fun String.decodeBase64(): ByteArray = KotlinBase64.decode(this)

object KeyPairGenerator {
    fun generate(): Pair<ByteArray, ByteArray> {
        val privateKey = CryptoUtils.generateKey()
        val publicKey = CryptoUtils.deriveKeyFromPhrase(CryptoUtils.bytesToBase64(privateKey))
        return publicKey to privateKey
    }

    fun derivePublicKey(privateKey: ByteArray): ByteArray {
        return CryptoUtils.deriveKeyFromPhrase(CryptoUtils.bytesToBase64(privateKey))
    }
}
