package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.domain.model.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _selectedContacts = MutableStateFlow<List<Long>>(emptyList())
    val selectedContacts: StateFlow<List<Long>> = _selectedContacts.asStateFlow()

    private val _availableContacts = MutableStateFlow<List<Contact>>(emptyList())
    val availableContacts: StateFlow<List<Contact>> = _availableContacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun updateGroupName(name: String) {
        _groupName.value = name
    }

    fun toggleContactSelection(contactId: Long) {
        val current = _selectedContacts.value.toMutableList()
        if (current.contains(contactId)) current.remove(contactId)
        else current.add(contactId)
        _selectedContacts.value = current
    }

    fun createGroup() {
        val name = _groupName.value.trim()
        if (name.isEmpty() || _selectedContacts.value.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            groupRepository.createGroup(name, _selectedContacts.value)
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
