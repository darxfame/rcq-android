package app.rcq.android.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.Session
import app.rcq.android.model.UserStatus
import kotlinx.coroutines.launch

@Composable
internal fun GroupInfoScreen(session: Session, groupId: Int, onBack: () -> Unit, onLeft: () -> Unit) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val groups by session.groups.collectAsState()
    val contacts by session.contacts.collectAsState()
    val group = groups.firstOrNull { it.id == groupId }
    val ownUin = session.uin ?: 0
    val isOwner = group?.ownerUin == ownUin
    var confirmDestructive by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }

    if (group == null) {
        // Group vanished (left/deleted) — bounce back.
        Box(Modifier.fillMaxSize().background(c.bgPrimary))
        return
    }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = c.accent, modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(12.dp))
            Text("Group info", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Groups, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Text(group.name, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(if (group.members.size == 1) "1 member" else "${group.members.size} members", color = c.textSecondary, fontSize = 13.sp)
            group.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = c.textSecondary, fontSize = 13.sp)
            }
        }

        group.pinnedText?.takeIf { it.isNotBlank() }?.let { pin ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp)) {
                Text("📌 $pin", color = c.textPrimary, fontSize = 13.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("MEMBERS", color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isOwner) {
                Row(Modifier.clip(RoundedCornerShape(percent = 50)).clickable { showAddMember = true }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.PersonAdd, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text("Add", color = c.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(group.members, key = { it.uin }) { m ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusIcon(m.presence, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.nickname + if (m.uin == ownUin) " (you)" else "", color = c.textPrimary, fontSize = 15.sp)
                        Text("#${m.uin}", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (m.role == "owner") Text("👑 owner", color = c.textSecondary, fontSize = 11.sp)
                    else if (m.role == "admin") Text("admin", color = c.textSecondary, fontSize = 11.sp)
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).clickable { confirmDestructive = true }.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (isOwner) Icons.Filled.Delete else Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color(0xFFE5484D), modifier = Modifier.size(18.dp))
                Text(if (isOwner) "Delete group" else "Leave group", color = Color(0xFFE5484D), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmDestructive) {
        AlertDialog(
            onDismissRequest = { confirmDestructive = false },
            containerColor = c.bgSecondary,
            title = { Text(if (isOwner) "Delete group?" else "Leave group?", color = c.textPrimary) },
            text = { Text(if (isOwner) "This deletes the group for everyone." else "You'll stop receiving messages from this group.", color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDestructive = false
                    scope.launch {
                        runCatching { if (isOwner) session.deleteGroup(groupId) else session.leaveGroup(groupId) }
                        onLeft()
                    }
                }) { Text(if (isOwner) "Delete" else "Leave", color = Color(0xFFE5484D)) }
            },
            dismissButton = { TextButton(onClick = { confirmDestructive = false }) { Text("Cancel", color = c.textSecondary) } },
        )
    }

    if (showAddMember) {
        val candidates = contacts.filter { ct -> group.members.none { it.uin == ct.uin } }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            containerColor = c.bgSecondary,
            title = { Text("Add member", color = c.textPrimary) },
            text = {
                if (candidates.isEmpty()) {
                    Text("All your contacts are already in this group.", color = c.textSecondary)
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
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text("Close", color = c.textSecondary) } },
        )
    }
}
