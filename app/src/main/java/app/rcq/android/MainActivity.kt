package app.rcq.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Background = Color(0xFF0F1115)
private val Surface = Color(0xFF1A1D23)
private val TextPrimary = Color(0xFFE6E8EC)
private val TextSecondary = Color(0xFF8A8F99)
private val Accent = Color(0xFF3B82F6)

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
            s is UiState.Registered && peer != null ->
                ChatScreen(session, myUin = s.uin, peer = peer, onBack = { chatPeer = null })
            s is UiState.Onboarding -> Onboarding(onStart = ::register)
            s is UiState.Registering -> Registering()
            s is UiState.Registered -> Home(uin = s.uin, onOpenChat = { chatPeer = it })
            s is UiState.Failed -> Failed(message = s.message, onRetry = ::register)
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
        CapsuleButton(label = "Start", onClick = onStart)
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
        CapsuleButton(label = "Try again", onClick = onRetry)
    }
}

@Composable
private fun Home(uin: Int, onOpenChat: (Int) -> Unit) {
    var peerInput by remember { mutableStateOf("") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("Your UIN", color = TextSecondary, fontSize = 14.sp)
        Text("#$uin", color = TextPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = peerInput,
            onValueChange = { peerInput = it.filter(Char::isDigit) },
            label = { Text("Message a UIN", color = TextSecondary) },
            singleLine = true,
        )
        val target = peerInput.toIntOrNull()
        CapsuleButton(
            label = "Open chat",
            enabled = target != null,
            onClick = { target?.let(onOpenChat) },
        )
    }
}

@Composable
private fun ChatScreen(session: Session, myUin: Int, peer: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<Pair<Boolean, String>>() } // fromMe, text
    var draft by remember { mutableStateOf("") }

    DisposableEffect(peer) {
        session.connect { sender, text ->
            if (sender == peer) messages.add(false to text)
        }
        onDispose { session.disconnect() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Back", color = Accent, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(16.dp))
            Text("#$peer", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages) { (fromMe, text) ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (fromMe) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(
                        text,
                        color = if (fromMe) Color.White else TextPrimary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (fromMe) Accent else Surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", color = TextSecondary) },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (canSend) Accent else Surface)
                    .clickable(enabled = canSend) {
                        val body = draft.trim()
                        draft = ""
                        messages.add(true to body)
                        scope.launch { runCatching { session.sendText(peer, body) } }
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text("Send", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CapsuleButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) Accent else Surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
