package com.rcq.messenger.crypto

import android.util.Base64
import android.util.Log
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.json.JSONObject
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kdf.HKDF
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * iOS-compatible ECIES v=1 wire format.
 *
 * Wire: base64({"v":1,"ek":"<eph Curve25519 pub 32B b64>","ct":"<nonce+ct+tag b64>"})
 * Plaintext: {"from":ownUIN,"spub":"<signing pub b64>","sig":"<ed25519 sig b64>","env":"<Envelope JSON b64>"}
 * Envelope JSON: {"kind":"text","id":"<UUID>","text":"..."} etc.
 */
@Singleton
class EciesCrypto @Inject constructor() {

    companion object {
        private const val TAG = "EciesCrypto"
        private const val WIRE_V1 = 1
        private val HKDF_INFO_V1 = "RCQ-1to1-v1".toByteArray(Charsets.UTF_8)
        private const val NONCE_LEN = 12
        private const val TAG_BITS = 128
    }

    var ownUin: Long = 0L
    var identityKeyPair: ECKeyPair? = null
    var signingKeyPair: ECKeyPair? = null

    fun isReady() = ownUin != 0L && identityKeyPair != null && signingKeyPair != null

    // Envelope JSON builders (iOS codec keys)
    fun buildTextEnvelope(id: String, text: String): JSONObject = JSONObject().apply {
        put("kind", "text")
        put("id", id)
        put("text", text)
    }

    fun parseEnvelopeText(envJson: JSONObject): String? =
        if (envJson.optString("kind") == "text") envJson.optString("text").takeIf { it.isNotEmpty() } else null

    // -----------------------------------------------------------------------
    // encryptV1: returns base64 wire payload
    // -----------------------------------------------------------------------
    fun encryptV1(envelopeJson: JSONObject, recipientIdentityKeyB64: String): String {
        val idKP = identityKeyPair ?: error("ECIES identityKeyPair not loaded")
        val sigKP = signingKeyPair ?: error("ECIES signingKeyPair not loaded")

        val recipientRaw = Base64.decode(recipientIdentityKeyB64, Base64.NO_WRAP)
        val recipientPub = rawToEcPub(recipientRaw)

        val ephemeral = Curve.generateKeyPair()
        val ekBytes = ecPubToRaw(ephemeral.publicKey)

        val shared = Curve.calculateAgreement(recipientPub, ephemeral.privateKey)
        val aeadKey = HKDF.deriveSecrets(shared, ekBytes + recipientRaw, HKDF_INFO_V1, 32)

        val envBytes = envelopeJson.toString().toByteArray(Charsets.UTF_8)
        val envB64 = Base64.encodeToString(envBytes, Base64.NO_WRAP)

        val sig = Curve.calculateSignature(sigKP.privateKey, ekBytes + envBytes)
        val spubB64 = Base64.encodeToString(ecPubToRaw(sigKP.publicKey), Base64.NO_WRAP)

        val inner = JSONObject().apply {
            put("from", ownUin)
            put("spub", spubB64)
            put("sig", Base64.encodeToString(sig, Base64.NO_WRAP))
            put("env", envB64)
        }.toString().toByteArray(Charsets.UTF_8)

        val nonce = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }
        val combined = chacha(inner, aeadKey, nonce, ekBytes, encrypt = true)

        val wire = JSONObject().apply {
            put("v", WIRE_V1)
            put("ek", Base64.encodeToString(ekBytes, Base64.NO_WRAP))
            put("ct", Base64.encodeToString(combined, Base64.NO_WRAP))
        }
        return Base64.encodeToString(wire.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    // -----------------------------------------------------------------------
    // decryptV1: returns null on any failure
    //
    // signerPubKeyB64 is returned so the CALLER can enforce TOFU / key pinning:
    //   - if the contact has a stored signing_key, caller must verify it matches
    //   - if unknown sender (no stored key), caller stores this as TOFU
    // We do NOT trust `from` blindly — the field is inside the AEAD but is
    // still caller-controlled (attacker could self-encrypt with from=victim).
    // -----------------------------------------------------------------------
    data class Decrypted(
        val senderUin: Long,
        val envelopeJson: JSONObject,
        /** Raw 32-byte base64 of the signing key that produced the signature. */
        val signerPubKeyB64: String
    )

    fun decryptV1(wireB64: String): Decrypted? = runCatching {
        val idKP = identityKeyPair ?: return null
        val wireBytes = Base64.decode(wireB64, Base64.NO_WRAP)
        val wire = JSONObject(String(wireBytes, Charsets.UTF_8))
        if (wire.optInt("v", -1) != WIRE_V1) return null

        val ekBytes = Base64.decode(wire.getString("ek"), Base64.NO_WRAP)
        val combined = Base64.decode(wire.getString("ct"), Base64.NO_WRAP)

        val ourPubRaw = ecPubToRaw(idKP.publicKey)
        val shared = Curve.calculateAgreement(rawToEcPub(ekBytes), idKP.privateKey)
        val aeadKey = HKDF.deriveSecrets(shared, ekBytes + ourPubRaw, HKDF_INFO_V1, 32)

        val nonce = combined.copyOf(NONCE_LEN)
        val cipherTag = combined.copyOfRange(NONCE_LEN, combined.size)
        val plain = chacha(cipherTag, aeadKey, nonce, ekBytes, encrypt = false)

        val inner = JSONObject(String(plain, Charsets.UTF_8))
        val from = inner.getLong("from")
        val envBytes = Base64.decode(inner.getString("env"), Base64.NO_WRAP)
        val spubB64 = inner.getString("spub")
        val spubRaw = Base64.decode(spubB64, Base64.NO_WRAP)
        val sigBytes = Base64.decode(inner.getString("sig"), Base64.NO_WRAP)

        if (!Curve.verifySignature(rawToEcPub(spubRaw), ekBytes + envBytes, sigBytes)) {
            Log.w(TAG, "decryptV1: sig verify failed from $from")
            return null
        }

        // Return signerPubKeyB64 so the caller can enforce TOFU / pinning.
        // We do NOT validate here because we lack DB access — caller must do it.
        Decrypted(from, JSONObject(String(envBytes, Charsets.UTF_8)), spubB64)
    }.onFailure { Log.w(TAG, "decryptV1 failed: ${it.message}") }.getOrNull()

    // -----------------------------------------------------------------------
    // ChaCha20-Poly1305 via BouncyCastle
    // encrypt: returns nonce(12) + ciphertext + tag(16)
    // decrypt: input is ciphertext+tag (nonce provided separately)
    // -----------------------------------------------------------------------
    private fun chacha(
        input: ByteArray, key: ByteArray, nonce: ByteArray,
        aad: ByteArray, encrypt: Boolean
    ): ByteArray {
        val cipher = ChaCha20Poly1305()
        cipher.init(encrypt, AEADParameters(KeyParameter(key), TAG_BITS, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(input.size))
        var len = cipher.processBytes(input, 0, input.size, out, 0)
        len += cipher.doFinal(out, len)
        val result = out.copyOf(len)
        return if (encrypt) nonce + result else result
    }

    // -----------------------------------------------------------------------
    // Key format helpers
    // libsignal serialize() = 0x05 prefix + 32 bytes; iOS uses raw 32 bytes
    // -----------------------------------------------------------------------
    fun ecPubToRaw(pub: ECPublicKey): ByteArray = pub.serialize().copyOfRange(1, 33)

    fun rawToEcPub(raw: ByteArray): ECPublicKey {
        require(raw.size == 32) { "rawToEcPub: expected 32B got ${raw.size}" }
        return Curve.decodePoint(byteArrayOf(0x05) + raw, 0)
    }

    fun ecPubToRawB64(pub: ECPublicKey): String =
        Base64.encodeToString(ecPubToRaw(pub), Base64.NO_WRAP)
}
