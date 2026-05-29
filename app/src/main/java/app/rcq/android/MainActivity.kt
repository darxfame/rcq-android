package app.rcq.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.data.LocalStores
import app.rcq.android.ui.CapsuleButton
import app.rcq.android.ui.ChatScreen
import app.rcq.android.ui.HomeScreen
import app.rcq.android.ui.RcqTheme
import app.rcq.android.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalStores.init(applicationContext)
        val session = Session(applicationContext)
        setContent {
            val mode by LocalStores.themeMode.collectAsState()
            RcqTheme(mode) { RcqApp(session) }
        }
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
        modifier = Modifier.fillMaxSize().background(RcqTheme.colors.bgPrimary).systemBarsPadding(),
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
            s is UiState.Registered -> HomeScreen(session, s.uin, onOpenChat = { chatPeer = it }, onOpenSettings = { showSettings = true })
            s is UiState.Onboarding -> Onboarding(onStart = ::register)
            s is UiState.Registering -> Registering()
            s is UiState.Failed -> Failed(s.message, onRetry = ::register)
        }
    }
}

@Composable
private fun Onboarding(onStart: () -> Unit) {
    val c = RcqTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("RCQ", color = c.textPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("Private messaging. No phone number.", color = c.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        CapsuleButton("Start", onClick = onStart)
    }
}

@Composable
private fun Registering() {
    val c = RcqTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = c.accent)
        Text("Creating your account…", color = c.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit) {
    val c = RcqTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Text("Couldn't connect", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        CapsuleButton("Try again", onClick = onRetry)
    }
}
