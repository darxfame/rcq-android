package com.rcq.messenger.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.media.RecordingState
import com.rcq.messenger.domain.model.MessageStatus
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.ui.chat.components.ReplyPreview
import com.rcq.messenger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val replyTo by viewModel.replyTo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val chatTitle by viewModel.chatTitle.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val activeVoiceId by viewModel.activeVoiceId.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    var showAttachMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chatTitle.firstOrNull()?.uppercase() ?: "?", color = Primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = chatTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Call */ }) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOwnMessage = message.senderId == currentUserId,
                        onReply = { viewModel.setReplyTo(message) },
                        onForward = { viewModel.forwardMessage(message) },
                        onEdit = { viewModel.editMessage(message, message.content) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onReact = { viewModel.addReaction(message.id, "👍") },
                        onVoicePlay = { _ -> viewModel.playVoice(message) },
                        onVoicePause = { viewModel.pauseVoice() },
                        isVoicePlaying = activeVoiceId == message.mediaId,
                        playbackState = playbackState
                    )
                }
            }

            TypingIndicator(isVisible = isTyping)

            replyTo?.let { reply ->
                ReplyPreview(
                    originalMessage = reply.content,
                    originalSender = "User", // TODO: Get actual sender name
                    onDismiss = { viewModel.setReplyTo(null) }
                )
            }

            Box {
                MessageInput(
                    text = messageText,
                    onTextChange = viewModel::updateMessageText,
                    onSend = viewModel::sendMessage,
                    onAttach = { showAttachMenu = true },
                    onVoice = {
                        if (recordingState == RecordingState.RECORDING) {
                            viewModel.stopAndSendVoiceMessage()
                        } else {
                            viewModel.startVoiceRecording()
                        }
                    },
                    isRecording = recordingState == RecordingState.RECORDING
                )
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Photo") },
                        leadingIcon = { Icon(Icons.Default.Image, null) },
                        onClick = { showAttachMenu = false; photoPickerLauncher.launch("image/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("Video") },
                        leadingIcon = { Icon(Icons.Default.VideoLibrary, null) },
                        onClick = { showAttachMenu = false; videoPickerLauncher.launch("video/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("File") },
                        leadingIcon = { Icon(Icons.Default.AttachFile, null) },
                        onClick = { showAttachMenu = false; filePickerLauncher.launch("*/*") }
                    )
                    DropdownMenuItem(
                        text = { Text("Location") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        onClick = { showAttachMenu = false; requestLocationAndSend() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onReply: () -> Unit,
    onForward: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReact: () -> Unit = {},
    onVoicePlay: (String) -> Unit = {},
    onVoicePause: () -> Unit = {},
    isVoicePlaying: Boolean = false,
    playbackState: com.rcq.messenger.media.PlaybackState = com.rcq.messenger.media.PlaybackState.IDLE
) {
    val rcq = LocalRCQColors.current
    val bubbleColor = if (isOwnMessage) rcq.bubbleSelf else rcq.bubbleOther
    val bubbleShape = RoundedCornerShape(
        topStart = RCQMetrics.bubbleRadius,
        topEnd = RCQMetrics.bubbleRadius,
        bottomStart = if (isOwnMessage) RCQMetrics.bubbleRadius else 2.dp,
        bottomEnd = if (isOwnMessage) 2.dp else RCQMetrics.bubbleRadius
    )
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RCQMetrics.screenHPad, vertical = 3.dp)
                .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            if (message.replyToId != null) {
                Row(
                    modifier = Modifier
                        .background(rcq.bgSecondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(2.dp).height(28.dp).background(rcq.accent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.replyToContent ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = rcq.textSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
            ) {
                if (!isOwnMessage) {
                    Box(
                        modifier = Modifier
                            .size(RCQMetrics.avatarSm)
                            .clip(CircleShape)
                            .background(rcq.bgSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", color = rcq.accent, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Column(
                    horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
                ) {
                    val isMedia = message.kind in listOf(
                        MessageKind.PHOTO, MessageKind.VIDEO, MessageKind.VOICE,
                        MessageKind.FILE, MessageKind.PREMIUM_PHOTO, MessageKind.PREMIUM_VIDEO
                    )
                    if (isMedia) {
                        com.rcq.messenger.ui.chat.components.MediaMessageBubble(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onMediaClick = {},
                            onVoicePlay = onVoicePlay,
                            onVoicePause = onVoicePause,
                            playbackState = if (isVoicePlaying) playbackState else com.rcq.messenger.media.PlaybackState.IDLE,
                            modifier = Modifier
                        )
                    } else if (message.kind == MessageKind.LOCATION && message.latitude != null && message.longitude != null) {
                        Column(
                            modifier = Modifier
                                .background(bubbleColor, bubbleShape)
                                .padding(RCQMetrics.bubbleHPad, RCQMetrics.bubbleVPad)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = rcq.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Location", fontWeight = FontWeight.Medium, color = rcq.textPrimary)
                            }
                            Text(
                                "${String.format("%.4f", message.latitude)}, ${String.format("%.4f", message.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = rcq.textSecondary
                            )
                        }
                    } else if (message.deletedForEveryone) {
                        Row(
                            modifier = Modifier
                                .background(rcq.bgSecondary, bubbleShape)
                                .padding(RCQMetrics.bubbleHPad, RCQMetrics.bubbleVPad),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = rcq.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Message deleted",
                                color = rcq.textSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = RCQFontSize.bubble,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(bubbleColor, bubbleShape)
                                .padding(RCQMetrics.bubbleHPad, RCQMetrics.bubbleVPad)
                        ) {
                            Text(
                                text = message.content,
                                color = rcq.textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = RCQFontSize.bubble
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
                    ) {
                        if (message.editedAt != null && !message.deletedForEveryone) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edited",
                                tint = rcq.textSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = RCQFontSize.timestamp,
                            color = rcq.textSecondary
                        )
                        if (isOwnMessage) {
                            Spacer(modifier = Modifier.width(3.dp))
                            MessageStatusIcon(status = message.status)
                        }
                    }
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.AutoMirrored.Filled.Reply, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Reply") } },
                onClick = { showMenu = false; onReply() }
            )
            if (isOwnMessage) {
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Edit") } },
                    onClick = { showMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = ColorError); Spacer(Modifier.width(8.dp)); Text("Delete", color = ColorError) } },
                    onClick = { showMenu = false; onDelete() }
                )
            }
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Forward, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Forward") } },
                onClick = { showMenu = false; onForward() }
            )
            DropdownMenuItem(
                text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AddReaction, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("React") } },
                onClick = { showMenu = false; onReact() }
            )
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
    onVoice: () -> Unit,
    isRecording: Boolean = false
) {
    val rcq = LocalRCQColors.current
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
                onClick = onSend,
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

