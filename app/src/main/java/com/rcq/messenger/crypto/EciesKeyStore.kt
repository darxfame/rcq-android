package com.rcq.messenger.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.KeyPairGenerator
import net.i2p.crypto.eddsa.spec.EdDSAGenParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.security.SecureRandom
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages two separate key types for ECIES v=1 (iOS-compatible):
 *
 * identity key pair (X25519/Curve25519) — ECDH key exchange.
 *   identity_key = raw 32-byte X25519 pub, sent at /auth/register.
 *
 * signing key pair (Ed25519) — envelope signature.
 *   signing_key = raw 32-byte Ed25519 pub, sent at /auth/register.
 *   MUST be Ed25519 (not X25519): iOS Curve25519.Signing is Ed25519 (Edwards),
 *   sending X25519 Montgomery bytes causes iOS signature verification to fail.
 */
@Singleton
class EciesKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EciesKeyStore"
        private const val PREFS = "rcq_ecies_keys"
        private const val K_IDENTITY = "ecies_identity"      // X25519 priv:pub
        private const val K_SIGNING_ED = "ecies_signing_ed"  // Ed25519 seed
        private val ED25519_SPEC = EdDSANamedCurveTable.getByName("Ed25519")
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    fun loadOrGenerate(ecies: EciesCrypto) {
        ecies.identityKeyPair = loadOrGenX25519(K_IDENTITY)
        ecies.ed25519PrivKey = loadOrGenEd25519(K_SIGNING_ED)
        Log.d(TAG, "ECIES ready. idPub=${ecies.ecPubToRawB64(ecies.identityKeyPair!!.publicKey).take(8)}…")
    }

    fun identityPubB64(ecies: EciesCrypto): String =
        ecies.ecPubToRawB64(ecies.identityKeyPair?.publicKey ?: error("call loadOrGenerate first"))

    fun signingPubB64(ecies: EciesCrypto): String = ecies.signingPubKeyB64()

    // -----------------------------------------------------------------------
    // X25519 identity key
    // -----------------------------------------------------------------------
    private fun loadOrGenX25519(key: String): ECKeyPair {
        val stored = prefs.getString(key, null)
        if (stored != null) {
            runCatching {
                val parts = stored.split(":")
                if (parts.size == 2) {
                    val priv = Curve.decodePrivatePoint(Base64.decode(parts[0], Base64.NO_WRAP))
                    val pub = Curve.decodePoint(Base64.decode(parts[1], Base64.NO_WRAP), 0)
                    return ECKeyPair(pub, priv)
                }
            }.onFailure { Log.w(TAG, "loadOrGenX25519 failed: ${it.message}") }
        }
        val kp = Curve.generateKeyPair()
        val privB64 = Base64.encodeToString(kp.privateKey.serialize(), Base64.NO_WRAP)
        val pubB64 = Base64.encodeToString(kp.publicKey.serialize(), Base64.NO_WRAP)
        prefs.edit().putString(key, "$privB64:$pubB64").apply()
        Log.d(TAG, "Generated ECIES identity (X25519) key")
        return kp
    }

    // -----------------------------------------------------------------------
    // Ed25519 signing key — 32-byte seed stored as base64
    // -----------------------------------------------------------------------
    private fun loadOrGenEd25519(key: String): EdDSAPrivateKey {
        val stored = prefs.getString(key, null)
        if (stored != null) {
            runCatching {
                val seed = Base64.decode(stored, Base64.NO_WRAP)
                if (seed.size == 32) {
                    return EdDSAPrivateKey(EdDSAPrivateKeySpec(seed, ED25519_SPEC))
                }
            }.onFailure { Log.w(TAG, "loadOrGenEd25519 failed: ${it.message}") }
        }
        val kpg = KeyPairGenerator()
        kpg.initialize(EdDSAGenParameterSpec("Ed25519"), SecureRandom())
        val kp = kpg.generateKeyPair()
        val priv = kp.private as EdDSAPrivateKey
        prefs.edit().putString(key, Base64.encodeToString(priv.seed, Base64.NO_WRAP)).apply()
        Log.d(TAG, "Generated ECIES signing (Ed25519) key")
        return priv
    }
}
