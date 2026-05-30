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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import app.rcq.android.R
import app.rcq.android.Session
import app.rcq.android.data.LanguageManager
import app.rcq.android.data.LocalStores
import app.rcq.android.net.RcqApi
import kotlinx.coroutines.launch

/** Sub-screens inside Settings (kept self-contained, no nav graph). */
private enum class SettingsRoute { ROOT, PROFILE, PRIVACY, NOTIFICATIONS, BLOCKED, CUSTOM_SERVER, SOUNDS, LANGUAGE }

@Composable
internal fun SettingsScreen(session: Session, uin: Int, onBack: () -> Unit, onBurned: (Int?) -> Unit, onMigrated: (Int) -> Unit) {
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
        SettingsRoute.SOUNDS -> SoundsScreen { route = SettingsRoute.ROOT }
        SettingsRoute.LANGUAGE -> LanguageScreen { route = SettingsRoute.ROOT }
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
    onBurned: (Int?) -> Unit,
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
        Toast.makeText(context, context.getString(R.string.common_uin_copied), Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.settings_title), onBack)

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
                        Icon(Icons.Filled.ContentCopy, stringResource(R.string.common_copy_uin), tint = c.textSecondary,
                            modifier = Modifier.size(15.dp).clickable { copyUin() })
                    }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_sec_appearance))
            SegmentedTheme(themeMode) { LocalStores.setThemeMode(it) }
            SectionFooter(stringResource(R.string.settings_foot_appearance))
            Spacer(Modifier.height(12.dp))
            val lang by LanguageManager.current.collectAsState()
            SettingsGroup {
                SettingsRow(Icons.Filled.Language, stringResource(R.string.onboard_language), value = LanguageManager.displayName(lang)) { onOpen(SettingsRoute.LANGUAGE) }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_sec_privacy))
            SettingsGroup {
                SettingsRow(Icons.Filled.Lock, stringResource(R.string.settings_row_privacy)) { onOpen(SettingsRoute.PRIVACY) }
                Divider()
                SettingsRow(Icons.Filled.Notifications, stringResource(R.string.settings_row_notifications)) { onOpen(SettingsRoute.NOTIFICATIONS) }
                Divider()
                SettingsRow(Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.settings_row_sounds)) { onOpen(SettingsRoute.SOUNDS) }
                Divider()
                SettingsRow(Icons.Outlined.Block, stringResource(R.string.settings_row_blocked), value = if (blockedCount > 0) "$blockedCount" else null) { onOpen(SettingsRoute.BLOCKED) }
            }
            SectionFooter(stringResource(R.string.settings_foot_privacy))

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_sec_history))
            SettingsGroup {
                SettingsRow(Icons.Filled.DeleteSweep, stringResource(R.string.settings_row_clear_history), destructive = true) { confirmClear = true }
            }
            SectionFooter(stringResource(R.string.settings_foot_history))

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_sec_about))
            SettingsGroup {
                SettingsRow(Icons.Filled.Info, stringResource(R.string.settings_row_about), value = appVersion(context)) { showAbout = true }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.settings_sec_account))
            SettingsGroup {
                SettingsRow(Icons.Filled.Autorenew, stringResource(R.string.settings_row_move_uin)) { if (!migrating) confirmMigrate = true }
            }
            Text(
                stringResource(R.string.cs_move_footer),
                color = c.textSecondary, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
                textAlign = TextAlign.Center,
            )

            SettingsGroup {
                SettingsRow(Icons.Filled.LocalFireDepartment, stringResource(R.string.settings_row_burn), destructive = true) { confirmBurn = true }
            }
            Text(
                stringResource(R.string.cs_burn_footer),
                color = c.textSecondary, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 20.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.cs_clear_title),
            body = stringResource(R.string.cs_clear_body),
            confirm = stringResource(R.string.common_clear), destructive = true,
            onConfirm = { confirmClear = false; session.clearHistory(); Toast.makeText(context, context.getString(R.string.cs_history_cleared), Toast.LENGTH_SHORT).show() },
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmMigrate) {
        ConfirmDialog(
            title = stringResource(R.string.cs_move_title),
            body = stringResource(R.string.cs_move_body),
            confirm = stringResource(R.string.common_move), destructive = false,
            onConfirm = {
                confirmMigrate = false
                migrating = true
                scope.launch {
                    val newUin = runCatching { session.migrateToNewUin() }.getOrNull()
                    migrating = false
                    if (newUin != null) {
                        Toast.makeText(context, context.getString(R.string.cs_moved_toast, newUin), Toast.LENGTH_LONG).show()
                        onMigrated(newUin)
                    } else {
                        Toast.makeText(context, context.getString(R.string.cs_move_failed), Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { confirmMigrate = false },
        )
    }
    if (confirmBurn) {
        ConfirmDialog(
            title = stringResource(R.string.cs_burn_title),
            body = stringResource(R.string.cs_burn_body),
            confirm = stringResource(R.string.cs_burn_cta), destructive = true,
            onConfirm = { confirmBurn = false; scope.launch { onBurned(session.burnAccount()) } },
            onDismiss = { confirmBurn = false },
        )
    }
    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = c.bgSecondary,
            confirmButton = { TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.common_done), color = c.accent) } },
            title = { Text("RCQ", color = c.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.cs_about_tagline), color = c.textSecondary, fontSize = 14.sp)
                    Text(stringResource(R.string.cs_about_version, appVersion(context)), color = c.textMono, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text(stringResource(R.string.cs_about_features), color = c.textSecondary, fontSize = 12.sp)
                }
            },
        )
    }
}

// ── Profile editor ───────────────────────────────────────────────────

@Composable
internal fun ProfileEditScreen(session: Session, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ownUin = session.uin ?: 0
    val ownStatus by session.status.collectAsState()
    val profileViews by app.rcq.android.data.VisitStore.recentViews.collectAsState()
    var nickname by remember { mutableStateOf(session.nickname) }
    var statusMessage by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var interests by remember { mutableStateOf("") }
    var homepage by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        session.loadProfile()?.let { p ->
            nickname = p.nickname ?: nickname
            statusMessage = p.status_message ?: ""
            firstName = p.first_name ?: ""
            lastName = p.last_name ?: ""
            gender = p.gender
            age = p.age?.toString() ?: ""
            city = p.city ?: ""
            country = p.country ?: ""
            about = p.about ?: ""
            interests = p.interests.joinToString(", ")
            homepage = p.homepage ?: ""
        }
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.pe_title), onBack, trailing = {
            TextButton(enabled = !saving && nickname.isNotBlank(), onClick = {
                saving = true
                scope.launch {
                    session.updateProfile(RcqApi.UpdateMeBody(
                        nickname = nickname.trim(),
                        status_message = statusMessage.trim(),
                        first_name = firstName.trim(),
                        last_name = lastName.trim(),
                        gender = gender,
                        age = age.toIntOrNull(),
                        city = city.trim(),
                        country = country.trim(),
                        about = about.trim(),
                        interests = interests.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        homepage = homepage.trim(),
                    ))
                    saving = false
                    Toast.makeText(context, context.getString(R.string.pe_saved), Toast.LENGTH_SHORT).show()
                    onBack()
                }
            }) { Text(stringResource(R.string.common_save), color = if (nickname.isNotBlank()) c.accent else c.textSecondary) }
        })

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Identity header card (avatar + UIN), like the iOS profile.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusIcon(ownStatus, size = 48.dp)
                Column {
                    Text(nickname.ifBlank { "—" }, color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("$ownUin", color = c.textMono, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
            // Profile views (own-profile only; tallied locally from sealed visit pings).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bgSecondary).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pe_views_title), color = c.textPrimary, fontSize = 15.sp)
                    Text(stringResource(R.string.pe_views_desc), color = c.textSecondary, fontSize = 11.sp)
                }
                Text("$profileViews", color = c.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Field(stringResource(R.string.pe_nickname), nickname) { nickname = it }
            Field(stringResource(R.string.pe_status_message), statusMessage) { statusMessage = it }
            Field(stringResource(R.string.pe_first_name), firstName) { firstName = it }
            Field(stringResource(R.string.pe_last_name), lastName) { lastName = it }
            SectionLabel(stringResource(R.string.common_gender))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to stringResource(R.string.common_male), "female" to stringResource(R.string.common_female), "other" to stringResource(R.string.common_other)).forEach { (key, label) ->
                    val sel = gender == key
                    Box(
                        Modifier.clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else c.bgSecondary)
                            .clickable { gender = if (sel) null else key }.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text(label, color = if (sel) Color.White else c.textSecondary, fontSize = 13.sp) }
                }
            }
            Field(stringResource(R.string.pe_age), age, keyboardDigits = true) { age = it.filter(Char::isDigit).take(3) }
            Field(stringResource(R.string.common_city), city) { city = it }
            Field(stringResource(R.string.common_country), country) { country = it }
            Field(stringResource(R.string.common_about), about, minLines = 3) { about = it }
            Field(stringResource(R.string.pe_interests), interests) { interests = it }
            SectionFooter(stringResource(R.string.pe_interests_hint))
            Field(stringResource(R.string.pe_website), homepage) { homepage = it }
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
    val screenSec by app.rcq.android.data.LocalStores.screenSecurity.collectAsState()

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
        SettingsTopBar(stringResource(R.string.settings_row_privacy), onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            VisibilityPicker(stringResource(R.string.pv_last_seen), lastSeen, listOf("everyone", "contacts", "nobody"), stringResource(R.string.pv_last_seen_desc)) { lastSeen = it; save(RcqApi.UpdateMeBody(last_seen_visibility = it)) }

            // Persistent presence + how long it lingers (iOS parity).
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pv_stay_visible), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.pv_stay_visible_desc), color = c.textSecondary, fontSize = 11.sp)
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

            VisibilityPicker(stringResource(R.string.pv_profile_card), profileVis, listOf("everyone", "contacts", "nobody"), stringResource(R.string.pv_profile_card_desc)) { profileVis = it; save(RcqApi.UpdateMeBody(profile_visibility = it)) }
            VisibilityPicker(stringResource(R.string.common_gender), genderVis, listOf("everyone", "contacts", "nobody"), stringResource(R.string.pv_gender_desc)) { genderVis = it; save(RcqApi.UpdateMeBody(gender_visibility = it)) }
            VisibilityPicker(stringResource(R.string.pv_invite), invitePolicy, listOf("everyone", "contacts", "nobody"), stringResource(R.string.pv_invite_desc)) { invitePolicy = it; save(RcqApi.UpdateMeBody(group_invite_policy = it)) }
            VisibilityPicker(stringResource(R.string.pv_receipts), receipts, listOf("everyone", "contacts", "nobody"), stringResource(R.string.pv_receipts_desc)) { receipts = it; save(RcqApi.UpdateMeBody(read_receipts_visibility = it)) }

            // Block screenshots (device-local; FLAG_SECURE applied by MainActivity).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pv_screen_security), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.pv_screen_security_desc), color = c.textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = screenSec,
                    onCheckedChange = { app.rcq.android.data.LocalStores.setScreenSecurity(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel(stringResource(R.string.pv_network))
            SettingsGroup {
                val host = session.currentServer
                SettingsRow(
                    Icons.Filled.Dns,
                    stringResource(R.string.pv_custom_server),
                    value = if (host == RcqApi.DEFAULT_HOST) stringResource(R.string.pv_default) else host,
                    onClick = onOpenCustomServer,
                )
            }
        }
    }
}

@Composable
private fun SoundsScreen(onBack: () -> Unit) {
    val c = RcqTheme.colors
    val msgOn by LocalStores.soundMessages.collectAsState()
    val presOn by LocalStores.soundPresence.collectAsState()
    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.settings_row_sounds), onBack)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingToggleRow(stringResource(R.string.snd_message_title), stringResource(R.string.snd_message_desc), msgOn) { LocalStores.setSoundMessages(it) }
            SettingToggleRow(stringResource(R.string.snd_presence_title), stringResource(R.string.snd_presence_desc), presOn) { LocalStores.setSoundPresence(it) }
            SectionFooter(stringResource(R.string.snd_footer))
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = RcqTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bgSecondary).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 15.sp)
            Text(subtitle, color = c.textSecondary, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.accent))
    }
}

@Composable
private fun LanguageScreen(onBack: () -> Unit) {
    val c = RcqTheme.colors
    val activity = LocalContext.current as? android.app.Activity
    val current by LanguageManager.current.collectAsState()
    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.onboard_language), onBack)
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(LanguageManager.supported, key = { it.code }) { lang ->
                Row(
                    Modifier.fillMaxWidth().clickable { activity?.let { LanguageManager.set(it, lang.code) } }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lang.nativeName, color = c.textPrimary, fontSize = 16.sp)
                        if (lang.englishName != lang.nativeName) Text(lang.englishName, color = c.textSecondary, fontSize = 12.sp)
                    }
                    if (lang.code == current) Icon(Icons.Filled.Check, null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
                Divider()
            }
        }
        SectionFooter(stringResource(R.string.lang_footer))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val c = RcqTheme.colors
    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.settings_row_notifications), onBack)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.height(40.dp))
            Icon(Icons.Filled.Notifications, null, tint = c.textSecondary, modifier = Modifier.size(44.dp))
            Text(stringResource(R.string.notif_push), color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.notif_desc), color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
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
        SettingsTopBar(stringResource(R.string.settings_row_blocked), onBack)
        if (blocked.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(60.dp))
                Icon(Icons.Outlined.Block, null, tint = c.textSecondary, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.blocked_empty), color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
                            Text(stringResource(R.string.blocked_unblock), color = c.accent)
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
            val newUin = runCatching { session.registerNewAccount("user-${(1000..9999).random()}", input) }.getOrNull()
            switching = false
            if (newUin != null) {
                Toast.makeText(context, context.getString(R.string.csrv_connected, session.currentServer), Toast.LENGTH_LONG).show()
                onSwitched(newUin)
            } else {
                Toast.makeText(context, context.getString(R.string.csrv_unreachable), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        SettingsTopBar(stringResource(R.string.pv_custom_server), onBack, trailing = {
            TextButton(enabled = isDirty && !switching, onClick = { confirm = true }) {
                Text(stringResource(R.string.common_save), color = if (isDirty && !switching) c.accent else c.textSecondary)
            }
        })

        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                stringResource(R.string.csrv_intro),
                color = c.textSecondary, fontSize = 14.sp,
            )

            // Current server card.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.bgSecondary).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.csrv_current), color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(current, color = c.textPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            }

            Field(stringResource(R.string.csrv_host), draft) { draft = it }

            // Destructive-switch warning.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFE0A106), modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.csrv_warning),
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
                    Text(stringResource(R.string.csrv_reset_btn), color = Color(0xFFE5484D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (switching) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(stringResource(R.string.csrv_switching), color = c.textSecondary, fontSize = 13.sp)
                }
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.csrv_confirm_title, target),
            body = stringResource(R.string.csrv_confirm_body, target, current),
            confirm = stringResource(R.string.common_switch), destructive = true,
            onConfirm = { confirm = false; applySwitch(draft) },
            onDismiss = { confirm = false },
        )
    }
    if (resetting) {
        ConfirmDialog(
            title = stringResource(R.string.csrv_reset_title),
            body = stringResource(R.string.csrv_reset_body, RcqApi.DEFAULT_HOST, current),
            confirm = stringResource(R.string.common_reset), destructive = true,
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
        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent, modifier = Modifier.size(26.dp).clickable(onClick = onBack))
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
        listOf(ThemeMode.SYSTEM to stringResource(R.string.theme_auto), ThemeMode.LIGHT to stringResource(R.string.theme_light), ThemeMode.DARK to stringResource(R.string.theme_dark)).forEach { (m, label) ->
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgSecondary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEach { opt ->
                val sel = value == opt
                val optLabel = when (opt) {
                    "everyone" -> stringResource(R.string.vis_everyone)
                    "contacts" -> stringResource(R.string.vis_contacts)
                    "nobody" -> stringResource(R.string.vis_nobody)
                    else -> opt.replaceFirstChar { it.uppercase() }
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else Color.Transparent)
                        .clickable { onPick(opt) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(optLabel, color = if (sel) Color.White else c.textSecondary, fontSize = 12.sp) }
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
    )
}

private fun appVersion(context: Context): String = runCatching {
    val pm = context.packageManager.getPackageInfo(context.packageName, 0)
    "${pm.versionName}"
}.getOrDefault("0.1")
