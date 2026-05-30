package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.util.Base64

/**
 * Signal Protocol is asymmetric: Alice encrypts to Bob, Bob decrypts.
 * Tests use two separate stores — senderStore (Alice) and recipientStore (Bob).
 */
class SessionManagerTest {

    // Alice = local node (the sender in E2E tests)
    private lateinit var senderKeyPair: IdentityKeyPair
    private lateinit var senderStore: InMemorySignalProtocolStore
    private lateinit var senderKeyStore: SignalKeyStore
    private lateinit var sessionManager: SessionManager

    // Bob = remote user (the recipient in E2E tests)
    private lateinit var recipientKeyPair: IdentityKeyPair
    private lateinit var recipientStore: InMemorySignalProtocolStore
    private lateinit var recipientKeyStore: SignalKeyStore
    private lateinit var recipientSessionManager: SessionManager

    private val recipientUin = 123L  // Bob's UIN
    private val senderUin = 999L     // Alice's UIN (used as session key by Bob)

    @Before
    fun setUp() {
        // Alice
        senderKeyPair = IdentityKeyPair.generate()
        senderStore = InMemorySignalProtocolStore(senderKeyPair)
        senderKeyStore = SignalKeyStore(senderStore)
        sessionManager = SessionManager(senderKeyStore)

        // Bob
        recipientKeyPair = IdentityKeyPair.generate()
        recipientStore = InMemorySignalProtocolStore(recipientKeyPair)
        recipientKeyStore = SignalKeyStore(recipientStore)
        recipientSessionManager = SessionManager(recipientKeyStore)

        setupSession()
    }

    /**
     * Bob generates his pre-keys and stores them.
     * Alice builds a session using Bob's PreKeyBundle.
     * After this, Alice can encrypt to Bob; Bob can decrypt from Alice.
     */
    private fun setupSession() {
        val preKeyPair = Curve.generateKeyPair()
        val preKeyRecord = PreKeyRecord(1, preKeyPair)
        recipientStore.storePreKey(1, preKeyRecord)

        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(
            recipientKeyPair.privateKey,
            signedPreKeyPair.publicKey.serialize()
        )
        val signedPreKeyRecord = SignedPreKeyRecord(1, System.currentTimeMillis(), signedPreKeyPair, signature)
        recipientStore.storeSignedPreKey(1, signedPreKeyRecord)

        val bundle = PreKeyBundle(
            0, 1, 1, preKeyPair.publicKey,
            1, signedPreKeyPair.publicKey, signature,
            recipientKeyPair.publicKey
        )
        SessionBuilder(senderKeyStore, SignalProtocolAddress(recipientUin.toString(), 1)).process(bundle)
    }

    @Test
    fun testEncryptDecryptMessage() {
        val plaintext = "Hello Signal Protocol"

        // Alice encrypts to Bob
        val ciphertext = sessionManager.encryptMessage(recipientUin, plaintext)
        assertNotNull(ciphertext)

        // Bob decrypts from Alice (senderUin is the session key in Bob's store)
        val decrypted = recipientSessionManager.decryptMessage(senderUin, ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testHasSession() {
        assertTrue(sessionManager.hasSession(recipientUin))
        assertFalse(sessionManager.hasSession(9999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(sessionManager.hasSession(recipientUin))
        sessionManager.deleteSession(recipientUin)
        assertFalse(sessionManager.hasSession(recipientUin))
    }

    @Test
    fun testEncryptMultipleMessages() {
        val messages = listOf("First message", "Second message", "Third message")
        val ciphertexts = mutableListOf<String>()

        messages.forEach { message ->
            val ct = sessionManager.encryptMessage(recipientUin, message)
            ciphertexts.add(Base64.getEncoder().encodeToString(ct.serialize()))
        }

        assertEquals(3, ciphertexts.size)
        ciphertexts.forEach { assertNotNull(it) }
    }
}
