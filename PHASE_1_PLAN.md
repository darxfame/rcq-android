# Phase 1 — Core Messaging — Implementation Plan

**Status:** 🚧 IN PROGRESS  
**Created:** 2026-05-25  
**Branch:** `phase-1-core-messaging`  
**Base:** `phase-0-critical-fixes`  
**Duration:** 2-3 weeks  
**Target:** Feature-complete messaging with E2EE

---

## Overview

Phase 1 implements core real-time messaging with end-to-end encryption (Signal Protocol), message reactions, read receipts, and real-time UI updates.

### What We're Building

**Core Features:**
1. ✅ **E2EE (Signal Protocol)** — Double Ratchet encryption
2. ✅ **Message Reactions** — Emoji reactions on messages
3. ✅ **Message Editing** — Edit sent messages
4. ✅ **Message Deletion** — Delete for me / delete for everyone
5. ✅ **Read Receipts** — Show delivery/read status
6. ✅ **Reply to Message** — Quote previous message
7. ✅ **Forward Message** — Share message to other chats
8. ✅ **Typing Indicators** — Show when user is typing
9. ✅ **Disappearing Messages** — Auto-delete after time
10. ✅ **Link Previews** — URL preview in messages

### Success Criteria

- ✅ Messages encrypted with Signal Protocol (ECIES + Double Ratchet)
- ✅ Can send/receive plain and encrypted messages
- ✅ Message model includes all fields (reactions, edits, TTL, etc.)
- ✅ Reactions work (add/remove/list)
- ✅ Read receipts tracked (sending/sent/delivered/read)
- ✅ Reply UI shows quoted message
- ✅ Real-time UI updates on WebSocket events
- ✅ No message duplication (idempotent insert)

---

## 1. Message Model Enhancement

### Current State
```kotlin
data class Message(
    val id: Long,
    val chatId: Long,
    val fromUin: Long,
    val text: String,
    val createdAt: Long
)
```

### Target State
```kotlin
data class Message(
    val id: Long,
    val chatId: Long,
    val fromUin: Long,
    val text: String,
    val createdAt: Long,
    
    // Reactions (Phase 1.2)
    val reactions: Map<String, List<Long>> = emptyMap(),
    
    // Edit history (Phase 1.3)
    val editedAt: Long? = null,
    val editHistory: List<String> = emptyList(),
    
    // Delivery tracking (Phase 1.4)
    val deliveryState: DeliveryState = DeliveryState.SENDING,
    
    // Reply to message (Phase 1.5)
    val replyToId: Long? = null,
    val replyToText: String? = null,
    val replyToAuthorUin: Long? = null,
    
    // Forward (Phase 1.6)
    val forwardedFromUin: Long? = null,
    val forwardedFromName: String? = null,
    
    // Disappearing messages (Phase 1.7)
    val ttlSeconds: Int? = null,
    val expiresAt: Long? = null,
    
    // Media (Phase 2)
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val mediaSize: Long? = null,
    
    // Encrypted payload (Phase 1.1)
    val ciphertext: String? = null,
    val nonce: String? = null,
    val ephemeralKey: String? = null,
    val signalType: String? = null
)

enum class DeliveryState {
    SENDING,      // Uploading to server
    SENT,         // Server accepted
    DELIVERED,    // Recipient received
    READ          // Recipient read
}
```

**Files to Update:**
- [ ] `domain/model/Message.kt` — Add all fields
- [ ] `domain/model/DeliveryState.kt` — New enum
- [ ] `data/db/MessageEntity.kt` — Add columns
- [ ] `data/db/RCQDatabase.kt` — Migration to v3

---

## 2. E2EE Implementation (Signal Protocol)

### Architecture

```
Message Flow (Encrypted):

[Sender] 
  ↓ user types message
[Message: "Hello"]
  ↓ get session with recipient
[SignalSession.establish(recipientUin)]
  ↓ fetch pre-key bundle from server
[GET /keys/{recipientUin}/bundle]
  ↓ encrypt with Double Ratchet
[CryptoService.encrypt(message, session)]
  ↓ create sealed envelope
[Envelope {
    to_uin: recipient,
    ciphertext: encrypted_bytes,
    nonce: encryption_nonce,
    ephemeral_key: curve25519_key,
    signal_type: "encrypted"
}]
  ↓ send to server
[POST /messages/send with Envelope]
  ↓
[Server stores encrypted envelope]
  ↓
[Recipient receives via WebSocket]
[WebSocketEvent.NewMessage {ciphertext, nonce, ...}]
  ↓ get session with sender
[SignalSession.establish(senderUin)]
  ↓ decrypt with Double Ratchet
[CryptoService.decrypt(envelope, session)]
  ↓
[Message: "Hello"] ✅
```

### Libraries

```gradle
// Signal Protocol (Java implementation)
implementation 'org.signal:libsignal-android:0.40.0'

// Or lower-level signal-protocol-java
implementation 'org.signal:signal-protocol-java:0.40.0'
```

### Implementation Plan

#### 1.1a: Key Generation & Storage

**Files:**
- [ ] `data/crypto/KeyManager.kt` (new)
  - Generate identity key pair
  - Generate pre-key bundle
  - Store in Android Keystore
  
- [ ] `data/crypto/SignalProtocolStore.kt` (new)
  - Implements Signal Protocol Store interface
  - Manages identity keys, session keys, pre-keys
  - SQLite-backed for Android
  
- [ ] `domain/model/KeyBundle.kt` (new)
  - Identity key
  - Signing key
  - Pre-key list
  - Signed pre-key

**API Endpoints:**
```kotlin
// In RCQApiService.kt
@POST("keys/bundle")
suspend fun uploadKeyBundle(@Body bundle: KeyBundle): Response<Unit>

@GET("keys/{uin}/bundle")
suspend fun getKeyBundle(@Path("uin") uin: Long): Response<KeyBundle>
```

#### 1.1b: Session Management

**Files:**
- [ ] `data/crypto/SessionManager.kt` (new)
  - Establish sessions with contacts
  - Manage Double Ratchet state
  - Track session version (v1, v2, etc.)
  
- [ ] `domain/model/SessionState.kt` (new)
  - Session ID
  - Recipient UIN
  - Current ratchet state
  - Creation timestamp

#### 1.1c: Encryption/Decryption

**Files:**
- [ ] `data/crypto/CryptoService.kt` (new)
  - `encrypt(plaintext, recipientUin): Envelope` 
  - `decrypt(envelope, senderUin): String`
  - ECIES (Elliptic Curve Integrated Encryption Scheme)
  - AES-GCM with 256-bit key

**Algorithm:**
```
ECIES:
1. Generate ephemeral Curve25519 key pair
2. DH with recipient's public key → shared secret
3. KDF (HKDF) → encryption key
4. AES-GCM encrypt plaintext
5. Return (ciphertext, ephemeral_public_key, nonce, auth_tag)

Decryption:
1. Receive ephemeral_public_key from sender
2. DH with our private key → shared secret (same as sender's)
3. KDF → encryption key
4. AES-GCM decrypt ciphertext
5. Return plaintext
```

**Files:**
- [ ] `data/crypto/EciesService.kt` (new)
  - ECIES encryption/decryption
  - Key derivation (HKDF-SHA256)
  - AES-GCM 256-bit

---

## 3. Message Repository Updates

### ChatRepository.kt Changes

**New Methods:**
```kotlin
// Send encrypted message
suspend fun sendMessage(
    chatId: Long,
    text: String,
    replyToId: Long? = null
): Result<Message>

// Add reaction
suspend fun addReaction(
    messageId: Long,
    reaction: String
): Result<Unit>

// Remove reaction
suspend fun removeReaction(
    messageId: Long,
    reaction: String
): Result<Unit>

// Edit message
suspend fun editMessage(
    messageId: Long,
    newText: String
): Result<Unit>

// Delete message
suspend fun deleteMessage(
    messageId: Long,
    forEveryone: Boolean = false
): Result<Unit>

// Mark as read
suspend fun markAsRead(messageIds: List<Long>): Result<Unit>

// Update delivery state
suspend fun updateDeliveryState(
    messageId: Long,
    state: DeliveryState
): Result<Unit>
```

### Handle WebSocket Events

**In chatRepository:**
```kotlin
init {
    viewModelScope.launch {
        webSocketManager.eventFlow.collect { event ->
            when (event) {
                is WebSocketEvent.NewMessage -> {
                    val decrypted = cryptoService.decrypt(event.ciphertext, event.fromUin)
                    val message = Message(text = decrypted, ...)
                    messageDao.insert(message.toEntity())
                    _messages.emit(getAllMessages(chatId))
                }
                
                is WebSocketEvent.MessageReaction -> {
                    messageDao.addReaction(event.messageId, event.reaction, event.fromUin)
                    _messages.emit(getAllMessages(chatId))
                }
                
                is WebSocketEvent.MessageEdited -> {
                    messageDao.updateMessage(event.messageId, event.newText)
                    _messages.emit(getAllMessages(chatId))
                }
                
                is WebSocketEvent.TypingStarted -> {
                    _typingUsers.emit(_typingUsers.value + event.fromUin)
                }
                
                is WebSocketEvent.TypingStopped -> {
                    _typingUsers.emit(_typingUsers.value - event.fromUin)
                }
                
                is WebSocketEvent.MessageRead -> {
                    messageDao.updateDeliveryState(event.messageIds, DeliveryState.READ)
                    _messages.emit(getAllMessages(chatId))
                }
                
                // ... more handlers
            }
        }
    }
}
```

---

## 4. Message UI Components

### ChatDetailScreen.kt

```kotlin
@Composable
fun ChatDetailScreen(
    chatId: Long,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages = viewModel.messages.collectAsState()
    val typingUsers = viewModel.typingUsers.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ChatHeader(chat = viewModel.chat.value)
        
        Divider()
        
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true
        ) {
            items(
                items = messages.value,
                key = { it.id }
            ) { message ->
                MessageBubble(
                    message = message,
                    onReactionClick = { emoji ->
                        viewModel.toggleReaction(message.id, emoji)
                    },
                    onReply = {
                        viewModel.setReplyTo(message)
                    }
                )
            }
        }
        
        // Typing indicator
        if (typingUsers.value.isNotEmpty()) {
            TypingIndicator(count = typingUsers.value.size)
        }
        
        Divider()
        
        // Input area
        ChatInputField(
            replyTo = viewModel.replyTo.value,
            onSend = { text ->
                viewModel.sendMessage(text)
                viewModel.clearReplyTo()
            },
            onTyping = {
                viewModel.sendTypingIndicator()
            }
        )
    }
}
```

### MessageBubble.kt

```kotlin
@Composable
fun MessageBubble(
    message: Message,
    onReactionClick: (String) -> Unit,
    onReply: () -> Unit
) {
    Column {
        // Reply preview (if replying to another message)
        if (message.replyToId != null) {
            ReplyPreview(
                text = message.replyToText ?: "",
                author = message.replyToAuthorUin.toString()
            )
        }
        
        // Main bubble
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .padding(8.dp)
                .contextMenu {
                    // Right-click menu
                    ContextMenuItem("Reply") { onReply() }
                    ContextMenuItem("Edit") { /* edit */ }
                    ContextMenuItem("Delete") { /* delete */ }
                    ContextMenuItem("Forward") { /* forward */ }
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Message text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Edit indicator
                if (message.editedAt != null) {
                    Text(
                        text = "(edited)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                // Timestamp + delivery state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall
                    )
                    
                    DeliveryStateIcon(message.deliveryState)
                }
            }
        }
        
        // Reactions row
        if (message.reactions.isNotEmpty()) {
            ReactionRow(
                reactions = message.reactions,
                onReactionClick = onReactionClick
            )
        }
    }
}
```

### ReactionRow.kt

```kotlin
@Composable
fun ReactionRow(
    reactions: Map<String, List<Long>>,
    onReactionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        reactions.forEach { (emoji, users) ->
            ReactionChip(
                emoji = emoji,
                count = users.size,
                onClick = { onReactionClick(emoji) }
            )
        }
        
        // Add reaction button
        ReactionPicker(onReactionSelected = onReactionClick)
    }
}

@Composable
fun ReactionChip(
    emoji: String,
    count: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(32.dp)
            .padding(2.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        if (count > 1) {
            Text(count.toString(), fontSize = 12.sp)
        }
    }
}
```

### ChatInputField.kt

```kotlin
@Composable
fun ChatInputField(
    replyTo: Message?,
    onSend: (String) -> Unit,
    onTyping: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var typingTimer by remember { mutableStateOf(0L) }
    
    Column {
        // Reply preview
        if (replyTo != null) {
            ReplyPreview(
                text = replyTo.text,
                author = replyTo.fromUin.toString(),
                onDismiss = { /* clear reply */ }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    
                    // Send typing indicator
                    val now = System.currentTimeMillis()
                    if (now - typingTimer > 1000) {
                        onTyping()
                        typingTimer = now
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp),
                placeholder = { Text("Message...") },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            // Send button
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}
```

---

## 5. API Endpoints

**Add to RCQApiService.kt:**

```kotlin
// ==================== MESSAGES ====================

@GET("chats/{id}/messages")
suspend fun getMessages(
    @Path("id") chatId: Long,
    @Query("limit") limit: Int = 50,
    @Query("offset") offset: Int = 0
): Response<List<Message>>

@POST("messages/send")
suspend fun sendMessage(
    @Body envelope: MessageEnvelope
): Response<MessageResponse>

@PUT("messages/{id}")
suspend fun editMessage(
    @Path("id") messageId: Long,
    @Body request: EditMessageRequest
): Response<Unit>

@DELETE("messages/{id}")
suspend fun deleteMessage(
    @Path("id") messageId: Long,
    @Query("for_everyone") forEveryone: Boolean = false
): Response<Unit>

// ==================== REACTIONS ====================

@POST("messages/{id}/reactions")
suspend fun addReaction(
    @Path("id") messageId: Long,
    @Body request: AddReactionRequest
): Response<Unit>

@DELETE("messages/{id}/reactions/{emoji}")
suspend fun removeReaction(
    @Path("id") messageId: Long,
    @Path("emoji") emoji: String
): Response<Unit>

@GET("messages/{id}/reactions")
suspend fun getReactions(
    @Path("id") messageId: Long
): Response<ReactionsResponse>

// ==================== READ RECEIPTS ====================

@POST("messages/read")
suspend fun markAsRead(
    @Body request: MarkAsReadRequest
): Response<Unit>

// ==================== TYPING ====================

@POST("chats/{id}/typing")
suspend fun sendTypingIndicator(
    @Path("id") chatId: Long
): Response<Unit>

// ==================== DATA CLASSES ====================

@Serializable
data class MessageEnvelope(
    val to_uin: Long,
    val ciphertext: String,
    val nonce: String,
    val ephemeral_key: String,
    val signal_type: String,
    val reply_to_id: Long? = null
)

@Serializable
data class MessageResponse(
    val id: Long,
    val created_at: Long
)

@Serializable
data class EditMessageRequest(
    val text: String
)

@Serializable
data class AddReactionRequest(
    val emoji: String
)

@Serializable
data class MarkAsReadRequest(
    val message_ids: List<Long>
)

@Serializable
data class ReactionsResponse(
    val reactions: Map<String, List<Long>>
)
```

---

## 6. Database Updates

### MessageEntity v3

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Long,
    
    val chatId: Long,
    val fromUin: Long,
    val text: String,
    val createdAt: Long,
    
    // Phase 1.2: Reactions
    val reactions: String? = null, // JSON: {"👍": [123, 456], "❤": [789]}
    
    // Phase 1.3: Edit
    val editedAt: Long? = null,
    val editHistory: String? = null, // JSON: ["Old text", "Updated text"]
    
    // Phase 1.4: Delivery
    val deliveryState: String = "SENDING", // SENDING, SENT, DELIVERED, READ
    
    // Phase 1.5: Reply
    val replyToId: Long? = null,
    val replyToText: String? = null,
    val replyToAuthorUin: Long? = null,
    
    // Phase 1.6: Forward
    val forwardedFromUin: Long? = null,
    val forwardedFromName: String? = null,
    
    // Phase 1.7: TTL
    val ttlSeconds: Int? = null,
    val expiresAt: Long? = null,
    
    // E2EE
    val ciphertext: String? = null,
    val nonce: String? = null,
    val ephemeralKey: String? = null,
    val signalType: String? = null
)
```

### MessageDao Updates

```kotlin
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    fun getMessages(chatId: Long): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)
    
    @Update
    suspend fun update(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)
    
    // Reactions
    @Query("UPDATE messages SET reactions = :reactions WHERE id = :id")
    suspend fun updateReactions(id: Long, reactions: String)
    
    // Edit
    @Query("UPDATE messages SET text = :newText, editedAt = :editedAt WHERE id = :id")
    suspend fun editMessage(id: Long, newText: String, editedAt: Long)
    
    // Delivery state
    @Query("UPDATE messages SET deliveryState = :state WHERE id = :id")
    suspend fun updateDeliveryState(id: Long, state: String)
    
    // Mark as read
    @Query("UPDATE messages SET deliveryState = 'READ' WHERE id IN (:ids)")
    suspend fun markAsRead(ids: List<Long>)
}
```

### RCQDatabase v3

```kotlin
@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        MessageEntity::class,  // v3 with all fields
    ],
    version = 3,  // Updated from 2
    exportSchema = true
)
abstract class RCQDatabase : RoomDatabase() {
    // ... DAO methods
}
```

---

## 7. ViewModels

### ChatViewModel Updates

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val cryptoService: CryptoService,
    private val webSocketManager: WebSocketManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val chatId = savedStateHandle.get<Long>("chatId")!!
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _typingUsers = MutableStateFlow<Set<Long>>(emptySet())
    val typingUsers: StateFlow<Set<Long>> = _typingUsers.asStateFlow()
    
    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo.asStateFlow()
    
    init {
        loadMessages()
        listenToWebSocketEvents()
    }
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                text = text,
                replyToId = _replyTo.value?.id
            ).onSuccess {
                _messages.emit(chatRepository.getMessages(chatId))
                _replyTo.emit(null)
            }
        }
    }
    
    fun toggleReaction(messageId: Long, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(messageId, emoji)
            _messages.emit(chatRepository.getMessages(chatId))
        }
    }
    
    fun editMessage(messageId: Long, newText: String) {
        viewModelScope.launch {
            chatRepository.editMessage(messageId, newText)
            _messages.emit(chatRepository.getMessages(chatId))
        }
    }
    
    fun deleteMessage(messageId: Long, forEveryone: Boolean = false) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId, forEveryone)
            _messages.emit(chatRepository.getMessages(chatId))
        }
    }
    
    fun setReplyTo(message: Message) {
        _replyTo.value = message
    }
    
    fun clearReplyTo() {
        _replyTo.value = null
    }
    
    fun sendTypingIndicator() {
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(chatId)
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _messages.emit(messages)
            }
        }
    }
    
    private fun listenToWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.eventFlow.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        if (event.toUin == userUin) {
                            val decrypted = cryptoService.decrypt(event)
                            chatRepository.addMessage(decrypted)
                            _messages.emit(chatRepository.getMessages(chatId))
                        }
                    }
                    
                    is WebSocketEvent.MessageReaction -> {
                        chatRepository.addReaction(event.messageId, event.reaction)
                        _messages.emit(chatRepository.getMessages(chatId))
                    }
                    
                    is WebSocketEvent.TypingStarted -> {
                        _typingUsers.emit(_typingUsers.value + event.fromUin)
                    }
                    
                    is WebSocketEvent.TypingStopped -> {
                        _typingUsers.emit(_typingUsers.value - event.fromUin)
                    }
                    
                    // ... more handlers
                }
            }
        }
    }
}
```

---

## 8. Testing Strategy

### Unit Tests

```kotlin
// CryptoServiceTest.kt
@Test
fun testEncryptDecrypt() {
    val plaintext = "Hello, World!"
    val ciphertext = cryptoService.encrypt(plaintext, recipientUin)
    val decrypted = cryptoService.decrypt(ciphertext, senderUin)
    assertEquals(plaintext, decrypted)
}

// MessageRepositoryTest.kt
@Test
fun testSendMessage() = runTest {
    val message = Message(text = "Test")
    chatRepository.sendMessage(chatId, "Test")
    
    val messages = messageDao.getMessages(chatId).first()
    assertEquals(1, messages.size)
    assertEquals("Test", messages[0].text)
}

@Test
fun testReactionToggle() = runTest {
    val messageId = 123L
    chatRepository.addReaction(messageId, "👍")
    
    val message = messageDao.get(messageId)
    assertTrue(message.reactions.contains("👍"))
    
    chatRepository.removeReaction(messageId, "👍")
    val updated = messageDao.get(messageId)
    assertFalse(updated.reactions.contains("👍"))
}
```

### Manual Testing Checklist

```
□ Send plain message
  - Type message
  - Send
  - Appears in list
  - Delivery state: SENDING → SENT → DELIVERED

□ Send encrypted message
  - Enable E2EE
  - Send message
  - Check logcat: encryption/decryption logs
  - Message appears on recipient

□ Reactions
  - Long-press message
  - Select emoji
  - Emoji appears in reactions row
  - Long-press again
  - Emoji removed

□ Edit message
  - Sent message appears
  - Tap menu → Edit
  - Change text
  - "Edited" label appears

□ Delete message
  - Delete for me: only disappears locally
  - Delete for everyone: disappears for all

□ Reply to message
  - Swipe on message or tap menu → Reply
  - Input field shows reply preview
  - Send message
  - Reply preview appears above message

□ Typing indicator
  - Start typing
  - Other side shows "User is typing..."
  - Stop typing for 3s
  - Disappears

□ Read receipts
  - Send message
  - Recipient opens chat
  - Status changes to READ
  - Double checkmark appears
```

---

## Estimated Timeline

- **1.1: E2EE Setup** — 3-4 days (key generation, storage, sessions)
- **1.2-1.4: Core Features** — 4-5 days (reactions, edit, delete, read receipts)
- **1.5-1.7: Advanced Features** — 3-4 days (reply, forward, typing, TTL)
- **Testing & Polish** — 2-3 days
- **Buffer** — 2-3 days

**Total:** 2-3 weeks ✅

---

## Deliverables

### Code Files (~3,000 LOC)
- [ ] `domain/model/Message.kt` — Enhanced message model
- [ ] `domain/model/DeliveryState.kt` — Delivery state enum
- [ ] `data/crypto/CryptoService.kt` — Encryption/decryption
- [ ] `data/crypto/EciesService.kt` — ECIES implementation
- [ ] `data/crypto/SessionManager.kt` — Signal sessions
- [ ] `data/crypto/KeyManager.kt` — Key generation/storage
- [ ] `data/db/MessageEntity.kt` — v3 schema
- [ ] `data/db/MessageDao.kt` — Updated queries
- [ ] `data/db/RCQDatabase.kt` — Migration to v3
- [ ] `data/api/RCQApiService.kt` — New endpoints
- [ ] `data/repository/ChatRepository.kt` — Enhanced with E2EE
- [ ] `ui/chat/ChatDetailScreen.kt` — Main chat UI
- [ ] `ui/chat/MessageBubble.kt` — Message display
- [ ] `ui/chat/ReactionRow.kt` — Reactions UI
- [ ] `ui/chat/ChatInputField.kt` — Input with reply preview
- [ ] `ui/chat/ChatViewModel.kt` — Enhanced ViewModel

### Documentation
- [ ] `PHASE_1_IMPLEMENTATION.md` — Implementation details
- [ ] `PHASE_1_SUMMARY.md` — Completion report

---

## Next Phase (Phase 2)

Once Phase 1 is complete:
- Media upload/download with E2EE
- Voice message recorder/player
- File attachments
- Location sharing
- Album/multi-photo

---

**Branch:** `phase-1-core-messaging`  
**Base:** `phase-0-critical-fixes`  
**Status:** Planning complete, ready to implement  
**Target Completion:** Mid-June 2026
