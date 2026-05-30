package app.rcq.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for the local identity — the Android analogue
 * of the iOS Keychain. Holds the UIN, the JWT, the display nickname, and
 * the two private keys (X25519 identity, Ed25519 signing). The server
 * never sees the private halves.
 *
 * MVP scope: a single identity. Multi-account (the iOS-style roster with
 * per-account prefixes) is a later milestone; for now there is exactly one
 * slot, so keys are unprefixed.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val isRegistered: Boolean
        get() = uin != null && !token.isNullOrEmpty()

    val uin: Int?
        get() = if (prefs.contains(K_UIN)) prefs.getInt(K_UIN, 0) else null

    val token: String?
        get() = prefs.getString(K_TOKEN, null)

    val nickname: String?
        get() = prefs.getString(K_NICK, null)

    val identityPrivate: ByteArray?
        get() = prefs.getString(K_ID_PRIV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    val signingPrivate: ByteArray?
        get() = prefs.getString(K_SIGN_PRIV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    /** Host this identity is registered on (e.g. an org island), or null
     *  for the default public server. Set at registration; the account +
     *  keys are bound to this server. */
    val serverHost: String?
        get() = prefs.getString(K_SERVER, null)

    /** Persists a complete identity in one transaction. The iOS client
     *  notes that the JWT and private keys must be written together —
     *  losing either before persistence makes the account unreachable
     *  (no password, no recovery). */
    fun saveIdentity(
        uin: Int,
        token: String,
        nickname: String,
        identityPrivate: ByteArray,
        signingPrivate: ByteArray,
        serverHost: String? = null,
    ) {
        prefs.edit()
            .putInt(K_UIN, uin)
            .putString(K_TOKEN, token)
            .putString(K_NICK, nickname)
            .putString(K_ID_PRIV, Base64.encodeToString(identityPrivate, Base64.NO_WRAP))
            .putString(K_SIGN_PRIV, Base64.encodeToString(signingPrivate, Base64.NO_WRAP))
            .putString(K_SERVER, serverHost)
            .apply()
    }

    /** Update just the cached display nickname (after a profile edit). */
    fun updateNickname(nickname: String) {
        prefs.edit().putString(K_NICK, nickname).apply()
    }

    fun wipe() = prefs.edit().clear().apply()

    private companion object {
        const val FILE = "rcq.identity.v1"
        const val K_UIN = "uin"
        const val K_TOKEN = "token"
        const val K_NICK = "nickname"
        const val K_ID_PRIV = "identity_private"
        const val K_SIGN_PRIV = "signing_private"
        const val K_SERVER = "server_host"
    }
}
