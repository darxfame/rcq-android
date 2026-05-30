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

class CryptoServiceE2ETest {

    private lateinit var cryptoService: CryptoService
    private lateinit var sessionManager: SessionManager
    private lateinit var signalKeyStore: SignalKeyStore
    private lateinit var identityKeyPair: IdentityKeyPair
    private val testUin = 456L

    @Before
    fun setUp() {
        identityKeyPair = IdentityKeyPair.generate()
        signalKeyStore = SignalKeyStore(InMemorySignalProtocolStore(identityKeyPair))
        sessionManager = SessionManager(signalKeyStore)
        cryptoService = CryptoService(sessionManager, signalKeyStore, EciesCrypto())
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
    fun testFullE2EEncryption() {
        val plaintext = "Secret message for E2E test"

        val encrypted = cryptoService.encryptMessage(testUin, plaintext)
        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.signalType > 0)

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
        messages.forEach { message ->
            encryptedMessages.add(cryptoService.encryptMessage(testUin, message))
        }

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
        assertNotNull(Base64.getDecoder().decode(identityKey))
    }

    @Test
    fun testEncryptedMessageFormat() {
        val plaintext = "Test message format"
        val encrypted = cryptoService.encryptMessage(testUin, plaintext)

        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.ciphertext.isNotEmpty())
        assertTrue(encrypted.signalType in 1..2)
        assertNotNull(Base64.getDecoder().decode(encrypted.ciphertext))
    }
}
