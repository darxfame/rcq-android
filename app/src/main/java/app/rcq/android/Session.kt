package app.rcq.android

import android.content.Context
import android.util.Base64
import app.rcq.android.crypto.Envelope
import app.rcq.android.crypto.IdentityKeys
import app.rcq.android.crypto.SealedSender
import app.rcq.android.data.MessageDb
import app.rcq.android.data.SecureStore
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.Contact
import app.rcq.android.model.PendingRequest
import app.rcq.android.net.RcqApi
import app.rcq.android.net.RcqSocket
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters

/**
 * The app's single coordinator: identity, REST, WebSocket, encrypted
 * storage, local message DB, and crypto. Exposes observable state
 * (StateFlows) the Compose UI collects. Models the iOS client's
 * AppState/MessageService/ContactService roles, scoped to 1:1 text.
 */
class Session(context: Context) {
    private val store = SecureStore(context.applicationContext)
    private val api = RcqApi()
    private val socket = RcqSocket()
    private val db = MessageDb(context.applicationContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _pending = MutableStateFlow<List<PendingRequest>>(emptyList())
    val pending: StateFlow<List<PendingRequest>> = _pending.asStateFlow()

    private val _messages = MutableStateFlow<Map<Int, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<Int, List<ChatMessage>>> = _messages.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    // uin -> recipient X25519 identity public (raw), from contacts or lookup.
    private val peerIdentityCache = HashMap<Int, ByteArray>()

    @Volatile
    private var started = false

    init {
        if (store.isRegistered) {
            api.setToken(store.token)
        }
    }

    val isRegistered: Boolean get() = store.isRegistered
    val uin: Int? get() = store.uin

    /** Mint identity, register, persist, then bring the session online. */
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
        start()
        return resp.uin
    }

    /** Load local history, open the WebSocket, drain the offline queue,
     *  and refresh the contact graph. Idempotent enough to call on every
     *  app launch when already registered. */
    fun start() {
        if (started) return
        val uin = store.uin ?: return
        val token = store.token ?: return
        started = true
        loadMessagesFromDb()
        socket.connect(
            uin = uin,
            token = token,
            onEvent = ::handleEvent,
            onState = { _connected.value = it },
        )
        scope.launch { runCatching { drainQueue() } }
        scope.launch { runCatching { refreshContacts() } }
        scope.launch { runCatching { refreshPending() } }
    }

    fun stop() = socket.disconnect()

    // ── messaging ────────────────────────────────────────────────────

    suspend fun sendText(toUin: Int, text: String) {
        val env = Envelope.text(text)
        // Persist + show immediately; failure to send leaves it in history
        // (no delivery-state tracking yet — a later refinement).
        store(ChatMessage(env.id, toUin, fromMe = true, body = text, sentAt = System.currentTimeMillis()))
        val payload = SealedSender.encryptV1(
            envelope = env,
            recipientIdentityPub = recipientKey(toUin),
            ownUin = store.uin ?: error("not registered"),
            signingPriv = signingPriv(),
            signingPub = signingPub(),
        )
        api.sendSealed(toUin, payload)
    }

    private suspend fun recipientKey(uin: Int): ByteArray {
        peerIdentityCache[uin]?.let { return it }
        val keyB64 = _contacts.value.firstOrNull { it.uin == uin }?.identityKey
            ?: api.userInfo(uin).identity_key
            ?: throw IllegalStateException("peer has no identity key")
        return Base64.decode(keyB64, Base64.NO_WRAP).also { peerIdentityCache[uin] = it }
    }

    private fun ingest(payloadB64: String) {
        runCatching {
            val dec = SealedSender.decryptV1(payloadB64, identityPriv(), identityPub())
            val env = dec.envelope
            if (env is Envelope.Text) {
                store(ChatMessage(env.id, dec.senderUin, fromMe = false, body = env.text, sentAt = System.currentTimeMillis()))
            }
        }
    }

    private suspend fun drainQueue() {
        api.drainQueue().forEach { q -> q.payload?.let(::ingest) }
    }

    // ── contacts ─────────────────────────────────────────────────────

    suspend fun addContact(uin: Int) {
        api.requestContact(uin)
        runCatching { refreshContacts() }
        runCatching { refreshPending() }
    }

    suspend fun respond(requestId: Int, accept: Boolean) {
        api.respondContact(requestId, accept)
        runCatching { refreshContacts() }
        runCatching { refreshPending() }
    }

    private suspend fun refreshContacts() {
        _contacts.value = api.contacts().map {
            Contact(it.uin, it.nickname ?: "#${it.uin}", it.identity_key ?: "", it.signing_key, it.status)
        }
        // Seed the identity cache so sends to contacts skip a lookup.
        _contacts.value.forEach { c ->
            if (c.identityKey.isNotEmpty()) {
                peerIdentityCache[c.uin] = Base64.decode(c.identityKey, Base64.NO_WRAP)
            }
        }
    }

    private suspend fun refreshPending() {
        _pending.value = api.pending().map {
            PendingRequest(it.id, it.from_uin, it.nickname ?: "#${it.from_uin}")
        }
    }

    fun contactName(uin: Int): String =
        _contacts.value.firstOrNull { it.uin == uin }?.nickname ?: "#$uin"

    // ── WS events ────────────────────────────────────────────────────

    private fun handleEvent(type: String, obj: JsonObject) {
        when (type) {
            "message", "system" -> obj.get("payload")?.asString?.let(::ingest)
            "contact_request", "contact_response", "contact_removed" -> {
                scope.launch { runCatching { refreshContacts() }; runCatching { refreshPending() } }
            }
            else -> Unit // typing/presence/etc. ignored for now
        }
    }

    // ── persistence + flow updates ───────────────────────────────────

    private fun loadMessagesFromDb() {
        _messages.value = db.all().groupBy { it.peerUin }
    }

    private fun store(msg: ChatMessage) {
        // INSERT OR IGNORE dedups by envelope UUID (WS vs queue overlap).
        if (!db.insert(msg)) return
        val cur = _messages.value.toMutableMap()
        cur[msg.peerUin] = ((cur[msg.peerUin] ?: emptyList()) + msg).sortedBy { it.sentAt }
        _messages.value = cur
    }

    // ── own key material (derived from stored privates) ──────────────

    private fun identityPriv(): ByteArray = store.identityPrivate ?: error("no identity key")
    private fun identityPub(): ByteArray = X25519PrivateKeyParameters(identityPriv(), 0).generatePublicKey().encoded
    private fun signingPriv(): ByteArray = store.signingPrivate ?: error("no signing key")
    private fun signingPub(): ByteArray = Ed25519PrivateKeyParameters(signingPriv(), 0).generatePublicKey().encoded
}
