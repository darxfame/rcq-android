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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
        mutableStateOf<UiState>(
            session.uin?.let { UiState.Registered(it) } ?: UiState.Onboarding,
        )
    }

    fun register() {
        state = UiState.Registering
        scope.launch {
            state = try {
                val nickname = "user-${(1000..9999).random()}"
                UiState.Registered(session.registerNewAccount(nickname))
            } catch (e: Exception) {
                UiState.Failed(e.message ?: "Registration failed")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is UiState.Onboarding -> Onboarding(onStart = ::register)
            is UiState.Registering -> Registering()
            is UiState.Registered -> Registered(uin = s.uin)
            is UiState.Failed -> Failed(message = s.message, onRetry = ::register)
        }
    }
}

@Composable
private fun Onboarding(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("RCQ", color = TextPrimary, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Text(
            "Private messaging. No phone number.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        CapsuleButton(label = "Start", onClick = onStart)
    }
}

@Composable
private fun Registering() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(color = Accent)
        Text("Creating your account…", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun Registered(uin: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("You're in", color = TextSecondary, fontSize = 14.sp)
        Text(
            "#$uin",
            color = TextPrimary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun Failed(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Couldn't connect", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        CapsuleButton(label = "Try again", onClick = onRetry)
    }
}

@Composable
private fun CapsuleButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
