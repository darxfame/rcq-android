package com.rcq.messenger.crypto

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.SignalProtocolException
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyStore @Inject constructor(
    private val identityKeyPair: IdentityKeyPair
) : InMemorySignalProtocolStore(identityKeyPair, 0) {

    override fun storeSession(address: SignalProtocolAddress, record: SessionRecord) {
        super.storeSession(address, record)
        // TODO: Persist to DB async when SignalKeyDao is implemented
    }

    override fun loadSession(address: SignalProtocolAddress): SessionRecord {
        return super.loadSession(address)
    }

    override fun storePreKey(preKeyId: Int, record: PreKeyRecord) {
        super.storePreKey(preKeyId, record)
    }

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return super.loadPreKey(preKeyId) ?: throw SignalProtocolException("PreKey $preKeyId not found")
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord) {
        super.storeSignedPreKey(signedPreKeyId, record)
    }

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return super.loadSignedPreKey(signedPreKeyId) ?: throw SignalProtocolException("SignedPreKey $signedPreKeyId not found")
    }

    override fun isTrustedIdentity(address: SignalProtocolAddress, identityKey: IdentityKey, direction: org.signal.libsignal.protocol.state.IdentityKeyStore.Direction): Boolean {
        return super.isTrustedIdentity(address, identityKey, direction)
    }

    override fun getIdentityKeyPair(): IdentityKeyPair {
        return super.getIdentityKeyPair()
    }

    override fun getLocalRegistrationId(): Int {
        return super.getLocalRegistrationId()
    }

    override fun saveIdentity(address: SignalProtocolAddress, identityKey: IdentityKey): Boolean {
        return super.saveIdentity(address, identityKey)
    }
}