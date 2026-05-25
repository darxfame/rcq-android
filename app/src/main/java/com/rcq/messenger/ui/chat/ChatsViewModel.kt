package com.rcq.messenger.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _messages = MutableStateFlow<List<com.rcq.messenger.domain.model.Message>>(emptyList())
    val messages: StateFlow<List<com.rcq.messenger.domain.model.Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _replyTo = MutableStateFlow<com.rcq.messenger.domain.model.Message?>(null)
    val replyTo: StateFlow<com.rcq.messenger.domain.model.Message?> = _replyTo.asStateFlow()

    private val _chatInfo = MutableStateFlow<Pair<String, String>?>(null) // targetNickname, targetAvatar
    val chatInfo: StateFlow<Pair<String, String>?> = _chatInfo.asStateFlow()

    private val _currentUserId = MutableStateFlow(0L)
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    private var chatId: String = ""

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().onSuccess { user ->
                _currentUserId.value = user.id
            }
        }
    }

    fun loadChat(id: String) {
        chatId = id
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getMessages(chatId).collect { messages ->
                _messages.value = messages.reversed()
            }
            _isLoading.value = false
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun setReplyTo(message: com.rcq.messenger.domain.model.Message?) {
        _replyTo.value = message
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            val message = com.rcq.messenger.domain.model.Message(
                id = java.util.UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = _currentUserId.value,
                content = text,
                timestamp = System.currentTimeMillis(),
                replyToId = _replyTo.value?.id,
                replyToContent = _replyTo.value?.content
            )

            chatRepository.sendMessage(chatId, message)
                .onSuccess {
                    _messageText.value = ""
                    _replyTo.value = null
                }
                .onFailure { error ->
                    // TODO: Show error to user
                }
        }
    }

    fun loadMoreMessages() {
        viewModelScope.launch {
            val oldestMessage = _messages.value.minByOrNull { it.timestamp } ?: return@launch
            val moreMessages = chatRepository.loadMoreMessages(chatId, oldestMessage.timestamp)
            _messages.value = (moreMessages + _messages.value).distinctBy { it.id }
        }
    }
}