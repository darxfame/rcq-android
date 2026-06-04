package com.rcq.messenger.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.media.RecordingState
import com.rcq.messenger.domain.model.MessageStatus
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.ui.chat.components.ReplyPreview
import com.rcq.messenger.ui.common.EmoticonPicker
import com.rcq.messenger.ui.common.AvatarImage
import com.rcq.messenger.ui.common.StatusIndicator
import com.rcq.messenger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onCall: (Long) -> Unit = {},
    onGroupInfo: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val replyTo by viewModel.replyTo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val targetUin by viewModel.targetUin.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val activeVoiceId by viewModel.activeVoiceId.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val pinnedText by viewModel.pinnedText.collectAsState()
    val inChatSearchResults by viewModel.inChatSearchResults.collectAsState()
    val peerStatus by viewModel.peerStatus.collectAsState()
    val chatAvatar by viewModel.chatAvatar.collectAsState()
    val senderNames by viewModel.senderNames.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showInChatSearch by remember { mutableStateOf(false) }
    var showForwardPicker by remember { mutableStateOf<Message?>(null) }
    var reactingMessage by remember { mutableStateOf<Message?>(null) }
    val context = LocalContext.current
    val isGroupChat = remember(chatId) { !chatId.startsWith("direct_") }
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val requestLocationAndSend = {
        if (locationPermissionState.status.isGranted) {
            viewModel.sendLocationMessage(context)
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendPhotoMessage(it) } }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendVideoMessage(it) } }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.sendFileMessage(it) } }

    LaunchedEffect(chatId) {
        viewModel.loadChat(chatId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (isGroupChat) Modifier.clickable { onGroupInfo() } else Modifier
                    ) {
                        Box(modifier = Modifier.size(36.dp)) {
                            AvatarImage(
                                avatarUrl = chatAvatar,
                                displayName = chatTitle,
                                size = 36.dp
                            )
                            if (!isGroupChat && peerStatus != UserStatus.OFFLINE) {
                                StatusIndicator(
                                    status = peerStatus,
                                    size = 10,
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                chatTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LocalRCQColors.current.textPrimary,
                                maxLines = 1
                            )
                            val subtitle = when {
                                isGroupChat -> "${viewModel.memberCount} members"
                                peerStatus == UserStatus.ONLINE -> "online"
                                peerStatus == UserStatus.AWAY -> "away"
                                else -> "offline"
                            }
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalRCQColors.current.textSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onCall(targetUin) }) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            if (isGroupChat) {
                                DropdownMenuItem(
                                    text = { Text("Group info") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = { showMoreMenu = false; onGroupInfo() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Search in chat") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                onClick = { showMoreMenu = false; showInChatSearch = true }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute" else "Mute") },
                                leadingIcon = {
                                    Icon(
                                        if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        null
                                    )
                                },
                                onClick = { showMoreMenu = false; viewModel.toggleMute() }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear history") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { showMoreMenu = false; viewModel.clearHistory() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalRCQColors.current.bgPrimary,
                    titleContentColor = LocalRCQColors.current.textPrimary
                )
            )
        },
        containerColor = LocalRCQColors.current.bgPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            if (showInChatSearch) {
                var query by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LocalRCQColors.current.bgSecondary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                viewModel.searchInChat(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp),
                            placeholder = { Text("Search messages...") },
                            singleLine = true
                        )
                        IconButton(onClick = {
                            showInChatSearch = false
                            viewModel.clearInChatSearch()
                        }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                    if (inChatSearchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                            items(inChatSearchResults) { msg ->
                                Text(
                                    text = msg.content,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { }
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (!pinnedText.isNullOrBlank() && isGroupChat) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LocalRCQColors.current.bgSecondary,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            null,
                            tint = LocalRCQColors.current.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            pinnedText.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalRCQColors.current.textSecondary
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.kind == MessageKind.SYSTEM_NOTICE) {
                        RetroSystemMessage(text = message.content)
                    } else {
                        MessageBubble(
                            message = message,
                            isOwnMessage = message.senderId == currentUserId,
                            senderName = senderNames[message.senderId] ?: message.senderId.toString(),
                            showSenderName = isGroupChat && message.senderId != currentUserId,
                            onReply = { viewModel.setReplyTo(message) },
                            onForward = { showForwardPicker = message },
                            onEdit = { viewModel.editMessage(message, message.content) },
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onReact = { reactingMessage = message },
                            onVoicePlay = { _ -> viewModel.playVoice(message) },
                            onVoicePause = { viewModel.pauseVoice() },
                            isVoicePlaying = activeVoiceId == message.mediaId,
                            playbackState = playbackState
                        )
                    }
                }
            }

            reactingMessage?.let { msg ->
                ReactionPickerDialog(
                    onDismiss = { reactingMessage = null },
                    onReact = { emoji ->
                        viewModel.addReaction(msg.id, emoji)
                        reactingMessage = null
                    }
                )
            }

            showForwardPicker?.let { msg ->
                ForwardPickerDialog(
                    onDismiss = { showForwardPicker = null },
                    onPick = { targetChatId ->
                        viewModel.forwardMessageTo(msg, targetChatId)
                        showForwardPicker = null
                    },
                    contacts = contacts,
                    groups = groups
                )
            }

            TypingIndicator(isVisible = isTyping)

            replyTo?.let { reply ->
                ReplyPreview(
                    originalMessage = reply.content,
                    originalSender = if (reply.isFromMe) {
                        "Me"
                    } else {
                        senderNames[reply.senderId] ?: reply.senderId.toString()
                    },
                    onDismiss = { viewModel.setReplyTo(null) }
                )
            }

            Box {
                Column {
                    if (showEmojiPicker && recordingState != RecordingState.RECORDING) {
                        EmoticonPicker(
                            onEmoticonSelected = { token ->
                                viewModel.updateMessageText(messageText + token)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MessageInput(
                        text = messageText,
                        onTextChange = viewModel::updateMessageText,
                        onSend = viewModel::sendMessage,
                        onAttach = { showAttachMenu = true },
                        onEmoji = { showEmojiPicker = !showEmojiPicker },
                        onVoice = {
                            if (recordingState == RecordingState.RECORDING) {
                                viewModel.stopAndSendVoiceMessage()
                            } else {
                                showEmojiPicker = false
                                viewModel.startVoiceRecording()
                            }
                        },
                        isRecording = recordingState == RecordingState.RECORDING
                    )
                }
                if (showAttachMenu) {
                    AttachmentSheet(
                        onDismiss = { showAttachMenu = false },
                        onPhoto = { photoPickerLauncher.launch("image/*") },
                        onVideo = { videoPickerLauncher.launch("video/*") },
                        onFile = { filePickerLauncher.launch("*/*") },
                        onLocation = requestLocationAndSend
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionPickerDialog(
    onDismiss: () -> Unit,
    onReact: (String) -> Unit
) {
    val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "😡", "🔥", "👏", "🎉", "💯")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React") },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reactions) { emoji ->
                    Text(
                        emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.clickable { onReact(emoji) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ForwardPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    contacts: List<Contact>,
    groups: List<Group>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to...") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (contacts.isNotEmpty()) {
                    item {
                        Text(
                            "Contacts",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(contacts) { contact ->
                        ListItem(
                            headlineContent = { Text(contact.nickname) },
                            modifier = Modifier.clickable { onPick("direct_${contact.userId}") }
                        )
                    }
                }
                if (groups.isNotEmpty()) {
                    item {
                        Text(
                            "Groups",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(groups) { group ->
                        ListItem(
                            headlineContent = { Text(group.name) },
                            modifier = Modifier.clickable { onPick(group.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(
    onDismiss: () -> Unit,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onFile: () -> Unit,
    onLocation: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "Attach",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = LocalRCQColors.current.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AttachmentAction(
                    icon = Icons.Default.Image,
                    label = "Photo",
                    onClick = { onDismiss(); onPhoto() }
                )
                AttachmentAction(
                    icon = Icons.Default.VideoLibrary,
                    label = "Video",
                    onClick = { onDismiss(); onVideo() }
                )
                AttachmentAction(
                    icon = Icons.Default.AttachFile,
                    label = "File",
                    onClick = { onDismiss(); onFile() }
                )
                AttachmentAction(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    onClick = { onDismiss(); onLocation() }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AttachmentAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val rcq = LocalRCQColors.current
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(rcq.bgSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = rcq.accent)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = rcq.textSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    senderName: String,
    showSenderName: Boolean = false,
    onReply: () -> Unit,
    onForward: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReact: () -> Unit = {},
    onVoicePlay: (String) -> Unit = {},
    onVoicePause: () -> Unit = {},
    isVoicePlaying: Boolean = false,
    playbackState: Any? = null
) {
    val rcq = LocalRCQColors.current
    var showMenu by remember { mutableStateOf(false) }
    val bubbleColor = if (isOwnMessage) rcq.bubbleSelf else rcq.bubbleOther
    val textColor = rcq.textPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOwnMessage) 64.dp else 8.dp,
                end = if (isOwnMessage) 8.dp else 64.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        contentAlignment = if (isOwnMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            if (showSenderName) {
                Text(
                    senderName,
                    color = rcq.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                )
            }
            Box {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isOwnMessage) 18.dp else 4.dp,
                        topEnd = if (isOwnMessage) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    color = bubbleColor,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                        message.replyToId?.let {
                            Surface(
                                color = rcq.accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Text(
                                    message.replyToContent?.takeIf { it.isNotBlank() } ?: "Reply",
                                    color = rcq.accent,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(6.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        when {
                            message.deletedForEveryone -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = rcq.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Message deleted",
                                        color = rcq.textSecondary,
                                        fontSize = RCQFontSize.bubble,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                            message.kind == MessageKind.LOCATION && message.latitude != null && message.longitude != null -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null,
                                        tint = rcq.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Location", fontWeight = FontWeight.Medium, color = textColor)
                                }
                                Text(
                                    "${String.format(Locale.getDefault(), "%.4f", message.latitude)}, ${
                                        String.format(Locale.getDefault(), "%.4f", message.longitude)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = rcq.textSecondary
                                )
                            }
                            message.kind == MessageKind.VOICE -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isVoicePlaying) onVoicePause()
                                            else onVoicePlay(message.mediaId ?: message.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (isVoicePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = rcq.accent
                                        )
                                    }
                                    Text("Voice message", color = textColor, fontSize = RCQFontSize.bubble)
                                }
                            }
                            message.kind != MessageKind.TEXT && message.content.isBlank() -> {
                                val label = when (message.kind) {
                                    MessageKind.PHOTO, MessageKind.PREMIUM_PHOTO -> "Photo"
                                    MessageKind.VIDEO, MessageKind.PREMIUM_VIDEO -> "Video"
                                    MessageKind.FILE -> message.fileName ?: "File"
                                    MessageKind.POLL -> "Poll"
                                    else -> message.kind.name.lowercase().replaceFirstChar { it.uppercase() }
                                }
                                Text(
                                    text = label,
                                    color = textColor,
                                    fontSize = RCQFontSize.bubble,
                                    lineHeight = 20.sp
                                )
                            }
                            else -> {
                                Text(
                                    text = message.content,
                                    color = textColor,
                                    fontSize = RCQFontSize.bubble,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        val reactionCounts = message.reactions.values.groupingBy { it }.eachCount()
                        if (reactionCounts.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                reactionCounts.entries.take(5).forEach { (emoji, count) ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = rcq.bgSecondary,
                                        modifier = Modifier.clickable { onReact() }
                                    ) {
                                        Text(
                                            "$emoji $count",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 12.sp,
                                            color = rcq.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        ) {
                            val timeStr = remember(message.timestamp) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                            }
                            Text(
                                timeStr,
                                color = rcq.textSecondary,
                                fontSize = RCQFontSize.timestamp
                            )
                            if (isOwnMessage) {
                                Spacer(Modifier.width(4.dp))
                                val (tickText, tickColor) = when (message.status) {
                                    MessageStatus.READ -> "✓✓" to rcq.accent
                                    MessageStatus.DELIVERED -> "✓✓" to rcq.textSecondary
                                    else -> "✓" to rcq.textSecondary
                                }
                                Text(
                                    tickText,
                                    color = tickColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reply") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null) },
                        onClick = { showMenu = false; onReply() }
                    )
                    DropdownMenuItem(
                        text = { Text("Forward") },
                        leadingIcon = { Icon(Icons.Default.Forward, null) },
                        onClick = { showMenu = false; onForward() }
                    )
                    DropdownMenuItem(
                        text = { Text("React") },
                        leadingIcon = { Text("😀") },
                        onClick = { showMenu = false; onReact() }
                    )
                    if (isOwnMessage) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = ColorError) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorError) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val rcq = LocalRCQColors.current
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to rcq.textSecondary
        MessageStatus.SENT -> Icons.Default.Check to rcq.textSecondary
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to rcq.textSecondary
        MessageStatus.READ -> Icons.Default.DoneAll to rcq.accent
        MessageStatus.FAILED -> Icons.Default.Error to ColorError
    }
    Icon(imageVector = icon, contentDescription = status.name, tint = tint, modifier = Modifier.size(12.dp))
}

@Composable
fun TypingIndicator(isVisible: Boolean, userName: String = "User") {
    val rcq = LocalRCQColors.current
    if (isVisible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RCQMetrics.screenHPad, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(RCQMetrics.avatarSm).clip(CircleShape).background(rcq.bgSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = rcq.accent, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(rcq.bubbleOther, RoundedCornerShape(RCQMetrics.bubbleRadius))
                    .padding(RCQMetrics.bubbleHPad, RCQMetrics.bubbleVPad)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$userName печатает", style = MaterialTheme.typography.bodySmall, color = rcq.textSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Row {
                        repeat(3) { index ->
                            val alpha by animateFloatAsState(
                                targetValue = if ((System.currentTimeMillis() / 500) % 3 == index.toLong()) 1f else 0.3f,
                                animationSpec = tween(500),
                                label = "typing_dot_$index"
                            )
                            Box(modifier = Modifier.size(4.dp).background(rcq.textSecondary.copy(alpha = alpha), CircleShape))
                            if (index < 2) Spacer(modifier = Modifier.width(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onEmoji: () -> Unit,
    onVoice: () -> Unit,
    isRecording: Boolean = false
) {
    val rcq = LocalRCQColors.current
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isRecording) ColorError.copy(alpha = 0.08f) else rcq.bgSecondary)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isRecording) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.Add, "Attach", tint = rcq.textSecondary)
            }
            IconButton(onClick = onEmoji) {
                Icon(Icons.Default.InsertEmoticon, "Emoji", tint = rcq.textSecondary)
            }
        }

        TextField(
            value = if (isRecording) "Recording..." else text,
            onValueChange = if (isRecording) { _ -> } else onTextChange,
            modifier = Modifier.weight(1f),
            enabled = !isRecording,
            placeholder = { Text("Message...", color = rcq.textSecondary) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = rcq.inputBg,
                unfocusedContainerColor = rcq.inputBg,
                disabledContainerColor = rcq.inputBg,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedTextColor = rcq.textPrimary,
                unfocusedTextColor = rcq.textPrimary,
                disabledTextColor = rcq.textSecondary
            ),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.width(6.dp))

        if (text.isNotBlank() && !isRecording) {
            IconButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSend() },
                modifier = Modifier.size(44.dp).background(rcq.accent, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = androidx.compose.ui.graphics.Color.White)
            }
        } else {
            IconButton(
                onClick = onVoice,
                modifier = Modifier.size(44.dp).background(
                    if (isRecording) ColorError else androidx.compose.ui.graphics.Color.Transparent, CircleShape
                )
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    if (isRecording) "Stop recording" else "Voice",
                    tint = if (isRecording) androidx.compose.ui.graphics.Color.White else rcq.textSecondary
                )
            }
        }
    }
}

@Composable
fun RetroSystemMessage(text: String) {
    val rcq = LocalRCQColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = rcq.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
