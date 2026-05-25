package com.rcq.messenger.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val contacts: StateFlow<List<Contact>> = contactRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequestsCount: StateFlow<Int> = contactRepository.pendingRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            contactRepository.syncContacts()
            _isLoading.value = false
        }
    }

    fun removeContact(userId: Long) {
        viewModelScope.launch {
            contactRepository.removeContact(userId)
        }
    }

    fun blockContact(userId: Long) {
        viewModelScope.launch {
            contactRepository.blockContact(userId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onContactClick: (Long) -> Unit,
    onAddContact: () -> Unit,
    onPendingRequests: () -> Unit = {}
) {
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingCount by viewModel.pendingRequestsCount.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter { it.nickname.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contacts",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Pending requests button (like iOS)
                    BadgedBox(
                        badge = {
                            if (pendingCount > 0) {
                                Badge { Text(pendingCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onPendingRequests) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Pending Requests")
                        }
                    }
                    // Add contact button
                    IconButton(onClick = onAddContact) {
                        Icon(Icons.Default.Search, contentDescription = "Add Contact")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = Primary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search contacts...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (filteredContacts.isEmpty() && !isLoading) {
                EmptyContactsState(onAddContact = onAddContact)
            } else {
                LazyColumn {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItem(
                            contact = contact,
                            onClick = { onContactClick(contact.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.nickname.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when (contact.status) {
                            com.rcq.messenger.domain.model.UserStatus.ONLINE -> Online
                            com.rcq.messenger.domain.model.UserStatus.AWAY -> Warning
                            com.rcq.messenger.domain.model.UserStatus.BUSY, com.rcq.messenger.domain.model.UserStatus.DND -> Error
                            com.rcq.messenger.domain.model.UserStatus.INVISIBLE, com.rcq.messenger.domain.model.UserStatus.OFFLINE -> Offline
                        }
                    )
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.customNickname ?: contact.nickname,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = when (contact.status) {
                    com.rcq.messenger.domain.model.UserStatus.ONLINE -> "Online"
                    com.rcq.messenger.domain.model.UserStatus.AWAY -> "Away"
                    com.rcq.messenger.domain.model.UserStatus.BUSY, com.rcq.messenger.domain.model.UserStatus.DND -> "Do not disturb"
                    com.rcq.messenger.domain.model.UserStatus.INVISIBLE, com.rcq.messenger.domain.model.UserStatus.OFFLINE -> "Offline"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        if (contact.isFavorite) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Warning,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyContactsState(onAddContact: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No contacts yet",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add contacts by their RCQ ID",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddContact,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.PersonAdd, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = { },
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select members",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* Create group */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create Group")
            }
        }
    }
}