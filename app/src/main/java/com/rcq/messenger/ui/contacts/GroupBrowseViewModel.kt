package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupBrowseViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _groups = groupRepository.getGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")

    val filteredGroups: StateFlow<List<Group>> = combine(_groups, searchQuery) { groups, query ->
        if (query.isBlank()) groups
        else groups.filter {
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            groupRepository.syncGroups().onFailure { error.value = it.message }
            isLoading.value = false
        }
    }
}
