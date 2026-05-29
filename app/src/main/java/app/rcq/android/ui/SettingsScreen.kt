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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.Session
import app.rcq.android.data.LocalStores
import kotlinx.coroutines.launch

/**
 * App settings, mirroring the iOS SettingsView grammar: a profile header
 * card on top, then grouped sections (Appearance), and a destructive
 * burn-account action with a footer. Presence/status lives on the Home
 * header status picker — it's intentionally NOT duplicated here.
 */
@Composable
internal fun SettingsScreen(session: Session, uin: Int, onBack: () -> Unit, onBurned: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ownStatus by session.status.collectAsState()
    val themeMode by LocalStores.themeMode.collectAsState()
    var confirmBurn by remember { mutableStateOf(false) }

    fun copyUin() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("UIN", "$uin"))
        Toast.makeText(context, "UIN copied", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        // Top bar.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.accent, modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Settings", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Profile header card — status flower + nickname + UIN, tap to copy UIN.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bgSecondary)
                    .clickable { copyUin() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatusIcon(ownStatus, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(session.nickname, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("#$uin", color = c.textMono, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Icon(Icons.Filled.ContentCopy, "Copy UIN", tint = c.textSecondary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Appearance")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf(ThemeMode.SYSTEM to "Auto", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (mode, label) ->
                    val selected = themeMode == mode
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (selected) c.accent else Color.Transparent)
                            .clickable { LocalStores.setThemeMode(mode) }.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = if (selected) Color.White else c.textSecondary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
        }

        // Destructive: burn account, pinned to the bottom with a footer.
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).clickable { confirmBurn = true }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Burn account", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold) }
            Text(
                "Wipes this account everywhere. Irreversible.",
                color = c.textSecondary, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (confirmBurn) {
        AlertDialog(
            onDismissRequest = { confirmBurn = false },
            containerColor = c.bgSecondary,
            title = { Text("Burn account?", color = c.textPrimary) },
            text = { Text("This deletes your account and all local data. It cannot be undone.", color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmBurn = false; scope.launch { runCatching { session.burnAccount() }; onBurned() } }) {
                    Text("Burn", color = Color(0xFFE5484D))
                }
            },
            dismissButton = { TextButton(onClick = { confirmBurn = false }) { Text("Cancel", color = c.textSecondary) } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = RcqTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}
