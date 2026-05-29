package com.rcq.messenger.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Chat
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.MessageStatus
import com.rcq.messenger.media.MediaService
import com.rcq.messenger.media.MediaType
import com.rcq.messenger.media.VoiceRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val chats: StateFlow<List<Chat>> = chatRepository.getChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.syncChats()
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
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
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val mediaService: MediaService,
    private val voiceRecorder: VoiceRecorder
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

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private var chatId: String = ""
    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().onSuccess { user ->
                _currentUserId.value = user.id
                chatRepository.setCurrentUserUin(user.id)
            }
        }
    }

    fun loadChat(id: String) {
        if (chatId == id) return
        chatId = id
        messagesJob?.cancel()
        _isLoading.value = true

        // Load chat metadata (title, avatar)
        viewModelScope.launch {
            chatRepository.getChat(chatId)?.let { chat ->
                _chatTitle.value = chat.targetNickname.ifEmpty { "Chat" }
                _chatAvatar.value = chat.targetAvatar
            }
            // Sync messages from server
            chatRepository.syncMessages(chatId)
        }

        // Observe DB flow — doesn't block, sets loading false on first emission
        messagesJob = chatRepository.getMessages(chatId)
            .onEach { msgs ->
                _messages.value = msgs.sortedByDescending { it.timestamp }
                _isLoading.value = false
            }
            .launchIn(viewModelScope)
    }

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
        // TODO Phase 2: show chat selector, then resend to chosen chat
        _sendError.value = "Forward: coming soon"
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            val oldest = _messages.value.minByOrNull { it.timestamp } ?: return@launch
            val more = chatRepository.loadMoreMessages(chatId, oldest.timestamp)
            _messages.value = (_messages.value + more).distinctBy { it.id }
                .sortedByDescending { it.timestamp }
        }
    }
}
