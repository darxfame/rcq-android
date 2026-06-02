package app.rcq.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.R
import app.rcq.android.Session
import app.rcq.android.crypto.RecoveryPhrase
import kotlinx.coroutines.launch

/**
 * Restore an existing account on a fresh install from its 24-word recovery
 * phrase. Calls [Session.recoverAccount] which proves key ownership to the
 * server and rebinds onto the recovered UIN.
 */
@Composable
fun RestoreScreen(session: Session, onBack: () -> Unit, onRestored: (Int) -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phrase by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val wordCount = remember(phrase) { RecoveryPhrase.parse(phrase).size }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(enabled = !busy, onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.restore_title), color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.restore_hint), color = c.textSecondary, fontSize = 14.sp)

            OutlinedTextField(
                value = phrase,
                onValueChange = { phrase = it; error = null },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                placeholder = { Text(stringResource(R.string.restore_phrase_ph), color = c.textSecondary) },
                enabled = !busy,
            )
            Text(stringResource(R.string.restore_wordcount, wordCount), color = if (wordCount == 24) c.accent else c.textSecondary, fontSize = 12.sp)

            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.restore_server_ph), color = c.textSecondary) },
                singleLine = true,
                enabled = !busy,
            )

            error?.let { Text(it, color = Color(0xFFE5484D), fontSize = 13.sp) }

            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = c.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.restore_restoring), color = c.textSecondary, fontSize = 14.sp)
                }
            } else {
                CapsuleButton(stringResource(R.string.restore_button), enabled = wordCount == 24, modifier = Modifier.fillMaxWidth()) {
                    busy = true
                    error = null
                    scope.launch {
                        val words = RecoveryPhrase.parse(phrase)
                        val res = runCatching {
                            session.recoverAccount(words, server.trim().ifBlank { null })
                        }
                        busy = false
                        res.onSuccess { onRestored(it) }
                            .onFailure { e ->
                                error = when {
                                    e is IllegalArgumentException -> R.string.restore_err_invalid
                                    (e.message ?: "").contains("404") || (e.message ?: "").contains("identity_not_found") -> R.string.restore_err_notfound
                                    else -> R.string.restore_err_generic
                                }.let { id -> context.getString(id) }
                            }
                    }
                }
            }
        }
    }
}
