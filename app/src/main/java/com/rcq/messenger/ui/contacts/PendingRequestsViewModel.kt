package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.api.ContactRequest
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingRequestsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val webSocketService: WebSocketService
) : ViewModel() {

    private val _requests = MutableStateFlow<List<ContactRequest>>(emptyList())
    val requests: StateFlow<List<ContactRequest>> = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRequests()
        connectWebSocket()
        listenForContactRequests()
    }

    private fun connectWebSocket() {
        webSocketService.connect()
    }

    private fun listenForContactRequests() {
        webSocketService.contactRequests
            .onEach { wsRequest ->
                // Add new request from WebSocket
                val newRequest = ContactRequest(
                    id = wsRequest.id,
                    fromUin = wsRequest.fromUin,
                    nickname = wsRequest.nickname,
                    avatarUrl = null,
                    status = "pending"
                )
                _requests.value = _requests.value + newRequest
            }
            .launchIn(viewModelScope)
    }

    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // First sync contacts to get pending requests
            contactRepository.syncContacts()

            // Then get pending requests
            contactRepository.getContactRequests().fold(
                onSuccess = { reqs ->
                    _requests.value = reqs
                },
                onFailure = { e ->
                    _error.value = e.message
                    // Fall back to cached
                    _requests.value = contactRepository.getLocalPendingRequests()
                }
            )

            _isLoading.value = false
        }
    }

    fun acceptRequest(requestId: Long) {
        viewModelScope.launch {
            contactRepository.acceptContactRequest(requestId).fold(
                onSuccess = {
                    _requests.value = _requests.value.filter { it.id != requestId }
                },
                onFailure = { e ->
                    _error.value = "Failed to accept: ${e.message}"
                }
            )
        }
    }

    fun declineRequest(requestId: Long) {
        viewModelScope.launch {
            contactRepository.declineContactRequest(requestId).fold(
                onSuccess = {
                    _requests.value = _requests.value.filter { it.id != requestId }
                },
                onFailure = { e ->
                    _error.value = "Failed to decline: ${e.message}"
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketService.disconnect()
    }
}