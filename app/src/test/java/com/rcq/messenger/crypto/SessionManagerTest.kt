package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyBundle
import java.util.Base64

class SessionManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var signalKeyStore: SignalKeyStore
    private lateinit var identityKeyPair: IdentityKeyPair
    private val testUin = 123L

    @Before
    fun setUp() {
        identityKeyPair = IdentityKeyPair.generate()
        signalKeyStore = SignalKeyStore(InMemorySignalProtocolStore(identityKeyPair))
        sessionManager = SessionManager(signalKeyStore)
        setupTestSession()
    }

    private fun setupTestSession() {
        val address = SignalProtocolAddress(testUin.toString(), 1)
        val sessionBuilder = SessionBuilder(signalKeyStore, address)

        val preKeyPair = Curve.generateKeyPair()
        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, signedPreKeyPair.publicKey.serialize())

        val preKeyBundle = PreKeyBundle(
            0, 1, 1, preKeyPair.publicKey,
            1, signedPreKeyPair.publicKey, signature,
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

        messages.forEach { message ->
            val ciphertext = sessionManager.encryptMessage(testUin, message)
            ciphertexts.add(Base64.getEncoder().encodeToString(ciphertext.serialize()))
        }

        assertEquals(3, ciphertexts.size)
        ciphertexts.forEach { assertNotNull(it) }
    }
}
