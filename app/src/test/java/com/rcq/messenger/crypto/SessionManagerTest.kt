package com.rcq.messenger.crypto

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SessionManagerTest {

    private lateinit var alice: TestSignalUser
    private lateinit var bob: TestSignalUser

    @Before
    fun setUp() {
        alice = TestSignalStores.user(123L)
        bob = TestSignalStores.user(456L)
        alice.manager.buildSession(bob.uin, TestSignalStores.preKeyBundleFor(bob))
    }

    @Test
    fun testEncryptDecryptMessage() {
        val plaintext = "Hello Signal Protocol"

        val ciphertext = alice.manager.encryptMessage(bob.uin, plaintext)
        assertNotNull(ciphertext)

        val decrypted = bob.manager.decryptMessage(alice.uin, ciphertext)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testHasSession() {
        assertTrue(alice.manager.hasSession(bob.uin))
        assertFalse(alice.manager.hasSession(999L))
    }

    @Test
    fun testDeleteSession() {
        assertTrue(alice.manager.hasSession(bob.uin))

        alice.manager.deleteSession(bob.uin)

        assertFalse(alice.manager.hasSession(bob.uin))
    }

    @Test
    fun testEncryptMultipleMessages() {
        val messages = listOf("First message", "Second message", "Third message")
        val ciphertexts = messages.map { message ->
            alice.manager.encryptMessage(bob.uin, message)
        }

        ciphertexts.forEachIndexed { index, ciphertext ->
            assertEquals(messages[index], bob.manager.decryptMessage(alice.uin, ciphertext))
        }

        assertEquals(3, ciphertexts.size)
        ciphertexts.forEach { assertNotNull(it) }
    }
}
