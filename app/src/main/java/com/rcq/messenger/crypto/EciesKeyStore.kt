package com.rcq.messenger.crypto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import timber.log.Timber
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages two separate key types for ECIES v=1 (iOS-compatible).
 *
 * identity key pair (X25519) — ECDH key exchange.
 * signing key pair (Ed25519) — envelope signature via BouncyCastle Ed25519Signer (RFC 8032).
 *
 * Ed25519 seed stored as raw 32 bytes — identical format to
 * Apple CryptoKit Curve25519.Signing.PrivateKey.rawRepresentation.
 * Existing seeds written by the previous net.i2p.crypto.eddsa implementation
 * are byte-compatible — no migration needed.
 */
@Singleton
class EciesKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS        = "rcq_ecies_keys"
        private const val K_IDENTITY   = "ecies_identity"      // Base64(privKey):Base64(pubKey)
        private const val K_SIGNING_ED = "ecies_signing_ed"    // Base64(32-byte Ed25519 seed)
        private val ENC = Base64.getEncoder()
        private val DEC = Base64.getDecoder()
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun loadOrGenerate(ecies: EciesCrypto) {
        ecies.identityKeyPair  = loadOrGenX25519(K_IDENTITY)
        ecies.ed25519SeedBytes = loadOrGenEd25519Seed(K_SIGNING_ED)
        Timber.d("ECIES ready. idPub=${ecies.ecPubToRawB64(ecies.identityKeyPair!!.publicKey).take(8)}…")
    }

    fun identityPubB64(ecies: EciesCrypto): String =
        ecies.ecPubToRawB64(ecies.identityKeyPair ?: error("call loadOrGenerate first"))

    fun signingPubB64(ecies: EciesCrypto): String = ecies.signingPubKeyB64()

    fun hasKeys(): Boolean =
        prefs.getString(K_IDENTITY, null) != null &&
        prefs.getString(K_SIGNING_ED, null) != null

    private fun loadOrGenX25519(key: String): ECKeyPair {
        prefs.getString(key, null)?.let { stored ->
            runCatching {
                val parts = stored.split(":")
                if (parts.size == 2) {
                    val priv = Curve.decodePrivatePoint(DEC.decode(parts[0]))
                    val pub  = Curve.decodePoint(DEC.decode(parts[1]), 0)
                    return ECKeyPair(pub, priv)
                }
            }.onFailure { Timber.w("loadOrGenX25519 failed: ${it.message}") }
        }
        val kp = Curve.generateKeyPair()
        prefs.edit().putString(key,
            "${ENC.encodeToString(kp.privateKey.serialize())}:${ENC.encodeToString(kp.publicKey.serialize())}"
        ).apply()
        Timber.d("Generated ECIES identity (X25519) key")
        return kp
    }

    private fun loadOrGenEd25519Seed(key: String): ByteArray {
        prefs.getString(key, null)?.let { stored ->
            runCatching {
                val seed = DEC.decode(stored)
                if (seed.size == 32) return seed
            }.onFailure { Timber.w("loadOrGenEd25519Seed failed: ${it.message}") }
        }
        val seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(key, ENC.encodeToString(seed)).apply()
        Timber.d("Generated ECIES signing (Ed25519) seed")
        return seed
    }
}
