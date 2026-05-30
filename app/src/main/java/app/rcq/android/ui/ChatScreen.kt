package app.rcq.android.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.rcq.android.R
import app.rcq.android.Session
import app.rcq.android.crypto.Reply
import app.rcq.android.media.VoiceRecorder
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.DeliveryState
import app.rcq.android.model.UserStatus
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a chat thread is pointed at — a 1:1 peer or a group. */
sealed interface ChatTarget {
    data class Peer(val uin: Int) : ChatTarget
    data class Group(val id: Int) : ChatTarget
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatScreen(session: Session, target: ChatTarget, onBack: () -> Unit, onOpenGroupInfo: (Int) -> Unit = {}, onOpenPeerInfo: (Int) -> Unit = {}) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ownUin = session.uin ?: 0

    val isGroup = target is ChatTarget.Group
    val groupId = (target as? ChatTarget.Group)?.id
    val peer = (target as? ChatTarget.Peer)?.uin

    val peerAll by session.messages.collectAsState()
    val groupAll by session.groupMessages.collectAsState()
    val contacts by session.contacts.collectAsState()
    val groups by session.groups.collectAsState()
    val typingFrom by session.typingFrom.collectAsState()

    val messages = if (isGroup) groupAll[groupId] ?: emptyList() else peerAll[peer] ?: emptyList()
    val peerContact = peer?.let { p -> contacts.firstOrNull { it.uin == p } }
    val group = groupId?.let { gid -> groups.firstOrNull { it.id == gid } }
    val canPost = group?.canPost(ownUin) ?: true
    val isTyping = !isGroup && typingFrom == peer

    var draft by remember { mutableStateOf("") }
    var actionMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var editMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var attachMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    fun authorName(m: ChatMessage): String = when {
        m.fromMe -> "You"
        isGroup -> group?.memberName(m.senderUin ?: 0) ?: "${m.senderUin}"
        else -> session.contactName(peer ?: 0)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val jpeg = withContext(Dispatchers.IO) { compressImage(context, uri) }
            if (jpeg != null) runCatching {
                if (isGroup) session.sendGroupPhoto(groupId!!, jpeg, null) else session.sendPhoto(peer!!, jpeg, null)
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val picked = withContext(Dispatchers.IO) { readPickedFile(context, uri) }
            if (picked != null) runCatching {
                if (isGroup) session.sendGroupFile(groupId!!, picked.bytes, picked.name, picked.mime)
                else session.sendFile(peer!!, picked.bytes, picked.name, picked.mime)
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val v = withContext(Dispatchers.IO) { readPickedVideo(context, uri) }
            if (v != null) runCatching {
                if (isGroup) session.sendGroupVideo(groupId!!, v.bytes, v.thumbB64, v.durationSec, null)
                else session.sendVideo(peer!!, v.bytes, v.thumbB64, v.durationSec, null)
            }
        }
    }

    // ── share location ───────────────────────────────────────────────
    fun doShareLocation() {
        scope.launch {
            val loc = withContext(Dispatchers.IO) { currentLocation(context) } ?: return@launch
            runCatching {
                if (isGroup) session.sendGroupLocation(groupId!!, loc.first, loc.second, null)
                else session.sendLocation(peer!!, loc.first, loc.second, null)
            }
        }
    }
    val locPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) doShareLocation()
    }
    fun shareLocation() {
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (ok) doShareLocation() else locPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ── voice recording ──────────────────────────────────────────────
    val recorder = remember { VoiceRecorder(context.cacheDir) }
    var recording by remember { mutableStateOf(false) }
    var recElapsed by remember { mutableStateOf(0) }
    LaunchedEffect(recording) {
        if (recording) { recElapsed = 0; while (true) { delay(1000); recElapsed++ } }
    }
    DisposableEffect(Unit) { onDispose { recorder.cancel() } }
    fun startRecording() { if (runCatching { recorder.start() }.isSuccess) recording = true }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
    }
    fun onMic() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
    fun stopAndSendVoice() {
        recording = false
        val res = recorder.stop() ?: return
        scope.launch {
            runCatching {
                if (isGroup) session.sendGroupVoice(groupId!!, res.first, res.second)
                else session.sendVoice(peer!!, res.first, res.second)
            }
        }
    }
    fun cancelRecording() { recording = false; recorder.cancel() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    // Mark this thread active+read while open; clear again on a new
    // message arriving here is handled in Session.bumpUnreadIfInbound.
    val thisThread = if (isGroup) app.rcq.android.data.LocalStores.groupThread(groupId!!) else app.rcq.android.data.LocalStores.peerThread(peer!!)
    DisposableEffect(target) {
        session.openThread(thisThread)
        if (!isGroup && peer != null) session.sendReadReceipts(peer)
        onDispose {
            session.closeThread()
            if (!isGroup && peer != null) session.sendTyping(peer, false)
        }
    }
    // A message can land while the chat is already open — re-clear so the
    // badge never lingers after the user has seen it.
    LaunchedEffect(messages.size) { app.rcq.android.data.LocalStores.clearUnread(thisThread) }

    Column(Modifier.fillMaxSize().background(c.bgPrimary).imePadding()) {
        // Header.
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary.copy(alpha = 0.6f))
                .clickable { if (isGroup) groupId?.let(onOpenGroupInfo) else peer?.let(onOpenPeerInfo) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onBack),
            )
            Spacer(Modifier.width(6.dp))
            if (isGroup) {
                GroupAvatar(group, session, 28.dp)
            } else {
                StatusIcon(peerContact?.presence ?: UserStatus.OFFLINE, size = 26.dp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                val title = if (isGroup) (group?.name ?: stringResource(R.string.chat_group)) else session.contactName(peer ?: 0)
                Text(title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    isGroup -> {
                        val n = group?.members?.size ?: 0
                        pluralStringResource(R.plurals.members, n, n)
                    }
                    isTyping -> stringResource(R.string.chat_typing)
                    peerContact == null -> "$peer"
                    peerContact.presence == UserStatus.OFFLINE && peerContact.lastSeen != null -> stringResource(R.string.last_seen_fmt, relativeLastSeen(peerContact.lastSeen, context))
                    else -> stringResource(peerContact.presence.labelRes).lowercase()
                }
                Text(sub, color = if (isTyping) c.accent else c.textSecondary, fontSize = 12.sp)
            }
            // Search within this thread (own click consumes the tap so the
            // header's open-info click doesn't also fire).
            Icon(
                Icons.Filled.Search, stringResource(R.string.chat_search_hint), tint = c.accent,
                modifier = Modifier.size(24.dp).clip(CircleShape).clickable { showSearch = true },
            )
        }

        // Pinned banner (groups).
        group?.pinnedText?.takeIf { it.isNotBlank() }?.let { pin ->
            Row(Modifier.fillMaxWidth().background(c.bgSecondary).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.PushPin, null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                Text(pin, color = c.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages, key = { it.id }) { m ->
                MessageBubble(
                    session, m,
                    senderName = if (isGroup && !m.fromMe) authorName(m) else null,
                    onRetry = { scope.launch { runCatching { session.resend(m) } } },
                    onLongPress = { actionMsg = m },
                )
            }
        }

        replyTarget?.let { rt ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
                Box(Modifier.width(3.dp).height(34.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(authorName(rt), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(previewOf(rt, context), color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Filled.Close, stringResource(R.string.chat_cancel_reply), tint = c.textSecondary, modifier = Modifier.clickable { replyTarget = null }.padding(8.dp).size(18.dp))
            }
        }

        if (!canPost) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.chat_owner_only), color = c.textSecondary, fontSize = 13.sp)
            }
        } else {
            Composer(
                draft = draft,
                accentColor = c.accent,
                onDraftChange = {
                    draft = it
                    if (!isGroup && peer != null) session.sendTyping(peer, it.isNotBlank())
                },
                onAttach = { attachMenu = true },
                onSend = {
                    val body = draft.trim(); draft = ""
                    val reply = replyTarget?.let { Reply(it.id, previewOf(it, context), authorName(it)) }
                    replyTarget = null
                    if (!isGroup && peer != null) session.sendTyping(peer, false)
                    scope.launch {
                        runCatching {
                            if (isGroup) session.sendGroupText(groupId!!, body, reply)
                            else session.sendText(peer!!, body, reply)
                        }
                    }
                },
                recording = recording,
                recElapsed = recElapsed,
                onMic = { onMic() },
                onStopVoice = { stopAndSendVoice() },
                onCancelVoice = { cancelRecording() },
            )
        }
    }

    actionMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { actionMsg = null },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(if (m.kind == "photo") R.string.chat_a_photo else R.string.chat_a_message), color = c.textPrimary) },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "🔥").forEach { e ->
                            Text(
                                e, fontSize = 24.sp,
                                modifier = Modifier.clip(CircleShape).clickable {
                                    scope.launch { runCatching { session.sendReaction(m, e) } }
                                    actionMsg = null
                                }.padding(4.dp),
                            )
                        }
                    }
                    MessageAction(stringResource(R.string.chat_reply)) { replyTarget = m; actionMsg = null }
                    if (m.kind == "text") MessageAction(stringResource(R.string.chat_copy)) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("message", m.body))
                        actionMsg = null
                    }
                    if (m.fromMe && m.kind == "text") MessageAction(stringResource(R.string.chat_edit)) { editMsg = m; actionMsg = null }
                    if (m.fromMe && m.state == DeliveryState.FAILED) MessageAction(stringResource(R.string.chat_retry)) {
                        scope.launch { runCatching { session.resend(m) } }; actionMsg = null
                    }
                    if (m.fromMe) MessageAction(stringResource(R.string.chat_delete_all), danger = true) {
                        scope.launch { runCatching { session.sendDeleteForEveryone(m) } }; actionMsg = null
                    }
                    MessageAction(stringResource(R.string.chat_delete_me), danger = true) { session.deleteLocal(m); actionMsg = null }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionMsg = null }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }

    editMsg?.let { m ->
        var editText by remember(m.id) { mutableStateOf(m.body) }
        AlertDialog(
            onDismissRequest = { editMsg = null },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(R.string.chat_edit_title), color = c.textPrimary) },
            text = {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgPrimary).padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newText = editText.trim()
                    val orig = m
                    editMsg = null
                    if (newText.isNotEmpty() && newText != orig.body) scope.launch { runCatching { session.sendEdit(orig, newText) } }
                }) { Text(stringResource(R.string.common_save), color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { editMsg = null }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }

    if (attachMenu) {
        AlertDialog(
            onDismissRequest = { attachMenu = false },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(R.string.chat_attach), color = c.textPrimary) },
            text = {
                Column {
                    MessageAction(stringResource(R.string.chat_attach_photo)) { attachMenu = false; picker.launch("image/*") }
                    MessageAction(stringResource(R.string.chat_attach_video)) { attachMenu = false; videoPicker.launch("video/*") }
                    MessageAction(stringResource(R.string.chat_attach_file)) { attachMenu = false; filePicker.launch("*/*") }
                    MessageAction(stringResource(R.string.chat_attach_location)) { attachMenu = false; shareLocation() }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { attachMenu = false }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }

    // In-chat message search — stacks over the thread (it's a later child of
    // the host Box). Tapping a hit scrolls the list to that message.
    if (showSearch) {
        InChatSearchOverlay(
            messages = messages,
            onClose = { showSearch = false },
            onSelect = { msg ->
                showSearch = false
                scope.launch {
                    val idx = messages.indexOfFirst { it.id == msg.id }
                    if (idx >= 0) listState.animateScrollToItem(idx)
                }
            },
        )
    }
}

@Composable
private fun Composer(
    draft: String,
    accentColor: Color,
    onDraftChange: (String) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    recording: Boolean,
    recElapsed: Int,
    onMic: () -> Unit,
    onStopVoice: () -> Unit,
    onCancelVoice: () -> Unit,
) {
    val c = RcqTheme.colors
    if (recording) {
        // Recording bar: cancel · ● rec timer · send.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.Close, stringResource(R.string.common_cancel), tint = c.textSecondary,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onCancelVoice).padding(8.dp),
            )
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE5484D)))
                Text(stringResource(R.string.chat_recording, formatDuration(recElapsed)), color = c.textPrimary, fontSize = 15.sp)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.accent).clickable(onClick = onStopVoice),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_send), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.AddPhotoAlternate, stringResource(R.string.chat_attach), tint = c.textSecondary,
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onAttach).padding(8.dp),
        )
        Box(
            Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) Text(stringResource(R.string.chat_input_hint), color = c.textSecondary, fontSize = 15.sp)
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = TextStyle(color = c.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canSend = draft.isNotBlank()
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(if (canSend) c.accent else c.bgSecondary)
                .clickable(onClick = { if (canSend) onSend() else onMic() }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (canSend) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                stringResource(if (canSend) R.string.chat_send else R.string.chat_record_voice),
                tint = if (canSend) Color.White else c.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** m:ss for a duration in seconds. */
private fun formatDuration(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

@Composable
private fun MessageAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (danger) Color(0xFFE5484D) else RcqTheme.colors.textPrimary,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

private fun previewOf(m: ChatMessage, context: android.content.Context): String = when (m.kind) {
    "photo" -> context.getString(R.string.chat_prev_photo)
    "file" -> m.fileName ?: context.getString(R.string.chat_prev_file)
    "voice" -> context.getString(R.string.chat_prev_voice)
    "video" -> context.getString(R.string.chat_prev_video)
    "location" -> context.getString(R.string.chat_prev_location)
    else -> m.body.take(100)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(session: Session, m: ChatMessage, senderName: String?, onRetry: () -> Unit, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val failed = m.state == DeliveryState.FAILED
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromMe) Alignment.End else Alignment.Start) {
        if (senderName != null) {
            Text(senderName, color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 1.dp))
        }
        if (m.kind == "photo") {
            Box(Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)) { PhotoBubble(session, m) }
            if (m.body.isNotEmpty()) {
                Text(
                    m.body, color = c.textPrimary, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(10.dp)).background(if (m.fromMe) c.bubbleSelf else c.bubbleOther).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else if (m.kind == "file") {
            FileBubble(session, m, onLongPress)
        } else if (m.kind == "video") {
            VideoBubble(session, m, onLongPress)
            if (m.body.isNotEmpty()) {
                Text(
                    m.body, color = c.textPrimary, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(10.dp)).background(if (m.fromMe) c.bubbleSelf else c.bubbleOther).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else if (m.kind == "voice") {
            VoiceBubble(session, m, onLongPress)
        } else if (m.kind == "location") {
            LocationBubble(m, onLongPress)
        } else {
            Column(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (m.fromMe) c.bubbleSelf else c.bubbleOther)
                    .combinedClickable(onClick = { if (failed) onRetry() }, onLongClick = onLongPress)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (m.replyToSnippet != null) {
                    Column(
                        Modifier.padding(bottom = 4.dp).clip(RoundedCornerShape(6.dp)).background(c.accent.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(m.replyToAuthor.orEmpty(), color = c.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(m.replyToSnippet, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(m.body, color = c.textPrimary, fontSize = 15.sp)
            }
        }
        if (m.reactions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
            ) {
                m.reactions.distinct().forEach { emoji ->
                    Text(
                        emoji, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(formatTime(m.sentAt), color = c.textSecondary, fontSize = 10.sp)
            if (m.edited) Text("edited", color = c.textSecondary, fontSize = 10.sp)
            if (m.fromMe) {
                if (failed) Text("failed · tap to retry", color = Color(0xFFE5484D), fontSize = 10.sp, modifier = Modifier.clickable(onClick = onRetry))
                else Text(stateGlyph(m.state), color = if (m.state == DeliveryState.READ) c.accent else c.textSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PhotoBubble(session: Session, m: ChatMessage) {
    val c = RcqTheme.colors
    val bytes by produceState<ByteArray?>(initialValue = null, m.mediaId) {
        value = if (m.mediaId != null && m.mediaKey != null) session.fetchImage(m.mediaId, m.mediaKey) else null
    }
    val image = remember(bytes) {
        bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }
    }
    Box(Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(color = c.accent, modifier = Modifier.size(22.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileBubble(session: Session, m: ChatMessage, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (m.fromMe) c.bubbleSelf else c.bubbleOther)
            .combinedClickable(
                onClick = {
                    val mid = m.mediaId; val key = m.mediaKey
                    if (mid != null && key != null) scope.launch {
                        val bytes = session.fetchImage(mid, key)
                        if (bytes != null) openFile(context, bytes, m.fileName ?: "file", m.fileMime ?: "application/octet-stream")
                    }
                },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(Icons.Filled.Description, null, tint = c.accent, modifier = Modifier.size(24.dp))
        Column {
            Text(m.fileName ?: "file", color = c.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatFileSize(m.fileSize ?: 0L), color = c.textSecondary, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoiceBubble(session: Session, m: ChatMessage, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    val player = remember { android.media.MediaPlayer() }
    DisposableEffect(Unit) { onDispose { runCatching { player.release() } } }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (m.fromMe) c.bubbleSelf else c.bubbleOther)
            .combinedClickable(
                onClick = {
                    if (playing) {
                        runCatching { player.pause() }
                        playing = false
                    } else {
                        val mid = m.mediaId; val key = m.mediaKey
                        if (mid != null && key != null) scope.launch {
                            val bytes = session.fetchImage(mid, key) ?: return@launch
                            runCatching {
                                val f = java.io.File(context.cacheDir, "voice-${m.id}.m4a")
                                if (!f.exists() || f.length() == 0L) f.writeBytes(bytes)
                                player.reset()
                                player.setDataSource(f.absolutePath)
                                player.setOnCompletionListener { playing = false }
                                player.prepare()
                                player.start()
                                playing = true
                            }
                        }
                    }
                },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            stringResource(R.string.chat_play_voice), tint = c.accent, modifier = Modifier.size(26.dp),
        )
        Text(formatDuration(m.durationSec ?: 0), color = c.textPrimary, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoBubble(session: Session, m: ChatMessage, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val thumb = remember(m.id) {
        m.thumbB64?.takeIf { it.isNotEmpty() }?.let {
            runCatching {
                val b = android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.bgSecondary)
            .combinedClickable(
                onClick = {
                    val mid = m.mediaId; val key = m.mediaKey
                    if (mid != null && key != null) scope.launch {
                        val bytes = session.fetchImage(mid, key)
                        if (bytes != null) openFile(context, bytes, "video-${m.id}.mp4", "video/mp4")
                    }
                },
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            Image(bitmap = thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PlayArrow, stringResource(R.string.chat_play_video), tint = Color.White, modifier = Modifier.size(30.dp))
        }
        (m.durationSec ?: 0).takeIf { it > 0 }?.let {
            Text(
                formatDuration(it), color = Color.White, fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationBubble(m: ChatMessage, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val lat = m.lat ?: 0.0
    val lng = m.lng ?: 0.0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (m.fromMe) c.bubbleSelf else c.bubbleOther)
            .combinedClickable(
                onClick = {
                    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(RCQ)"))
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=$lat,$lng"))
                    runCatching { context.startActivity(geo) }.recoverCatching { context.startActivity(web) }
                },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = c.accent, modifier = Modifier.size(24.dp))
        Column {
            Text(if (m.body.isNotEmpty()) m.body else stringResource(R.string.chat_prev_location), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("%.5f, %.5f".format(lat, lng), color = c.textSecondary, fontSize = 11.sp)
        }
    }
}

/** Best-effort current location: last-known across providers, else one
 *  fresh fix (8s timeout). The caller checks the permission. */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return null
    for (p in listOf(
        android.location.LocationManager.GPS_PROVIDER,
        android.location.LocationManager.NETWORK_PROVIDER,
        android.location.LocationManager.PASSIVE_PROVIDER,
    )) {
        runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { return it.latitude to it.longitude }
    }
    val provider = when {
        runCatching { lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) }.getOrDefault(false) -> android.location.LocationManager.GPS_PROVIDER
        runCatching { lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) }.getOrDefault(false) -> android.location.LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    return withTimeoutOrNull(8000) {
        suspendCancellableCoroutine { cont ->
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(loc: android.location.Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(loc.latitude to loc.longitude)
                }
                override fun onProviderDisabled(p: String) {}
                override fun onProviderEnabled(p: String) {}
                @Deprecated("legacy callback")
                override fun onStatusChanged(p: String?, status: Int, extras: android.os.Bundle?) {}
            }
            runCatching { lm.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper()) }
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        }
    }
}

/** Read a picked file's bytes + display name + MIME from a content URI. */
private fun readPickedFile(context: Context, uri: Uri): PickedFile? = runCatching {
    val cr = context.contentResolver
    var name = "file"
    cr.query(uri, null, null, null, null)?.use { cur ->
        val idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cur.moveToFirst()) cur.getString(idx)?.let { name = it }
    }
    val mime = cr.getType(uri) ?: "application/octet-stream"
    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
    PickedFile(bytes, name, mime)
}.getOrNull()

private data class PickedFile(val bytes: ByteArray, val name: String, val mime: String)

private data class PickedVideo(val bytes: ByteArray, val thumbB64: String, val durationSec: Int)

/** Read a picked video: raw bytes + a base64 JPEG poster frame (so the
 *  bubble renders before the blob downloads) + duration in seconds. */
private fun readPickedVideo(context: Context, uri: Uri): PickedVideo? = runCatching {
    val cr = context.contentResolver
    val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
    val mmr = android.media.MediaMetadataRetriever()
    val (thumbB64, durSec) = try {
        mmr.setDataSource(context, uri)
        val durMs = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val b64 = mmr.getFrameAtTime(0)?.let { bm ->
            val maxSide = 320
            val longest = maxOf(bm.width, bm.height)
            val scaled = if (longest > maxSide) {
                val f = maxSide.toFloat() / longest
                Bitmap.createScaledBitmap(bm, (bm.width * f).toInt().coerceAtLeast(1), (bm.height * f).toInt().coerceAtLeast(1), true)
            } else bm
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
            android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
        } ?: ""
        b64 to (durMs / 1000L).toInt()
    } finally {
        runCatching { mmr.release() }
    }
    PickedVideo(bytes, thumbB64, durSec)
}.getOrNull()

/** Write decrypted bytes to the cache and hand them to a viewer via a
 *  FileProvider URI (chooser fallback so the user can always save it). */
private fun openFile(context: Context, bytes: ByteArray, fileName: String, mime: String) {
    runCatching {
        val dir = java.io.File(context.cacheDir, "files").apply { mkdirs() }
        val f = java.io.File(dir, fileName.replace('/', '_'))
        f.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(view, fileName).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1000.0)
    else -> "$bytes B"
}

private fun compressImage(context: Context, uri: Uri): ByteArray? {
    val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val maxSide = 1200
    val longest = maxOf(src.width, src.height)
    val scaled = if (longest > maxSide) {
        val f = maxSide.toFloat() / longest
        Bitmap.createScaledBitmap(src, (src.width * f).toInt(), (src.height * f).toInt(), true)
    } else src
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
    return out.toByteArray()
}

private fun stateGlyph(s: DeliveryState): String = when (s) {
    DeliveryState.SENDING -> "·"
    DeliveryState.SENT -> "✓"
    DeliveryState.DELIVERED -> "✓✓"
    DeliveryState.READ -> "✓✓"
    DeliveryState.FAILED -> "✕"
}
