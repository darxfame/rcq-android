package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.User
import com.rcq.messenger.domain.model.UserStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactInfoViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {
    private val _contact = MutableStateFlow<Contact?>(null)
    val contact: StateFlow<Contact?> = _contact.asStateFlow()

    private val _commonGroups = MutableStateFlow<List<Group>>(emptyList())
    val commonGroups: StateFlow<List<Group>> = _commonGroups.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var loadJob: Job? = null

    fun load(userId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _error.value = null
            val cached = contactRepository.getContacts()
                .first()
                .firstOrNull { it.userId == userId }
            if (cached != null) _contact.value = cached

            userRepository.getUserByUin(userId)
                .onSuccess { user -> _contact.value = user.toContact(cached, userId) }
                .onFailure { e ->
                    if (cached == null) _error.value = e.message ?: "User not found"
                }

            _commonGroups.value = groupRepository.getGroups()
                .first()
                .filter { group -> group.memberIds.contains(userId) }
        }
    }

    fun openChat(userId: Long, onChat: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.openOrCreateChat(userId)
                .onSuccess { onChat(it) }
                .onFailure { e -> _error.value = e.message ?: "Cannot open chat" }
        }
    }

    fun blockContact(userId: Long) {
        viewModelScope.launch {
            contactRepository.blockContact(userId)
                .onFailure { e -> _error.value = e.message ?: "Cannot block contact" }
        }
    }

    fun removeContact(userId: Long, onRemoved: () -> Unit) {
        viewModelScope.launch {
            contactRepository.removeContact(userId)
                .onSuccess { onRemoved() }
                .onFailure { e -> _error.value = e.message ?: "Cannot remove contact" }
        }
    }
}

private fun User.toContact(cached: Contact?, fallbackUserId: Long): Contact = Contact(
    userId = id.takeIf { it != 0L } ?: fallbackUserId,
    nickname = nickname.ifBlank { cached?.nickname ?: fallbackUserId.toString() },
    avatarUrl = avatarUrl ?: cached?.avatarUrl,
    status = status.takeIf { it != UserStatus.OFFLINE } ?: cached?.status ?: UserStatus.OFFLINE,
    lastSeen = lastSeen ?: cached?.lastSeen,
    isBlocked = isBlocked || (cached?.isBlocked == true),
    isFavorite = cached?.isFavorite ?: isFavorite,
    notificationSound = cached?.notificationSound ?: notificationSound,
    customNickname = cached?.customNickname ?: customNickname,
    statusMessage = statusMessage ?: cached?.statusMessage,
    chatId = cached?.chatId,
    unreadCount = cached?.unreadCount ?: 0,
    lastMessage = cached?.lastMessage
)
