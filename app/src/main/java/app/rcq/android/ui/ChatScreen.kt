package app.rcq.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.filled.Groups
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
import app.rcq.android.Session
import app.rcq.android.crypto.Reply
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.DeliveryState
import app.rcq.android.model.UserStatus
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a chat thread is pointed at — a 1:1 peer or a group. */
sealed interface ChatTarget {
    data class Peer(val uin: Int) : ChatTarget
    data class Group(val id: Int) : ChatTarget
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatScreen(session: Session, target: ChatTarget, onBack: () -> Unit, onOpenGroupInfo: (Int) -> Unit = {}) {
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
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    fun authorName(m: ChatMessage): String = when {
        m.fromMe -> "You"
        isGroup -> group?.memberName(m.senderUin ?: 0) ?: "#${m.senderUin}"
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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    DisposableEffect(target) { onDispose { if (!isGroup && peer != null) session.sendTyping(peer, false) } }

    Column(Modifier.fillMaxSize().background(c.bgPrimary).imePadding()) {
        // Header.
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary.copy(alpha = 0.6f))
                .clickable(enabled = isGroup) { groupId?.let(onOpenGroupInfo) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.accent,
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
                val title = if (isGroup) (group?.name ?: "Group") else session.contactName(peer ?: 0)
                Text(title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    isGroup -> {
                        val n = group?.members?.size ?: 0
                        if (n == 1) "1 member" else "$n members"
                    }
                    isTyping -> "typing…"
                    peerContact == null -> "#$peer"
                    peerContact.presence == UserStatus.OFFLINE && peerContact.lastSeen != null -> "last seen ${relativeLastSeen(peerContact.lastSeen)}"
                    else -> peerContact.presence.label.lowercase()
                }
                Text(sub, color = if (isTyping) c.accent else c.textSecondary, fontSize = 12.sp)
            }
        }

        // Pinned banner (groups).
        group?.pinnedText?.takeIf { it.isNotBlank() }?.let { pin ->
            Row(Modifier.fillMaxWidth().background(c.bgSecondary).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("📌 $pin", color = c.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    Text(previewOf(rt), color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("✕", color = c.textSecondary, modifier = Modifier.clickable { replyTarget = null }.padding(8.dp))
            }
        }

        if (!canPost) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Only the owner can post in this group.", color = c.textSecondary, fontSize = 13.sp)
            }
        } else {
            Composer(
                draft = draft,
                accentColor = c.accent,
                onDraftChange = {
                    draft = it
                    if (!isGroup && peer != null) session.sendTyping(peer, it.isNotBlank())
                },
                onAttach = { picker.launch("image/*") },
                onSend = {
                    val body = draft.trim(); draft = ""
                    val reply = replyTarget?.let { Reply(it.id, previewOf(it), authorName(it)) }
                    replyTarget = null
                    if (!isGroup && peer != null) session.sendTyping(peer, false)
                    scope.launch {
                        runCatching {
                            if (isGroup) session.sendGroupText(groupId!!, body, reply)
                            else session.sendText(peer!!, body, reply)
                        }
                    }
                },
            )
        }
    }

    actionMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { actionMsg = null },
            containerColor = c.bgSecondary,
            title = { Text(if (m.kind == "photo") "Photo" else "Message", color = c.textPrimary) },
            text = {
                Column {
                    MessageAction("Reply") { replyTarget = m; actionMsg = null }
                    if (m.kind != "photo") MessageAction("Copy") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("message", m.body))
                        actionMsg = null
                    }
                    if (m.fromMe && m.state == DeliveryState.FAILED) MessageAction("Retry") {
                        scope.launch { runCatching { session.resend(m) } }; actionMsg = null
                    }
                    MessageAction("Delete for me", danger = true) { session.deleteLocal(m); actionMsg = null }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { actionMsg = null }) { Text("Cancel", color = c.textSecondary) } },
        )
    }
}

@Composable
private fun Composer(draft: String, accentColor: Color, onDraftChange: (String) -> Unit, onAttach: () -> Unit, onSend: () -> Unit) {
    val c = RcqTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Filled.AddPhotoAlternate, "Attach", tint = c.textSecondary,
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onAttach).padding(8.dp),
        )
        Box(
            Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (draft.isEmpty()) Text("Message", color = c.textSecondary, fontSize = 15.sp)
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
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (canSend) Color.White else c.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MessageAction(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (danger) Color(0xFFE5484D) else RcqTheme.colors.textPrimary,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

private fun previewOf(m: ChatMessage): String =
    if (m.kind == "photo") "📷 Photo" else m.body.take(100)

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(formatTime(m.sentAt), color = c.textSecondary, fontSize = 10.sp)
            if (m.fromMe) {
                if (failed) Text("failed · tap to retry", color = Color(0xFFE5484D), fontSize = 10.sp, modifier = Modifier.clickable(onClick = onRetry))
                else Text(stateGlyph(m.state), color = c.textSecondary, fontSize = 10.sp)
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
    DeliveryState.FAILED -> "✕"
}
