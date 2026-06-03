package com.rcq.messenger.data.repository

import android.util.Log
import com.rcq.messenger.data.api.ContactRequest
import com.rcq.messenger.data.api.CreateChatRequest
import com.rcq.messenger.data.api.PresenceUpdateRequest
import com.rcq.messenger.data.api.RespondContactRequestBody
import com.rcq.messenger.data.api.SendContactRequestBody
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.api.GroupApiResponse
import com.rcq.messenger.data.db.*
import com.rcq.messenger.domain.model.*
import com.rcq.messenger.crypto.CryptoService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import com.rcq.messenger.data.websocket.ConnectionState
import com.rcq.messenger.data.websocket.WebSocketService
import com.rcq.messenger.data.websocket.WsEvent

@Singleton
class UserRepository @Inject constructor(
    private val api: RCQApiService,
    private val userDao: UserDao
) {
    suspend fun getCurrentUser(): Result<User> = runCatching {
        api.getCurrentUser().let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to get user")
        }
    }

    suspend fun getUser(userId: Long): Result<User> = runCatching {
        api.getUser(userId).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("User not found")
        }
    }

    suspend fun getUserByUin(uin: Long): Result<User> = runCatching {
        Log.d("UserRepository", "Searching UIN: $uin")
        api.getUserByUin(uin).let { response ->
            Log.d("UserRepository", "Response code: ${response.code()}")
            Log.d("UserRepository", "Response body: ${response.body()}")
            Log.d("UserRepository", "Response error: ${response.errorBody()?.string()}")
            if (response.isSuccessful) response.body()!!
            else throw Exception("User not found: ${response.code()}")
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        api.searchUsers(query).let { response ->
            if (response.isSuccessful) response.body()!!
            else emptyList()
        }
    }

    suspend fun updateProfile(user: User): Result<User> = runCatching {
        api.updateProfile(user).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to update profile")
        }
    }

    suspend fun updatePresence(status: String): Result<Unit> = runCatching {
        api.updatePresence(PresenceUpdateRequest(status)).let { response ->
            if (!response.isSuccessful) {
                throw Exception("Failed to update presence: ${response.code()}")
            }
        }
    }
}

@Singleton
class ContactRepository @Inject constructor(
    private val api: RCQApiService,
    private val contactDao: ContactDao
) {
    companion object {
        // Dev account: auto-added to contacts without request flow (mirrors iOS .Dev badge)
        const val DEV_UIN = 911L
        private const val LEGACY_DEV_UIN = 84048L
    }

    // Local cache for pending contact requests (from server sync)
    private val _pendingRequests = MutableStateFlow<List<ContactRequest>>(emptyList())
    val pendingRequests: StateFlow<List<ContactRequest>> = _pendingRequests.asStateFlow()

    fun getContacts(): Flow<List<Contact>> = contactDao.getContacts().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getBlockedContacts(): Flow<List<Contact>> = contactDao.getBlockedContacts().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun syncContacts(): Result<Unit> = runCatching {
        // Get contacts from server
        Log.d("ContactRepository", "Syncing contacts...")
        api.getContacts().let { response ->
            Log.d("ContactRepository", "getContacts response: ${response.code()}")
            if (response.isSuccessful) {
                val users = response.body()!!
                val first = users.firstOrNull()
                Log.d("ContactRepository", "Got ${users.size} contacts. First: ${first?.nickname} / uin=${first?.id}")
                // Clear and insert contacts (map from User to ContactEntity)
                contactDao.insertAll(users.map { it.toContactEntity() })
            } else {
                throw Exception("getContacts failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        }

        // Auto-add dev account without request flow (mirrors iOS .Dev behavior)
        removeLegacyDevFallback()
        ensureDevContact()

        // Pending requests are secondary metadata; they must not block visible contacts
        // or the auto-added .Dev contact when the endpoint is slow/degraded.
        val pendingSynced = withTimeoutOrNull(5_000L) {
            Log.d("ContactRepository", "Fetching pending requests...")
            api.getContactRequests().let { response ->
                Log.d("ContactRepository", "getContactRequests response: ${response.code()}")
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("ContactRepository", "Pending requests body: $body")
                    _pendingRequests.value = body ?: emptyList()
                } else {
                    Log.e("ContactRepository", "getContactRequests failed: ${response.errorBody()?.string()}")
                }
            }
            true
        } ?: false
        if (!pendingSynced) Log.w("ContactRepository", "getContactRequests timed out; keeping cached pending requests")
    }

    private suspend fun removeLegacyDevFallback() {
        val legacy = contactDao.getContactByUserId(LEGACY_DEV_UIN) ?: return
        if (legacy.nickname == ".Dev") {
            contactDao.deleteByUserId(LEGACY_DEV_UIN)
            Log.d("ContactRepository", "Removed legacy .Dev fallback ($LEGACY_DEV_UIN)")
        }
    }

    private suspend fun ensureDevContact() {
        if (contactDao.getContactByUserId(DEV_UIN) != null) return
        val insertedFromServer = runCatching {
            api.getUserByUin(DEV_UIN).let { resp ->
                if (resp.isSuccessful) {
                    resp.body()?.let { user ->
                        contactDao.insertContact(user.toContactEntity())
                        Log.d("ContactRepository", "Dev contact ($DEV_UIN) auto-added")
                        return@runCatching true
                    }
                } else {
                    Log.w("ContactRepository", "ensureDevContact lookup failed: ${resp.code()}")
                }
                false
            }
        }.onFailure { Log.w("ContactRepository", "ensureDevContact failed: ${it.message}") }
            .getOrDefault(false)

        if (!insertedFromServer && contactDao.getContactByUserId(DEV_UIN) == null) {
            contactDao.insertContact(
                ContactEntity(
                    userId = DEV_UIN,
                    nickname = ".Dev",
                    status = UserStatus.ONLINE.name
                )
            )
            Log.w("ContactRepository", "Dev contact ($DEV_UIN) inserted from local fallback")
        }
    }

    suspend fun getContactRequests(): Result<List<ContactRequest>> = runCatching {
        // First try from server
        Log.d("ContactRepository", "Fetching pending requests explicitly...")
        val response = api.getContactRequests()
        Log.d("ContactRepository", "getContactRequests (explicit) code: ${response.code()}")
        if (response.isSuccessful) {
            val requests = response.body() ?: emptyList()
            Log.d("ContactRepository", "Got ${requests.size} pending requests: $requests")
            // Update local cache
            _pendingRequests.value = requests
            requests
        } else {
            Log.e("ContactRepository", "getContactRequests (explicit) failed: ${response.errorBody()?.string()}")
            // Fall back to locally cached pending requests from sync
            _pendingRequests.value
        }
    }

    fun getLocalPendingRequests(): List<ContactRequest> = _pendingRequests.value

    suspend fun sendContactRequest(toUin: Long): Result<Unit> = runCatching {
        Log.d("ContactRepository", "Sending contact request to UIN: $toUin")
        api.sendContactRequest(SendContactRequestBody(toUin)).let { response ->
            Log.d("ContactRepository", "sendContactRequest response: ${response.code()}")
            if (!response.isSuccessful) {
                Log.e("ContactRepository", "sendContactRequest failed: ${response.errorBody()?.string()}")
                throw Exception("Failed to send contact request: ${response.code()}")
            }
        }
    }

    suspend fun acceptContactRequest(requestId: Long): Result<Unit> = runCatching {
        api.respondToContactRequest(RespondContactRequestBody(requestId, true)).let { response ->
            if (response.isSuccessful) {
                syncContacts()
            } else throw Exception("Failed to accept request")
        }
    }

    suspend fun declineContactRequest(requestId: Long): Result<Unit> = runCatching {
        api.respondToContactRequest(RespondContactRequestBody(requestId, false)).let { response ->
            if (response.isSuccessful) Unit
            else throw Exception("Failed to decline request")
        }
    }

    suspend fun addContact(userId: Long): Result<Unit> = runCatching {
        // Use send contact request instead of direct add
        api.sendContactRequest(SendContactRequestBody(userId)).let { response ->
            if (response.isSuccessful) Unit
            else throw Exception("Failed to add contact")
        }
    }

    suspend fun removeContact(userId: Long): Result<Unit> = runCatching {
        api.removeContact(userId).let { response ->
            if (response.isSuccessful) {
                contactDao.deleteByUserId(userId)
            } else throw Exception("Failed to remove contact")
        }
    }

    suspend fun blockContact(userId: Long): Result<Unit> = runCatching {
        api.blockContact(userId).let { response ->
            if (response.isSuccessful) syncContacts()
            else throw Exception("Failed to block contact")
        }
    }

    suspend fun unblockContact(userId: Long): Result<Unit> = runCatching {
        api.unblockContact(userId).let { response ->
            if (response.isSuccessful) syncContacts()
            else throw Exception("Failed to unblock contact")
        }
    }
}

@Singleton
class ChatRepository @Inject constructor(
    private val api: RCQApiService,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val groupDao: com.rcq.messenger.data.db.GroupDao,
    private val webSocketService: WebSocketService,
    private val cryptoService: CryptoService,
    private val notificationHelper: com.rcq.messenger.service.NotificationHelper,
    private val outboxDao: PendingOutboxDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _typingEvents = MutableSharedFlow<Pair<String, Long>>(extraBufferCapacity = 16)

    /** Emits true when someone is typing in [chatId], false when stopped. */
    fun typingState(chatId: String): Flow<Boolean> = _typingEvents
        .filter { (cid, _) -> cid == chatId }
        .map { (_, userId) -> userId != 0L }

    suspend fun clearUnreadCount(chatId: String) = chatDao.clearUnreadCount(chatId)
    suspend fun setPinned(chatId: String, pinned: Boolean) = chatDao.setPinned(chatId, pinned)
    suspend fun setMuted(chatId: String, muted: Boolean) = chatDao.setMuted(chatId, muted)
    suspend fun setArchived(chatId: String, archived: Boolean) = chatDao.setArchived(chatId, archived)
    suspend fun clearHistory(chatId: String) = messageDao.deleteChatMessages(chatId)

    suspend fun searchMessages(query: String): List<Message> =
        if (query.length < 2) emptyList()
        else messageDao.searchMessages(query).map { it.toDomain() }

    suspend fun searchInChat(chatId: String, query: String): List<Message> =
        if (query.length < 2) emptyList()
        else messageDao.searchInChat(chatId, query).map { it.toDomain() }
    @Volatile private var currentUserUin: Long = 0L

    fun setCurrentUserUin(uin: Long) { currentUserUin = uin }

    init {
        fetchOfflineQueue()
        webSocketService.connectionState
            .filter { it == ConnectionState.CONNECTED }
            .onEach { scope.launch { syncOfflineQueue() } }
            .launchIn(scope)
        webSocketService.events
            .onEach { event ->
                when (event) {
                    is WsEvent.ContactRequest -> {
                        notificationHelper.showContactRequestNotification(
                            fromUin = event.fromUin,
                            nickname = event.fromNickname
                        )
                    }
                    is WsEvent.ContactRemoved -> {
                        contactDao.deleteByUserId(event.peerUin)
                    }
                    is WsEvent.MessageNew -> {
                        try {
                            val obj = event.raw
                            val groupId = obj["group_id"]?.jsonPrimitive?.intOrNull

                            // payload is the only source of id, kind, sender — never the raw WS object
                            val rawPayload = obj["payload"]?.jsonPrimitive?.contentOrNull
                            if (rawPayload == null) {
                                Log.w("ChatRepository", "WS MessageNew: missing payload, dropping")
                                return@onEach
                            }
                            val decrypted = runCatching { cryptoService.decryptWrapped(rawPayload) }
                                .onFailure { e -> Log.w("ChatRepository", "decryptWrapped failed: ${e.message}") }
                                .getOrNull()

                            // TOFU — warn but do not drop (avoid losing messages due to key rotation)
                            if (decrypted?.signerPubKeyB64 != null) {
                                val stored = contactDao.getContactByUserId(decrypted.senderUin)?.signingKey
                                if (stored != null && stored != decrypted.signerPubKeyB64) {
                                    Log.w("ChatRepository", "ECIES signing key mismatch for ${decrypted.senderUin} — delivering anyway")
                                } else if (stored == null) {
                                    contactDao.updateSigningKey(decrypted.senderUin, decrypted.signerPubKeyB64)
                                }
                            }

                            // Extract sender even when decrypt fails so we can show a placeholder
                            val senderUin = decrypted?.senderUin
                                ?: obj["from"]?.jsonPrimitive?.longOrNull
                                ?: obj["sender_uin"]?.jsonPrimitive?.longOrNull
                                ?: obj["sender"]?.jsonPrimitive?.longOrNull
                                ?: 0L
                            if (senderUin == 0L && decrypted == null) {
                                Log.w("ChatRepository", "WS MessageNew: cannot identify sender — dropping")
                                return@onEach
                            }

                            // senderUin already derived above (handles decrypt==null case)
                            if (currentUserUin != 0L && senderUin == currentUserUin) return@onEach

                            val chatId = when {
                                groupId != null -> groupId.toString()
                                senderUin != 0L -> "direct_$senderUin"
                                else -> { Log.w("ChatRepository", "WS MessageNew: cannot determine chatId"); return@onEach }
                            }

                            val serverTimeStr = obj["server_time"]?.jsonPrimitive?.contentOrNull
                            val timestamp = serverTimeStr?.let {
                                runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
                            } ?: System.currentTimeMillis()

                            val msgId = decrypted?.messageId
                                ?: obj["message_id"]?.jsonPrimitive?.contentOrNull
                                ?: java.util.UUID.randomUUID().toString()
                            val msgKind = runCatching {
                                json.decodeFromString<com.rcq.messenger.domain.model.MessageKind>("\"${decrypted?.kind ?: "text"}\"")
                            }.getOrDefault(com.rcq.messenger.domain.model.MessageKind.TEXT)
                            val msgContent = decrypted?.content ?: "🔒 Зашифрованное сообщение"

                            val msg = com.rcq.messenger.domain.model.Message(
                                id = msgId,
                                chatId = chatId,
                                senderId = senderUin,
                                isFromMe = false,
                                kind = msgKind,
                                content = msgContent,
                                timestamp = timestamp
                            )
                            Log.d("ChatRepository", "WS ingest: id=${msg.id} chatId=$chatId kind=${decrypted?.kind} sender=$senderUin decrypted=${decrypted != null}")
                            messageDao.insertMessage(msg.toEntity())

                            // Upsert chat row + notification
                            val now = System.currentTimeMillis()
                            val existingChat = chatDao.getChat(chatId)
                            val senderName = existingChat?.targetNickname
                                ?: contactDao.getContactByUserId(senderUin)?.nickname
                                ?: senderUin.toString()
                            notificationHelper.showMessageNotification(
                                chatId = chatId,
                                senderName = senderName,
                                message = msg.content.ifBlank { "📎 Attachment" }
                            )
                            if (existingChat != null) {
                                chatDao.incrementUnreadCount(chatId, now)
                            } else {
                                chatDao.insertChat(ChatEntity(
                                    id = chatId,
                                    targetId = senderUin,
                                    targetNickname = senderName,
                                    targetAvatar = null,
                                    unreadCount = 1,
                                    isPinned = false,
                                    isMuted = false,
                                    isArchived = false,
                                    createdAt = now,
                                    updatedAt = now
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Failed to handle incoming message: ${e.message}")
                        }
                    }
                    is WsEvent.MessageDelivered -> {
                        messageDao.updateMessageStatus(event.messageId, "DELIVERED")
                    }
                    is WsEvent.MessageRead -> {
                        messageDao.updateMessageStatus(event.messageId, "READ")
                    }
                    is WsEvent.TypingStarted -> {
                        _typingEvents.tryEmit(event.chatId to event.userId)
                    }
                    is WsEvent.TypingStopped -> {
                        _typingEvents.tryEmit(event.chatId to 0L)
                    }
                    is WsEvent.MessageEdited -> {
                        messageDao.updateContent(event.messageId, event.content, System.currentTimeMillis())
                    }
                    is WsEvent.MessageDeleted -> {
                        messageDao.deleteMessage(event.messageId)
                    }
                    is WsEvent.MessageDeletedForEveryone -> {
                        messageDao.markDeletedForEveryone(event.messageId)
                    }
                    is WsEvent.MessageReaction -> {
                        if (event.reactions.isNotEmpty()) {
                            val reactionsJson = json.encodeToString(event.reactions)
                            messageDao.updateReactions(event.messageId, reactionsJson)
                        }
                    }
                    is WsEvent.PresenceOnline -> contactDao.updateStatus(event.uin, "ONLINE")
                    is WsEvent.PresenceAway -> contactDao.updateStatus(event.uin, "AWAY")
                    is WsEvent.PresenceDnd -> contactDao.updateStatus(event.uin, "DND")
                    is WsEvent.PresenceInvisible -> contactDao.updateStatus(event.uin, "INVISIBLE")
                    is WsEvent.PresenceOffline -> contactDao.updateStatus(event.uin, "OFFLINE")
                    is WsEvent.GroupUpdated -> {
                        scope.launch {
                            try {
                                val groupId = event.groupId
                                if (groupId.isNotEmpty()) {
                                    val response = api.getGroup(groupId)
                                    if (response.isSuccessful) {
                                        response.body()?.let { group ->
                                            groupDao.insertGroup(group.toGroupEntity())
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "GroupUpdated sync failed: ${e.message}")
                            }
                        }
                    }
                    is WsEvent.GroupDeleted -> {
                        scope.launch {
                            try {
                                groupDao.deleteGroup(event.groupId)
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "GroupDeleted handler failed: ${e.message}")
                            }
                        }
                    }
                    is WsEvent.Unknown -> {
                        if (event.type == "pong" || event.type == "opened") {
                            scope.launch { syncOfflineQueue() }
                        }
                    }
                    else -> Unit
                }
            }
            .launchIn(scope)
    }

    private fun fetchOfflineQueue() {
        scope.launch {
            syncOfflineQueue()
        }
    }

    private suspend fun syncOfflineQueue() {
        runCatching {
            val response = api.getMessageQueue()
            if (!response.isSuccessful) {
                Log.e("ChatRepository", "getMessageQueue failed: ${response.code()} ${response.errorBody()?.string()}")
                return@runCatching
            }
            val rows = response.body() ?: return@runCatching
            val directAckIds = mutableListOf<Int>()
            val groupAckIds = mutableListOf<Int>()
            for (row in rows) {
                val ingested = ingestQueueRow(row)
                if (ingested) {
                    if (row.groupId == null) directAckIds.add(row.id)
                    else groupAckIds.add(row.id)
                }
            }
            if (directAckIds.isNotEmpty() || groupAckIds.isNotEmpty()) {
                runCatching {
                    api.ackMessageQueue(
                        com.rcq.messenger.data.api.QueueAckBody(
                            directIds = directAckIds,
                            groupIds = groupAckIds
                        )
                    )
                }
            }
        }.onFailure { e ->
            Log.e("ChatRepository", "syncOfflineQueue exception: ${e.message}", e)
        }
    }

    private suspend fun ingestQueueRow(row: com.rcq.messenger.data.api.QueuedMessage): Boolean {
        return try {
            val decrypted = runCatching { cryptoService.decryptWrapped(row.payload) }
                .onFailure { e -> Log.w("ChatRepository", "decryptWrapped failed for queue row ${row.id}: ${e.message}") }
                .getOrNull()
            val senderId = decrypted?.senderUin
                ?: run {
                    Log.w("ChatRepository", "Cannot identify sender for row ${row.id}, skipping")
                    return false
                }
            if (currentUserUin != 0L && senderId == currentUserUin) return true
            val chatId = when {
                row.groupId != null -> row.groupId.toString()
                senderId != 0L -> "direct_$senderId"
                else -> return false
            }
            val timestamp = row.receivedAt?.let {
                runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull()
            } ?: System.currentTimeMillis()
            val msg = com.rcq.messenger.domain.model.Message(
                id = decrypted?.messageId ?: "queue_${row.id}",
                chatId = chatId,
                senderId = senderId,
                isFromMe = false,
                kind = runCatching {
                    json.decodeFromString<com.rcq.messenger.domain.model.MessageKind>("\"${decrypted?.kind ?: "text"}\"")
                }.getOrDefault(com.rcq.messenger.domain.model.MessageKind.TEXT),
                content = decrypted?.content ?: "🔒 Зашифрованное сообщение",
                timestamp = timestamp,
                receivedWhileAway = true
            )
            messageDao.insertMessage(msg.toEntity())
            val now = System.currentTimeMillis()
            val existingChat = chatDao.getChat(chatId)
            val senderName = existingChat?.targetNickname
                ?: contactDao.getContactByUserId(senderId)?.nickname
                ?: senderId.toString()
            if (existingChat != null) {
                chatDao.incrementUnreadCount(chatId, now)
            } else {
                chatDao.insertChat(com.rcq.messenger.domain.model.ChatEntity(
                    id = chatId,
                    targetId = senderId,
                    targetNickname = senderName,
                    targetAvatar = null,
                    unreadCount = 1,
                    isPinned = false,
                    isMuted = false,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                ))
            }
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "ingestQueueRow failed: ${e.message}")
            false
        }
    }

    suspend fun clearAllData() {
        chatDao.clearAll()
        messageDao.clearAll()
        contactDao.clearAll()
    }

    suspend fun getChat(chatId: String): Chat? {
        chatDao.getChat(chatId)?.let { return it.toDomain() }
        // For group chats, create a ChatEntity on first access so messages can be stored
        if (!chatId.startsWith("direct_")) {
            val group = groupDao.getGroup(chatId) ?: return null
            val now = System.currentTimeMillis()
            val entity = ChatEntity(
                id = chatId,
                targetId = group.creatorId,
                targetNickname = group.name,
                targetAvatar = group.avatarUrl,
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
            chatDao.insertChat(entity)
            return entity.toDomain()
        }
        return null
    }

    fun getChats(): Flow<List<Chat>> = chatDao.getChats().map { entities ->
        entities.map { it.toDomain() }
    }

    // Chats are a client-side concept only — server has no /chats endpoint.
    // They are materialized from Room rows created by outgoing sends,
    // WebSocket events, and /messages/queue ingestion.
    suspend fun syncChats(): Result<Unit> = runCatching {
        syncOfflineQueue()
    }

    // We derive the chatId from the target UIN and store locally.
    suspend fun createChat(targetId: Long): Result<Chat> = runCatching {
        chatDao.getChatByTargetId(targetId)?.toDomain() ?: run {
            val contact = contactDao.getContactByUserId(targetId)
            val now = System.currentTimeMillis()
            val entity = ChatEntity(
                id = "direct_$targetId",
                targetId = targetId,
                targetNickname = contact?.customNickname ?: contact?.nickname ?: targetId.toString(),
                targetAvatar = contact?.avatarUrl,
                unreadCount = 0,
                isPinned = false,
                isMuted = false,
                isArchived = false,
                createdAt = now,
                updatedAt = now
            )
            chatDao.insertChat(entity)
            entity.toDomain()
        }
    }

    suspend fun openOrCreateChat(targetId: Long): Result<String> = runCatching {
        chatDao.getChatByTargetId(targetId)?.id
            ?: createChat(targetId).getOrThrow().id
    }

    fun getMessages(chatId: String, limit: Int = 50): Flow<List<Message>> =
        messageDao.getMessages(chatId, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    // Server has no GET /chats/{id}/messages — messages arrive via WebSocket
    // and the offline queue on login. This is intentionally a no-op.
    suspend fun syncMessages(chatId: String, limit: Int = 50): Result<Unit> = Result.success(Unit)

    suspend fun loadMoreMessages(chatId: String, before: Long, limit: Int = 50): List<Message> {
        return messageDao.getMessagesBefore(chatId, before, limit).map { it.toDomain() }
    }

    suspend fun sendMessage(chatId: String, message: Message): Result<Message> =
        runCatching {
            val optimisticEntity = message.toEntity().copy(status = "SENDING")
            messageDao.insertMessage(optimisticEntity)

            val groupEntity = if (!chatId.startsWith("direct_")) groupDao.getGroup(chatId) else null
            if (groupEntity != null) {
                sendGroupMessage(chatId, message, groupEntity, optimisticEntity)
            } else {
                val chat = chatDao.getChat(chatId)
                if (chat == null) {
                    messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
                    throw Exception("Chat not found: $chatId")
                }
                sendDirectMessage(message, chat.targetId, optimisticEntity)
            }
        }.onFailure { e ->
            if (e is java.io.IOException) {
                val recipientUin = chatDao.getChat(chatId)?.targetId ?: 0L
                if (recipientUin != 0L) {
                    outboxDao.insert(
                        PendingOutboxEntity(
                            localId = message.id,
                            chatId = chatId,
                            recipientUin = recipientUin,
                            plainContent = message.content,
                            messageKind = message.kind.name,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    messageDao.updateMessageStatus(message.id, "PENDING")
                    Log.d("ChatRepository", "Queued ${message.id} to outbox (network unavailable)")
                }
            }
        }

    private suspend fun sendDirectMessage(
        message: Message,
        recipientUin: Long,
        optimisticEntity: com.rcq.messenger.domain.model.MessageEntity
    ): Message {
        if (!cryptoService.hasSession(recipientUin)) {
            val bundleResp = api.fetchPreKeyBundle(recipientUin)
            if (bundleResp.isSuccessful && bundleResp.body() != null) {
                cryptoService.buildSession(recipientUin, bundleResp.body()!!)
            } else {
                messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
                throw Exception("Cannot fetch key bundle (${bundleResp.code()})")
            }
        }

        val recipientIdentityKey = contactDao.getContactByUserId(recipientUin)?.identityKey
        val wrapped = runCatching {
            cryptoService.encryptWrapped(
                message.senderId, recipientUin, message.content, recipientIdentityKey
            )
        }.getOrElse { e ->
            messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
            throw e
        }

        messageDao.updateMessage(optimisticEntity.copy(
            ciphertext = wrapped.payload, signalType = wrapped.signalType, isEncrypted = true
        ))

        val request = com.rcq.messenger.data.api.SealedMessageRequest(
            toUin = recipientUin,
            envelopeType = wrapped.envelopeType,
            payload = wrapped.payload
        )
        return api.sendSealedMessage(request).let { response ->
            if (response.isSuccessful) {
                val resp = response.body()!!
                // Use server-assigned ID if non-empty, otherwise keep optimistic UUID
                val finalId = resp.serverTime.ifBlank { message.id }
                if (finalId != message.id) messageDao.deleteMessage(message.id)
                val sentEntity = message.copy(id = finalId, status = MessageStatus.SENT).toEntity().copy(
                    ciphertext = wrapped.payload, signalType = wrapped.signalType,
                    isEncrypted = true, status = "SENT"
                )
                messageDao.insertMessage(sentEntity)
                message.copy(id = finalId, status = MessageStatus.SENT)
            } else {
                messageDao.updateMessage(optimisticEntity.copy(status = "PENDING"))
                throw Exception("Send failed: ${response.code()}")
            }
        }
    }

    private suspend fun sendGroupMessage(
        chatId: String,
        message: Message,
        group: com.rcq.messenger.domain.model.GroupEntity,
        optimisticEntity: com.rcq.messenger.domain.model.MessageEntity
    ): Message {
        val recipients = mutableListOf<com.rcq.messenger.data.api.GroupSealedRecipient>()
        val groupForSend = refreshGroupForSendIfNeeded(group, message.senderId)

        for (memberUin in groupForSend.memberIds) {
            if (memberUin == message.senderId) continue // skip self

            try {
                var memberContact = contactDao.getContactByUserId(memberUin)
                if (!cryptoService.hasSession(memberUin)) {
                    val bundleResp = api.fetchPreKeyBundle(memberUin)
                    if (bundleResp.isSuccessful && bundleResp.body() != null) {
                        val bundle = bundleResp.body()!!
                        cryptoService.buildSession(memberUin, bundle)
                        memberContact = upsertMemberSignalIdentity(memberUin, bundle.identityKey, memberContact)
                    } else {
                        Log.w("ChatRepository", "No key bundle for group member $memberUin — skipping")
                        continue
                    }
                }
                val memberIdentityKey = memberContact?.identityKey?.takeIf { it.isNotBlank() }
                if (memberIdentityKey == null) {
                    Log.w("ChatRepository", "Missing ECIES identity_key for group member $memberUin; using Signal session fallback")
                }
                val wrapped = cryptoService.encryptWrapped(message.senderId, memberUin, message.content, memberIdentityKey)
                recipients.add(com.rcq.messenger.data.api.GroupSealedRecipient(
                    toUin = memberUin, payload = wrapped.payload
                ))
            } catch (e: Exception) {
                Log.w("ChatRepository", "Encrypt for member $memberUin failed: ${e.message}")
            }
        }

        if (recipients.isEmpty()) {
            messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
            throw Exception("No group members could be encrypted to")
        }

        val groupIdInt = group.id.toIntOrNull() ?: run {
            messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
            throw Exception("Invalid group id: ${group.id}")
        }

        val request = com.rcq.messenger.data.api.GroupSealedMessageRequest(
            groupId = groupIdInt,
            envelopeType = "message",
            payloads = recipients
        )

        return api.sendGroupSealedMessage(request).let { response ->
            if (response.isSuccessful) {
                messageDao.updateMessage(optimisticEntity.copy(status = "SENT"))
                message.copy(status = MessageStatus.SENT)
            } else {
                messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
                throw Exception("Group send failed: ${response.code()}")
            }
        }
    }

    private suspend fun refreshGroupForSendIfNeeded(
        group: com.rcq.messenger.domain.model.GroupEntity,
        senderUin: Long
    ): com.rcq.messenger.domain.model.GroupEntity {
        if (group.memberIds.any { it != senderUin }) return group
        val groupId = group.id
        return runCatching {
            val response = api.getGroup(groupId)
            if (!response.isSuccessful) return@runCatching group
            val apiGroup = response.body() ?: return@runCatching group
            val fresh = apiGroup.toGroupEntity()
            groupDao.insertGroup(fresh)
            apiGroup.members.forEach { member ->
                val existing = contactDao.getContactByUserId(member.uin.toLong())
                contactDao.insertContact(
                    ContactEntity(
                        userId = member.uin.toLong(),
                        nickname = member.nickname,
                        status = member.status,
                        identityKey = member.identityKey,
                        signingKey = member.signingKey,
                        signalIdentityKey = member.signalIdentityKey,
                        isBlocked = existing?.isBlocked ?: false,
                        avatarUrl = existing?.avatarUrl,
                        lastSeen = existing?.lastSeen,
                        isFavorite = existing?.isFavorite ?: false,
                        notificationSound = existing?.notificationSound,
                        customNickname = existing?.customNickname,
                        statusMessage = existing?.statusMessage
                    )
                )
            }
            fresh
        }.onFailure { e ->
            Log.w("ChatRepository", "refreshGroupForSendIfNeeded failed for $groupId: ${e.message}")
        }.getOrDefault(group)
    }

    private suspend fun upsertMemberSignalIdentity(
        memberUin: Long,
        signalIdentityKey: String,
        existing: ContactEntity?
    ): ContactEntity {
        val updated = (existing ?: ContactEntity(
            userId = memberUin,
            nickname = memberUin.toString()
        )).copy(signalIdentityKey = signalIdentityKey)
        contactDao.insertContact(updated)
        return updated
    }

    suspend fun editMessage(chatId: String, message: Message): Result<Message> = runCatching {
        api.editMessage(chatId, message.id, message).let { response ->
            if (response.isSuccessful) response.body()!!.also {
                messageDao.updateMessage(it.toEntity())
            }
            else throw Exception("Failed to edit message")
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> = runCatching {
        api.deleteMessage(chatId, messageId).let { response ->
            if (response.isSuccessful) {
                messageDao.deleteMessage(messageId)
            } else throw Exception("Failed to delete message")
        }
    }

    suspend fun addReaction(chatId: String, messageId: String, reaction: Reaction): Result<Message> = runCatching {
        api.addReaction(chatId, messageId, reaction).let { response ->
            if (response.isSuccessful) response.body()!!
            else throw Exception("Failed to add reaction")
        }
    }
}

// Extension functions
private val json = Json { ignoreUnknownKeys = true }

private fun ContactEntity.toDomain() = Contact(
    userId = userId, nickname = nickname,
    avatarUrl = avatarUrl, status = try { UserStatus.valueOf(status.uppercase()) } catch (e: Exception) { UserStatus.OFFLINE },
    lastSeen = lastSeen, isBlocked = isBlocked, isFavorite = isFavorite,
    notificationSound = notificationSound, customNickname = customNickname,
    statusMessage = statusMessage
)

private fun Contact.toEntity() = ContactEntity(
    userId = userId, nickname = nickname,
    avatarUrl = avatarUrl, status = status.name,
    lastSeen = lastSeen, isBlocked = isBlocked, isFavorite = isFavorite,
    notificationSound = notificationSound, customNickname = customNickname,
    statusMessage = statusMessage
)

private fun User.toContactEntity() = ContactEntity(
    userId = id,
    nickname = nickname,
    avatarUrl = avatarUrl,
    status = status.name,
    lastSeen = lastSeen,
    isBlocked = isBlocked,
    isFavorite = isFavorite,
    notificationSound = notificationSound,
    customNickname = customNickname,
    identityKey = identityKey,
    signingKey = signingKey,
    signalIdentityKey = signalIdentityKey,
    statusMessage = statusMessage
)

private fun ChatEntity.toDomain() = Chat(
    id = id, targetId = targetId, targetNickname = targetNickname,
    targetAvatar = targetAvatar, unreadCount = unreadCount,
    isPinned = isPinned, isMuted = isMuted, isArchived = isArchived,
    createdAt = createdAt, updatedAt = updatedAt,
    lastMessage = if (lastMessageContent != null && lastMessageTimestamp != null) {
        Message(
            id = "", chatId = id, senderId = 0L,
            kind = try { MessageKind.valueOf(lastMessageKind ?: "TEXT") } catch (e: Exception) { MessageKind.TEXT },
            content = lastMessageContent,
            timestamp = lastMessageTimestamp
        )
    } else null
)

private fun Chat.toEntity() = ChatEntity(
    id = id, targetId = targetId, targetNickname = targetNickname,
    targetAvatar = targetAvatar, unreadCount = unreadCount,
    isPinned = isPinned, isMuted = isMuted, isArchived = isArchived,
    createdAt = createdAt, updatedAt = updatedAt,
    lastMessageContent = lastMessage?.content,
    lastMessageTimestamp = lastMessage?.timestamp,
    lastMessageKind = lastMessage?.kind?.name
)

private fun GroupApiResponse.toGroupEntity() = GroupEntity(
    id = id.toString(),
    name = name,
    avatarUrl = null,
    description = description,
    creatorId = ownerUin.toLong(),
    memberIds = members.map { it.uin.toLong() },
    adminIds = members.filter { it.role == "admin" || it.role == "owner" }.map { it.uin.toLong() },
    createdAt = System.currentTimeMillis(),
    pinnedText = pinnedText
)

private fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    isFromMe = isFromMe,
    kind = MessageKind.valueOf(kind),
    content = content,
    mediaId = mediaId,
    timestamp = timestamp,
    status = MessageStatus.valueOf(status),
    receivedWhileAway = receivedWhileAway,
    deletedForEveryone = deletedForEveryone,
    reactions = reactions?.let { json.decodeFromString(it) } ?: emptyMap(),
    thumbnailB64 = thumbnailB64,
    durationSec = durationSec,
    ttlSeconds = ttlSeconds,
    forwardedFromName = forwardedFromName,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToAuthorName = replyToAuthorName,
    editedAt = editedAt,
    premiumPriceTokens = premiumPriceTokens,
    premiumUnlocked = premiumUnlocked,
    albumId = albumId,
    fileName = fileName,
    fileMime = fileMime,
    fileSizeBytes = fileSizeBytes,
    latitude = latitude,
    longitude = longitude,
    pollId = pollId,
    mentionedUserIds = mentionedUserIds?.let { json.decodeFromString(it) } ?: emptyList()
)

private fun Message.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = senderId,
    isFromMe = isFromMe,
    kind = kind.name,
    content = content,
    mediaId = mediaId,
    timestamp = timestamp,
    status = status.name,
    receivedWhileAway = receivedWhileAway,
    deletedForEveryone = deletedForEveryone,
    reactions = if (reactions.isEmpty()) null else json.encodeToString(reactions),
    thumbnailB64 = thumbnailB64,
    durationSec = durationSec,
    ttlSeconds = ttlSeconds,
    forwardedFromName = forwardedFromName,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToAuthorName = replyToAuthorName,
    editedAt = editedAt,
    premiumPriceTokens = premiumPriceTokens,
    premiumUnlocked = premiumUnlocked,
    albumId = albumId,
    fileName = fileName,
    fileMime = fileMime,
    fileSizeBytes = fileSizeBytes,
    latitude = latitude,
    longitude = longitude,
    pollId = pollId,
    mentionedUserIds = if (mentionedUserIds.isEmpty()) null else json.encodeToString(mentionedUserIds),
    // Signal Protocol E2EE fields - defaults for non-encrypted messages
    ciphertext = null,
    signalType = 1,
    isEncrypted = false
)
