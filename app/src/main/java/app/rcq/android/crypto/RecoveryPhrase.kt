package app.rcq.android.crypto

import android.content.Context
import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.MessageDigest

/**
 * BIP39 mnemonic over the 32-byte recovery seed — the human-writable form of
 * the account's [IdentityKeys] derivation seed. 32 bytes (256 bits) + an 8-bit
 * SHA-256 checksum = 264 bits = 24 words from the standard 2048-word English
 * list (bundled at `assets/bip39-english.txt`). Standard BIP39 so a phrase
 * round-trips identically on iOS, and the checksum catches transcription typos.
 *
 * Also hosts the Ed25519 challenge signing used by account recovery
 * (`/auth/recover`): proving possession of the signing private key.
 */
object RecoveryPhrase {

    @Volatile private var cachedWords: List<String>? = null

    fun wordlist(context: Context): List<String> = cachedWords ?: synchronized(this) {
        cachedWords ?: context.applicationContext.assets.open("bip39-english.txt")
            .bufferedReader().useLines { seq -> seq.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
            .also { cachedWords = it }
    }

    /** 32-byte seed → 24 words. */
    fun encode(seed: ByteArray, context: Context): List<String> {
        require(seed.size == 32) { "recovery seed must be 32 bytes" }
        val words = wordlist(context)
        val hash = sha256(seed)
        val csBits = seed.size * 8 / 32 // 8
        val bits = ArrayList<Boolean>(seed.size * 8 + csBits)
        for (b in seed) for (i in 7 downTo 0) bits.add((b.toInt() ushr i) and 1 == 1)
        for (i in 0 until csBits) bits.add((hash[i / 8].toInt() ushr (7 - i % 8)) and 1 == 1)
        val out = ArrayList<String>(bits.size / 11)
        var idx = 0
        while (idx < bits.size) {
            var v = 0
            for (j in 0 until 11) v = (v shl 1) or (if (bits[idx + j]) 1 else 0)
            out.add(words[v]); idx += 11
        }
        return out
    }

    /** 24 words → 32-byte seed, or null if word/count/checksum is invalid. */
    fun decode(input: List<String>, context: Context): ByteArray? {
        if (input.size != 24) return null
        val words = wordlist(context)
        val bits = ArrayList<Boolean>(264)
        for (w in input) {
            val i = words.indexOf(w.trim().lowercase())
            if (i < 0) return null
            for (j in 10 downTo 0) bits.add((i ushr j) and 1 == 1)
        }
        val csBits = bits.size / 33 // 8
        val entBits = bits.size - csBits // 256
        val ent = ByteArray(entBits / 8)
        for (i in 0 until entBits) if (bits[i]) ent[i / 8] = (ent[i / 8].toInt() or (1 shl (7 - i % 8))).toByte()
        val hash = sha256(ent)
        for (i in 0 until csBits) {
            val expected = (hash[i / 8].toInt() ushr (7 - i % 8)) and 1 == 1
            if (bits[entBits + i] != expected) return null
        }
        return ent
    }

    /** Parse free-form user input ("word word ...", any spacing/case) → words. */
    fun parse(raw: String): List<String> =
        raw.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    /** base64 Ed25519 signature over [challenge], proving private-key ownership
     *  for `/auth/recover`. */
    fun signChallenge(signingPrivate: ByteArray, challenge: String): String {
        val signer = Ed25519Signer().apply {
            init(true, Ed25519PrivateKeyParameters(signingPrivate, 0))
            val msg = challenge.toByteArray(Charsets.UTF_8)
            update(msg, 0, msg.size)
        }
        return Base64.encodeToString(signer.generateSignature(), Base64.NO_WRAP)
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)
}
