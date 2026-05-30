package app.rcq.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for one local identity — the Android analogue
 * of the iOS Keychain. Holds the UIN, the JWT, the display nickname, the
 * two private keys (X25519 identity, Ed25519 signing), and the server the
 * identity lives on. The server never sees the private halves.
 *
 * Multi-account: every key is namespaced by the owning [Account.id] prefix,
 * so a single encrypted file backs the whole roster. A pre-multi-account
 * install wrote its identity under bare (unprefixed) keys — [AccountManager]
 * calls [migrateLegacyToAccount] once to lift those under Account[0]'s
 * prefix. Static peek/wipe helpers let the switcher read another account's
 * label and let a delete wipe one account without touching the others.
 */
class SecureStore(context: Context, accountId: String) {

    private val prefs: SharedPreferences = openPrefs(context)
    private val p = "$accountId."

    val isRegistered: Boolean
        get() = uin != null && !token.isNullOrEmpty()

    val uin: Int?
        get() = if (prefs.contains(p + K_UIN)) prefs.getInt(p + K_UIN, 0) else null

    val token: String?
        get() = prefs.getString(p + K_TOKEN, null)

    val nickname: String?
        get() = prefs.getString(p + K_NICK, null)

    val identityPrivate: ByteArray?
        get() = prefs.getString(p + K_ID_PRIV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    val signingPrivate: ByteArray?
        get() = prefs.getString(p + K_SIGN_PRIV, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    /** Host this identity is registered on, or null for the default public
     *  server. Set at registration; the account + keys are bound to it. */
    val serverHost: String?
        get() = prefs.getString(p + K_SERVER, null)

    /** Persists a complete identity in one transaction. The JWT and private
     *  keys must be written together — losing either before persistence
     *  makes the account unreachable (no password, no recovery). */
    fun saveIdentity(
        uin: Int,
        token: String,
        nickname: String,
        identityPrivate: ByteArray,
        signingPrivate: ByteArray,
        serverHost: String? = null,
    ) {
        prefs.edit()
            .putInt(p + K_UIN, uin)
            .putString(p + K_TOKEN, token)
            .putString(p + K_NICK, nickname)
            .putString(p + K_ID_PRIV, Base64.encodeToString(identityPrivate, Base64.NO_WRAP))
            .putString(p + K_SIGN_PRIV, Base64.encodeToString(signingPrivate, Base64.NO_WRAP))
            .putString(p + K_SERVER, serverHost)
            .apply()
    }

    /** Update just the cached display nickname (after a profile edit). */
    fun updateNickname(nickname: String) {
        prefs.edit().putString(p + K_NICK, nickname).apply()
    }

    /** Repoint this identity to a new UIN + token after a migration. Keeps
     *  the keys, nickname and server (the keys are reused server-side). */
    fun updateAccount(uin: Int, token: String) {
        prefs.edit().putInt(p + K_UIN, uin).putString(p + K_TOKEN, token).apply()
    }

    /** Wipe just this account's slots (the shared file's other accounts
     *  stay intact). */
    fun wipe() = wipeKeys(prefs, p)

    companion object {
        private const val FILE = "rcq.identity.v1"
        private const val K_UIN = "uin"
        private const val K_TOKEN = "token"
        private const val K_NICK = "nickname"
        private const val K_ID_PRIV = "identity_private"
        private const val K_SIGN_PRIV = "signing_private"
        private const val K_SERVER = "server_host"
        private val STRING_KEYS = listOf(K_TOKEN, K_NICK, K_ID_PRIV, K_SIGN_PRIV, K_SERVER)

        private fun openPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context.applicationContext,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private fun wipeKeys(prefs: SharedPreferences, prefix: String) {
            val e = prefs.edit()
            (STRING_KEYS + K_UIN).forEach { e.remove(prefix + it) }
            e.apply()
        }

        /** True if a pre-multi-account install left an unprefixed identity. */
        fun hasLegacyIdentity(context: Context): Boolean = openPrefs(context).contains(K_UIN)

        fun peekLegacyServerHost(context: Context): String? =
            openPrefs(context).getString(K_SERVER, null)

        /** Lift the legacy unprefixed identity under [accountId]'s prefix,
         *  then drop the legacy keys. Idempotent (no-op without a legacy UIN). */
        fun migrateLegacyToAccount(context: Context, accountId: String) {
            val prefs = openPrefs(context)
            if (!prefs.contains(K_UIN)) return
            val p = "$accountId."
            val e = prefs.edit()
            e.putInt(p + K_UIN, prefs.getInt(K_UIN, 0))
            STRING_KEYS.forEach { k -> prefs.getString(k, null)?.let { e.putString(p + k, it) } }
            e.remove(K_UIN)
            STRING_KEYS.forEach { e.remove(it) }
            e.apply()
        }

        /** Remove every slot for [accountId] (local account delete / burn). */
        fun wipeAccount(context: Context, accountId: String) = wipeKeys(openPrefs(context), "$accountId.")

        /** Read another account's UIN without making it active (for the
         *  switcher / manage list). */
        fun peekUin(context: Context, accountId: String): Int? {
            val prefs = openPrefs(context)
            val key = "$accountId.$K_UIN"
            return if (prefs.contains(key)) prefs.getInt(key, 0) else null
        }

        /** Read another account's nickname without making it active. */
        fun peekNickname(context: Context, accountId: String): String? =
            openPrefs(context).getString("$accountId.$K_NICK", null)
    }
}
