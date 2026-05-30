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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.Session
import app.rcq.android.data.LocalStores
import app.rcq.android.net.RcqApi
import kotlinx.coroutines.launch

/** Sub-screens inside Settings (kept self-contained, no nav graph). */
private enum class SettingsRoute { ROOT, PROFILE, PRIVACY, NOTIFICATIONS, BLOCKED, CUSTOM_SERVER }

@Composable
internal fun SettingsScreen(session: Session, uin: Int, onBack: () -> Unit, onBurned: () -> Unit, onMigrated: (Int) -> Unit) {
    var route by remember { mutableStateOf(SettingsRoute.ROOT) }
    when (route) {
        SettingsRoute.ROOT -> SettingsRoot(
            session, uin,
            onBack = onBack,
            onBurned = onBurned,
            onMigrated = onMigrated,
            onOpen = { route = it },
        )
        SettingsRoute.PROFILE -> ProfileEditScreen(session) { route = SettingsRoute.ROOT }
        SettingsRoute.PRIVACY -> PrivacyScreen(
            session,
            onOpenCustomServer = { route = SettingsRoute.CUSTOM_SERVER },
        ) { route = SettingsRoute.ROOT }
        SettingsRoute.NOTIFICATIONS -> NotificationsScreen { route = SettingsRoute.ROOT }
        SettingsRoute.BLOCKED -> BlockedUsersScreen(session) { route = SettingsRoute.ROOT }
        SettingsRoute.CUSTOM_SERVER -> CustomServerScreen(
            session,
            onBack = { route = SettingsRoute.ROOT },
            onSwitched = { newUin -> onMigrated(newUin); onBack() },
        )
    }
}

@Composable
private fun SettingsRoot(
    session: Session,
    uin: Int,
    onBack: () -> Unit,
    onBurned: () -> Unit,
    onMigrated: (Int) -> Unit,
    onOpen: (SettingsRoute) -> Unit,
) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ownStatus by session.status.collectAsState()
    val themeMode by LocalStores.themeMode.collectAsState()
    val contacts by session.contacts.collectAsState()
    var confirmBurn by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmMigrate by remember { mutableStateOf(false) }
    var migrating by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val blockedCount = contacts.count { it.blocked }

    fun copyUin() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("UIN", "$uin"))
        Toast.makeText(context, "UIN copied", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Settings", onBack)

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Profile header card — opens the editor.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bgSecondary)
                    .clickable { onOpen(SettingsRoute.PROFILE) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                StatusIcon(ownStatus, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(session.nickname, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$uin", color = c.textMono, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Icon(Icons.Filled.ContentCopy, "Copy UIN", tint = c.textSecondary,
                            modifier = Modifier.size(15.dp).clickable { copyUin() })
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Appearance")
            SegmentedTheme(themeMode) { LocalStores.setThemeMode(it) }
            SectionFooter("Choose light, dark, or follow the system theme.")

            Spacer(Modifier.height(22.dp))
            SectionLabel("Privacy")
            SettingsGroup {
                SettingsRow(Icons.Filled.Lock, "Privacy & Network") { onOpen(SettingsRoute.PRIVACY) }
                Divider()
                SettingsRow(Icons.Filled.Notifications, "Notifications") { onOpen(SettingsRoute.NOTIFICATIONS) }
                Divider()
                SettingsRow(Icons.Outlined.Block, "Blocked users", value = if (blockedCount > 0) "$blockedCount" else null) { onOpen(SettingsRoute.BLOCKED) }
            }
            SectionFooter("Control who can see you, who can reach you, and which server you're on.")

            Spacer(Modifier.height(22.dp))
            SectionLabel("History")
            SettingsGroup {
                SettingsRow(Icons.Filled.DeleteSweep, "Clear chat history", destructive = true) { confirmClear = true }
            }
            SectionFooter("Removes messages from this device only. Your account and contacts stay.")

            Spacer(Modifier.height(22.dp))
            SectionLabel("About")
            SettingsGroup {
                SettingsRow(Icons.Filled.Info, "About RCQ", value = appVersion(context)) { showAbout = true }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Account")
            SettingsGroup {
                SettingsRow(Icons.Filled.Autorenew, "Move to a new UIN") { if (!migrating) confirmMigrate = true }
            }
            Text(
                "Get a fresh UIN. Your contacts, groups and keys are kept; local chat history is cleared.",
                color = c.textSecondary, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                textAlign = TextAlign.Center,
            )

            SettingsGroup {
                SettingsRow(Icons.Filled.LocalFireDepartment, "Burn account", destructive = true) { confirmBurn = true }
            }
            Text(
                "Wipes this account everywhere. Irreversible.",
                color = c.textSecondary, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear chat history?",
            body = "Removes all messages from this device. Your account and contacts stay.",
            confirm = "Clear", destructive = true,
            onConfirm = { confirmClear = false; session.clearHistory(); Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show() },
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmMigrate) {
        ConfirmDialog(
            title = "Move to a new UIN?",
            body = "You get a freshly-allocated UIN. Contacts, groups and your keys are kept; local chat history on this device is cleared.",
            confirm = "Move", destructive = false,
            onConfirm = {
                confirmMigrate = false
                migrating = true
                scope.launch {
                    val newUin = runCatching { session.migrateToNewUin() }.getOrNull()
                    migrating = false
                    if (newUin != null) {
                        Toast.makeText(context, "Moved to #$newUin", Toast.LENGTH_LONG).show()
                        onMigrated(newUin)
                    } else {
                        Toast.makeText(context, "Migration failed. Try again later.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { confirmMigrate = false },
        )
    }
    if (confirmBurn) {
        ConfirmDialog(
            title = "Burn account?",
            body = "This deletes your account and all local data. It cannot be undone.",
            confirm = "Burn", destructive = true,
            onConfirm = { confirmBurn = false; scope.launch { runCatching { session.burnAccount() }; onBurned() } },
            onDismiss = { confirmBurn = false },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = c.bgSecondary,
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Done", color = c.accent) } },
            title = { Text("RCQ", color = c.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Private messaging. No phone number.", color = c.textSecondary, fontSize = 14.sp)
                    Text("Version ${appVersion(context)}", color = c.textMono, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("End-to-end encrypted. Sealed sender. Open source.", color = c.textSecondary, fontSize = 12.sp)
                }
            },
        )
    }
}

// ── Profile editor ───────────────────────────────────────────────────

@Composable
private fun ProfileEditScreen(session: Session, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf(session.nickname) }
    var statusMessage by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        session.loadProfile()?.let { p ->
            nickname = p.nickname ?: nickname
            statusMessage = p.status_message ?: ""
            gender = p.gender
            age = p.age?.toString() ?: ""
            city = p.city ?: ""
            country = p.country ?: ""
            about = p.about ?: ""
        }
        loaded = true
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Edit profile", onBack, trailing = {
            TextButton(enabled = !saving && nickname.isNotBlank(), onClick = {
                saving = true
                scope.launch {
                    session.updateProfile(RcqApi.UpdateMeBody(
                        nickname = nickname.trim(),
                        status_message = statusMessage.trim(),
                        gender = gender,
                        age = age.toIntOrNull(),
                        city = city.trim(),
                        country = country.trim(),
                        about = about.trim(),
                    ))
                    saving = false
                    Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }) { Text("Save", color = if (nickname.isNotBlank()) c.accent else c.textSecondary) }
        })

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Nickname", nickname) { nickname = it }
            Field("Status message", statusMessage) { statusMessage = it }
            SectionLabel("Gender")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "Male", "female" to "Female", "other" to "Other").forEach { (key, label) ->
                    val sel = gender == key
                    Box(
                        Modifier.clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else c.bgSecondary)
                            .clickable { gender = if (sel) null else key }.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(label, color = if (sel) Color.White else c.textSecondary, fontSize = 13.sp) }
                }
            }
            Field("Age", age, keyboardDigits = true) { age = it.filter(Char::isDigit).take(3) }
            Field("City", city) { city = it }
            Field("Country", country) { country = it }
            Field("About", about, minLines = 3) { about = it }
        }
    }
}

// ── Privacy & Network ────────────────────────────────────────────────

@Composable
private fun PrivacyScreen(session: Session, onOpenCustomServer: () -> Unit, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    var lastSeen by remember { mutableStateOf("everyone") }
    var genderVis by remember { mutableStateOf("nobody") }
    var profileVis by remember { mutableStateOf("everyone") }
    var invitePolicy by remember { mutableStateOf("everyone") }
    var receipts by remember { mutableStateOf("everyone") }
    var presencePersistent by remember { mutableStateOf(false) }
    var presenceTtl by remember { mutableStateOf(1440) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        session.loadProfile()?.let { p ->
            lastSeen = p.last_seen_visibility ?: "everyone"
            genderVis = p.gender_visibility ?: "nobody"
            profileVis = p.profile_visibility ?: "everyone"
            invitePolicy = p.group_invite_policy ?: "everyone"
            receipts = p.read_receipts_visibility ?: "everyone"
            presencePersistent = p.presence_persistent ?: false
            presenceTtl = p.presence_ttl_minutes ?: 1440
        }
    }

    fun save(body: RcqApi.UpdateMeBody) { scope.launch { runCatching { session.updateProfile(body) } } }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Privacy & Network", onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            VisibilityPicker("Last seen", lastSeen, listOf("everyone", "contacts", "nobody"), "Who can see when you were last online.") { lastSeen = it; save(RcqApi.UpdateMeBody(last_seen_visibility = it)) }

            // Persistent presence + how long it lingers (iOS parity).
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Stay visible after you leave", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Keep showing as recently online for a while after the app closes.", color = c.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = presencePersistent,
                        onCheckedChange = { presencePersistent = it; save(RcqApi.UpdateMeBody(presence_persistent = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                    )
                }
                if (presencePersistent) {
                    val ttls = listOf(30 to "30m", 60 to "1h", 180 to "3h", 480 to "8h", 1440 to "24h")
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        ttls.forEach { (mins, label) ->
                            val sel = presenceTtl == mins
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else Color.Transparent)
                                    .clickable { presenceTtl = mins; save(RcqApi.UpdateMeBody(presence_ttl_minutes = mins)) }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(label, color = if (sel) Color.White else c.textSecondary, fontSize = 12.sp) }
                        }
                    }
                }
            }

            VisibilityPicker("Profile card", profileVis, listOf("everyone", "contacts", "nobody"), "Who can open your full profile (city, age, about).") { profileVis = it; save(RcqApi.UpdateMeBody(profile_visibility = it)) }
            VisibilityPicker("Gender", genderVis, listOf("everyone", "contacts", "nobody"), "Who can see your gender on your profile.") { genderVis = it; save(RcqApi.UpdateMeBody(gender_visibility = it)) }
            VisibilityPicker("Who can add me to groups", invitePolicy, listOf("everyone", "contacts", "nobody"), "Who is allowed to add you to a group.") { invitePolicy = it; save(RcqApi.UpdateMeBody(group_invite_policy = it)) }
            VisibilityPicker("Read receipts", receipts, listOf("everyone", "contacts", "nobody"), "Who sees your blue ticks. Turning this off hides theirs too.") { receipts = it; save(RcqApi.UpdateMeBody(read_receipts_visibility = it)) }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Network")
            SettingsGroup {
                val host = session.currentServer
                SettingsRow(
                    Icons.Filled.Dns,
                    "Custom server",
                    value = if (host == RcqApi.DEFAULT_HOST) "Default" else host,
                    onClick = onOpenCustomServer,
                )
            }
        }
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val c = RcqTheme.colors
    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Notifications", onBack)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(40.dp))
            Icon(Icons.Filled.Notifications, null, tint = c.textSecondary, modifier = Modifier.size(44.dp))
            Text("Push notifications", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Per-chat mute is available from a long-press on the home screen. System push delivery is configured in Android Settings.", color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

// ── Blocked users ────────────────────────────────────────────────────

@Composable
private fun BlockedUsersScreen(session: Session, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val contacts by session.contacts.collectAsState()
    val blocked = contacts.filter { it.blocked }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Blocked users", onBack)
        if (blocked.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(60.dp))
                Icon(Icons.Outlined.Block, null, tint = c.textSecondary, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text("No blocked users", color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(blocked, key = { it.uin }) { ct ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusIcon(ct.presence, size = 26.dp)
                        Column(Modifier.weight(1f)) {
                            Text(ct.nickname, color = c.textPrimary, fontSize = 15.sp)
                            Text("${ct.uin}", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        TextButton(onClick = { scope.launch { runCatching { session.toggleBlock(ct.uin) } } }) {
                            Text("Unblock", color = c.accent)
                        }
                    }
                }
            }
        }
    }
}

// ── Custom server ────────────────────────────────────────────────────

/** Point this device at a different backend (iOS CustomServerSheet
 *  parity). Switching is destructive — the current UIN/token/contacts
 *  only exist on the current server — so we confirm, then burn the
 *  account and mint a fresh identity on the chosen server. */
@Composable
private fun CustomServerScreen(session: Session, onBack: () -> Unit, onSwitched: (Int) -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val current = session.currentServer
    var draft by remember { mutableStateOf(current) }
    var switching by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }

    // Bare host the user typed (scheme/path stripped); blank → default.
    fun normalized(s: String): String = s.trim()
        .removePrefix("https://").removePrefix("http://").removePrefix("wss://").removePrefix("ws://")
        .substringBefore('/').trim()
        .ifBlank { RcqApi.DEFAULT_HOST }

    val target = normalized(draft)
    val isDirty = target != current
    val onCustom = current != RcqApi.DEFAULT_HOST

    fun applySwitch(input: String?) {
        switching = true
        scope.launch {
            val newUin = runCatching { session.switchServer(input) }.getOrNull()
            switching = false
            if (newUin != null) {
                Toast.makeText(context, "Connected to ${session.currentServer}", Toast.LENGTH_LONG).show()
                onSwitched(newUin)
            } else {
                Toast.makeText(context, "Couldn't reach that server. Nothing changed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar("Custom server", onBack, trailing = {
            TextButton(enabled = isDirty && !switching, onClick = { confirm = true }) {
                Text("Save", color = if (isDirty && !switching) c.accent else c.textSecondary)
            }
        })

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                "Connect to an organisation's island or your own self-hosted RCQ server instead of the public one.",
                color = c.textSecondary, fontSize = 14.sp,
            )

            // Current server card.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bgSecondary).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CURRENT SERVER", color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(current, color = c.textPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            }

            Field("Server host", draft) { draft = it }

            // Destructive-switch warning.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFE0A106), modifier = Modifier.size(20.dp))
                Text(
                    "Switching servers starts a new account. Your current UIN, contacts and groups live on the current server and won't move. This can't be undone.",
                    color = c.textSecondary, fontSize = 12.sp,
                )
            }

            if (onCustom) {
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary)
                        .clickable(enabled = !switching) { resetting = true }.padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Restore, null, tint = Color(0xFFE5484D), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset to the public server", color = Color(0xFFE5484D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (switching) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Switching…", color = c.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = "Switch to $target?",
            body = "This starts a fresh account on $target. Your current account on $current stays where it is but won't be reachable from this device anymore.",
            confirm = "Switch", destructive = true,
            onConfirm = { confirm = false; applySwitch(draft) },
            onDismiss = { confirm = false },
        )
    }
    if (resetting) {
        ConfirmDialog(
            title = "Reset to the public server?",
            body = "Starts a fresh account on ${RcqApi.DEFAULT_HOST}. Your account on $current stays where it is but won't be reachable from this device anymore.",
            confirm = "Reset", destructive = true,
            onConfirm = { resetting = false; applySwitch(null) },
            onDismiss = { resetting = false },
        )
    }
}

// ── shared bits ──────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    val c = RcqTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.accent, modifier = Modifier.size(26.dp).clickable(onClick = onBack))
        Spacer(Modifier.width(12.dp))
        Text(title, color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = RcqTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
}

/** Small grey explanation under a settings group, iOS section-footer style. */
@Composable
private fun SectionFooter(text: String) {
    Text(text, color = RcqTheme.colors.textSecondary, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp))
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RcqTheme.colors.bgSecondary)) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 48.dp).background(RcqTheme.colors.divider))
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, value: String? = null, destructive: Boolean = false, onClick: () -> Unit) {
    val c = RcqTheme.colors
    val tint = if (destructive) Color(0xFFE5484D) else c.accent
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = if (destructive) Color(0xFFE5484D) else c.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = c.textSecondary, fontSize = 14.sp)
        Icon(Icons.Filled.ChevronRight, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SegmentedTheme(mode: ThemeMode, onPick: (ThemeMode) -> Unit) {
    val c = RcqTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(ThemeMode.SYSTEM to "Auto", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark").forEach { (m, label) ->
            val sel = mode == m
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else Color.Transparent)
                    .clickable { onPick(m) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (sel) Color.White else c.textSecondary, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
}

@Composable
private fun VisibilityPicker(label: String, value: String, options: List<String>, desc: String? = null, onPick: (String) -> Unit) {
    val c = RcqTheme.colors
    fun pretty(s: String) = s.replaceFirstChar { it.uppercase() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEach { opt ->
                val sel = value == opt
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else Color.Transparent)
                        .clickable { onPick(opt) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(pretty(opt), color = if (sel) Color.White else c.textSecondary, fontSize = 12.sp) }
            }
        }
        if (desc != null) Text(desc, color = c.textSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun Field(label: String, value: String, keyboardDigits: Boolean = false, minLines: Int = 1, onChange: (String) -> Unit) {
    val c = RcqTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = c.textSecondary) },
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, destructive: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val c = RcqTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(title, color = c.textPrimary) },
        text = { Text(body, color = c.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (destructive) Color(0xFFE5484D) else c.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}

private fun appVersion(context: Context): String = runCatching {
    val pm = context.packageManager.getPackageInfo(context.packageName, 0)
    "${pm.versionName}"
}.getOrDefault("0.1")
