package com.rcq.messenger.crypto

import org.junit.Test
import org.junit.Before
import org.junit.Assert.*
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord

class SignalKeyStoreTest {

    private lateinit var signalKeyStore: SignalKeyStore
    private lateinit var identityKeyPair: IdentityKeyPair

    @Before
    fun setUp() {
        identityKeyPair = IdentityKeyPair.generate()
        signalKeyStore = TestSignalStores.signalKeyStore(identityKeyPair)
    }

    @Test
    fun testStoreAndLoadPreKey() {
        val preKeyId = 1
        val preKeyPair = Curve.generateKeyPair()
        val preKeyRecord = PreKeyRecord(preKeyId, preKeyPair)

        signalKeyStore.storePreKey(preKeyId, preKeyRecord)
        val loadedPreKey = signalKeyStore.loadPreKey(preKeyId)

        assertNotNull(loadedPreKey)
        assertEquals(preKeyId, loadedPreKey.id)
    }

    @Test(expected = IllegalStateException::class)
    fun testLoadNonExistentPreKey() {
        signalKeyStore.loadPreKey(999)
    }

    @Test
    fun testStoreAndLoadSignedPreKey() {
        val signedPreKeyId = 1
        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, signedPreKeyPair.publicKey.serialize())
        val signedPreKeyRecord = SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKeyPair, signature)

        signalKeyStore.storeSignedPreKey(signedPreKeyId, signedPreKeyRecord)
        val loadedSignedPreKey = signalKeyStore.loadSignedPreKey(signedPreKeyId)

        assertNotNull(loadedSignedPreKey)
        assertEquals(signedPreKeyId, loadedSignedPreKey.id)
    }

    @Test(expected = IllegalStateException::class)
    fun testLoadNonExistentSignedPreKey() {
        signalKeyStore.loadSignedPreKey(999)
    }

    @Test
    fun testGetIdentityKeyPair() {
        val retrievedKeyPair = signalKeyStore.identityKeyPair
        assertEquals(identityKeyPair.publicKey, retrievedKeyPair.publicKey)
        assertEquals(identityKeyPair.privateKey, retrievedKeyPair.privateKey)
    }

    @Test
    fun testGetLocalRegistrationId() {
        val registrationId = signalKeyStore.localRegistrationId
        assertTrue(registrationId > 0)
    }
}
