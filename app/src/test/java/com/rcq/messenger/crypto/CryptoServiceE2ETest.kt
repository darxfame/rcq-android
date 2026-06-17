package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.signal.libsignal.protocol.message.CiphertextMessage
import java.util.Base64

class CryptoServiceE2ETest {

    private lateinit var alice: TestSignalUser
    private lateinit var bob: TestSignalUser

    @Before
    fun setUp() {
        alice = TestSignalStores.user(111L)
        bob = TestSignalStores.user(456L)
        alice.service.buildSession(bob.uin, TestSignalStores.preKeyBundleFor(bob))
    }

    @Test
    fun testFullE2EEncryption() {
        val plaintext = "Secret message for E2E test"

        // Encrypt message
        val encrypted = alice.service.encryptMessage(bob.uin, plaintext)
        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.signalType > 0)

        // Decrypt message
        val decrypted = bob.service.decryptMessage(alice.uin, encrypted.ciphertext, encrypted.signalType)
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
            val encrypted = alice.service.encryptMessage(bob.uin, message)
            encryptedMessages.add(encrypted)
        }

        // Decrypt all messages
        encryptedMessages.forEachIndexed { index, encrypted ->
            val decrypted = bob.service.decryptMessage(alice.uin, encrypted.ciphertext, encrypted.signalType)
            assertEquals(messages[index], decrypted)
        }
    }

    @Test
    fun testHasSession() {
        assertTrue(alice.service.hasSession(bob.uin))
        assertFalse(alice.service.hasSession(999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(alice.service.hasSession(bob.uin))

        alice.service.deleteSession(bob.uin)

        assertFalse(alice.service.hasSession(bob.uin))
    }

    @Test
    fun testGetIdentityKey() {
        val identityKey = alice.service.getIdentityKey()
        assertNotNull(identityKey)
        assertTrue(identityKey.isNotEmpty())
        // Should be valid Base64
        assertNotNull(Base64.getDecoder().decode(identityKey))
    }

    @Test
    fun testEncryptedMessageFormat() {
        val plaintext = "Test message format"
        val encrypted = alice.service.encryptMessage(bob.uin, plaintext)

        // Verify encrypted message structure
        assertNotNull(encrypted.ciphertext)
        assertTrue(encrypted.ciphertext.isNotEmpty())
        assertTrue(encrypted.signalType == CiphertextMessage.PREKEY_TYPE || encrypted.signalType == CiphertextMessage.WHISPER_TYPE)

        // Ciphertext should be valid Base64
        assertNotNull(Base64.getDecoder().decode(encrypted.ciphertext))
    }
}
