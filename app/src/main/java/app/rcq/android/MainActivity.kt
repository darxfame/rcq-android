package app.rcq.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.model.ChatMessage
import app.rcq.android.model.Contact
import app.rcq.android.model.DeliveryState
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Background = Color(0xFF0F1115)
private val Surface = Color(0xFF1A1D23)
private val TextPrimary = Color(0xFFE6E8EC)
private val TextSecondary = Color(0xFF8A8F99)
private val Accent = Color(0xFF3B82F6)
private val Online = Color(0xFF34C759)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val session = Session(applicationContext)
        setContent { RcqApp(session) }
    }
}

private sealed interface UiState {
    data object Onboarding : UiState
    data object Registering : UiState
    data class Registered(val uin: Int) : UiState
    data class Failed(val message: String) : UiState
}

@Composable
private fun RcqApp(session: Session) {
    val scope = rememberCoroutineScope()
    var state by remember {
        mutableStateOf<UiState>(session.uin?.let { UiState.Registered(it) } ?: UiState.Onboarding)
    }
    var chatPeer by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is UiState.Registered) session.start()
    }

    fun register() {
        state = UiState.Registering
        scope.launch {
            state = try {
                UiState.Registered(session.registerNewAccount("user-${(1000..9999).random()}"))
            } catch (e: Exception) {
                UiState.Failed(e.message ?: "Registration failed")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background).systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val s = state
        val peer = chatPeer
        when {
            s is UiState.Registered && peer != null -> ChatScreen(session, peer, onBack = { chatPeer = null })
            s is UiState.Registered && showSettings -> SettingsScreen(
                session, s.uin,
                onBack = { showSettings = false },
                onBurned = { showSettings = false; chatPeer = null; state = UiState.Onboarding },
            )
            s is UiState.Registered -> Home(session, s.uin, onOpenChat = { chatPeer = it }, onOpenSettings = { showSettings = true })
            s is UiState.Onboarding -> Onboarding(onStart = ::register)
            s is UiState.Registering -> Registering()
            s is UiState.Failed -> Failed(s.message, onRetry = ::register)
        }
    }
}

@Composable
private fun Onboarding(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("RCQ", color = TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("Private messaging. No phone number.", color = TextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        CapsuleButton("Start", onClick = onStart)
    }
}

@Composable
private fun Registering() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Accent)
        Text("Creating your account…", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("Couldn't connect", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        CapsuleButton("Try again", onClick = onRetry)
    }
}

@Composable
private fun Home(session: Session, uin: Int, onOpenChat: (Int) -> Unit, onOpenSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val contacts by session.contacts.collectAsState()
    val pending by session.pending.collectAsState()
    val connected by session.connected.collectAsState()
    val messages by session.messages.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("RCQ", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) Online else TextSecondary))
                    Text("#$uin", color = TextSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Text("⚙", color = TextSecondary, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onOpenSettings).padding(end = 14.dp))
            Box(
                modifier = Modifier.clip(CircleShape).background(Accent).clickable { showAdd = true }.padding(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("+ Add", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (pending.isNotEmpty()) {
                item { SectionLabel("Requests") }
                items(pending) { req ->
                    PendingRow(
                        name = req.fromNickname,
                        onAccept = { scope.launch { runCatching { session.respond(req.requestId, true) } } },
                        onDecline = { scope.launch { runCatching { session.respond(req.requestId, false) } } },
                    )
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
            if (contacts.isEmpty() && pending.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("No contacts yet.\nTap + Add and enter a UIN.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            } else if (contacts.isNotEmpty()) {
                item { SectionLabel("Contacts") }
                val sorted = contacts.sortedByDescending { messages[it.uin]?.lastOrNull()?.sentAt ?: 0L }
                items(sorted) { c -> ContactRow(c, messages[c.uin]?.lastOrNull(), onClick = { onOpenChat(c.uin) }) }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onAdd = { target -> scope.launch { runCatching { session.addContact(target) } }; showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun ContactRow(c: Contact, last: ChatMessage?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Surface), contentAlignment = Alignment.Center) {
            Text(c.nickname.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(c.nickname, color = TextPrimary, fontSize = 16.sp)
            val preview = when {
                last == null -> "#${c.uin}"
                last.kind == "photo" -> (if (last.fromMe) "You: " else "") + "📷 Photo"
                else -> (if (last.fromMe) "You: " else "") + last.body
            }
            Text(
                preview,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = if (last == null) FontFamily.Monospace else FontFamily.Default,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (last != null) Text(formatTime(last.sentAt), color = TextSecondary, fontSize = 11.sp)
            if (c.status == "online") Box(Modifier.size(8.dp).clip(CircleShape).background(Online))
        }
    }
}

@Composable
private fun PendingRow(name: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("Accept", color = Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAccept).padding(8.dp))
        Spacer(Modifier.width(4.dp))
        Text("Decline", color = TextSecondary, modifier = Modifier.clickable(onClick = onDecline).padding(8.dp))
    }
}

@Composable
private fun AddContactDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Add contact", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                label = { Text("UIN", color = TextSecondary) },
                singleLine = true,
            )
        },
        confirmButton = {
            val target = input.toIntOrNull()
            TextButton(enabled = target != null, onClick = { target?.let(onAdd) }) {
                Text("Add request", color = if (target != null) Accent else TextSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
    )
}

@Composable
private fun ChatScreen(session: Session, peer: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val all by session.messages.collectAsState()
    val messages = all[peer] ?: emptyList()
    val typingFrom by session.typingFrom.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val jpeg = withContext(Dispatchers.IO) { compressImage(context, uri) }
            if (jpeg != null) runCatching { session.sendPhoto(peer, jpeg, null) }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    DisposableEffect(peer) { onDispose { session.sendTyping(peer, false) } }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("‹ Back", color = Accent, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(session.contactName(peer), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (typingFrom == peer) Text("typing…", color = Accent, fontSize = 12.sp)
            }
        }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { m -> MessageBubble(session, m, onRetry = { scope.launch { runCatching { session.resend(m) } } }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("📎", fontSize = 22.sp, modifier = Modifier.clickable { picker.launch("image/*") }.padding(end = 10.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it; session.sendTyping(peer, it.isNotBlank()) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", color = TextSecondary) },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank()
            Box(
                modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(if (canSend) Accent else Surface)
                    .clickable(enabled = canSend) {
                        val body = draft.trim(); draft = ""
                        session.sendTyping(peer, false)
                        scope.launch { runCatching { session.sendText(peer, body) } }
                    }.padding(horizontal = 18.dp, vertical = 14.dp),
            ) { Text("Send", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun MessageBubble(session: Session, m: ChatMessage, onRetry: () -> Unit) {
    val failed = m.state == DeliveryState.FAILED
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (m.fromMe) Alignment.End else Alignment.Start) {
        if (m.kind == "photo") {
            PhotoBubble(session, m)
            if (m.body.isNotEmpty()) {
                Text(
                    m.body,
                    color = if (m.fromMe) Color.White else TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(10.dp)).background(if (m.fromMe) Accent else Surface).padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        } else {
            Text(
                m.body,
                color = if (m.fromMe) Color.White else TextPrimary,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (m.fromMe) Accent else Surface)
                    .then(if (failed) Modifier.clickable(onClick = onRetry) else Modifier)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(formatTime(m.sentAt), color = TextSecondary, fontSize = 10.sp)
            if (m.fromMe) {
                if (failed) Text("failed · tap to retry", color = Color(0xFFE5484D), fontSize = 10.sp, modifier = Modifier.clickable(onClick = onRetry))
                else Text(stateGlyph(m.state), color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PhotoBubble(session: Session, m: ChatMessage) {
    val bytes by produceState<ByteArray?>(initialValue = null, m.mediaId) {
        value = if (m.mediaId != null && m.mediaKey != null) session.fetchImage(m.mediaId, m.mediaKey) else null
    }
    val image = remember(bytes) {
        bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() }
    }
    Box(
        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(14.dp)).background(Surface),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(22.dp))
        }
    }
}

private fun compressImage(context: android.content.Context, uri: android.net.Uri): ByteArray? {
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

private fun formatTime(ts: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts))

private fun stateGlyph(s: DeliveryState): String = when (s) {
    DeliveryState.SENDING -> "·"
    DeliveryState.SENT -> "✓"
    DeliveryState.DELIVERED -> "✓✓"
    DeliveryState.FAILED -> "✕"
}

@Composable
private fun SettingsScreen(session: Session, uin: Int, onBack: () -> Unit, onBurned: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var status by remember { mutableStateOf("online") }
    var confirmBurn by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("‹ Back", color = Accent, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(16.dp))
            Text("Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        SectionLabel("Your UIN")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("#$uin", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("Copy", color = Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier
                .clickable {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("UIN", "$uin"))
                }
                .padding(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Status")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("online" to "Online", "away" to "Away", "dnd" to "Do Not Disturb").forEach { (key, label) ->
                val selected = status == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) Accent else Surface)
                        .clickable { status = key; scope.launch { session.setStatus(key) } }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) { Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp) }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
                .clickable { confirmBurn = true }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Burn account", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold) }
        Text(
            "Wipes this account everywhere. Irreversible.",
            color = TextSecondary, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            textAlign = TextAlign.Center,
        )
    }

    if (confirmBurn) {
        AlertDialog(
            onDismissRequest = { confirmBurn = false },
            containerColor = Surface,
            title = { Text("Burn account?", color = TextPrimary) },
            text = { Text("This deletes your account and all local data. It cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmBurn = false; scope.launch { runCatching { session.burnAccount() }; onBurned() } }) {
                    Text("Burn", color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { confirmBurn = false }) { Text("Cancel", color = TextSecondary) } },
        )
    }
}

@Composable
private fun CapsuleButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(if (enabled) Accent else Surface)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 40.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}
