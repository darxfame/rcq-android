package com.rcq.messenger.ui.contacts

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.di.PreferencesKeys
import com.rcq.messenger.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _isOwnerOrAdmin = MutableStateFlow(false)
    val isOwnerOrAdmin: StateFlow<Boolean> = _isOwnerOrAdmin.asStateFlow()

    var ownUin: Long = 0L
        private set

    fun load(groupId: String) {
        viewModelScope.launch {
            ownUin = dataStore.data.first()[PreferencesKeys.USER_UIN] ?: 0L
            groupRepository.getGroup(groupId).onSuccess { group ->
                _group.value = group
                _isOwnerOrAdmin.value = group.adminIds.contains(ownUin) || group.ownerId == ownUin
            }
        }
    }

    fun renameGroup(name: String) {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.updateGroup(group.copy(name = name)).onSuccess { updated ->
                _group.value = updated
            }
        }
    }

    fun setPostPolicy(policy: String) {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.updateGroup(
                group.copy(settings = group.settings.copy(anyoneCanSend = policy == "all"))
            ).onSuccess { updated ->
                _group.value = updated
            }
        }
    }

    fun setPinnedText(text: String) {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.updateGroup(group.copy(pinnedText = text)).onSuccess { updated ->
                _group.value = updated
            }
        }
    }

    fun removeMember(memberUin: Long) {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.removeMember(group.id, memberUin).onSuccess {
                load(group.id)
            }
        }
    }

    fun leaveGroup() {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.removeMember(group.id, ownUin)
        }
    }

    fun deleteGroup() {
        val group = _group.value ?: return
        viewModelScope.launch {
            groupRepository.deleteGroup(group.id)
        }
    }
}
