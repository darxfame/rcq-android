package com.rcq.messenger.crypto

import java.util.Base64
import timber.log.Timber
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.signers.Ed25519Signer
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
 * ECDH key (identityKeyPair): Curve25519/X25519 — key agreement only.
 * Signing key (ed25519SeedBytes): Ed25519 RFC 8032 via BouncyCastle Ed25519Signer.
 *   iOS uses Curve25519.Signing (Apple CryptoKit) = Ed25519 RFC 8032.
 *   Seed = 32 bytes, identical to iOS Curve25519.Signing.PrivateKey.rawRepresentation.
 *
 * Wire v=1: base64({"v":1,"ek":"<X25519 pub 32B>","ct":"<nonce+ct+tag>"})
 * Inner:    {"from":uin,"spub":"<Ed25519 pub 32B>","sig":"<64B>","env":"<Envelope>"}
 */
@Singleton
class EciesCrypto @Inject constructor() {

    companion object {
        private const val WIRE_V1   = 1
        private val HKDF_INFO_V1    = "RCQ-1to1-v1".toByteArray(Charsets.UTF_8)
        private const val NONCE_LEN = 12
        private const val TAG_BITS  = 128
        private val ENC = Base64.getEncoder()
        private val DEC = Base64.getDecoder()
    }

    var ownUin: Long = 0L
    var identityKeyPair: ECKeyPair? = null    // X25519 for ECDH
    var ed25519SeedBytes: ByteArray? = null   // 32-byte Ed25519 seed (RFC 8032)

    fun isReady() = ownUin != 0L && identityKeyPair != null && ed25519SeedBytes != null

    /** Raw 32-byte Ed25519 public key for registration (iOS-compatible signing_key). */
    fun signingPubKeyBytes(): ByteArray {
        val seed = ed25519SeedBytes ?: return ByteArray(32)
        return Ed25519PrivateKeyParameters(seed).generatePublicKey().encoded
    }

    fun signingPubKeyB64(): String = ENC.encodeToString(signingPubKeyBytes())

    fun buildTextEnvelope(id: String, text: String): JSONObject = JSONObject().apply {
        put("kind", "text"); put("id", id); put("text", text)
    }

    fun parseEnvelopeText(envJson: JSONObject): String? =
        if (envJson.optString("kind") == "text") envJson.optString("text").takeIf { it.isNotEmpty() } else null

    // -----------------------------------------------------------------------
    // encryptV1 — matches iOS SignalCryptoService.encrypt(envelope:for:)
    // iOS toSign = ephemeralPubBytes + envelopeJSON = ekBytes + envBytes
    // -----------------------------------------------------------------------
    fun encryptV1(envelopeJson: JSONObject, recipientIdentityKeyB64: String): String {
        val seed  = ed25519SeedBytes ?: error("ECIES ed25519SeedBytes not loaded")

        val recipientRaw = DEC.decode(recipientIdentityKeyB64)
        val recipientPub = rawToEcPub(recipientRaw)

        val ephemeral = Curve.generateKeyPair()
        val ekBytes   = ecPubToRaw(ephemeral.publicKey)
        val shared    = Curve.calculateAgreement(recipientPub, ephemeral.privateKey)
        val aeadKey   = HKDF.deriveSecrets(shared, ekBytes + recipientRaw, HKDF_INFO_V1, 32)

        val envBytes = envelopeJson.toString().toByteArray(Charsets.UTF_8)
        // Sign: ek || env — identical to iOS: ephemeralPubBytes + envelopeJSON
        val sig = ed25519Sign(seed, ekBytes + envBytes)

        val inner = JSONObject().apply {
            put("from", ownUin)
            put("spub", signingPubKeyB64())
            put("sig",  ENC.encodeToString(sig))
            put("env",  ENC.encodeToString(envBytes))
        }.toString().toByteArray(Charsets.UTF_8)

        val nonce    = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }
        val combined = chacha(inner, aeadKey, nonce, ekBytes, encrypt = true)

        return ENC.encodeToString(JSONObject().apply {
            put("v",  WIRE_V1)
            put("ek", ENC.encodeToString(ekBytes))
            put("ct", ENC.encodeToString(combined))
        }.toString().toByteArray(Charsets.UTF_8))
    }

    // -----------------------------------------------------------------------
    // decryptV1 — matches iOS SignalCryptoService.decryptV1(wire:)
    // -----------------------------------------------------------------------
    data class Decrypted(val senderUin: Long, val envelopeJson: JSONObject, val signerPubKeyB64: String)

    fun decryptV1(wireB64: String): Decrypted? = runCatching {
        val idKP     = identityKeyPair ?: return null
        val wire     = JSONObject(String(DEC.decode(wireB64), Charsets.UTF_8))
        if (wire.optInt("v", -1) != WIRE_V1) return null

        val ekBytes   = DEC.decode(wire.getString("ek"))
        val combined  = DEC.decode(wire.getString("ct"))
        val ourPubRaw = ecPubToRaw(idKP.publicKey)
        val shared    = Curve.calculateAgreement(rawToEcPub(ekBytes), idKP.privateKey)
        val aeadKey   = HKDF.deriveSecrets(shared, ekBytes + ourPubRaw, HKDF_INFO_V1, 32)

        val nonce     = combined.copyOf(NONCE_LEN)
        val cipherTag = combined.copyOfRange(NONCE_LEN, combined.size)
        val plain     = chacha(cipherTag, aeadKey, nonce, ekBytes, encrypt = false)

        val inner    = JSONObject(String(plain, Charsets.UTF_8))
        val from     = inner.getLong("from")
        val envBytes = DEC.decode(inner.getString("env"))
        val spubB64  = inner.getString("spub")
        val spubRaw  = DEC.decode(spubB64)
        val sigBytes = DEC.decode(inner.getString("sig"))

        // Verify: iOS spub.isValidSignature(sigBytes, for: ekBytes + envBytes)
        if (!ed25519Verify(spubRaw, ekBytes + envBytes, sigBytes)) {
            Timber.w("decryptV1: Ed25519 sig verify failed from $from")
            return null
        }
        Decrypted(from, JSONObject(String(envBytes, Charsets.UTF_8)), spubB64)
    }.onFailure { Timber.w("decryptV1 failed: ${it.message}") }.getOrNull()

    // -----------------------------------------------------------------------
    // Ed25519 via BouncyCastle Ed25519Signer (RFC 8032)
    // Wire-compatible with iOS Curve25519.Signing (Apple CryptoKit).
    // seed = 32 bytes = iOS Curve25519.Signing.PrivateKey.rawRepresentation
    // -----------------------------------------------------------------------
    private fun ed25519Sign(seed: ByteArray, data: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(seed))
        signer.update(data, 0, data.size)
        return signer.generateSignature()  // 64 bytes
    }

    private fun ed25519Verify(pubKeyBytes: ByteArray, data: ByteArray, sig: ByteArray): Boolean =
        runCatching {
            val verifier = Ed25519Signer()
            verifier.init(false, Ed25519PublicKeyParameters(pubKeyBytes))
            verifier.update(data, 0, data.size)
            verifier.verifySignature(sig)
        }.getOrElse { false }

    // -----------------------------------------------------------------------
    // ChaCha20-Poly1305 via BouncyCastle
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
    // X25519 helpers: libsignal serialize() = 0x05 + 32 bytes; iOS = raw 32
    // -----------------------------------------------------------------------
    fun ecPubToRaw(pub: ECPublicKey): ByteArray = pub.serialize().copyOfRange(1, 33)

    fun rawToEcPub(raw: ByteArray): ECPublicKey {
        require(raw.size == 32) { "rawToEcPub: expected 32B, got ${raw.size}" }
        return Curve.decodePoint(byteArrayOf(0x05) + raw, 0)
    }

    fun ecPubToRawB64(pub: ECPublicKey): String = ENC.encodeToString(ecPubToRaw(pub))

    fun ecPubToRawB64(kp: ECKeyPair): String = ecPubToRawB64(kp.publicKey)
}
