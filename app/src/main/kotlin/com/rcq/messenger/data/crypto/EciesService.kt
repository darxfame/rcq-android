package com.rcq.messenger.data.crypto

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * ECIES (Elliptic Curve Integrated Encryption Scheme) Implementation.
 * 
 * Uses AES-256-GCM for authenticated encryption with associated data.
 * 
 * CRITICAL (Phase 1.1):
 * - Encrypt/decrypt with AES-GCM
 * - Generate random nonces
 * - Handle authentication tags
 * 
 * Algorithm:
 * 1. Generate 256-bit random key
 * 2. Generate 96-bit random nonce (IV)
 * 3. AES-GCM encrypt plaintext
 * 4. Return (ciphertext, nonce, auth_tag)
 * 
 * On decryption:
 * 1. Receive (ciphertext, nonce, auth_tag)
 * 2. AES-GCM decrypt with key
 * 3. Return plaintext (or null if auth fails)
 */
class EciesService {
    
    companion object {
        private const val TAG = "EciesService"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256  // bits
        private const val NONCE_SIZE = 96  // bits (12 bytes)
        private const val AUTH_TAG_SIZE = 128  // bits (16 bytes)
    }
    
    /**
     * Encrypt plaintext using AES-256-GCM.
     * 
     * @param plaintext Message to encrypt
     * @param key 256-bit symmetric key (32 bytes)
     * @return Triple(ciphertext_hex, nonce_hex, authTag_hex)
     */
    fun encrypt(plaintext: String, key: ByteArray): Triple<String, String, String> {
        require(key.size == 32) { "Key must be 32 bytes (256 bits)" }
        
        try {
            // Generate random 96-bit nonce
            val nonce = ByteArray(12)
            Random.nextBytes(nonce)
            
            // Create cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(key, 0, key.size, "AES")
            val gcmSpec = GCMParameterSpec(AUTH_TAG_SIZE, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            // Encrypt
            val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
            val ciphertext = cipher.doFinal(plainBytes)
            
            // Extract auth tag from ciphertext (last 16 bytes)
            val authTag = ciphertext.sliceArray(ciphertext.size - 16 until ciphertext.size)
            val encryptedData = ciphertext.sliceArray(0 until ciphertext.size - 16)
            
            Log.d(TAG, "Encrypted ${plainBytes.size} bytes to ${encryptedData.size} bytes (+ 16 byte auth tag)")
            
            return Triple(
                encryptedData.toHex(),
                nonce.toHex(),
                authTag.toHex()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Decrypt ciphertext using AES-256-GCM.
     * 
     * @param ciphertextHex Encrypted data (hex-encoded)
     * @param nonceHex Initialization vector (hex-encoded)
     * @param authTagHex Authentication tag (hex-encoded)
     * @param key 256-bit symmetric key (32 bytes)
     * @return Plaintext or null if authentication fails
     */
    fun decrypt(ciphertextHex: String, nonceHex: String, authTagHex: String, key: ByteArray): String? {
        require(key.size == 32) { "Key must be 32 bytes (256 bits)" }
        
        try {
            val ciphertext = ciphertextHex.fromHex()
            val nonce = nonceHex.fromHex()
            val authTag = authTagHex.fromHex()
            
            require(nonce.size == 12) { "Nonce must be 12 bytes (96 bits)" }
            require(authTag.size == 16) { "Auth tag must be 16 bytes (128 bits)" }
            
            // Reconstruct full ciphertext with auth tag
            val fullCiphertext = ciphertext + authTag
            
            // Create cipher
            val cipher = Cipher.getInstance(ALGORITHM)
            val secretKey = SecretKeySpec(key, 0, key.size, "AES")
            val gcmSpec = GCMParameterSpec(AUTH_TAG_SIZE, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // Decrypt
            val plaintext = cipher.doFinal(fullCiphertext)
            
            Log.d(TAG, "Decrypted ${ciphertext.size} bytes")
            
            return plaintext.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Generate a random 256-bit symmetric key.
     */
    fun generateKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey().encoded
    }
    
    /**
     * Hex encoding helpers.
     */
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    
    private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
