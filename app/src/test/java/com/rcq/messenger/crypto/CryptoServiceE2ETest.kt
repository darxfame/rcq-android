package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.util.KeyHelper

class CryptoServiceE2ETest {

    private lateinit var cryptoService: CryptoService
    private lateinit var sessionManager: SessionManager
    private lateinit var signalKeyStore: SignalKeyStore
    private lateinit var identityKeyPair: IdentityKeyPair
    private val testUin = 456L

    @Before
    fun setUp() {
        identityKeyPair = IdentityKeyPair.generate()
        signalKeyStore = SignalKeyStore(identityKeyPair)
        sessionManager = SessionManager(signalKeyStore)
        cryptoService = CryptoService(sessionManager, signalKeyStore)

        // Setup a session for E2E testing
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
    fun testFullE2EEncryption() {
        val plaintext = "Secret message for E2E test"

        // Encrypt message
        val encrypted = cryptoService.encryptMessage(testUin, plaintext)
        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.signalType > 0)

        // Decrypt message
        val decrypted = cryptoService.decryptMessage(testUin, encrypted.ciphertext, encrypted.signalType)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testMultipleMessagesE2E() {
        val messages = listOf(
            "First encrypted message",
            "Second encrypted message",
            "Third encrypted message"
        )

        val encryptedMessages = mutableListOf<CryptoService.EncryptedMessage>()

        // Encrypt all messages
        messages.forEach { message ->
            val encrypted = cryptoService.encryptMessage(testUin, message)
            encryptedMessages.add(encrypted)
        }

        // Decrypt all messages
        encryptedMessages.forEachIndexed { index, encrypted ->
            val decrypted = cryptoService.decryptMessage(testUin, encrypted.ciphertext, encrypted.signalType)
            assertEquals(messages[index], decrypted)
        }
    }

    @Test
    fun testHasSession() {
        assertTrue(cryptoService.hasSession(testUin))
        assertFalse(cryptoService.hasSession(999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(cryptoService.hasSession(testUin))

        cryptoService.deleteSession(testUin)

        assertFalse(cryptoService.hasSession(testUin))
    }

    @Test
    fun testGetIdentityKey() {
        val identityKey = cryptoService.getIdentityKey()
        assertNotNull(identityKey)
        assertTrue(identityKey.isNotEmpty())
        // Should be valid Base64
        assertNotNull(android.util.Base64.decode(identityKey, android.util.Base64.NO_WRAP))
    }

    @Test
    fun testEncryptedMessageFormat() {
        val plaintext = "Test message format"
        val encrypted = cryptoService.encryptMessage(testUin, plaintext)

        // Verify encrypted message structure
        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.ciphertext.isNotEmpty())
        assertTrue(encrypted.signalType in 1..2) // PreKey or Whisper type

        // Ciphertext should be valid Base64
        assertNotNull(android.util.Base64.decode(encrypted.ciphertext, android.util.Base64.NO_WRAP))
    }
}