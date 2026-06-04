package com.rcq.messenger.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.ui.chat.inbox.InboxRow
import com.rcq.messenger.ui.chat.inbox.InboxSearchResults
import com.rcq.messenger.ui.chat.inbox.InboxTarget
import com.rcq.messenger.ui.common.AvatarImage
import com.rcq.messenger.ui.common.StatusIndicator
import com.rcq.messenger.ui.theme.*

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
    val archivedCount by viewModel.archivedCount.collectAsState()

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
                    itemsIndexed(inboxState.rows, key = { _, it -> it.id }) { idx, row ->
                        InboxItem(row = row, onClick = { openRow(row) })
                        if (idx < inboxState.rows.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                thickness = 0.5.dp,
                                color = rcq.divider
                            )
                        }
                    }
                    if (archivedCount > 0) {
                        item(key = "archive") {
                            TextButton(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = rcq.textSecondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Archive ($archivedCount)", color = rcq.textSecondary)
                            }
                        }
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
private fun InboxItem(row: InboxRow, onClick: () -> Unit) {
    val rcq = LocalRCQColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp)) {
            AvatarImage(
                avatarUrl = row.avatarUrl,
                displayName = row.title,
                size = 44.dp
            )
            if (row.status != null) {
                StatusIndicator(
                    status = row.status,
                    size = 12,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = rcq.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.preview ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = rcq.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = row.timestamp ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = if ((row.unreadCount ?: 0) > 0) rcq.accent else rcq.textSecondary
            )
            if ((row.unreadCount ?: 0) > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(rcq.accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if ((row.unreadCount ?: 0) > 99) "99+" else row.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
