package app.rcq.android

import android.content.Context
import android.util.Base64
import app.rcq.android.crypto.Envelope
import app.rcq.android.crypto.IdentityKeys
import app.rcq.android.crypto.MediaCrypto
import app.rcq.android.crypto.Reply
import app.rcq.android.crypto.SealedSender
import app.rcq.android.data.LocalStores
import app.rcq.android.data.MessageDb
import app.rcq.android.data.SecureStore
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.Contact
import app.rcq.android.model.DeliveryState
import app.rcq.android.model.GroupMember
import app.rcq.android.model.PendingRequest
import app.rcq.android.model.RcqGroup
import app.rcq.android.model.UserStatus
import app.rcq.android.net.RcqApi
import app.rcq.android.net.RcqSocket
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val gson = com.google.gson.Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _pending = MutableStateFlow<List<PendingRequest>>(emptyList())
    val pending: StateFlow<List<PendingRequest>> = _pending.asStateFlow()

    private val _messages = MutableStateFlow<Map<Int, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<Int, List<ChatMessage>>> = _messages.asStateFlow()

    private val _groups = MutableStateFlow<List<RcqGroup>>(emptyList())
    val groups: StateFlow<List<RcqGroup>> = _groups.asStateFlow()

    /** Group threads keyed by group id (separate from the 1:1 [messages]). */
    private val _groupMessages = MutableStateFlow<Map<Int, List<ChatMessage>>>(emptyMap())
    val groupMessages: StateFlow<Map<Int, List<ChatMessage>>> = _groupMessages.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _typingFrom = MutableStateFlow<Int?>(null)
    val typingFrom: StateFlow<Int?> = _typingFrom.asStateFlow()
    private var typingSeq = 0

    /** Own presence status, reflected in the header status picker. */
    private val _status = MutableStateFlow(UserStatus.ONLINE)
    val status: StateFlow<UserStatus> = _status.asStateFlow()

    val nickname: String get() = store.nickname ?: "—"

    // uin -> recipient X25519 identity public (raw), from contacts or lookup.
    private val peerIdentityCache = HashMap<Int, ByteArray>()
    // media_id -> decrypted plaintext bytes (sender seeds it; receiver caches).
    private val imageCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()

    @Volatile
    private var started = false

    // True once the WebSocket has connected at least once; gates the
    // reconnect-driven graph resync so the initial launch doesn't double up.
    @Volatile
    private var everConnected = false

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
            onState = { up ->
                _connected.value = up
                if (up) {
                    // A reconnect (after an offline gap) re-pulls the graph so
                    // a roster that failed to load earlier recovers without a
                    // cold start. The first connect is skipped — start() below
                    // already kicked the initial load.
                    if (everConnected) syncGraph()
                    everConnected = true
                }
            },
        )
        syncGraph()
    }

    /** Pull the contact graph + offline queue, each retried and
     *  soft-failing independently. A transient failure at launch used to
     *  strand the UI with an empty roster until the next cold start; the
     *  retry (and the reconnect-driven re-call) make it recover on its own. */
    private fun syncGraph() {
        scope.launch { runCatching { withRetry { drainQueue() } } }
        scope.launch { runCatching { withRetry { refreshContacts() } } }
        scope.launch { runCatching { withRetry { refreshPending() } } }
        scope.launch { runCatching { withRetry { refreshGroups() } } }
    }

    fun stop() = socket.disconnect()

    /** Wipe local message history (both 1:1 and group threads) without
     *  touching the account. Mirrors iOS "Clear history". */
    fun clearHistory() {
        db.wipe()
        _messages.value = emptyMap()
        _groupMessages.value = emptyMap()
    }

    /** Publish own presence status. Optimistic local update, soft-fail
     *  on the network call. */
    suspend fun setStatus(status: UserStatus) {
        _status.value = status
        runCatching { api.setStatus(status.wire) }
    }

    // ── contact moderation ───────────────────────────────────────────

    /** Toggle block server-side, then refresh the roster. */
    suspend fun toggleBlock(uin: Int) {
        runCatching { api.blockContact(uin) }
        runCatching { refreshContacts() }
    }

    /** Mutual remove + local silent-drop of future sealed messages. */
    suspend fun removeContact(uin: Int) {
        LocalStores.addRemoved(uin)
        runCatching { api.removeContact(uin) }
        runCatching { refreshContacts() }
    }

    suspend fun report(uin: Int, reason: String) {
        runCatching { api.report(uin, reason) }
    }

    // ── groups ───────────────────────────────────────────────────────

    private fun mapGroup(g: RcqApi.GroupOut): RcqGroup = RcqGroup(
        id = g.id,
        name = g.name ?: "Group ${g.id}",
        description = g.description,
        ownerUin = g.owner_uin,
        postPolicy = g.post_policy ?: "all",
        isClosed = g.is_closed,
        membersHidden = g.members_hidden,
        pinnedText = g.pinned_text,
        avatarMediaId = g.avatar_media_id,
        avatarMediaKey = g.avatar_media_key,
        members = g.members.map {
            GroupMember(
                uin = it.uin,
                nickname = it.nickname ?: "#${it.uin}",
                role = it.role ?: "member",
                status = it.status,
                identityKey = it.identity_key ?: "",
                signingKey = it.signing_key,
            )
        },
        createdAt = parseIso(g.created_at),
    )

    private suspend fun refreshGroups() {
        _groups.value = api.groups().map(::mapGroup).sortedByDescending { it.createdAt ?: 0L }
    }

    /** Upsert a group from a WS event. If the embedded roster no longer
     *  contains us (we left / were removed), drop it locally instead —
     *  mirrors the iOS GroupService.upsert rule. */
    private fun upsertGroup(g: RcqGroup) {
        val me = store.uin
        // Self-removal rule: drop a group we're no longer a member of. Guard
        // on a non-empty roster so a partial/empty WS payload (e.g. the
        // server echoing group_created back to the creator) can't nuke a
        // group we just created.
        if (me != null && g.members.isNotEmpty() && g.members.none { it.uin == me }) {
            _groups.value = _groups.value.filterNot { it.id == g.id }
            return
        }
        _groups.value = (_groups.value.filterNot { it.id == g.id } + g)
            .sortedByDescending { it.createdAt ?: 0L }
    }

    fun group(id: Int): RcqGroup? = _groups.value.firstOrNull { it.id == id }
    fun groupName(id: Int): String = group(id)?.name ?: "Group $id"

    suspend fun createGroup(name: String, memberUins: List<Int>): RcqGroup {
        val g = mapGroup(api.createGroup(name, memberUins))
        upsertGroup(g)
        return g
    }

    suspend fun addGroupMember(id: Int, uin: Int) {
        runCatching { upsertGroup(mapGroup(api.addGroupMember(id, uin))) }
    }

    suspend fun leaveGroup(id: Int) {
        val me = store.uin ?: return
        runCatching { api.leaveGroup(id, me) }
        _groups.value = _groups.value.filterNot { it.id == id }
    }

    suspend fun deleteGroup(id: Int) {
        runCatching { api.deleteGroup(id) }
        _groups.value = _groups.value.filterNot { it.id == id }
    }

    /** Owner/admin: compress + encrypt + upload an avatar blob, then PATCH
     *  the group with the new media id + per-blob key. Throws on failure
     *  so the caller can surface it. */
    suspend fun setGroupAvatar(id: Int, jpeg: ByteArray) {
        val key = MediaCrypto.newKey()
        val blob = MediaCrypto.seal(jpeg, key)
        val keyB64 = Base64.encodeToString(key, Base64.NO_WRAP)
        val upload = api.uploadBlob(blob)
        imageCache[upload.media_id] = jpeg
        upsertGroup(mapGroup(api.patchGroup(id, RcqApi.GroupPatchBody(avatar_media_id = upload.media_id, avatar_media_key = keyB64))))
    }

    /** Owner/admin: rename / re-describe / re-pin a group. */
    suspend fun patchGroup(id: Int, name: String? = null, description: String? = null, pinnedText: String? = null) {
        upsertGroup(mapGroup(api.patchGroup(id, RcqApi.GroupPatchBody(name = name, description = description, pinned_text = pinnedText))))
    }

    // ── own profile + privacy ────────────────────────────────────────

    suspend fun loadProfile(): RcqApi.MeProfile? {
        val me = store.uin ?: return null
        return runCatching { api.getMe(me) }.getOrNull()
    }

    suspend fun updateProfile(body: RcqApi.UpdateMeBody): RcqApi.MeProfile? {
        val updated = runCatching { api.updateMe(body) }.getOrNull()
        // Reflect a nickname change locally (the header reads store.nickname).
        if (updated != null && !body.nickname.isNullOrBlank()) store.updateNickname(body.nickname)
        return updated
    }

    /** Contacts the user has blocked (for the Blocked Users settings screen). */
    fun blockedContacts(): List<Contact> = _contacts.value.filter { it.blocked }

    suspend fun sendGroupText(groupId: Int, text: String, replyTo: app.rcq.android.crypto.Reply? = null) {
        val env = Envelope.text(text, replyTo)
        sendGroupEnvelope(groupId, env, env.id, text, kind = "text", replyTo = replyTo)
    }

    suspend fun sendGroupPhoto(groupId: Int, jpeg: ByteArray, caption: String?) {
        val key = MediaCrypto.newKey()
        val blob = MediaCrypto.seal(jpeg, key)
        val keyB64 = Base64.encodeToString(key, Base64.NO_WRAP)
        val upload = api.uploadBlob(blob)
        imageCache[upload.media_id] = jpeg
        val env = Envelope.photo(upload.media_id, keyB64, caption)
        sendGroupEnvelope(groupId, env, env.id, caption ?: "", kind = "photo", mediaId = upload.media_id, mediaKey = keyB64)
    }

    /** Encrypt the envelope once per member (skipping self) and fan out in
     *  a single /messages/group-sealed POST. No group key — each blob is a
     *  v=1 sealed envelope, identical to 1:1 (rcq-spec 6.4). */
    private suspend fun sendGroupEnvelope(
        groupId: Int,
        env: Envelope,
        id: String,
        body: String,
        kind: String,
        mediaId: String? = null,
        mediaKey: String? = null,
        replyTo: app.rcq.android.crypto.Reply? = null,
    ) {
        val me = store.uin ?: return
        storeGroup(
            ChatMessage(
                id = id, peerUin = 0, fromMe = true, body = body,
                sentAt = System.currentTimeMillis(), state = DeliveryState.SENDING,
                kind = kind, mediaId = mediaId, mediaKey = mediaKey,
                replyToSnippet = replyTo?.snippet, replyToAuthor = replyTo?.authorName,
                groupId = groupId, senderUin = me,
            )
        )
        fanOutGroup(groupId, env, id)
    }

    /** Encrypt [env] once per member (skipping self) and POST the fan-out;
     *  flips the local bubble's delivery state. Shared by send + resend. */
    private suspend fun fanOutGroup(groupId: Int, env: Envelope, id: String) {
        val me = store.uin ?: return
        val group = group(groupId) ?: return
        try {
            val resp = withRetry {
                val payloads = group.members
                    .filter { it.uin != me && it.identityKey.isNotEmpty() }
                    .map { m ->
                        RcqApi.GroupPayload(
                            to_uin = m.uin,
                            payload = SealedSender.encryptV1(
                                envelope = env,
                                recipientIdentityPub = Base64.decode(m.identityKey, Base64.NO_WRAP),
                                ownUin = me,
                                signingPriv = signingPriv(),
                                signingPub = signingPub(),
                            ),
                        )
                    }
                if (payloads.isEmpty()) {
                    // Lone member (everyone else left) — nothing to send, treat as sent.
                    RcqApi.SendResponse(delivered = false)
                } else {
                    api.sendGroupSealed(groupId, payloads)
                }
            }
            updateGroupMsgState(groupId, id, if (resp.delivered) DeliveryState.DELIVERED else DeliveryState.SENT)
        } catch (e: Exception) {
            updateGroupMsgState(groupId, id, DeliveryState.FAILED)
        }
    }

    /** Decrypt + store an inbound group message under its group thread. */
    private fun ingestGroup(payloadB64: String, groupId: Int) {
        runCatching {
            val dec = SealedSender.decryptV1(payloadB64, identityPriv(), identityPub())
            val now = System.currentTimeMillis()
            when (val env = dec.envelope) {
                is Envelope.Text -> storeGroup(
                    ChatMessage(env.id, 0, false, env.text, now, kind = "text", groupId = groupId, senderUin = dec.senderUin, replyToSnippet = env.replyTo?.snippet, replyToAuthor = env.replyTo?.authorName)
                )
                is Envelope.Photo -> storeGroup(
                    ChatMessage(env.id, 0, false, env.caption ?: "", now, kind = "photo", mediaId = env.mediaId, mediaKey = env.mediaKey, groupId = groupId, senderUin = dec.senderUin)
                )
                is Envelope.Reaction -> env.asset?.let { addGroupReaction(groupId, env.targetId, it) }
                is Envelope.Unknown -> Unit
            }
        }
    }

    /** Irreversible account burn: wipe server-side, then everything local
     *  (identity keychain + message DB + in-memory flows). After this the
     *  app is back to a fresh-install state. */
    suspend fun burnAccount() {
        runCatching { api.deleteAccount() }
        socket.disconnect()
        store.wipe()
        db.wipe()
        peerIdentityCache.clear()
        _contacts.value = emptyList()
        _pending.value = emptyList()
        _messages.value = emptyMap()
        _groups.value = emptyList()
        _groupMessages.value = emptyMap()
        _typingFrom.value = null
        started = false
    }

    // ── messaging ────────────────────────────────────────────────────

    suspend fun sendText(toUin: Int, text: String, replyTo: Reply? = null) {
        val env = Envelope.text(text, replyTo)
        store(ChatMessage(env.id, toUin, fromMe = true, body = text, sentAt = System.currentTimeMillis(), state = DeliveryState.SENDING, replyToSnippet = replyTo?.snippet, replyToAuthor = replyTo?.authorName))
        sendEnvelope(env, env.id, toUin)
    }

    /** Local-only delete (removes from this device; no wire message). */
    fun deleteLocal(msg: ChatMessage) {
        db.delete(msg.id)
        if (msg.groupId != null) {
            val cur = _groupMessages.value.toMutableMap()
            cur[msg.groupId] = (cur[msg.groupId] ?: emptyList()).filterNot { it.id == msg.id }
            _groupMessages.value = cur
        } else {
            val cur = _messages.value.toMutableMap()
            cur[msg.peerUin] = (cur[msg.peerUin] ?: emptyList()).filterNot { it.id == msg.id }
            _messages.value = cur
        }
    }

    /** Encrypt+upload an already-compressed JPEG, then send a photo
     *  envelope carrying the media id + per-blob key (rcq-spec 9). The
     *  local bubble appears once the blob is uploaded. */
    suspend fun sendPhoto(toUin: Int, jpeg: ByteArray, caption: String?) {
        val key = MediaCrypto.newKey()
        val blob = MediaCrypto.seal(jpeg, key)
        val keyB64 = Base64.encodeToString(key, Base64.NO_WRAP)
        val upload = api.uploadBlob(blob)            // throws on failure (caller catches)
        imageCache[upload.media_id] = jpeg            // own bubble renders without re-download
        val env = Envelope.photo(upload.media_id, keyB64, caption)
        store(ChatMessage(env.id, toUin, true, caption ?: "", System.currentTimeMillis(), DeliveryState.SENDING, kind = "photo", mediaId = upload.media_id, mediaKey = keyB64))
        sendEnvelope(env, env.id, toUin)
    }

    /** Retry a previously-failed outgoing message (same UUID, so no dup). */
    suspend fun resend(msg: ChatMessage) {
        if (!msg.fromMe || msg.state != DeliveryState.FAILED) return
        if (msg.groupId != null) {
            val env: Envelope = if (msg.kind == "photo" && msg.mediaId != null && msg.mediaKey != null)
                Envelope.Photo(msg.id, msg.mediaId, msg.mediaKey, msg.body.ifEmpty { null })
            else Envelope.Text(msg.id, msg.body)
            updateGroupMsgState(msg.groupId, msg.id, DeliveryState.SENDING)
            fanOutGroup(msg.groupId, env, msg.id)
            return
        }
        updateMessageState(msg.id, msg.peerUin, DeliveryState.SENDING)
        val env: Envelope = if (msg.kind == "photo" && msg.mediaId != null && msg.mediaKey != null) {
            Envelope.Photo(msg.id, msg.mediaId, msg.mediaKey, msg.body.ifEmpty { null })
        } else {
            Envelope.Text(msg.id, msg.body)
        }
        sendEnvelope(env, msg.id, msg.peerUin)
    }

    private suspend fun sendEnvelope(env: Envelope, id: String, toUin: Int) {
        try {
            val resp = withRetry {
                val payload = SealedSender.encryptV1(
                    envelope = env,
                    recipientIdentityPub = recipientKey(toUin),
                    ownUin = store.uin ?: error("not registered"),
                    signingPriv = signingPriv(),
                    signingPub = signingPub(),
                )
                api.sendSealed(toUin, payload)
            }
            updateMessageState(id, toUin, if (resp.delivered) DeliveryState.DELIVERED else DeliveryState.SENT)
        } catch (e: Exception) {
            updateMessageState(id, toUin, DeliveryState.FAILED)
        }
    }

    /**
     * Network calls fail *transiently* far more often than they fail for
     * real: a stale keep-alive socket the server already closed (the
     * classic "first POST after idle resets, the very next one works"), a
     * DNS blip, a momentary 5xx from a backend worker. A single attempt
     * then giving up is why a message "sometimes needs a manual
     * tap-to-retry" — and why the contact roster sometimes comes up empty
     * until the next cold start. So retry automatically: a few quick
     * attempts with backoff. For sends this is duplicate-safe — the
     * envelope UUID is stable across attempts, so the recipient's
     * INSERT-OR-IGNORE dedups any blob that landed before a lost response;
     * for idempotent GETs (roster, queue) a retry is free.
     */
    private suspend fun <T> withRetry(attempts: Int = 3, block: suspend () -> T): T {
        var last: Exception? = null
        repeat(attempts) { i ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                android.util.Log.w("RCQnet", "attempt ${i + 1}/$attempts failed: ${e.javaClass.simpleName}: ${e.message}")
                if (i < attempts - 1) delay(300L * (i + 1) * (i + 1)) // 300ms, then 1.2s
            }
        }
        throw last ?: IllegalStateException("request failed")
    }

    /** React to [target] with [emoji]: optimistic local add (deduped) then
     *  a sealed `reaction` envelope to the 1:1 peer or fanned out to the
     *  group. A reaction has no bubble or delivery state of its own, so it
     *  rides the best-effort control path, not [sendEnvelope]. */
    suspend fun sendReaction(target: ChatMessage, emoji: String) {
        val env = Envelope.reaction(target.id, emoji)
        if (target.groupId != null) {
            addGroupReaction(target.groupId, target.id, emoji)
            fanOutControl(target.groupId, env)
        } else {
            addPeerReaction(target.peerUin, target.id, emoji)
            sendControl(target.peerUin, env)
        }
    }

    /** Encrypt + send a control envelope (e.g. a reaction) to one peer.
     *  Reuses the send-retry but tracks no delivery state. */
    private suspend fun sendControl(toUin: Int, env: Envelope) {
        runCatching {
            withRetry {
                val payload = SealedSender.encryptV1(
                    envelope = env,
                    recipientIdentityPub = recipientKey(toUin),
                    ownUin = store.uin ?: error("not registered"),
                    signingPriv = signingPriv(),
                    signingPub = signingPub(),
                )
                api.sendSealed(toUin, payload)
            }
        }
    }

    /** Fan a control envelope out to every other group member, best effort. */
    private suspend fun fanOutControl(groupId: Int, env: Envelope) {
        val me = store.uin ?: return
        val group = group(groupId) ?: return
        runCatching {
            val payloads = group.members
                .filter { it.uin != me && it.identityKey.isNotEmpty() }
                .map { m ->
                    RcqApi.GroupPayload(
                        to_uin = m.uin,
                        payload = SealedSender.encryptV1(
                            envelope = env,
                            recipientIdentityPub = Base64.decode(m.identityKey, Base64.NO_WRAP),
                            ownUin = me,
                            signingPriv = signingPriv(),
                            signingPub = signingPub(),
                        ),
                    )
                }
            if (payloads.isNotEmpty()) withRetry { api.sendGroupSealed(groupId, payloads) }
        }
    }

    /** Download + decrypt a media blob, cached by media id. */
    suspend fun fetchImage(mediaId: String, mediaKey: String): ByteArray? {
        imageCache[mediaId]?.let { return it }
        return runCatching {
            val blob = api.getBlob(mediaId)
            val key = Base64.decode(mediaKey, Base64.NO_WRAP)
            MediaCrypto.open(blob, key).also { imageCache[mediaId] = it }
        }.getOrNull()
    }

    fun sendTyping(toUin: Int, active: Boolean) {
        socket.send("{\"type\":\"typing\",\"to_uin\":$toUin,\"active\":$active}")
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
            // Removed contacts are silently dropped — sealed sender means
            // the server can't filter by sender, so we gate on receipt.
            if (LocalStores.isRemoved(dec.senderUin)) return@runCatching
            val now = System.currentTimeMillis()
            when (val env = dec.envelope) {
                is Envelope.Text ->
                    store(ChatMessage(env.id, dec.senderUin, false, env.text, now, replyToSnippet = env.replyTo?.snippet, replyToAuthor = env.replyTo?.authorName))
                is Envelope.Photo ->
                    store(ChatMessage(env.id, dec.senderUin, false, env.caption ?: "", now, kind = "photo", mediaId = env.mediaId, mediaKey = env.mediaKey))
                is Envelope.Reaction -> env.asset?.let { addPeerReaction(dec.senderUin, env.targetId, it) }
                is Envelope.Unknown -> Unit
            }
        }
    }

    private suspend fun drainQueue() {
        api.drainQueue().forEach { q ->
            val payload = q.payload ?: return@forEach
            if (q.group_id != null) ingestGroup(payload, q.group_id) else ingest(payload)
        }
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
            Contact(
                uin = it.uin,
                nickname = it.nickname ?: "#${it.uin}",
                identityKey = it.identity_key ?: "",
                signingKey = it.signing_key,
                status = it.status,
                statusMessage = it.status_message,
                blocked = it.blocked,
                gender = it.gender,
                lastSeen = parseIso(it.last_seen),
            )
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

    fun contact(uin: Int): Contact? = _contacts.value.firstOrNull { it.uin == uin }

    /** Parse a server ISO-8601 timestamp (with or without timezone) to
     *  epoch millis. Pydantic emits naive UTC for `last_seen`; tolerate
     *  both forms. */
    private fun parseIso(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return runCatching { java.time.Instant.parse(s).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli() }
            .recoverCatching {
                java.time.LocalDateTime.parse(s).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            }
            .getOrNull()
    }

    // ── WS events ────────────────────────────────────────────────────

    private fun handleEvent(type: String, obj: JsonObject) {
        when (type) {
            "message", "system" -> {
                val payload = obj.get("payload")?.asString
                val gid = obj.get("group_id")?.takeIf { !it.isJsonNull }?.asInt
                if (payload != null) {
                    if (gid != null) ingestGroup(payload, gid) else ingest(payload)
                }
            }
            "group_created", "group_membership_changed" -> {
                obj.getAsJsonObject("group")?.let { gj ->
                    scope.launch {
                        runCatching { upsertGroup(mapGroup(gson.fromJson(gj, RcqApi.GroupOut::class.java))) }
                    }
                }
            }
            "group_deleted" -> {
                obj.get("group_id")?.asInt?.let { gid -> _groups.value = _groups.value.filterNot { it.id == gid } }
            }
            "contact_request", "contact_response", "contact_removed" -> {
                scope.launch { runCatching { refreshContacts() }; runCatching { refreshPending() } }
            }
            "typing" -> {
                val from = obj.get("from_uin")?.asInt
                val active = obj.get("active")?.asBoolean ?: false
                if (active && from != null) {
                    _typingFrom.value = from
                    val seq = ++typingSeq
                    scope.launch { delay(6000); if (typingSeq == seq) _typingFrom.value = null }
                } else {
                    _typingFrom.value = null
                }
            }
            "presence" -> scope.launch { runCatching { refreshContacts() } }
            else -> Unit
        }
    }

    // ── persistence + flow updates ───────────────────────────────────

    private fun loadMessagesFromDb() {
        val all = db.all()
        _messages.value = all.filter { it.groupId == null }.groupBy { it.peerUin }
        _groupMessages.value = all.filter { it.groupId != null }.groupBy { it.groupId!! }
    }

    private fun store(msg: ChatMessage) {
        // INSERT OR IGNORE dedups by envelope UUID (WS vs queue overlap).
        if (!db.insert(msg)) return
        val cur = _messages.value.toMutableMap()
        cur[msg.peerUin] = ((cur[msg.peerUin] ?: emptyList()) + msg).sortedBy { it.sentAt }
        _messages.value = cur
        bumpUnreadIfInbound(msg, LocalStores.peerThread(msg.peerUin))
    }

    /** Bump the unread badge for a genuinely-new inbound message, unless
     *  the user is currently looking at that thread. Own (fromMe)
     *  messages never count. */
    private fun bumpUnreadIfInbound(msg: ChatMessage, thread: String) {
        if (msg.fromMe) return
        if (thread == activeThread) { LocalStores.clearUnread(thread); return }
        LocalStores.bumpUnread(thread)
    }

    /** The thread the user currently has open (or null). Set by the UI so
     *  inbound messages to it don't raise a badge, and so a message that
     *  arrives while it's open is immediately marked read. */
    @Volatile
    var activeThread: String? = null
        private set

    fun openThread(thread: String) {
        activeThread = thread
        LocalStores.clearUnread(thread)
    }

    fun closeThread() {
        activeThread = null
    }

    private fun updateMessageState(id: String, peer: Int, state: DeliveryState) {
        db.updateState(id, state)
        val cur = _messages.value.toMutableMap()
        cur[peer] = (cur[peer] ?: emptyList()).map { if (it.id == id) it.copy(state = state) else it }
        _messages.value = cur
    }

    private fun storeGroup(msg: ChatMessage) {
        if (!db.insert(msg)) return
        val gid = msg.groupId ?: return
        val cur = _groupMessages.value.toMutableMap()
        cur[gid] = ((cur[gid] ?: emptyList()) + msg).sortedBy { it.sentAt }
        _groupMessages.value = cur
        bumpUnreadIfInbound(msg, LocalStores.groupThread(gid))
    }

    private fun updateGroupMsgState(groupId: Int, id: String, state: DeliveryState) {
        db.updateState(id, state)
        val cur = _groupMessages.value.toMutableMap()
        cur[groupId] = (cur[groupId] ?: emptyList()).map { if (it.id == id) it.copy(state = state) else it }
        _groupMessages.value = cur
    }

    /** Add [emoji] to a 1:1 message's reaction set (deduped), persisting +
     *  publishing the change. No-op if the message isn't in the thread or
     *  the emoji is already present. */
    private fun addPeerReaction(peer: Int, targetId: String, emoji: String) {
        val cur = _messages.value.toMutableMap()
        val list = cur[peer] ?: return
        var changed = false
        val updated = list.map { m ->
            if (m.id == targetId && !m.reactions.contains(emoji)) {
                changed = true
                val r = m.reactions + emoji
                db.updateReactions(targetId, r)
                m.copy(reactions = r)
            } else m
        }
        if (changed) { cur[peer] = updated; _messages.value = cur }
    }

    /** Group analogue of [addPeerReaction]. */
    private fun addGroupReaction(groupId: Int, targetId: String, emoji: String) {
        val cur = _groupMessages.value.toMutableMap()
        val list = cur[groupId] ?: return
        var changed = false
        val updated = list.map { m ->
            if (m.id == targetId && !m.reactions.contains(emoji)) {
                changed = true
                val r = m.reactions + emoji
                db.updateReactions(targetId, r)
                m.copy(reactions = r)
            } else m
        }
        if (changed) { cur[groupId] = updated; _groupMessages.value = cur }
    }

    // ── own key material (derived from stored privates) ──────────────

    private fun identityPriv(): ByteArray = store.identityPrivate ?: error("no identity key")
    private fun identityPub(): ByteArray = X25519PrivateKeyParameters(identityPriv(), 0).generatePublicKey().encoded
    private fun signingPriv(): ByteArray = store.signingPrivate ?: error("no signing key")
    private fun signingPub(): ByteArray = Ed25519PrivateKeyParameters(signingPriv(), 0).generatePublicKey().encoded
}
