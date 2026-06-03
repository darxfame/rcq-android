package com.rcq.messenger.ui.chat

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.di.PreferencesKeys
import com.rcq.messenger.domain.model.Chat
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.MessageStatus
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.media.MediaService
import com.rcq.messenger.media.MediaType
import com.rcq.messenger.media.VoiceRecorder
import com.rcq.messenger.ui.chat.inbox.InboxMapper
import com.rcq.messenger.ui.chat.inbox.InboxUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val inboxMapper = InboxMapper()

    val chats: StateFlow<List<Chat>> = chatRepository.getChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val contacts: StateFlow<List<Contact>> = contactRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val groups: StateFlow<List<Group>> = groupRepository.getGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasLoadedOnce = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    private val inboxData = combine(chats, contacts, groups) { chats, contacts, groups ->
        InboxData(chats = chats, contacts = contacts, groups = groups)
    }

    val inboxState: StateFlow<InboxUiState> = combine(
        inboxData,
        _isLoading,
        _hasLoadedOnce,
        _searchQuery,
        _error
    ) { data, isLoading, hasLoadedOnce, searchQuery, error ->
        inboxMapper.buildState(
            chats = data.chats,
            contacts = data.contacts,
            groups = data.groups,
            isLoading = isLoading,
            hasLoadedOnce = hasLoadedOnce,
            searchQuery = searchQuery,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InboxUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val results = supervisorScope {
                val chatResult = async { chatRepository.syncChats() }
                val contactResult = async { contactRepository.syncContacts() }
                val groupResult = async {
                    withTimeoutOrNull(6_000) { groupRepository.syncGroups() }
                        ?: Result.failure(Exception("groups sync timed out"))
                }
                listOf(chatResult.await(), contactResult.await(), groupResult.await())
            }
            results
                .firstOrNull { it.isFailure }
                ?.onFailure { _error.value = it.message }
            _hasLoadedOnce.value = true
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createChat(targetId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.createChat(targetId)
                .onSuccess { /* Navigate to chat */ }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun openContactChat(targetId: Long, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.openOrCreateChat(targetId)
                .onSuccess(onOpened)
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}

private data class InboxData(
    val chats: List<Chat>,
    val contacts: List<Contact>,
    val groups: List<Group>,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val mediaService: MediaService,
    private val voiceRecorder: VoiceRecorder,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _replyTo = MutableStateFlow<Message?>(null)
    val replyTo: StateFlow<Message?> = _replyTo.asStateFlow()

    // Chat header info
    private val _chatTitle = MutableStateFlow("Chat")
    val chatTitle: StateFlow<String> = _chatTitle.asStateFlow()

    private val _chatAvatar = MutableStateFlow<String?>(null)
    val chatAvatar: StateFlow<String?> = _chatAvatar.asStateFlow()

    private val _currentUserId = MutableStateFlow(0L)
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    private val _targetUin = MutableStateFlow(0L)
    val targetUin: StateFlow<Long> = _targetUin.asStateFlow()

    private val _memberCount = MutableStateFlow(0)
    val memberCount: Int get() = _memberCount.value

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _pinnedText = MutableStateFlow<String?>(null)
    val pinnedText: StateFlow<String?> = _pinnedText.asStateFlow()

    val inChatSearchResults = MutableStateFlow<List<Message>>(emptyList())

    val contacts: StateFlow<List<Contact>> = contactRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<Group>> = groupRepository.getGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val peerStatus: StateFlow<UserStatus> = combine(_targetUin, contacts) { targetUin, contacts ->
        contacts.firstOrNull { it.userId == targetUin }?.status ?: UserStatus.OFFLINE
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UserStatus.OFFLINE)

    private var chatId: String = ""
    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                val uin = prefs[PreferencesKeys.USER_UIN] ?: 0L
                if (uin != 0L) {
                    _currentUserId.value = uin
                    chatRepository.setCurrentUserUin(uin)
                }
            }
        }
    }

    fun loadChat(id: String) {
        if (chatId == id) return
        chatId = id
        _targetUin.value = getPeerUin()
        messagesJob?.cancel()
        _isLoading.value = true

        viewModelScope.launch {
            _pinnedText.value = null
            chatRepository.getChat(chatId)?.let { chat ->
                _chatTitle.value = chat.targetNickname.ifEmpty { "Chat" }
                _chatAvatar.value = chat.targetAvatar
                _isMuted.value = chat.isMuted
            }
            if (!chatId.startsWith("direct_")) {
                groupRepository.getGroup(chatId).onSuccess { group ->
                    _chatTitle.value = group.name
                    _memberCount.value = group.memberCount
                    _pinnedText.value = group.pinnedText
                }
            } else {
                _memberCount.value = 0
            }
            chatRepository.clearUnreadCount(chatId)
            chatRepository.syncMessages(chatId)
        }

        messagesJob = chatRepository.getMessages(chatId)
            .onEach { msgs ->
                _messages.value = msgs.sortedBy { it.timestamp }
                _isLoading.value = false
            }
            .launchIn(viewModelScope)

        typingJob?.cancel()
        typingJob = chatRepository.typingState(chatId)
            .onEach { _isTyping.value = it }
            .launchIn(viewModelScope)
    }

    fun getPeerUin(): Long = chatId.removePrefix("direct_").toLongOrNull() ?: 0L

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun setReplyTo(message: Message?) {
        _replyTo.value = message
    }

    fun clearSendError() {
        _sendError.value = null
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            val message = Message(
                id = java.util.UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = _currentUserId.value,
                isFromMe = true,
                kind = MessageKind.TEXT,
                content = text,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING,
                replyToId = _replyTo.value?.id,
                replyToContent = _replyTo.value?.content,
                replyToAuthorName = _replyTo.value?.let {
                    // We don't store author name on the reply source here, leave null
                    null
                }
            )

            _messageText.value = ""
            _replyTo.value = null

            chatRepository.sendMessage(chatId, message)
                .onFailure { _sendError.value = "Send failed: ${it.message}" }
        }
    }

    // Recording / playback state forwarded from VoiceRecorder
    val recordingState = voiceRecorder.recordingState
    val playbackState = voiceRecorder.playbackState

    private val _activeVoiceId = MutableStateFlow<String?>(null)
    val activeVoiceId: StateFlow<String?> = _activeVoiceId.asStateFlow()

    fun sendPhotoMessage(uri: Uri) {
        val chatId = this.chatId
        if (chatId.isEmpty()) return
        viewModelScope.launch {
            val chat = chatRepository.getChat(chatId) ?: return@launch
            mediaService.uploadMedia(uri, MediaType.IMAGE, chat.targetId).onSuccess { result ->
                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = _currentUserId.value,
                    isFromMe = true,
                    kind = MessageKind.PHOTO,
                    content = "",
                    mediaId = result.mediaId,
                    fileName = result.mimeType,
                    fileSizeBytes = result.size,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENDING
                )
                chatRepository.sendMessage(chatId, message)
                    .onFailure { _sendError.value = "Failed to send photo: ${it.message}" }
            }.onFailure { _sendError.value = "Upload failed: ${it.message}" }
        }
    }

    fun startVoiceRecording() {
        voiceRecorder.startRecording()
    }

    fun stopAndSendVoiceMessage() {
        val chatId = this.chatId
        if (chatId.isEmpty()) return
        viewModelScope.launch {
            voiceRecorder.stopRecording().onSuccess { file ->
                val uri = android.net.Uri.fromFile(file)
                val chat = chatRepository.getChat(chatId) ?: return@launch
                mediaService.uploadMedia(uri, MediaType.VOICE, chat.targetId).onSuccess { result ->
                    val duration = voiceRecorder.getVoiceDuration(file)
                    val message = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        chatId = chatId,
                        senderId = _currentUserId.value,
                        isFromMe = true,
                        kind = MessageKind.VOICE,
                        content = "",
                        mediaId = result.mediaId,
                        durationSec = duration / 1000.0,
                        timestamp = System.currentTimeMillis(),
                        status = MessageStatus.SENDING
                    )
                    chatRepository.sendMessage(chatId, message)
                        .onFailure { _sendError.value = "Failed to send voice: ${it.message}" }
                }.onFailure { _sendError.value = "Upload failed: ${it.message}" }
            }.onFailure { _sendError.value = "Recording failed: ${it.message}" }
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
    }

    fun sendVideoMessage(uri: Uri) {
        val chatId = this.chatId
        if (chatId.isEmpty()) return
        viewModelScope.launch {
            val chat = chatRepository.getChat(chatId) ?: return@launch
            mediaService.uploadMedia(uri, MediaType.VIDEO, chat.targetId).onSuccess { result ->
                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = _currentUserId.value,
                    isFromMe = true,
                    kind = MessageKind.VIDEO,
                    content = "",
                    mediaId = result.mediaId,
                    fileMime = result.mimeType,
                    fileSizeBytes = result.size,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENDING
                )
                chatRepository.sendMessage(chatId, message)
                    .onFailure { _sendError.value = "Failed to send video: ${it.message}" }
            }.onFailure { _sendError.value = "Upload failed: ${it.message}" }
        }
    }

    fun sendLocationMessage(context: android.content.Context) {
        val chatId = this.chatId
        if (chatId.isEmpty()) return
        viewModelScope.launch {
            try {
                val fusedClient = com.google.android.gms.location.LocationServices
                    .getFusedLocationProviderClient(context)
                val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { cont.resume(it, null) }
                        .addOnFailureListener { cont.resume(null, null) }
                } ?: run { _sendError.value = "Location unavailable"; return@launch }
                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = _currentUserId.value,
                    isFromMe = true,
                    kind = MessageKind.LOCATION,
                    content = "${location.latitude},${location.longitude}",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENDING
                )
                chatRepository.sendMessage(chatId, message)
                    .onFailure { _sendError.value = "Failed to send location: ${it.message}" }
            } catch (e: SecurityException) {
                _sendError.value = "Location permission required"
            } catch (e: Exception) {
                _sendError.value = "Location error: ${e.message}"
            }
        }
    }

    fun sendFileMessage(uri: Uri) {
        val chatId = this.chatId
        if (chatId.isEmpty()) return
        viewModelScope.launch {
            val chat = chatRepository.getChat(chatId) ?: return@launch
            mediaService.uploadMedia(uri, MediaType.DOCUMENT, chat.targetId).onSuccess { result ->
                val message = Message(
                    id = java.util.UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = _currentUserId.value,
                    isFromMe = true,
                    kind = MessageKind.FILE,
                    content = "",
                    mediaId = result.mediaId,
                    fileMime = result.mimeType,
                    fileSizeBytes = result.size,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.SENDING
                )
                chatRepository.sendMessage(chatId, message)
                    .onFailure { _sendError.value = "Failed to send file: ${it.message}" }
            }.onFailure { _sendError.value = "Upload failed: ${it.message}" }
        }
    }

    fun playVoice(message: Message) {
        val mediaId = message.mediaId ?: return
        viewModelScope.launch {
            _activeVoiceId.value = mediaId
            // Try local cache first, then download
            val localFile = mediaService.getLocalMediaFile(mediaId)
            val file = if (localFile != null) {
                localFile
            } else {
                val senderUin = if (message.isFromMe) null else message.senderId
                mediaService.downloadMedia(mediaId, senderUin, 1)
                    .getOrNull() ?: run {
                        _activeVoiceId.value = null
                        return@launch
                    }
            }
            voiceRecorder.playVoiceMessage(file)
        }
    }

    fun pauseVoice() {
        voiceRecorder.pausePlayback()
        _activeVoiceId.value = null
    }

    fun forwardMessage(message: Message) {
        _sendError.value = "Forward: coming soon"
    }

    fun forwardMessageTo(message: Message, targetChatId: String) {
        if (targetChatId.isBlank()) return
        viewModelScope.launch {
            val forwarded = message.copy(
                id = java.util.UUID.randomUUID().toString(),
                chatId = targetChatId,
                senderId = _currentUserId.value,
                isFromMe = true,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING
            )
            chatRepository.sendMessage(targetChatId, forwarded)
                .onFailure { _sendError.value = "Forward failed: ${it.message}" }
        }
    }

    fun toggleMute() {
        val id = chatId
        if (id.isEmpty()) return
        val next = !_isMuted.value
        _isMuted.value = next
        viewModelScope.launch {
            chatRepository.setMuted(id, next)
                .let { }
        }
    }

    fun clearHistory() {
        val id = chatId
        if (id.isEmpty()) return
        viewModelScope.launch {
            chatRepository.clearHistory(id)
        }
    }

    fun searchInChat(query: String) {
        val id = chatId
        viewModelScope.launch {
            inChatSearchResults.value = if (id.isNotEmpty() && query.length >= 2) {
                chatRepository.searchInChat(id, query)
            } else {
                emptyList()
            }
        }
    }

    fun clearInChatSearch() {
        inChatSearchResults.value = emptyList()
    }

    fun editMessage(message: Message, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch {
            chatRepository.editMessage(chatId, message.copy(content = newContent))
                .onFailure { _sendError.value = "Edit failed: ${it.message}" }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, messageId)
                .onFailure { _sendError.value = "Delete failed: ${it.message}" }
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            val reaction = com.rcq.messenger.domain.model.Reaction(
                userId = _currentUserId.value,
                emoji = emoji,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.addReaction(chatId, messageId, reaction)
                .onFailure { _sendError.value = "Reaction failed: ${it.message}" }
        }
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            val oldest = _messages.value.minByOrNull { it.timestamp } ?: return@launch
            val more = chatRepository.loadMoreMessages(chatId, oldest.timestamp)
            _messages.value = (_messages.value + more).distinctBy { it.id }
                .sortedBy { it.timestamp }
        }
    }
}
