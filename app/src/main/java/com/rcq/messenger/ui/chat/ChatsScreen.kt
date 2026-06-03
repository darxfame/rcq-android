package com.rcq.messenger.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.ui.chat.inbox.InboxRow
import com.rcq.messenger.ui.chat.inbox.InboxSearchResults
import com.rcq.messenger.ui.chat.inbox.InboxTarget
import com.rcq.messenger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel = hiltViewModel(),
    onChatClick: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onNewDirectMessage: () -> Unit = {},
    onBrowseGroups: () -> Unit = {},
    onGroupClick: (String) -> Unit = onChatClick,
) {
    val inboxState by viewModel.inboxState.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val searchQuery = inboxState.searchQuery

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }

    val openRow: (InboxRow) -> Unit = { row ->
        when (val target = row.target) {
            is InboxTarget.Chat -> onChatClick(target.chatId)
            is InboxTarget.Contact -> viewModel.openContactChat(target.userId, onChatClick)
            is InboxTarget.Group -> onGroupClick(target.groupId)
        }
    }

    val rcq = LocalRCQColors.current
    Scaffold(
        topBar = {
            if (searchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search chats, contacts, groups…") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            searchActive = false
                            viewModel.updateSearchQuery("")
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                        text = "Chats",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "New Chat")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New Message") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = { menuExpanded = false; onNewDirectMessage() }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Group") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    onClick = { menuExpanded = false; onCreateGroup() }
                                )
                                DropdownMenuItem(
                                    text = { Text("Browse Groups") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    onClick = { menuExpanded = false; onBrowseGroups() }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = rcq.bgPrimary,
                        titleContentColor = rcq.textPrimary,
                        actionIconContentColor = rcq.accent
                    )
                )
            }
        },
        containerColor = rcq.bgPrimary
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchActive && searchQuery.isNotBlank()) {
                InboxSearchList(
                    results = inboxState.searchResults,
                    onRowClick = openRow
                )
            } else if (inboxState.showEmptyState) {
                EmptyChatsState()
            } else {
                LazyColumn {
                    items(inboxState.rows, key = { it.id }) { row ->
                        InboxItem(row = row, onClick = { openRow(row) })
                    }
                }
            }

            if (inboxState.isLoading && inboxState.rows.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun InboxSearchList(
    results: InboxSearchResults,
    onRowClick: (InboxRow) -> Unit
) {
    if (results.isEmpty) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Nothing found", color = LocalRCQColors.current.textSecondary)
        }
        return
    }
    LazyColumn {
        if (results.chats.isNotEmpty()) {
            item(key = "search_chats_header") { InboxSectionHeader("Chats") }
            items(results.chats, key = { "search_${it.id}" }) { row ->
                InboxItem(row = row, onClick = { onRowClick(row) })
            }
        }
        if (results.contacts.isNotEmpty()) {
            item(key = "search_contacts_header") { InboxSectionHeader("Contacts") }
            items(results.contacts, key = { "search_${it.id}" }) { row ->
                InboxItem(row = row, onClick = { onRowClick(row) })
            }
        }
        if (results.groups.isNotEmpty()) {
            item(key = "search_groups_header") { InboxSectionHeader("Groups") }
            items(results.groups, key = { "search_${it.id}" }) { row ->
                InboxItem(row = row, onClick = { onRowClick(row) })
            }
        }
    }
}

@Composable
private fun InboxSectionHeader(title: String) {
    val rcq = LocalRCQColors.current
    Text(
        text = title.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .background(rcq.bgSecondary.copy(alpha = 0.7f))
            .padding(horizontal = RCQMetrics.rowHPad, vertical = 6.dp),
        fontSize = RCQFontSize.caption,
        fontWeight = FontWeight.Bold,
        color = rcq.textSecondary
    )
}

@Composable
fun InboxItem(
    row: InboxRow,
    onClick: () -> Unit
) {
    val rcq = LocalRCQColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = RCQMetrics.rowHPad, vertical = RCQMetrics.rowVPad),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // JIMM-style: status dot at left edge
            Box(
                modifier = Modifier
                    .size(RCQMetrics.statusDot)
                    .clip(CircleShape)
                    .background(if (row.isMuted) rcq.textSecondary else StatusOnline)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Square mini-avatar
            Box(
                modifier = Modifier
                    .size(RCQMetrics.avatarLg)
                    .clip(RoundedCornerShape(3.dp))
                    .background(rcq.bgSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.title.firstOrNull()?.uppercase() ?: "?",
                    fontSize = RCQFontSize.nickname,
                    fontWeight = FontWeight.Bold,
                    color = rcq.accent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        row.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = RCQFontSize.nickname,
                        color = rcq.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        row.timestamp?.let {
                            Text(formatTime(it), fontSize = RCQFontSize.timestamp, color = rcq.textSecondary)
                        }
                        if (row.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "(${if (row.unreadCount > 99) "99+" else row.unreadCount})",
                                fontSize = RCQFontSize.monoSmall,
                                color = rcq.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    row.subtitle,
                    fontSize = RCQFontSize.caption,
                    color = rcq.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(thickness = RCQMetrics.dividerThick, color = rcq.divider, modifier = Modifier.padding(start = 28.dp))
    }
}

@Composable
fun EmptyChatsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No chats yet",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start a new conversation or create a group",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
