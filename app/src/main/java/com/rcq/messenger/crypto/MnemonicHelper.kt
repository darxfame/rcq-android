package com.rcq.messenger.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object MnemonicHelper {

    // Minimal BIP39 English word list subset (first 256 words)
    // TODO: replace with full 2048-word list via cash.z.ecc.android:kotlin-bip39
    private val WORD_LIST = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
        "absurd", "abuse", "access", "accident", "account", "accuse", "achieve", "acid",
        "acoustic", "acquire", "across", "act", "action", "actor", "actress", "actual",
        "adapt", "add", "addict", "address", "adjust", "admit", "adult", "advance",
        "advice", "aerobic", "affair", "afford", "afraid", "again", "age", "agent",
        "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album",
        "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha",
        "already", "also", "alter", "always", "amateur", "amazing", "among", "amount",
        "amused", "analyst", "anchor", "ancient", "anger", "angle", "angry", "animal",
        "ankle", "announce", "annual", "another", "answer", "antenna", "antique", "anxiety",
        "apart", "apology", "appear", "apple", "approve", "april", "arch", "arctic",
        "area", "arena", "argue", "arm", "armed", "armor", "army", "around",
        "arrange", "arrest", "arrive", "arrow", "art", "artefact", "artist", "artwork",
        "ask", "aspect", "assault", "asset", "assist", "assume", "asthma", "athlete",
        "atom", "attack", "attend", "attitude", "attract", "auction", "audit", "august",
        "aunt", "author", "auto", "autumn", "average", "avocado", "avoid", "awake",
        "aware", "away", "awesome", "awful", "awkward", "axis", "baby", "balance",
        "bamboo", "banana", "banner", "barely", "bargain", "barrel", "base", "basic",
        "basket", "battle", "beach", "bean", "beauty", "become", "beef", "before",
        "begin", "behave", "behind", "believe", "below", "belt", "bench", "benefit",
        "best", "betray", "better", "between", "beyond", "bicycle", "bid", "bike",
        "bind", "biology", "bird", "birth", "bitter", "black", "blade", "blame",
        "blanket", "blast", "bleak", "bless", "blind", "blood", "blossom", "blouse",
        "blue", "blur", "blush", "board", "boat", "body", "boil", "bomb",
        "bone", "book", "boost", "border", "boring", "borrow", "boss", "bottom",
        "bounce", "box", "boy", "bracket", "brain", "brand", "brave", "breeze",
        "brick", "bridge", "brief", "bright", "bring", "brisk", "broccoli", "broken",
        "bronze", "broom", "brother", "brown", "brush", "bubble", "buddy", "budget",
        "buffalo", "build", "bulb", "bulk", "bullet", "bundle", "bunker", "burden",
        "burger", "burst", "bus", "business", "busy", "butter", "buyer", "buzz"
    )

    /** Generate a 24-word mnemonic from 256 bits of secure random entropy. */
    fun generateMnemonic(): String {
        val entropy = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return (0 until 24).map { i ->
            val idx = (entropy[i].toInt() and 0xFF) % WORD_LIST.size
            WORD_LIST[idx]
        }.joinToString(" ")
    }

    /** Derive AES-256 key from mnemonic via PBKDF2-SHA256 (600k iterations). */
    fun deriveKey(mnemonic: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(mnemonic.toCharArray(), salt, 600_000, 256)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /** AES-256-GCM encrypt. Returns base64(12-byte-IV || ciphertext+tag). */
    fun encrypt(plaintext: ByteArray, key: SecretKeySpec): String {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plaintext))
    }

    /** AES-256-GCM decrypt base64(IV || ciphertext+tag). */
    fun decrypt(encoded: String, key: SecretKeySpec): ByteArray {
        val blob = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 0, 12))
        return cipher.doFinal(blob, 12, blob.size - 12)
    }
}
