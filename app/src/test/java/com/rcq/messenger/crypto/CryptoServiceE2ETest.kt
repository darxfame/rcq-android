package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.util.Base64

/**
 * E2E tests for CryptoService. Signal Protocol is asymmetric:
 * - senderService (Alice) encrypts to Bob (recipientUin)
 * - recipientService (Bob) decrypts from Alice (senderUin)
 * Two separate crypto stacks with independent key stores.
 */
class CryptoServiceE2ETest {

    // Alice — the sender
    private lateinit var senderKeyPair: IdentityKeyPair
    private lateinit var senderStore: InMemorySignalProtocolStore
    private lateinit var cryptoService: CryptoService  // Alice's service

    // Bob — the recipient
    private lateinit var recipientKeyPair: IdentityKeyPair
    private lateinit var recipientStore: InMemorySignalProtocolStore
    private lateinit var recipientService: CryptoService  // Bob's service

    private val recipientUin = 456L  // Bob's UIN
    private val senderUin = 789L     // Alice's UIN (used as session key by Bob)

    @Before
    fun setUp() {
        // Alice setup
        senderKeyPair = IdentityKeyPair.generate()
        senderStore = InMemorySignalProtocolStore(senderKeyPair)
        val senderKeyStore = SignalKeyStore(senderStore)
        val senderSessionManager = SessionManager(senderKeyStore)
        cryptoService = CryptoService(senderSessionManager, senderKeyStore, EciesCrypto())

        // Bob setup
        recipientKeyPair = IdentityKeyPair.generate()
        recipientStore = InMemorySignalProtocolStore(recipientKeyPair)
        val recipientKeyStore = SignalKeyStore(recipientStore)
        val recipientSessionManager = SessionManager(recipientKeyStore)
        recipientService = CryptoService(recipientSessionManager, recipientKeyStore, EciesCrypto())

        setupSession()
    }

    /**
     * Bob stores his pre-keys; Alice builds a session using Bob's PreKeyBundle.
     */
    private fun setupSession() {
        val preKeyPair = Curve.generateKeyPair()
        recipientStore.storePreKey(1, PreKeyRecord(1, preKeyPair))

        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            recipientKeyPair.privateKey,
            signedPreKeyPair.publicKey.serialize()
        )
        recipientStore.storeSignedPreKey(1, SignedPreKeyRecord(1, System.currentTimeMillis(), signedPreKeyPair, signature))

        val bundle = PreKeyBundle(
            0, 1, 1, preKeyPair.publicKey,
            1, signedPreKeyPair.publicKey, signature,
            recipientKeyPair.publicKey
        )
        SessionBuilder(senderStore, SignalProtocolAddress(recipientUin.toString(), 1)).process(bundle)
    }

    @Test
    fun testFullE2EEncryption() {
        val plaintext = "Secret message for E2E test"

        // Alice encrypts to Bob
        val encrypted = cryptoService.encryptMessage(recipientUin, plaintext)
        assertNotNull(encrypted.ciphertext)
        // First message to new contact is always PREKEY_TYPE (3)
        assertEquals(CiphertextMessage.PREKEY_TYPE, encrypted.signalType)

        // Bob decrypts from Alice
        val decrypted = recipientService.decryptMessage(senderUin, encrypted.ciphertext, encrypted.signalType)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testMultipleMessagesE2E() {
        val messages = listOf(
            "First encrypted message",
            "Second encrypted message",
            "Third encrypted message"
        )

        // Alice encrypts all messages to Bob
        val encryptedMessages = messages.map { cryptoService.encryptMessage(recipientUin, it) }

        // Bob decrypts all messages from Alice
        encryptedMessages.forEachIndexed { index, encrypted ->
            val decrypted = recipientService.decryptMessage(senderUin, encrypted.ciphertext, encrypted.signalType)
            assertEquals(messages[index], decrypted)
        }
    }

    @Test
    fun testHasSession() {
        assertTrue(cryptoService.hasSession(recipientUin))
        assertFalse(cryptoService.hasSession(99999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(cryptoService.hasSession(recipientUin))
        cryptoService.deleteSession(recipientUin)
        assertFalse(cryptoService.hasSession(recipientUin))
    }

    @Test
    fun testGetIdentityKey() {
        val identityKey = cryptoService.getIdentityKey()
        assertNotNull(identityKey)
        assertTrue(identityKey.isNotEmpty())
        // Must be valid Base64
        val decoded = Base64.getDecoder().decode(identityKey)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun testEncryptedMessageFormat() {
        val plaintext = "Test message format"
        val encrypted = cryptoService.encryptMessage(recipientUin, plaintext)

        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.ciphertext.isNotEmpty())
        // PREKEY_TYPE = 3, WHISPER_TYPE = 2 — both are valid Signal message types
        assertTrue("signalType must be 2 or 3, was ${encrypted.signalType}",
            encrypted.signalType == CiphertextMessage.WHISPER_TYPE || encrypted.signalType == CiphertextMessage.PREKEY_TYPE)
        // Ciphertext must be valid Base64
        val decoded = Base64.getDecoder().decode(encrypted.ciphertext)
        assertTrue(decoded.isNotEmpty())
    }
}
