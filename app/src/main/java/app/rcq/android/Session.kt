package app.rcq.android

import android.content.Context
import android.util.Base64
import app.rcq.android.crypto.IdentityKeys
import app.rcq.android.data.SecureStore
import app.rcq.android.net.RcqApi

/**
 * Ties together identity keygen, the REST client, and encrypted storage.
 * The single entry point the UI talks to for the account lifecycle.
 */
class Session(context: Context) {
    private val store = SecureStore(context.applicationContext)
    private val api = RcqApi()

    val isRegistered: Boolean get() = store.isRegistered
    val uin: Int? get() = store.uin
    val nickname: String? get() = store.nickname

    /**
     * Mint a fresh identity, register it on the server, and persist the
     * result. Generates X25519 + Ed25519 keypairs, sends the public halves
     * (base64) to `/auth/register`, and writes the UIN, JWT, and private
     * halves to encrypted storage in one go. Returns the allocated UIN.
     */
    suspend fun registerNewAccount(nickname: String): Int {
        val identity = IdentityKeys.generate()
        val resp = api.register(
            RcqApi.RegisterRequest(
                nickname = nickname,
                identity_key = Base64.encodeToString(identity.identityPublic, Base64.NO_WRAP),
                signing_key = Base64.encodeToString(identity.signingPublic, Base64.NO_WRAP),
            )
        )
        store.saveIdentity(
            uin = resp.uin,
            token = resp.token,
            nickname = nickname,
            identityPrivate = identity.identityPrivate,
            signingPrivate = identity.signingPrivate,
        )
        return resp.uin
    }
}
