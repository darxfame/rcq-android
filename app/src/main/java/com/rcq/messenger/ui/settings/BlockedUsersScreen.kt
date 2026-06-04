package com.rcq.messenger.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {
    val blockedUsers = contactRepository.getBlockedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unblock(userId: Long) {
        viewModelScope.launch { contactRepository.unblockContact(userId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBack: () -> Unit,
    vm: BlockedUsersViewModel = hiltViewModel()
) {
    val users by vm.blockedUsers.collectAsState()
    val rcq = LocalRCQColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users", color = rcq.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = rcq.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
            )
        },
        containerColor = rcq.bgPrimary
    ) { padding ->
        if (users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No blocked users", color = rcq.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(users, key = { it.userId }) { user ->
                    BlockedUserRow(user = user, onUnblock = { vm.unblock(user.userId) })
                    HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)
                }
            }
        }
    }
}

@Composable
private fun BlockedUserRow(user: Contact, onUnblock: () -> Unit) {
    val rcq = LocalRCQColors.current
    val displayName = user.customNickname?.takeIf { it.isNotBlank() }
        ?: user.nickname.takeIf { it.isNotBlank() }
        ?: user.userId.toString()
    ListItem(
        headlineContent = { Text(displayName, color = rcq.textPrimary) },
        supportingContent = { Text("UIN: ${user.userId}", color = rcq.textSecondary) },
        trailingContent = {
            TextButton(onClick = onUnblock) {
                Text("Unblock", color = rcq.accent)
            }
        },
        colors = ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
}
