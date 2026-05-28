package app.rcq.android.crypto

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * v=1 sealed-sender envelope, byte-compatible with the iOS client
 * (CryptoService.swift `encrypt` / `decryptV1`). This is the path used
 * whenever the recipient hasn't published a libsignal v=2 bundle, which
 * is every account today.
 *
 * Scheme:
 *   - ephemeral X25519 keypair per message
 *   - ECDH(ephemeral_priv, recipient_identity_pub) -> 32-byte secret
 *   - HKDF-SHA256(ikm=secret, salt=ephemeral_pub||recipient_pub,
 *                 info="RCQ-1to1-v1", L=32) -> AEAD key
 *   - inner plaintext JSON {from, spub, sig, env}, where
 *       env = base64(envelope JSON), sig = Ed25519(signing_priv) over
 *       (ephemeral_pub || envelope JSON bytes)
 *   - ChaCha20-Poly1305 seal, AAD = ephemeral_pub, combined = nonce(12)
 *       || ciphertext || tag(16)   (CryptoKit ChaChaPoly.combined layout)
 *   - wire JSON {v:1, ek:base64(ephemeral_pub), ct:base64(combined)},
 *       the whole thing base64'd as the `payload` sent to /messages/sealed
 */
object SealedSender {

    private val HKDF_INFO_V1 = "RCQ-1to1-v1".toByteArray(Charsets.UTF_8)
    private const val WIRE_V1 = 1

    class DecryptException(message: String) : Exception(message)

    data class Decrypted(
        val senderUin: Int,
        val envelope: Envelope,
        val senderSigningPub: ByteArray,
    )

    fun encryptV1(
        envelope: Envelope,
        recipientIdentityPub: ByteArray,
        ownUin: Int,
        signingPriv: ByteArray,
        signingPub: ByteArray,
    ): String {
        // Ephemeral X25519 keypair.
        val gen = X25519KeyPairGenerator().apply {
            init(X25519KeyGenerationParameters(SecureRandom()))
        }
        val kp = gen.generateKeyPair()
        val ephPriv = kp.private as X25519PrivateKeyParameters
        val ephPub = (kp.public as X25519PublicKeyParameters).encoded

        val aeadKey = deriveKey(ephPriv, X25519PublicKeyParameters(recipientIdentityPub, 0), ephPub, recipientIdentityPub)

        val envJson = envelope.toJsonBytes()

        // Ed25519 over ephemeral_pub || envelope JSON.
        val signer = Ed25519Signer().apply {
            init(true, Ed25519PrivateKeyParameters(signingPriv, 0))
            update(ephPub, 0, ephPub.size)
            update(envJson, 0, envJson.size)
        }
        val sig = signer.generateSignature()

        val inner = JsonObject().apply {
            addProperty("from", ownUin)
            addProperty("spub", b64(signingPub))
            addProperty("sig", b64(sig))
            addProperty("env", b64(envJson))
        }.toString().toByteArray(Charsets.UTF_8)

        val combined = aeadSeal(aeadKey, aad = ephPub, plaintext = inner)

        val wire = JsonObject().apply {
            addProperty("v", WIRE_V1)
            addProperty("ek", b64(ephPub))
            addProperty("ct", b64(combined))
        }.toString().toByteArray(Charsets.UTF_8)

        return b64(wire)
    }

    fun decryptV1(
        payloadB64: String,
        ownIdentityPriv: ByteArray,
        ownIdentityPub: ByteArray,
    ): Decrypted {
        val wire = JsonParser.parseString(String(unb64(payloadB64), Charsets.UTF_8)).asJsonObject
        val v = wire.get("v")?.asInt ?: 0
        if (v != WIRE_V1) throw DecryptException("unsupported wire version v=$v")
        val ek = unb64(wire.get("ek").asString)
        val ct = unb64(wire.get("ct").asString)

        val ownPriv = X25519PrivateKeyParameters(ownIdentityPriv, 0)
        val aeadKey = deriveKey(ownPriv, X25519PublicKeyParameters(ek, 0), ek, ownIdentityPub)

        val inner = try {
            aeadOpen(aeadKey, aad = ek, combined = ct)
        } catch (e: Exception) {
            throw DecryptException("AEAD open failed: ${e.message}")
        }

        val obj = JsonParser.parseString(String(inner, Charsets.UTF_8)).asJsonObject
        val from = obj.get("from")?.asInt ?: throw DecryptException("missing sender")
        val spub = unb64(obj.get("spub").asString)
        val sig = unb64(obj.get("sig").asString)
        val envBytes = unb64(obj.get("env").asString)

        // Verify Ed25519 over ephemeral_pub || envelope JSON.
        val verifier = Ed25519Signer().apply {
            init(false, Ed25519PublicKeyParameters(spub, 0))
            update(ek, 0, ek.size)
            update(envBytes, 0, envBytes.size)
        }
        if (!verifier.verifySignature(sig)) throw DecryptException("signature verify failed")

        return Decrypted(
            senderUin = from,
            envelope = Envelope.fromJsonBytes(envBytes),
            senderSigningPub = spub,
        )
    }

    // ── primitives ───────────────────────────────────────────────────

    /** HKDF-SHA256 with the X25519 ECDH secret as IKM and
     *  salt = localEphemeralOrEk || peerIdentityPub, matching iOS. */
    private fun deriveKey(
        priv: X25519PrivateKeyParameters,
        pub: X25519PublicKeyParameters,
        ek: ByteArray,
        recipientPub: ByteArray,
    ): ByteArray {
        val secret = ByteArray(32)
        priv.generateSecret(pub, secret, 0)
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(secret, ek + recipientPub, HKDF_INFO_V1))
        val out = ByteArray(32)
        hkdf.generateBytes(out, 0, 32)
        return out
    }

    private fun aeadSeal(key: ByteArray, aad: ByteArray, plaintext: ByteArray): ByteArray {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = ChaCha20Poly1305()
        cipher.init(true, AEADParameters(KeyParameter(key), 128, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        var len = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        len += cipher.doFinal(out, len)
        return nonce + out.copyOf(len)
    }

    private fun aeadOpen(key: ByteArray, aad: ByteArray, combined: ByteArray): ByteArray {
        val nonce = combined.copyOfRange(0, 12)
        val ctTag = combined.copyOfRange(12, combined.size)
        val cipher = ChaCha20Poly1305()
        cipher.init(false, AEADParameters(KeyParameter(key), 128, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(ctTag.size))
        var len = cipher.processBytes(ctTag, 0, ctTag.size, out, 0)
        len += cipher.doFinal(out, len)
        return out.copyOf(len)
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    /** Encrypt→decrypt round-trip self-check. Temporary, for bring-up. */
    fun selfTest(): String = try {
        val recipient = IdentityKeys.generate()
        val sender = IdentityKeys.generate()
        val payload = encryptV1(
            envelope = Envelope.text("hello-roundtrip-42"),
            recipientIdentityPub = recipient.identityPublic,
            ownUin = 555,
            signingPriv = sender.signingPrivate,
            signingPub = sender.signingPublic,
        )
        val dec = decryptV1(payload, recipient.identityPrivate, recipient.identityPublic)
        val text = (dec.envelope as? Envelope.Text)?.text
        if (text == "hello-roundtrip-42" && dec.senderUin == 555) {
            "PASS uin=${dec.senderUin} text=$text"
        } else {
            "FAIL got uin=${dec.senderUin} text=$text"
        }
    } catch (e: Exception) {
        "FAIL ${e.javaClass.simpleName}: ${e.message}"
    }
}
