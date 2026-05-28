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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Entry point of the RCQ Android client.
 *
 * Right now this is just the skeleton: a single Compose screen confirming
 * the project builds and runs. The messaging stack (anonymous
 * registration, libsignal crypto, WebSocket delivery) lands in the next
 * milestones, talking the same wire protocol as the iOS client against
 * the existing backend (api.rcq.app), per the rcq-spec contract.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RcqRoot() }
    }
}

private val Background = Color(0xFF0F1115)
private val TextSecondary = Color(0xFF8A8F99)

@Composable
private fun RcqRoot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "RCQ",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Android — skeleton",
                color = TextSecondary,
                fontSize = 14.sp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RcqRootPreview() {
    RcqRoot()
}
