package app.rcq.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.R
import app.rcq.android.Session

/**
 * Shows the active account's 24-word recovery phrase (BIP39 over the identity
 * seed) so the user can write it down and later restore the account on a fresh
 * device. Legacy accounts (no seed) get a clear "not available" notice.
 */
@Composable
fun RecoveryPhraseScreen(session: Session, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val phrase = remember { session.recoveryPhrase() }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.recovery_title), color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        if (phrase == null) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recovery_legacy), color = c.textSecondary, fontSize = 14.sp)
            }
            return@Column
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.recovery_warn), color = Color(0xFFE5A50A), fontSize = 13.sp)

            // Two-column numbered grid of the 24 words.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(phrase.subList(0, 12), phrase.subList(12, 24)).forEachIndexed { col, half ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        half.forEachIndexed { i, w ->
                            val n = col * 12 + i + 1
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.bgSecondary).padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("$n", color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.width(22.dp))
                                Text(w, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            CapsuleButton(stringResource(R.string.recovery_copy), modifier = Modifier.fillMaxWidth()) {
                copyToClipboard(context, phrase.joinToString(" "))
                Toast.makeText(context, context.getString(R.string.recovery_copied), Toast.LENGTH_SHORT).show()
            }
            Text(stringResource(R.string.recovery_note), color = c.textSecondary, fontSize = 12.sp)
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("recovery phrase", text))
}
