package com.rcq.messenger.ui.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.db.ContactDao
import com.rcq.messenger.data.repository.ChatRepository
import com.rcq.messenger.data.repository.ContactRepository
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.ui.theme.*
import com.rcq.messenger.ui.theme.LocalRetroMode
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.rcq.messenger.di.PreferencesKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val contactDao: ContactDao,
    private val groupRepository: GroupRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val contacts: StateFlow<List<Contact>> = contactRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Only show groups where the current user is a member
    val groups: StateFlow<List<Group>> = combine(
        groupRepository.getGroups(),
        dataStore.data.map { it[PreferencesKeys.USER_UIN] ?: 0L }
    ) { allGroups, ownUin ->
        if (ownUin == 0L) emptyList()
        else allGroups.filter { it.memberIds.contains(ownUin) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequestsCount: StateFlow<Int> = contactRepository.pendingRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _editingContact = MutableStateFlow<Contact?>(null)
    val editingContact: StateFlow<Contact?> = _editingContact.asStateFlow()

    private val _pendingChatId = MutableStateFlow<String?>(null)
    val pendingChatId: StateFlow<String?> = _pendingChatId.asStateFlow()

    private val _openChatError = MutableStateFlow<String?>(null)
    val openChatError: StateFlow<String?> = _openChatError.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            contactRepository.syncContacts()
                .onFailure { android.util.Log.e("ContactsVM", "syncContacts failed: ${it.message}") }
            groupRepository.syncGroups()
                .onFailure { android.util.Log.e("ContactsVM", "syncGroups failed: ${it.message}") }
            _isLoading.value = false
        }
    }

    fun openOrCreateChat(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.openOrCreateChat(userId)
                .onSuccess { chatId -> _pendingChatId.value = chatId }
                .onFailure { _openChatError.value = it.message }
            _isLoading.value = false
        }
    }

    fun consumePendingChatId() { _pendingChatId.value = null }
    fun clearOpenChatError() { _openChatError.value = null }

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

    fun startEditNickname(contact: Contact) { _editingContact.value = contact }
    fun cancelEditNickname() { _editingContact.value = null }
    fun saveCustomNickname(userId: Long, nickname: String) {
        viewModelScope.launch {
            contactDao.updateCustomNickname(userId, nickname.trim().ifEmpty { null })
            _editingContact.value = null
        }
    }
    fun toggleFavorite(userId: Long, currentValue: Boolean) {
        viewModelScope.launch { contactDao.setFavorite(userId, !currentValue) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel(),
    onContactClick: (Long) -> Unit,
    onChatClick: (String) -> Unit = {},
    onGroupClick: (String) -> Unit = {},
    onAddContact: () -> Unit,
    onPendingRequests: () -> Unit = {}
) {
    val contacts by viewModel.contacts.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingCount by viewModel.pendingRequestsCount.collectAsState()
    val editingContact by viewModel.editingContact.collectAsState()
    val pendingChatId by viewModel.pendingChatId.collectAsState()
    val openChatError by viewModel.openChatError.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }

    // Navigate to chat as soon as VM resolves the chatId
    LaunchedEffect(pendingChatId) {
        pendingChatId?.let { chatId ->
            viewModel.consumePendingChatId()
            onChatClick(chatId)
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            (it.customNickname ?: it.nickname).contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(editingContact) {
        nicknameInput = editingContact?.let { it.customNickname ?: it.nickname } ?: ""
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (editingContact != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEditNickname() },
            title = { Text("Edit Nickname") },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = SurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Primary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editingContact?.let { viewModel.saveCustomNickname(it.userId, nicknameInput) }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEditNickname() }) { Text("Cancel") }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(openChatError) {
        openChatError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOpenChatError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary,
                    trackColor = SurfaceVariant
                )
            }
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

            val retroMode = LocalRetroMode.current

            if (filteredContacts.isEmpty() && groups.isEmpty() && !isLoading) {
                EmptyContactsState(onAddContact = onAddContact)
            } else if (retroMode) {
                StatusGroupedContactList(
                    contacts = filteredContacts,
                    groups = groups,
                    onContactClick = { viewModel.openOrCreateChat(it.userId) },
                    onGroupClick = onGroupClick,
                    onToggleFavorite = { viewModel.toggleFavorite(it.userId, it.isFavorite) },
                    onEditNickname = { viewModel.startEditNickname(it) },
                    onBlock = { viewModel.blockContact(it.userId) },
                    onRemove = { viewModel.removeContact(it.userId) },
                )
            } else {
                LazyColumn {
                    if (groups.isNotEmpty()) {
                        item(key = "header_groups") {
                            SectionHeader(title = "Groups (${groups.size})")
                        }
                        items(groups, key = { "group_${it.id}" }) { group ->
                            GroupContactItem(
                                group = group,
                                onClick = { onGroupClick(group.id) }
                            )
                        }
                    }
                    if (filteredContacts.isNotEmpty()) {
                        item(key = "header_contacts") {
                            SectionHeader(title = "Contacts (${filteredContacts.size})")
                        }
                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactItem(
                                contact = contact,
                                onClick = { viewModel.openOrCreateChat(contact.userId) },
                                onToggleFavorite = { viewModel.toggleFavorite(contact.userId, contact.isFavorite) },
                                onEditNickname = { viewModel.startEditNickname(contact) },
                                onBlock = { viewModel.blockContact(contact.userId) },
                                onRemove = { viewModel.removeContact(contact.userId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onEditNickname: () -> Unit = {},
    onBlock: () -> Unit = {},
    onRemove: () -> Unit = {}
) {
    val rcq = LocalRCQColors.current
    val statusColor = when (contact.status) {
        com.rcq.messenger.domain.model.UserStatus.ONLINE -> StatusOnline
        com.rcq.messenger.domain.model.UserStatus.AWAY -> StatusAway
        com.rcq.messenger.domain.model.UserStatus.BUSY, com.rcq.messenger.domain.model.UserStatus.DND -> StatusBusy
        com.rcq.messenger.domain.model.UserStatus.INVISIBLE -> StatusInvisible
        com.rcq.messenger.domain.model.UserStatus.OFFLINE -> StatusOffline
    }
    val statusLabel = when (contact.status) {
        com.rcq.messenger.domain.model.UserStatus.ONLINE -> "Online"
        com.rcq.messenger.domain.model.UserStatus.AWAY -> "Away"
        com.rcq.messenger.domain.model.UserStatus.BUSY -> "Busy"
        com.rcq.messenger.domain.model.UserStatus.DND -> "Do not disturb"
        com.rcq.messenger.domain.model.UserStatus.INVISIBLE -> "Invisible"
        com.rcq.messenger.domain.model.UserStatus.OFFLINE -> "Offline"
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
                .padding(horizontal = RCQMetrics.rowHPad, vertical = RCQMetrics.rowVPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(RCQMetrics.avatarLg)
                        .clip(CircleShape)
                        .background(rcq.bgSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (contact.customNickname ?: contact.nickname).firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = rcq.accent
                    )
                }
                Box(
                    modifier = Modifier
                        .size(RCQMetrics.statusDot)
                        .clip(CircleShape)
                        .background(statusColor)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(RCQMetrics.rowHPad))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.customNickname ?: contact.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = rcq.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.userId.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = RCQFontSize.monoSmall,
                        color = rcq.textMono,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = " · $statusLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = RCQFontSize.monoSmall,
                        color = statusColor
                    )
                }
            }

            if (contact.isFavorite) {
                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = StatusAway, modifier = Modifier.size(18.dp))
            }
        }

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(if (contact.isFavorite) "Unfavorite" else "Favorite") },
                leadingIcon = { Icon(if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = StatusAway) },
                onClick = { menuExpanded = false; onToggleFavorite() }
            )
            DropdownMenuItem(
                text = { Text("Edit nickname") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuExpanded = false; onEditNickname() }
            )
            DropdownMenuItem(
                text = { Text("Block") },
                leadingIcon = { Icon(Icons.Default.Block, null, tint = ColorError) },
                onClick = { menuExpanded = false; onBlock() }
            )
            DropdownMenuItem(
                text = { Text("Remove") },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorError) },
                onClick = { menuExpanded = false; onRemove() }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun GroupContactItem(group: Group, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = Primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "${group.memberCount} members",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

private data class StatusGroup(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val contacts: List<Contact>,
    val defaultExpanded: Boolean = true,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusGroupedContactList(
    contacts: List<Contact>,
    groups: List<com.rcq.messenger.domain.model.Group>,
    onContactClick: (Contact) -> Unit,
    onGroupClick: (String) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onEditNickname: (Contact) -> Unit,
    onBlock: (Contact) -> Unit,
    onRemove: (Contact) -> Unit,
) {
    val grouped = remember(contacts) {
        listOf(
            StatusGroup("Online", StatusOnline,
                contacts.filter { it.status == com.rcq.messenger.domain.model.UserStatus.ONLINE }),
            StatusGroup("Away", StatusAway,
                contacts.filter { it.status == com.rcq.messenger.domain.model.UserStatus.AWAY }),
            StatusGroup("Busy / DND", StatusBusy,
                contacts.filter {
                    it.status == com.rcq.messenger.domain.model.UserStatus.BUSY ||
                    it.status == com.rcq.messenger.domain.model.UserStatus.DND
                }),
            StatusGroup("Invisible", StatusInvisible,
                contacts.filter { it.status == com.rcq.messenger.domain.model.UserStatus.INVISIBLE }),
            StatusGroup("Offline", StatusOffline,
                contacts.filter { it.status == com.rcq.messenger.domain.model.UserStatus.OFFLINE },
                defaultExpanded = false),
        ).filter { it.contacts.isNotEmpty() }
    }

    val expanded = remember(grouped) {
        mutableStateMapOf<String, Boolean>().also { map ->
            grouped.forEach { g -> map[g.label] = g.defaultExpanded }
        }
    }

    LazyColumn {
        if (groups.isNotEmpty()) {
            stickyHeader(key = "hdr_groups") {
                StatusGroupHeader(
                    label = "Groups",
                    count = groups.size,
                    color = Primary,
                    isExpanded = expanded["Groups"] ?: true,
                    onToggle = { expanded["Groups"] = !(expanded["Groups"] ?: true) }
                )
            }
            if (expanded["Groups"] != false) {
                items(groups, key = { "group_${it.id}" }) { group ->
                    GroupContactItem(group = group, onClick = { onGroupClick(group.id) })
                }
            }
        }

        grouped.forEach { group ->
            stickyHeader(key = "hdr_${group.label}") {
                StatusGroupHeader(
                    label = group.label,
                    count = group.contacts.size,
                    color = group.color,
                    isExpanded = expanded[group.label] ?: group.defaultExpanded,
                    onToggle = {
                        val cur = expanded[group.label] ?: group.defaultExpanded
                        expanded[group.label] = !cur
                    }
                )
            }
            if (expanded[group.label] != false) {
                items(group.contacts, key = { "c_${it.id}" }) { contact ->
                    ContactItem(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onToggleFavorite = { onToggleFavorite(contact) },
                        onEditNickname = { onEditNickname(contact) },
                        onBlock = { onBlock(contact) },
                        onRemove = { onRemove(contact) },
                    )
                }
            }
        }
    }
}

@Composable
fun StatusGroupHeader(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val rcq = LocalRCQColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rcq.bgSecondary)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelSmall,
            color = rcq.textSecondary,
            fontSize = 11.sp,
        )
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
