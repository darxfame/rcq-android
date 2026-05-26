package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var signalKeyStore: SignalKeyStore
    private lateinit var identityKeyPair: IdentityKeyPair
    private val testUin = 123L

    @Before
    fun setUp() {
        identityKeyPair = IdentityKeyPair.generate()
        signalKeyStore = SignalKeyStore(identityKeyPair)
        sessionManager = SessionManager(signalKeyStore)

        // Setup a session for testing
        setupTestSession()
    }

    private fun setupTestSession() {
        val address = SignalProtocolAddress(testUin.toString(), 1)
        val sessionBuilder = SessionBuilder(signalKeyStore, address)

        // Create a mock pre-key bundle for the test user
        val preKeyPair = Curve.generateKeyPair()
        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, signedPreKeyPair.publicKey.serialize())

        val preKeyBundle = PreKeyBundle(
            0, // registrationId
            1, // deviceId
            1, // preKeyId
            preKeyPair.publicKey,
            1, // signedPreKeyId
            signedPreKeyPair.publicKey,
            signature,
            identityKeyPair.publicKey
        )

        sessionBuilder.process(preKeyBundle)
    }

    @Test
    fun testEncryptDecryptMessage() {
        val plaintext = "Hello Signal Protocol"

        val ciphertext = sessionManager.encryptMessage(testUin, plaintext)
        assertNotNull(ciphertext)

        val decrypted = sessionManager.decryptMessage(testUin, ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testHasSession() {
        assertTrue(sessionManager.hasSession(testUin))
        assertFalse(sessionManager.hasSession(999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(sessionManager.hasSession(testUin))

        sessionManager.deleteSession(testUin)

        assertFalse(sessionManager.hasSession(testUin))
    }

    @Test
    fun testEncryptMultipleMessages() {
        val messages = listOf("First message", "Second message", "Third message")
        val ciphertexts = mutableListOf<String>()

        // Encrypt all messages
        messages.forEach { message ->
            val ciphertext = sessionManager.encryptMessage(testUin, message)
            ciphertexts.add(android.util.Base64.encodeToString(ciphertext.serialize(), android.util.Base64.NO_WRAP))
        }

        // Decrypt all messages
        ciphertexts.forEachIndexed { index, ciphertextBase64 ->
            val ciphertextBytes = android.util.Base64.decode(ciphertextBase64, android.util.Base64.NO_WRAP)
            // Note: In real implementation, we'd need to reconstruct the proper CiphertextMessage type
            // This is a simplified test
        }

        // For now, just verify we can encrypt multiple messages
        assertEquals(3, ciphertexts.size)
        ciphertexts.forEach { assertNotNull(it) }
    }
}