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
import com.rcq.messenger.domain.model.Chat
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
    onBrowseGroups: () -> Unit = {}
) {
    val chats by viewModel.chats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val filteredChats by remember(chats, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) chats
            else chats.filter { chat ->
                chat.targetNickname.contains(searchQuery, ignoreCase = true) ||
                    (chat.lastMessage?.content?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }

    val rcq = LocalRCQColors.current
    Scaffold(
        topBar = {
            if (searchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            placeholder = { Text("Search chats…") },
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
                            searchQuery = ""
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
            if (filteredChats.isEmpty() && !isLoading) {
                EmptyChatsState()
            } else {
                LazyColumn {
                    items(filteredChats, key = { it.id }) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = { onChatClick(chat.id) }
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    val rcq = LocalRCQColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = chat.targetNickname.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = rcq.accent
                )
            }
            if (!chat.isMuted) {
                Box(
                    modifier = Modifier
                        .size(RCQMetrics.statusDot)
                        .clip(CircleShape)
                        .background(if (chat.isPinned) rcq.accent else StatusOnline)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(RCQMetrics.rowHPad))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.targetNickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = rcq.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                chat.lastMessage?.let {
                    Text(
                        text = formatTime(it.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = RCQFontSize.timestamp,
                        color = rcq.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                chat.lastMessage?.let { message ->
                    Icon(
                        imageVector = when (message.kind) {
                            com.rcq.messenger.domain.model.MessageKind.PHOTO -> Icons.Default.Image
                            com.rcq.messenger.domain.model.MessageKind.VOICE -> Icons.Default.Mic
                            else -> Icons.Default.ChatBubbleOutline
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = rcq.textSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = RCQFontSize.caption,
                        color = rcq.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (chat.unreadCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(rcq.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = RCQFontSize.monoSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
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