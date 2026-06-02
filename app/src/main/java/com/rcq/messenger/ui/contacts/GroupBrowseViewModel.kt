package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class GroupBrowseViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _myGroups = groupRepository.getGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")

    private val _searchResults = MutableStateFlow<List<Group>>(emptyList())

    val filteredGroups: StateFlow<List<Group>> = combine(_myGroups, searchQuery, _searchResults) { mine, query, results ->
        if (query.isBlank()) {
            mine
        } else {
            // Partial match anywhere in the name on the user's own/known groups,
            // merged with server-side discovery results. Local first, deduped by id.
            val q = query.trim()
            val localMatches = mine.filter { it.name.contains(q, ignoreCase = true) }
            (localMatches + results).distinctBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        refresh()
        viewModelScope.launch {
            searchQuery
                .debounce(400)
                .collect { query ->
                    if (query.isNotBlank()) searchOnServer(query)
                    else _searchResults.value = emptyList()
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            groupRepository.syncGroups().onFailure { error.value = it.message }
            isLoading.value = false
        }
    }

    private fun searchOnServer(query: String) {
        viewModelScope.launch {
            isLoading.value = true
            groupRepository.searchPublicGroups(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { error.value = it.message }
            isLoading.value = false
        }
    }
}
