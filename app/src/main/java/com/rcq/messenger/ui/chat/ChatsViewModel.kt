package com.rcq.messenger.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Chat
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val userRepository: UserRepository
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

    fun loadMoreMessages() {
        viewModelScope.launch {
            val oldest = _messages.value.minByOrNull { it.timestamp } ?: return@launch
            val more = chatRepository.loadMoreMessages(chatId, oldest.timestamp)
            _messages.value = (_messages.value + more).distinctBy { it.id }
                .sortedByDescending { it.timestamp }
        }
    }
}
