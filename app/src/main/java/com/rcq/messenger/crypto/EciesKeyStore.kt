package com.rcq.messenger.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores Curve25519 key pairs for ECIES v=1 (iOS-compatible).
 *
 * identity key pair — for ECDH; identity_key = raw 32-byte pub sent at /auth/register.
 * signing key pair  — for Ed25519 envelope sig; signing_key = raw 32-byte pub sent at /auth/register.
 *
 * Stored as "<priv_b64>:<pub_b64>" in SharedPreferences (MODE_PRIVATE).
 * These are SEPARATE from Signal Protocol keys (SignalKeyStore).
 */
@Singleton
class EciesKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EciesKeyStore"
        private const val PREFS = "rcq_ecies_keys"
        private const val K_IDENTITY = "ecies_identity"
        private const val K_SIGNING = "ecies_signing"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /**
     * Loads existing keys or generates new ones. Configures [ecies] with the loaded pairs.
     */
    fun loadOrGenerate(ecies: EciesCrypto) {
        ecies.identityKeyPair = loadOrGen(K_IDENTITY)
        ecies.signingKeyPair = loadOrGen(K_SIGNING)
        Log.d(TAG, "ECIES ready. idPub=${ecies.ecPubToRawB64(ecies.identityKeyPair!!.publicKey).take(8)}…")
    }

    fun identityPubB64(ecies: EciesCrypto): String =
        ecies.ecPubToRawB64(ecies.identityKeyPair ?: error("call loadOrGenerate first"))

    fun signingPubB64(ecies: EciesCrypto): String =
        ecies.ecPubToRawB64(ecies.signingKeyPair ?: error("call loadOrGenerate first"))

    private fun EciesCrypto.ecPubToRawB64(kp: ECKeyPair): String = ecPubToRawB64(kp.publicKey)

    private fun loadOrGen(key: String): ECKeyPair {
        val stored = prefs.getString(key, null)
        if (stored != null) {
            runCatching {
                val parts = stored.split(":")
                if (parts.size == 2) {
                    val privBytes = Base64.decode(parts[0], Base64.NO_WRAP)
                    val pubBytes = Base64.decode(parts[1], Base64.NO_WRAP) // 33 bytes (0x05 + 32)
                    val priv = Curve.decodePrivatePoint(privBytes)
                    val pub = Curve.decodePoint(pubBytes, 0)
                    return ECKeyPair(pub, priv)
                }
            }.onFailure { Log.w(TAG, "loadOrGen $key failed: ${it.message}") }
        }
        return generateAndStore(key)
    }

    private fun generateAndStore(key: String): ECKeyPair {
        val kp = Curve.generateKeyPair()
        val privB64 = Base64.encodeToString(kp.privateKey.serialize(), Base64.NO_WRAP)
        val pubB64 = Base64.encodeToString(kp.publicKey.serialize(), Base64.NO_WRAP) // 33 bytes
        prefs.edit().putString(key, "$privB64:$pubB64").apply()
        Log.d(TAG, "Generated ECIES $key pair")
        return kp
    }
}
