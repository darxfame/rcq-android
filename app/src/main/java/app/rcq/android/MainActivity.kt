package app.rcq.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.data.LocalStores
import app.rcq.android.net.RcqApi
import app.rcq.android.ui.CapsuleButton
import app.rcq.android.ui.ChatScreen
import app.rcq.android.ui.ChatTarget
import app.rcq.android.ui.GroupInfoScreen
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
    var chatTarget by remember { mutableStateOf<ChatTarget?>(null) }
    var groupInfoId by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is UiState.Registered) session.start()
    }

    fun register(server: String? = null) {
        state = UiState.Registering
        scope.launch {
            state = try {
                UiState.Registered(session.registerNewAccount("user-${(1000..9999).random()}", server))
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
        val target = chatTarget
        val infoId = groupInfoId
        when {
            s is UiState.Registered && infoId != null -> GroupInfoScreen(
                session, infoId,
                onBack = { groupInfoId = null },
                onLeft = { groupInfoId = null; chatTarget = null },
            )
            s is UiState.Registered && target != null -> ChatScreen(
                session, target,
                onBack = { chatTarget = null },
                onOpenGroupInfo = { groupInfoId = it },
            )
            s is UiState.Registered && showSettings -> SettingsScreen(
                session, s.uin,
                onBack = { showSettings = false },
                onBurned = { showSettings = false; chatTarget = null; state = UiState.Onboarding },
                onMigrated = { newUin -> chatTarget = null; state = UiState.Registered(newUin) },
            )
            s is UiState.Registered -> HomeScreen(
                session, s.uin,
                onOpenChat = { chatTarget = ChatTarget.Peer(it) },
                onOpenGroup = { chatTarget = ChatTarget.Group(it) },
                onOpenSettings = { showSettings = true },
            )
            s is UiState.Onboarding -> Onboarding(onStart = ::register)
            s is UiState.Registering -> Registering()
            s is UiState.Failed -> Failed(s.message, onRetry = { register(null) })
        }
    }
}

@Composable
private fun Onboarding(onStart: (String?) -> Unit) {
    val c = RcqTheme.colors
    // Server is shown explicitly (prefilled with the default public host)
    // and editable — so it's clear which server you're joining and you can
    // point at an organisation island. Matches the iOS onboarding.
    var server by remember { mutableStateOf(RcqApi.DEFAULT_HOST) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.rcq_logo),
            contentDescription = "RCQ",
            modifier = Modifier.size(96.dp),
        )
        Text("RCQ", color = c.textPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text("Private messaging. No phone number.", color = c.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Server", color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (server.isEmpty()) Text("server host", color = c.textSecondary, fontSize = 14.sp)
                BasicTextField(
                    value = server,
                    onValueChange = { server = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                "Default is the public RCQ server. Change it to join an organisation's island.",
                color = c.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        CapsuleButton("Start", onClick = { onStart(server) })
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
