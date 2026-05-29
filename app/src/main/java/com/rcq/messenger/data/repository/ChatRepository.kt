package com.rcq.messenger.data.repository

import android.util.Log
import com.rcq.messenger.data.api.AddContactRequest
import com.rcq.messenger.data.api.ContactRequest
import com.rcq.messenger.data.api.CreateChatRequest
import com.rcq.messenger.data.api.PresenceUpdateRequest
import com.rcq.messenger.data.api.RespondContactRequestBody
import com.rcq.messenger.data.api.SendContactRequestBody
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.db.*
import com.rcq.messenger.domain.model.*
import com.rcq.messenger.crypto.CryptoService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
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
                Log.d("ContactRepository", "Got ${users.size} contacts")
                // Clear and insert contacts (map from User to ContactEntity)
                contactDao.insertAll(users.map { it.toContactEntity() })
            } else {
                Log.e("ContactRepository", "getContacts failed: ${response.errorBody()?.string()}")
            }
        }

        // Also fetch pending requests separately (like iOS does)
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
    private val webSocketService: WebSocketService,
    private val cryptoService: CryptoService,
    private val notificationHelper: com.rcq.messenger.service.NotificationHelper
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        fetchOfflineQueue()
        webSocketService.events
            .onEach { event ->
                when (event) {
                    is WsEvent.ContactRequest -> {
                        notificationHelper.showContactRequestNotification(
                            fromUin = event.fromUin,
                            nickname = event.fromNickname
                        )
                    }
                    is WsEvent.MessageNew -> {
                        try {
                            val obj = event.raw
                            val msgId = obj["id"]?.jsonPrimitive?.contentOrNull
                                ?: obj["message_id"]?.jsonPrimitive?.contentOrNull
                                ?: return@onEach
                            val senderId = obj["sender_uin"]?.jsonPrimitive?.longOrNull
                                ?: obj["senderUIN"]?.jsonPrimitive?.longOrNull ?: 0L
                            val serverChatId = obj["chat_id"]?.jsonPrimitive?.contentOrNull
                            val chatId = if (!serverChatId.isNullOrBlank()) serverChatId
                                         else "direct_$senderId"
                            // Try to decrypt E2EE ciphertext; fall back to plaintext field
                            val rawCiphertext = obj["ciphertext"]?.jsonPrimitive?.contentOrNull
                            val signalType = obj["signal_type"]?.jsonPrimitive?.intOrNull ?: -1
                            val content = if (rawCiphertext != null && signalType >= 0) {
                                runCatching {
                                    cryptoService.decryptMessage(senderId, rawCiphertext, signalType)
                                }.getOrElse {
                                    obj["text"]?.jsonPrimitive?.contentOrNull ?: ""
                                }
                            } else {
                                obj["text"]?.jsonPrimitive?.contentOrNull
                                    ?: obj["content"]?.jsonPrimitive?.contentOrNull ?: ""
                            }
                            val timestamp = obj["sent_at"]?.jsonPrimitive?.longOrNull
                                ?: obj["sentAt"]?.jsonPrimitive?.longOrNull
                                ?: System.currentTimeMillis()
                            val kindStr = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "text"
                            val msg = com.rcq.messenger.domain.model.Message(
                                id = msgId,
                                chatId = chatId,
                                senderId = senderId,
                                isFromMe = false,
                                kind = runCatching {
                                    json.decodeFromString<com.rcq.messenger.domain.model.MessageKind>("\"$kindStr\"")
                                }.getOrDefault(com.rcq.messenger.domain.model.MessageKind.TEXT),
                                content = content,
                                timestamp = timestamp
                            )
                            messageDao.insertMessage(msg.toEntity())

                            // Update chat metadata: increment unread count and update timestamp
                            try {
                                val now = System.currentTimeMillis()
                                val existingChat = chatDao.getChat(msg.chatId)
                                val senderName = existingChat?.targetNickname ?: "New message"
                                notificationHelper.showMessageNotification(
                                    chatId = msg.chatId,
                                    senderName = senderName,
                                    message = msg.content.ifBlank { "📎 Attachment" }
                                )
                                if (existingChat != null) {
                                    chatDao.incrementUnreadCount(msg.chatId, now)
                                } else {
                                    // Create minimal chat entry so it appears in the list
                                    val newChat = ChatEntity(
                                        id = msg.chatId,
                                        targetId = msg.senderId,
                                        targetNickname = msg.replyToAuthorName ?: "Unknown",
                                        targetAvatar = null,
                                        unreadCount = 1,
                                        isPinned = false,
                                        isMuted = false,
                                        isArchived = false,
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                    chatDao.insertChat(newChat)
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to update chat metadata: ${'$'}{e.message}")
                            }

                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Failed to handle incoming message: ${'$'}{e.message}")
                        }
                    }
                    else -> { /* ignore other events for now */ }
                }
            }
            .launchIn(scope)
    }

    private fun fetchOfflineQueue() {
        scope.launch {
            runCatching {
                val response = api.getMessageQueue()
                if (response.isSuccessful) {
                    response.body()?.forEach { msg ->
                        if (messageDao.getMessage(msg.id) == null) {
                            messageDao.insertMessage(msg.toEntity())
                            val chat = chatDao.getChat(msg.chatId)
                            if (chat != null) chatDao.incrementUnreadCount(msg.chatId, System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }

    suspend fun getChat(chatId: String): Chat? = chatDao.getChat(chatId)?.toDomain()

    fun getChats(): Flow<List<Chat>> = chatDao.getChats().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun syncChats(): Result<Unit> = runCatching {
        api.getChats().let { response ->
            if (response.isSuccessful) {
                chatDao.insertChats(response.body()!!.map { it.toEntity() })
            }
        }
    }

    // Chats are a client-side concept only — server has no /chats endpoint.
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

    suspend fun sendMessage(chatId: String, message: Message): Result<Message> = runCatching {
        val chat = chatDao.getChat(chatId) ?: throw Exception("Chat not found")
        val recipientUin = chat.targetId

        // Insert optimistically so message shows immediately in UI
        val optimisticEntity = message.toEntity().copy(status = "SENDING")
        messageDao.insertMessage(optimisticEntity)

        // Build Signal session if we don't have one yet (first message to this contact)
        if (!cryptoService.hasSession(recipientUin)) {
            val bundleResp = api.fetchPreKeyBundle(recipientUin)
            if (bundleResp.isSuccessful && bundleResp.body() != null) {
                cryptoService.buildSession(recipientUin, bundleResp.body()!!)
            } else {
                messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
                throw Exception("Cannot fetch key bundle for recipient (${bundleResp.code()})")
            }
        }

        val encrypted = runCatching { cryptoService.encryptMessage(recipientUin, message.content) }
            .getOrElse { e ->
                messageDao.updateMessage(optimisticEntity.copy(status = "FAILED"))
                throw e
            }

        val localEntity = optimisticEntity.copy(
            ciphertext = encrypted.ciphertext,
            signalType = encrypted.signalType,
            isEncrypted = true
        )
        messageDao.updateMessage(localEntity)

        // Matches iOS: { to_uin, envelope_type, payload }
        val request = com.rcq.messenger.data.api.SealedMessageRequest(
            toUin = recipientUin,
            envelopeType = "message",
            payload = encrypted.ciphertext
        )

        api.sendSealedMessage(request).let { response ->
            if (response.isSuccessful) {
                val resp = response.body()!!
                val sent = message.copy(id = resp.id, status = MessageStatus.SENT)
                val updatedEntity = sent.toEntity().copy(
                    ciphertext = encrypted.ciphertext,
                    signalType = encrypted.signalType,
                    isEncrypted = true,
                    status = "SENT"
                )
                messageDao.updateMessage(updatedEntity)
                sent
            } else {
                messageDao.updateMessage(localEntity.copy(status = "FAILED"))
                throw Exception("Failed to send message: ${response.code()}")
            }
        }
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
    notificationSound = notificationSound, customNickname = customNickname
)

private fun Contact.toEntity() = ContactEntity(
    userId = userId, nickname = nickname,
    avatarUrl = avatarUrl, status = status.name,
    lastSeen = lastSeen, isBlocked = isBlocked, isFavorite = isFavorite,
    notificationSound = notificationSound, customNickname = customNickname
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
    customNickname = customNickname
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