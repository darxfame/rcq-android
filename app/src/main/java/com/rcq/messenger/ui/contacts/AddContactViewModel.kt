package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.api.ContactRequest
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _contactRequests = MutableStateFlow<List<ContactRequest>>(emptyList())
    val contactRequests: StateFlow<List<ContactRequest>> = _contactRequests.asStateFlow()

    private val _sentRequests = MutableStateFlow<Set<Long>>(emptySet())
    val sentRequests: StateFlow<Set<Long>> = _sentRequests.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadContactRequests()
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        _error.value = null

        // Debounced search
        searchJob?.cancel()
        if (newQuery.length >= 3) {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                if (_query.value == newQuery) {
                    search()
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Try UIN search first if numeric
            val uin = q.toLongOrNull()
            if (uin != null) {
                userRepository.getUserByUin(uin).fold(
                    onSuccess = { user ->
                        _searchResults.value = listOf(user)
                    },
                    onFailure = {
                        // Fall back to nickname search
                        searchByNickname(q)
                    }
                )
            } else {
                searchByNickname(q)
            }

            _isLoading.value = false
        }
    }

    private suspend fun searchByNickname(query: String) {
        userRepository.searchUsers(query).fold(
            onSuccess = { users ->
                _searchResults.value = users
                if (users.isEmpty()) {
                    _error.value = "No users found"
                }
            },
            onFailure = { e ->
                _error.value = "Search failed: ${e.message}"
                _searchResults.value = emptyList()
            }
        )
    }

    private fun loadContactRequests() {
        viewModelScope.launch {
            contactRepository.syncContacts()
            contactRepository.getContactRequests().fold(
                onSuccess = { requests ->
                    _contactRequests.value = requests
                },
                onFailure = {
                    // Use cached requests if available
                    _contactRequests.value = contactRepository.getLocalPendingRequests()
                }
            )
        }
    }

    fun refreshRequests() {
        loadContactRequests()
    }

    fun sendRequest(userId: Long) {
        viewModelScope.launch {
            contactRepository.addContact(userId).fold(
                onSuccess = {
                    _sentRequests.value = _sentRequests.value + userId
                },
                onFailure = { e ->
                    _error.value = "Failed to send request: ${e.message}"
                }
            )
        }
    }

    fun acceptRequest(requestId: Long) {
        viewModelScope.launch {
            contactRepository.acceptContactRequest(requestId).fold(
                onSuccess = {
                    _contactRequests.value = _contactRequests.value.filter { it.id != requestId }
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
                    _contactRequests.value = _contactRequests.value.filter { it.id != requestId }
                },
                onFailure = { e ->
                    _error.value = "Failed to decline: ${e.message}"
                }
            )
        }
    }
}