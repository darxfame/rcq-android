package app.rcq.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import app.rcq.android.crypto.Envelope
import app.rcq.android.crypto.IdentityKeys
import app.rcq.android.crypto.SealedSender
import app.rcq.android.data.SecureStore
import app.rcq.android.net.RcqApi
import app.rcq.android.net.RcqSocket
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters

/**
 * Ties together identity keygen, the REST client, the WebSocket, and
 * encrypted storage — the single entry point the UI talks to for the
 * account lifecycle and 1:1 messaging.
 */
class Session(context: Context) {
    private val store = SecureStore(context.applicationContext)
    private val api = RcqApi()
    private val socket = RcqSocket()
    private val main = Handler(Looper.getMainLooper())

    // uin -> recipient X25519 identity public key (raw), cached after lookup.
    private val peerIdentityCache = HashMap<Int, ByteArray>()

    init {
        if (store.isRegistered) api.setToken(store.token)
    }

    val isRegistered: Boolean get() = store.isRegistered
    val uin: Int? get() = store.uin
    val nickname: String? get() = store.nickname

    /** Mint identity, register, persist. Returns the allocated UIN. */
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
        api.setToken(resp.token)
        return resp.uin
    }

    /** Encrypt [text] as a v=1 sealed envelope and POST it to [toUin]. */
    suspend fun sendText(toUin: Int, text: String) {
        val recipientPub = peerIdentityCache[toUin] ?: run {
            val info = api.userInfo(toUin)
            val keyB64 = info.identity_key ?: throw IllegalStateException("peer has no identity key")
            Base64.decode(keyB64, Base64.NO_WRAP).also { peerIdentityCache[toUin] = it }
        }
        val payload = SealedSender.encryptV1(
            envelope = Envelope.text(text),
            recipientIdentityPub = recipientPub,
            ownUin = store.uin ?: error("not registered"),
            signingPriv = signingPriv(),
            signingPub = signingPub(),
        )
        api.sendSealed(toUin, payload)
    }

    /** Open the WebSocket and decrypt inbound text envelopes. [onText] is
     *  always invoked on the main thread. */
    fun connect(onText: (senderUin: Int, text: String) -> Unit) {
        val uin = store.uin ?: return
        val token = store.token ?: return
        socket.connect(
            uin = uin,
            token = token,
            onPayload = { payloadB64 ->
                runCatching {
                    val dec = SealedSender.decryptV1(payloadB64, identityPriv(), identityPub())
                    val env = dec.envelope
                    if (env is Envelope.Text) {
                        main.post { onText(dec.senderUin, env.text) }
                    }
                }
            },
        )
    }

    fun disconnect() = socket.disconnect()

    // ── own key material (derived from stored privates) ──────────────

    private fun identityPriv(): ByteArray =
        store.identityPrivate ?: error("no identity key")

    private fun identityPub(): ByteArray =
        X25519PrivateKeyParameters(identityPriv(), 0).generatePublicKey().encoded

    private fun signingPriv(): ByteArray =
        store.signingPrivate ?: error("no signing key")

    private fun signingPub(): ByteArray =
        Ed25519PrivateKeyParameters(signingPriv(), 0).generatePublicKey().encoded
}
