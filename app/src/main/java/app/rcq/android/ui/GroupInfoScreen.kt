package app.rcq.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.R
import app.rcq.android.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun GroupInfoScreen(session: Session, groupId: Int, onBack: () -> Unit, onLeft: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val groups by session.groups.collectAsState()
    val contacts by session.contacts.collectAsState()
    val group = groups.firstOrNull { it.id == groupId }
    val ownUin = session.uin ?: 0
    val isOwner = group?.ownerUin == ownUin
    var confirmDestructive by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val jpeg = withContext(Dispatchers.IO) { compressImageFor(context, uri) }
            if (jpeg != null) runCatching { session.setGroupAvatar(groupId, jpeg) }
        }
    }

    if (group == null) {
        // Group vanished (left/deleted) — bounce back.
        Box(Modifier.fillMaxSize().background(c.bgPrimary))
        return
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent, modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.gi_title), color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (isOwner) {
                Icon(Icons.Filled.Edit, stringResource(R.string.gi_rename), tint = c.accent, modifier = Modifier.size(22.dp).clickable { showRename = true })
            }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(Modifier.then(if (isOwner) Modifier.clickable { avatarPicker.launch("image/*") } else Modifier)) {
                    GroupAvatar(group, session, 72.dp, glyphSize = 40.dp)
                }
                if (isOwner) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(c.accent).clickable { avatarPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CameraAlt, stringResource(R.string.gi_change_avatar), tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Text(group.name, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(pluralStringResource(R.plurals.members, group.members.size, group.members.size), color = c.textSecondary, fontSize = 13.sp)
            group.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = c.textSecondary, fontSize = 13.sp)
            }
        }

        // Non-owners see the pin read-only; owners get the editable row below.
        if (!isOwner) group.pinnedText?.takeIf { it.isNotBlank() }?.let { pin ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.PushPin, null, tint = c.accent, modifier = Modifier.size(16.dp))
                Text(pin, color = c.textPrimary, fontSize = 13.sp)
            }
        }

        if (isOwner) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.gi_settings), color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                // Pinned message — opens an editor dialog.
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).clickable { showPin = true }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.PushPin, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.gi_pinned), color = c.textPrimary, fontSize = 15.sp)
                        Text(group.pinnedText?.takeIf { it.isNotBlank() } ?: stringResource(R.string.gi_none), color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Filled.Edit, null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                }
                // Who can post.
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.gi_who_post), color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(percent = 50)).background(c.bgPrimary).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("all" to stringResource(R.string.vis_everyone), "owner_only" to stringResource(R.string.gi_post_owner)).forEach { (key, label) ->
                            val sel = group.postPolicy == key
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(percent = 50)).background(if (sel) c.accent else Color.Transparent)
                                    .clickable { if (!sel) scope.launch { runCatching { session.patchGroup(groupId, postPolicy = key) } } }.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(label, color = if (sel) Color.White else c.textSecondary, fontSize = 13.sp) }
                        }
                    }
                }
                GroupToggleRow(stringResource(R.string.gi_closed), stringResource(R.string.gi_closed_desc), group.isClosed) { v -> scope.launch { runCatching { session.patchGroup(groupId, isClosed = v) } } }
                GroupToggleRow(stringResource(R.string.gi_hide), stringResource(R.string.gi_hide_desc), group.membersHidden) { v -> scope.launch { runCatching { session.patchGroup(groupId, membersHidden = v) } } }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.gi_members), color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isOwner) {
                Row(Modifier.clip(RoundedCornerShape(percent = 50)).clickable { showAddMember = true }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.PersonAdd, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.home_bar_add), color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(group.members, key = { it.uin }) { m ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusIcon(m.presence, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.nickname + if (m.uin == ownUin) stringResource(R.string.gi_you) else "", color = c.textPrimary, fontSize = 15.sp)
                        Text("${m.uin}", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (m.role == "owner") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Star, null, tint = c.accent, modifier = Modifier.size(12.dp))
                            Text(stringResource(R.string.gi_owner), color = c.textSecondary, fontSize = 11.sp)
                        }
                    } else if (m.role == "admin") Text(stringResource(R.string.gi_admin), color = c.textSecondary, fontSize = 11.sp)
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).clickable { confirmDestructive = true }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (isOwner) Icons.Filled.Delete else Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFFE5484D), modifier = Modifier.size(18.dp))
                Text(stringResource(if (isOwner) R.string.gi_delete else R.string.gi_leave), color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmDestructive) {
        AlertDialog(
            onDismissRequest = { confirmDestructive = false },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(if (isOwner) R.string.gi_delete_q else R.string.gi_leave_q), color = c.textPrimary) },
            text = { Text(stringResource(if (isOwner) R.string.gi_delete_body else R.string.gi_leave_body), color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDestructive = false
                    scope.launch {
                        runCatching { if (isOwner) session.deleteGroup(groupId) else session.leaveGroup(groupId) }
                        onLeft()
                    }
                }) { Text(stringResource(if (isOwner) R.string.common_delete else R.string.gi_leave_cta), color = Color(0xFFE5484D)) }
            },
            dismissButton = { TextButton(onClick = { confirmDestructive = false }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }

    if (showAddMember) {
        val candidates = contacts.filter { ct -> group.members.none { it.uin == ct.uin } }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(R.string.gi_add_member), color = c.textPrimary) },
            text = {
                if (candidates.isEmpty()) {
                    Text(stringResource(R.string.gi_all_in), color = c.textSecondary)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(candidates, key = { it.uin }) { ct ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    scope.launch { runCatching { session.addGroupMember(groupId, ct.uin) } }
                                    showAddMember = false
                                }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StatusIcon(ct.presence, size = 24.dp)
                                Text(ct.nickname, color = c.textPrimary, fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text(stringResource(R.string.common_close), color = c.textSecondary) } },
        )
    }

    if (showRename) {
        var name by remember { mutableStateOf(group.name) }
        var desc by remember { mutableStateOf(group.description ?: "") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(R.string.gi_edit), color = c.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.gi_name), color = c.textSecondary) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.gi_description), color = c.textSecondary) }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val n = name.trim(); val d = desc.trim()
                    showRename = false
                    scope.launch {
                        runCatching {
                            session.patchGroup(
                                groupId,
                                name = if (n != group.name) n else null,
                                description = if (d != (group.description ?: "")) d else null,
                            )
                        }
                    }
                }) { Text(stringResource(R.string.common_save), color = if (name.isNotBlank()) c.accent else c.textSecondary) }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }

    if (showPin) {
        var pinText by remember { mutableStateOf(group.pinnedText ?: "") }
        AlertDialog(
            onDismissRequest = { showPin = false },
            containerColor = c.bgSecondary,
            title = { Text(stringResource(R.string.gi_pinned), color = c.textPrimary) },
            text = {
                OutlinedTextField(
                    value = pinText, onValueChange = { pinText = it },
                    placeholder = { Text(stringResource(R.string.gi_pin_placeholder), color = c.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = pinText.trim()
                    showPin = false
                    scope.launch { runCatching { session.patchGroup(groupId, pinnedText = t) } }
                }) { Text(stringResource(R.string.common_save), color = c.accent) }
            },
            dismissButton = { TextButton(onClick = { showPin = false }) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        )
    }
}

@Composable
private fun GroupToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = RcqTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 15.sp)
            Text(subtitle, color = c.textSecondary, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.accent))
    }
}

/** Shared JPEG downscale+compress for picked images (avatars). */
internal fun compressImageFor(context: android.content.Context, uri: android.net.Uri): ByteArray? {
    val src = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return null
    val maxSide = 640
    val longest = maxOf(src.width, src.height)
    val scaled = if (longest > maxSide) {
        val f = maxSide.toFloat() / longest
        android.graphics.Bitmap.createScaledBitmap(src, (src.width * f).toInt(), (src.height * f).toInt(), true)
    } else src
    val out = java.io.ByteArrayOutputStream()
    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
    return out.toByteArray()
}
