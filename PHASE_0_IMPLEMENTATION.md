# Phase 0 — Critical Bug Fixes — Implementation Guide

**Status:** 🚧 IN PROGRESS  
**Created:** 2026-05-25  
**Branch:** `phase-0-critical-fixes`

---

## Overview

Phase 0 fixes 4 critical issues blocking core messaging functionality:

1. **0.1** ContactEntity primary key (prevents duplicate contacts)
2. **0.2** UserStatus missing states (DND, INVISIBLE)
3. **0.3** WebSocket with exponential backoff (prevents connection thrashing)
4. **0.4** Full WebSocket event model (47+ event types)

---

## Completed Files

### ✅ 0.2: UserStatus.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/domain/model/UserStatus.kt`

**Changes:**
- Added `DND` (Do Not Disturb) status
- Added `INVISIBLE` status
- Added `@SerialName` annotations for correct JSON deserialization
- Added `toDisplayString()` helper for UI

**Serialization Mapping:**
```
Server JSON → Kotlin Enum
"online"    → ONLINE
"away"      → AWAY
"dnd"       → DND
"invisible" → INVISIBLE
"offline"   → OFFLINE
```

**Test:**
```kotlin
assertEquals("dnd", UserStatus.DND.name.lowercase())
assertEquals(5, UserStatus.values().size)
```

---

### ✅ 0.1: Contact.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/domain/model/Contact.kt`

**Changes:**
- Removed auto-generated `id` field
- Changed primary key to `userId` (maps to `uin` from server)
- Added `@SerialName("uin")` to map server field
- Added `@SerialName` for all snake_case JSON fields:
  - `avatar_url` → `avatarUrl`
  - `last_seen` → `lastSeen`
  - `blocked` → `isBlocked`
- Updated `ContactEntity` to use `userId` as `@PrimaryKey`
- Added `toEntity()` and `toDomain()` extension functions

**Before (BROKEN):**
```kotlin
@Entity
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,  // ❌ Auto-generated, causes duplicates
    val userId: Long,  // ❌ Server UIN never used as key
    // ...
)
```

**After (FIXED):**
```kotlin
@Entity
data class ContactEntity(
    @PrimaryKey
    val userId: Long,  // ✅ Maps to server "uin"
    // ...
)
```

**Why This Fixes Duplicates:**
1. Server returns: `[{"uin": 123, "nickname": "Alice"}, ...]`
2. Old code: Creates new `id` (say 1, 2, 3, ...) on each sync
3. New code: Uses `userId` (123) as key, REPLACE strategy updates existing

**Test:**
```kotlin
// Sync contacts twice
val contacts1 = api.getContacts()
contactDao.insertAll(contacts1.map { it.toEntity() })
val contacts2 = api.getContacts()
contactDao.insertAll(contacts2.map { it.toEntity() })
// Should have same count, not double
```

---

### ✅ 0.4: WebSocketEvent.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/domain/model/WebSocketEvent.kt`

**Changes:**
- Created sealed class with 40+ event types
- 12 categories (Messaging, Presence, Threads, Groups, Calls, Audio Rooms, Stories, Marketplace, Games, Auctions, Trades, Pets)
- Each event type is a data class with relevant fields
- Type-safe routing via sealed class

**Event Categories:**
```
Messaging:  NewMessage, MessageDeleted, MessageDeletedForEveryone, 
            MessageReaction, MessageRead, MessageEdited, 
            MessageBounced, TypingStarted, TypingStopped

Presence:   PresenceOnline, PresenceAway, PresenceDnd, 
            PresenceInvisible, PresenceOffline

Threads:    ThreadUpdated, ThreadDeleted
Groups:     GroupUpdated, GroupMemberJoined, GroupMemberLeft, 
            GroupDeleted
Calls:      CallOffer, CallAnswer, CallIceCandidate, CallEnd, 
            CallUpgrade, CallUpgradeAnswer
...and more
```

**Usage:**
```kotlin
webSocketManager.eventFlow.collect { event ->
    when (event) {
        is WebSocketEvent.NewMessage -> {
            chatRepository.handleNewMessage(event)
        }
        is WebSocketEvent.PresenceOnline -> {
            userRepository.updatePresence(event.userUin, UserStatus.ONLINE)
        }
        is WebSocketEvent.TypingStarted -> {
            chatRepository.showTyping(event.chatId, event.fromUin)
        }
        // ... more handlers
    }
}
```

---

### ✅ 0.3: WebSocketManager.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/data/ws/WebSocketManager.kt`

**Changes:**
- Exponential backoff: 1s → 2s → 4s → 8s → 16s → 30s (max)
- Ping/pong keepalive: every 25 seconds
- Stale watchdog: reconnect if 90s without messages
- Event routing to `eventFlow` (SharedFlow with 100 buffer)
- Auto-reconnect on failure (except code 1000 = client disconnect)
- Full error logging

**Backoff Algorithm:**
```kotlin
delay = min(1_000ms * 2^attempt, 30_000ms)
// Attempt 0: 1s
// Attempt 1: 2s
// Attempt 2: 4s
// Attempt 3: 8s
// Attempt 4: 16s
// Attempt 5+: 30s
```

**Lifecycle:**
```
connect(uin, token)
  ↓
[WebSocket Connect] onOpen()
  ├─ Start ping/pong (25s interval)
  └─ Start stale watchdog (90s timeout)
  ↓
onMessage(json) → parseEvent() → emit to eventFlow
  ↓
[On Close or Failure]
  ├─ Stop timers
  ├─ Calculate backoff delay
  └─ scheduleReconnect()
```

**Usage in Repository:**
```kotlin
@HiltViewModel
class ChatViewModel : ViewModel() {
    @Inject
    lateinit var webSocketManager: WebSocketManager
    
    init {
        viewModelScope.launch {
            webSocketManager.eventFlow.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> handleNewMessage(event)
                    // ...
                }
            }
        }
    }
}
```

---

### ✅ 0.1: ContactDao.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/data/db/ContactDao.kt`

**Changes:**
- Updated all queries to use `userId` (not `id`)
- `insertAll()` uses `OnConflictStrategy.REPLACE` for sync updates
- Added `getContactByUserId()` by primary key
- Added `blockContact()` / `unblockContact()` helpers

**REPLACE Strategy:**
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(contacts: List<ContactEntity>)
```

This means:
- If contact with `userId=123` exists → update it
- If contact with `userId=123` is new → insert it
- No duplicates, no primary key conflicts

---

### ✅ 0.1: RCQApiService.kt
**Location:** `app/src/main/kotlin/com/rcq/messenger/data/api/RCQApiService.kt`

**Changes:**
- Fixed `getContacts()` return type: `Response<List<Contact>>` (was `ContactList`)
- Added `updatePresenceStatus()` API endpoint (for Phase 0.3)
- Added `@SerialName` annotations to all request/response bodies
- Proper documentation with CRITICAL FIXES

**Before:**
```kotlin
@GET("contacts")
suspend fun getContacts(): Response<ContactList>  // ❌ Wrong!
// Expected: {"contacts": [...], "pendingRequests": [...]}
// Server sends: [...]
```

**After:**
```kotlin
@GET("contacts")
suspend fun getContacts(): Response<List<Contact>>  // ✅ Correct!
// Server sends: [...]
// Maps via @SerialName("uin") → Contact.userId
```

---

## TODO: Integration & Testing

### 1. Update RCQDatabase.kt
**File:** `app/src/main/kotlin/com/rcq/messenger/data/db/RCQDatabase.kt`

**Action:**
- Add `ContactDao` to `@Database` entities
- Remove old `ContactDao` if it exists
- Update version (increment `version = X`)

**Code:**
```kotlin
@Database(
    entities = [
        ContactEntity::class,
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        // ... others
    ],
    version = 2  // Increment version!
)
abstract class RCQDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    // ...
}
```

---

### 2. Update ChatRepository.kt (or ContactRepository.kt)
**File:** `app/src/main/kotlin/com/rcq/messenger/data/repository/ChatRepository.kt` or `ContactRepository.kt`

**Actions:**
- Update `getContacts()` to handle `Response<List<Contact>>`
- Update `syncContacts()` to use new DAO
- Add `updatePresence()` method calling API

**Code Example:**
```kotlin
suspend fun syncContacts(): Result<Unit> = runCatching {
    val response = api.getContacts()
    if (response.isSuccessful) {
        val contacts = response.body()!!  // ✅ Now List<Contact>
        val entities = contacts.map { it.toEntity() }
        contactDao.insertAll(entities)  // ✅ REPLACE strategy
    } else {
        throw Exception("Failed to sync contacts: ${response.code()}")
    }
}

suspend fun updatePresence(status: UserStatus): Result<Unit> = runCatching {
    api.updatePresenceStatus(
        UpdatePresenceRequest(status.name.lowercase())
    ).let { response ->
        if (!response.isSuccessful) throw Exception("Presence update failed")
    }
}
```

---

### 3. Create or Update AppModule.kt (DI)
**File:** `app/src/main/kotlin/com/rcq/messenger/di/AppModule.kt`

**Actions:**
- Provide `WebSocketManager` as singleton
- Provide `RCQApiService` with updated HTTP client

**Code:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Singleton
    @Provides
    fun provideWebSocketManager(okHttpClient: OkHttpClient): WebSocketManager {
        return WebSocketManager(okHttpClient)
    }
    
    @Singleton
    @Provides
    fun provideRCQApiService(okHttpClient: OkHttpClient): RCQApiService {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                Json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(RCQApiService::class.java)
    }
}
```

---

### 4. Update AuthViewModel.kt
**File:** `app/src/main/kotlin/com/rcq/messenger/ui/auth/AuthViewModel.kt`

**Actions:**
- On successful login, call `webSocketManager.connect(uin, token)`
- On logout, call `webSocketManager.disconnect()`

**Code:**
```kotlin
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val webSocketManager: WebSocketManager,
    private val preferencesDataStore: DataStore<Preferences>
) : ViewModel() {
    
    fun login(publicKey: String) {
        viewModelScope.launch {
            authRepository.login(publicKey).fold(
                onSuccess = { (uin, token) ->
                    // Save to preferences
                    saveCredentials(uin, token)
                    // Connect WebSocket
                    webSocketManager.connect(uin, token)
                    _uiState.value = AuthUiState.LoggedIn
                },
                onFailure = { e ->
                    _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
                }
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            webSocketManager.disconnect()
            clearCredentials()
            _uiState.value = AuthUiState.LoggedOut
        }
    }
}
```

---

### 5. Create ChatViewModel.kt (WebSocket Consumer)
**File:** `app/src/main/kotlin/com/rcq/messenger/ui/chat/ChatViewModel.kt`

**Actions:**
- Collect `webSocketManager.eventFlow`
- Route events to appropriate repositories
- Update UI state

**Code:**
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val webSocketManager: WebSocketManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val chatId = savedStateHandle.get<Long>("chatId")!!
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    init {
        loadMessages()
        listenToWebSocketEvents()
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _messages.value = messages
            }
        }
    }
    
    private fun listenToWebSocketEvents() {
        viewModelScope.launch {
            webSocketManager.eventFlow.collect { event ->
                when (event) {
                    is WebSocketEvent.NewMessage -> {
                        if (event.toUin == userUin) {
                            // Refresh message list for this chat
                            loadMessages()
                        }
                    }
                    is WebSocketEvent.TypingStarted -> {
                        if (event.chatId == chatId) {
                            // Show typing indicator
                        }
                    }
                    is WebSocketEvent.TypingStopped -> {
                        if (event.chatId == chatId) {
                            // Hide typing indicator
                        }
                    }
                    is WebSocketEvent.MessageReaction -> {
                        // Update message reactions
                    }
                    // ... more handlers
                    else -> {} // Ignore unknown events
                }
            }
        }
    }
}
```

---

## Testing Checklist

### Unit Tests
- [ ] `ContactEntity` primary key uniqueness (no duplicates on sync)
- [ ] `UserStatus` serialization/deserialization (all 5 values)
- [ ] `WebSocketEvent` parsing (all event types)
- [ ] WebSocket backoff calculation (1s, 2s, 4s, ..., 30s, 30s)

### Integration Tests
- [ ] Contact sync: fetch from API, insert to DB, no duplicates
- [ ] WebSocket connection: connect, ping/pong, reconnect on failure
- [ ] Presence update: set status, broadcast to contacts

### Manual Testing
- [ ] Start app → login → WebSocket connects
- [ ] Kill WebSocket connection (airplane mode) → auto-reconnects with backoff
- [ ] Send contact request from iOS → Android receives via WS
- [ ] Check logcat: "WebSocket connected", ping/pong messages
- [ ] Check contacts list: no duplicates after sync
- [ ] Change presence status → check last_seen on other clients

---

## Files Summary

| File | Type | Status | Purpose |
|------|------|--------|---------|
| `UserStatus.kt` | Model | ✅ DONE | Add DND, INVISIBLE states |
| `Contact.kt` | Model | ✅ DONE | Fix PK, add @SerialName |
| `ContactDao.kt` | DB | ✅ DONE | Query by userId, REPLACE strategy |
| `WebSocketEvent.kt` | Model | ✅ DONE | 40+ event types, sealed class |
| `WebSocketManager.kt` | Service | ✅ DONE | Backoff, ping/pong, reconnect |
| `RCQApiService.kt` | API | ✅ DONE | Fix getContacts, add presence |
| `RCQDatabase.kt` | DB | 🔴 TODO | Update entities & version |
| `ChatRepository.kt` | Repository | 🔴 TODO | Sync contacts with new DAO |
| `AppModule.kt` | DI | 🔴 TODO | Provide WebSocketManager |
| `AuthViewModel.kt` | ViewModel | 🔴 TODO | Connect/disconnect WebSocket |
| `ChatViewModel.kt` | ViewModel | 🔴 TODO | Consume WebSocket events |

---

## Next Steps (Phase 1)

Once Phase 0 is stable:

1. **E2EE (Signal Protocol)** — Add `signal-protocol-java` library
2. **Message Model Full** — Add media, reactions, TTL fields
3. **Read Receipts** — Track message delivery state
4. **Reply/Forward** — Quote previous messages
5. **Real-time UI** — Show typing indicators, delivery status

---

## References

- iOS implementation: [rcq-messenger/rcq-ios](https://github.com/rcq-messenger/rcq-ios)
- DIAGNOSIS.md: Root cause analysis
- MIGRATION_PLAN.md: Full phase roadmap
