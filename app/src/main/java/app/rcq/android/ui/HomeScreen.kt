package app.rcq.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.Session
import app.rcq.android.data.LocalStores
import app.rcq.android.model.Contact
import app.rcq.android.model.RcqGroup
import app.rcq.android.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One row's worth of long-press action, mirrors iOS ContextAction. */
internal data class ContextAction(
    val title: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal fun HomeScreen(
    session: Session,
    uin: Int,
    onOpenChat: (Int) -> Unit,
    onOpenGroup: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = RcqTheme.colors
    val scope = rememberCoroutineScope()
    val contacts by session.contacts.collectAsState()
    val groups by session.groups.collectAsState()
    val pending by session.pending.collectAsState()
    val connected by session.connected.collectAsState()
    val messages by session.messages.collectAsState()
    val ownStatus by session.status.collectAsState()
    val favorites by LocalStores.favorites.collectAsState()
    val archived by LocalStores.archived.collectAsState()
    val unread by LocalStores.unread.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showCreateGroup by remember { mutableStateOf(false) }
    var previewContact by remember { mutableStateOf<Contact?>(null) }
    var previewGroup by remember { mutableStateOf<RcqGroup?>(null) }
    var reportTarget by remember { mutableStateOf<Contact?>(null) }

    var collapsedFavorites by remember { mutableStateOf(false) }
    var collapsedGroups by remember { mutableStateOf(false) }
    var collapsedOnline by remember { mutableStateOf(false) }
    var collapsedOffline by remember { mutableStateOf(false) }
    var collapsedArchive by remember { mutableStateOf(true) }

    // Unread threads float to the top (iOS parity), then by recency.
    fun byRecency(list: List<Contact>) =
        list.sortedWith(
            compareByDescending<Contact> { (unread[LocalStores.peerThread(it.uin)] ?: 0) > 0 }
                .thenByDescending { messages[it.uin]?.lastOrNull()?.sentAt ?: 0L },
        )

    val nonArchived = contacts.filterNot { LocalStores.peerThread(it.uin) in archived }
    val favContacts = byRecency(nonArchived.filter { LocalStores.peerThread(it.uin) in favorites })
    val onlineContacts = byRecency(nonArchived.filter { it.presence != UserStatus.OFFLINE })
    val offlineContacts = byRecency(nonArchived.filter { it.presence == UserStatus.OFFLINE })
    val archivedContacts = byRecency(contacts.filter { LocalStores.peerThread(it.uin) in archived })
    val visibleGroups = groups.filterNot { LocalStores.groupThread(it.id) in archived }

    Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                nickname = session.nickname,
                uin = uin,
                connected = connected,
                ownStatus = ownStatus,
                onPickStatus = { scope.launch { session.setStatus(it) } },
            )

            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                if (pending.isNotEmpty()) {
                    item(key = "req-h") { SectionHeader("Requests", pending.size, collapsed = false, onToggle = {}) }
                    items(pending, key = { "p${it.requestId}" }) { req ->
                        PendingRow(
                            name = req.fromNickname,
                            onAccept = { scope.launch { runCatching { session.respond(req.requestId, true) } } },
                            onDecline = { scope.launch { runCatching { session.respond(req.requestId, false) } } },
                        )
                    }
                }

                if (contacts.isEmpty() && groups.isEmpty() && pending.isEmpty()) {
                    item(key = "empty") { EmptyState(onAdd = { showAdd = true }) }
                }

                contactSection("Favorites", favContacts, collapsedFavorites, "fav", unread, { collapsedFavorites = !collapsedFavorites }, onOpenChat, onLongPress = { previewContact = it })

                // Groups — header always shows a "+" to create, like iOS.
                item(key = "grp-h") {
                    SectionHeader("Groups", visibleGroups.size, collapsedGroups, { collapsedGroups = !collapsedGroups }) {
                        Icon(Icons.Filled.Add, "New group", tint = c.accent, modifier = Modifier.size(20.dp).clip(CircleShape).clickable { showCreateGroup = true })
                    }
                }
                if (!collapsedGroups) {
                    if (visibleGroups.isEmpty()) {
                        item(key = "grp-empty") {
                            Row(Modifier.fillMaxWidth().clickable { showCreateGroup = true }.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Add, null, tint = c.accent, modifier = Modifier.size(18.dp))
                                Text("Create a group", color = c.textPrimary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(items = visibleGroups, key = { it.id }) { g: RcqGroup ->
                            GroupRow(group = g, ownUin = uin, session = session, unread = unread[LocalStores.groupThread(g.id)] ?: 0, onClick = { onOpenGroup(g.id) }, onLongPress = { previewGroup = g })
                        }
                    }
                }

                contactSection("Online", onlineContacts, collapsedOnline, "on", unread, { collapsedOnline = !collapsedOnline }, onOpenChat, onLongPress = { previewContact = it })
                contactSection("Offline", offlineContacts, collapsedOffline, "off", unread, { collapsedOffline = !collapsedOffline }, onOpenChat, onLongPress = { previewContact = it })
                contactSection("Archive", archivedContacts, collapsedArchive, "arch", unread, { collapsedArchive = !collapsedArchive }, onOpenChat, onLongPress = { previewContact = it })

                item(key = "tail") { Spacer(Modifier.height(8.dp)) }
            }

            BottomBar(
                onAdd = { showAdd = true },
                onQr = { showQr = true },
                onSearch = { showSearch = true },
                onSettings = onOpenSettings,
            )
        }

        if (showSearch) {
            SearchOverlay(
                contacts = contacts,
                onClose = { showSearch = false },
                onSelect = { showSearch = false; onOpenChat(it.uin) },
            )
        }

        previewContact?.let { ct ->
            PreviewOverlay(
                title = ct.nickname,
                subtitle = "#${ct.uin}",
                avatar = { StatusIcon(ct.presence, size = 36.dp) },
                actions = contactActions(ct, session, scope, onOpenChat, onReport = { reportTarget = it }),
                onDismiss = { previewContact = null },
            )
        }
        previewGroup?.let { g ->
            PreviewOverlay(
                title = g.name,
                subtitle = if (g.members.size == 1) "1 member" else "${g.members.size} members",
                avatar = { GroupAvatar(g, session, 36.dp) },
                actions = groupActions(g, uin, session, scope, onOpenGroup),
                onDismiss = { previewGroup = null },
            )
        }
    }

    if (showAdd) {
        AddContactDialog(
            onAdd = { target -> scope.launch { runCatching { session.addContact(target) } }; showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    if (showQr) QrDialog(uin = uin, onDismiss = { showQr = false })
    if (showCreateGroup) {
        CreateGroupDialog(
            contacts = contacts,
            onCreate = { name, members ->
                scope.launch { runCatching { session.createGroup(name, members) }.onSuccess { onOpenGroup(it.id) } }
                showCreateGroup = false
            },
            onDismiss = { showCreateGroup = false },
        )
    }
    reportTarget?.let { ct ->
        ReportDialog(
            name = ct.nickname,
            onSubmit = { reason -> scope.launch { runCatching { session.report(ct.uin, reason) } }; reportTarget = null },
            onDismiss = { reportTarget = null },
        )
    }
}

private fun contactActions(
    contact: Contact,
    session: Session,
    scope: CoroutineScope,
    onOpenChat: (Int) -> Unit,
    onReport: (Contact) -> Unit,
): List<ContextAction> {
    val thread = LocalStores.peerThread(contact.uin)
    val fav = LocalStores.isFavorite(thread)
    val muted = LocalStores.isMuted(thread)
    val archived = LocalStores.isArchived(thread)
    return listOf(
        ContextAction("Send message", Icons.AutoMirrored.Filled.Chat) { onOpenChat(contact.uin) },
        ContextAction(if (fav) "Remove favorite" else "Add to favorites", if (fav) Icons.Filled.Star else Icons.Filled.StarBorder) { LocalStores.toggleFavorite(thread) },
        ContextAction(if (muted) "Unmute" else "Mute", if (muted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff) { LocalStores.toggleMute(thread) },
        ContextAction(if (archived) "Unarchive" else "Archive", if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive) { LocalStores.toggleArchive(thread) },
        ContextAction(if (contact.blocked) "Unblock" else "Block", if (contact.blocked) Icons.Outlined.Block else Icons.Filled.Block, destructive = !contact.blocked) { scope.launch { session.toggleBlock(contact.uin) } },
        ContextAction("Report", Icons.Filled.Flag, destructive = true) { onReport(contact) },
        ContextAction("Remove", Icons.Filled.PersonRemove, destructive = true) { scope.launch { session.removeContact(contact.uin) } },
    )
}

private fun groupActions(
    group: RcqGroup,
    ownUin: Int,
    session: Session,
    scope: CoroutineScope,
    onOpenGroup: (Int) -> Unit,
): List<ContextAction> {
    val thread = LocalStores.groupThread(group.id)
    val fav = LocalStores.isFavorite(thread)
    val muted = LocalStores.isMuted(thread)
    val archived = LocalStores.isArchived(thread)
    val isOwner = group.ownerUin == ownUin
    return listOf(
        ContextAction("Open chat", Icons.AutoMirrored.Filled.Chat) { onOpenGroup(group.id) },
        ContextAction(if (fav) "Remove favorite" else "Add to favorites", if (fav) Icons.Filled.Star else Icons.Filled.StarBorder) { LocalStores.toggleFavorite(thread) },
        ContextAction(if (muted) "Unmute" else "Mute", if (muted) Icons.Filled.Notifications else Icons.Filled.NotificationsOff) { LocalStores.toggleMute(thread) },
        ContextAction(if (archived) "Unarchive" else "Archive", if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive) { LocalStores.toggleArchive(thread) },
        if (isOwner)
            ContextAction("Delete group", Icons.Filled.Delete, destructive = true) { scope.launch { session.deleteGroup(group.id) } }
        else
            ContextAction("Leave group", Icons.AutoMirrored.Filled.ExitToApp, destructive = true) { scope.launch { session.leaveGroup(group.id) } },
    )
}

@Composable
private fun HomeHeader(
    nickname: String,
    uin: Int,
    connected: Boolean,
    ownStatus: UserStatus,
    onPickStatus: (UserStatus) -> Unit,
) {
    val c = RcqTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            StatusIcon(ownStatus, size = 30.dp, modifier = Modifier.clip(CircleShape).clickable { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(UserStatus.ONLINE, UserStatus.AWAY, UserStatus.DND, UserStatus.INVISIBLE, UserStatus.OFFLINE).forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st.label, color = c.textPrimary) },
                        leadingIcon = { StatusIcon(st, size = 18.dp) },
                        onClick = { onPickStatus(st); menu = false },
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(nickname, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) c.statusOnline else c.textSecondary))
                Text("#$uin", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.contactSection(
    title: String,
    rows: List<Contact>,
    collapsed: Boolean,
    keyPrefix: String,
    unread: Map<String, Int>,
    onToggle: () -> Unit,
    onOpenChat: (Int) -> Unit,
    onLongPress: (Contact) -> Unit,
) {
    if (rows.isEmpty()) return
    // Aggregate unread for the section header badge (shown when collapsed
    // so a folded section still signals new messages — iOS parity).
    val sectionUnread = rows.sumOf { unread[LocalStores.peerThread(it.uin)] ?: 0 }
    item(key = "h_$keyPrefix") {
        SectionHeader(title, rows.size, collapsed, onToggle) {
            UnreadBadge(sectionUnread)
        }
    }
    if (!collapsed) {
        items(rows, key = { "${keyPrefix}_${it.uin}" }) { ct ->
            ContactRowItem(ct, unread = unread[LocalStores.peerThread(ct.uin)] ?: 0, onClick = { onOpenChat(ct.uin) }, onLongPress = { onLongPress(ct) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupRow(group: RcqGroup, ownUin: Int, session: Session, unread: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val muted = LocalStores.isMuted(LocalStores.groupThread(group.id))
    Row(
        Modifier.fillMaxWidth().scale(scale)
            .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = onLongPress)
            .background(c.bgPrimary).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            GroupAvatar(group, session, 28.dp)
            UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(group.name, color = c.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (group.ownerUin == ownUin) Text("👑", fontSize = 11.sp)
                if (muted) Icon(Icons.Filled.NotificationsOff, null, tint = c.textSecondary, modifier = Modifier.size(11.dp))
            }
            Text(
                if (group.members.size == 1) "1 member" else "${group.members.size} members",
                color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactRowItem(contact: Contact, unread: Int, onClick: () -> Unit, onLongPress: () -> Unit) {
    val c = RcqTheme.colors
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val muted = LocalStores.isMuted(LocalStores.peerThread(contact.uin))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = onLongPress)
            .background(c.bgPrimary)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            StatusIcon(contact.presence, size = 28.dp)
            UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    contact.nickname,
                    color = if (contact.presence == UserStatus.OFFLINE) c.textSecondary else c.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                GenderIcon(contact.gender)
                if (contact.blocked) Icon(Icons.Outlined.Block, null, tint = c.statusBusy, modifier = Modifier.size(11.dp))
                if (muted) Icon(Icons.Filled.NotificationsOff, null, tint = c.textSecondary, modifier = Modifier.size(11.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("#${contact.uin}", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                val sub = when {
                    !contact.statusMessage.isNullOrEmpty() -> contact.statusMessage
                    contact.presence == UserStatus.OFFLINE && contact.lastSeen != null -> "last seen ${relativeLastSeen(contact.lastSeen)}"
                    else -> null
                }
                if (sub != null) {
                    Text(
                        "· $sub",
                        color = c.textSecondary,
                        fontSize = 12.sp,
                        fontStyle = if (!contact.statusMessage.isNullOrEmpty()) FontStyle.Italic else FontStyle.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRow(name: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    val c = RcqTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.bgPrimary).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PersonAdd, null, tint = c.accent, modifier = Modifier.size(24.dp))
        }
        Text(name, color = c.textPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("Accept", color = c.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAccept).padding(8.dp))
        Spacer(Modifier.width(4.dp))
        Text("Decline", color = c.textSecondary, modifier = Modifier.clickable(onClick = onDecline).padding(8.dp))
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = RcqTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = 60.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusIcon(UserStatus.ONLINE, size = 44.dp)
        Text("No contacts yet", color = c.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Add a friend by their UIN, or share your QR code to get started.", color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        CapsuleButton("Add a contact", onClick = onAdd)
    }
}

@Composable
private fun BottomBar(onAdd: () -> Unit, onQr: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    val c = RcqTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(c.bgSecondary)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(Icons.Filled.PersonAdd, "Add", onAdd)
        BarButton(Icons.Filled.QrCode2, "QR", onQr)
        BarButton(Icons.Filled.Search, "Search", onSearch)
        BarButton(Icons.Filled.Settings, "Settings", onSettings)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = RcqTheme.colors
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = c.textPrimary, modifier = Modifier.size(22.dp))
        Text(label, color = c.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PreviewOverlay(
    title: String,
    subtitle: String,
    avatar: @Composable () -> Unit,
    actions: List<ContextAction>,
    onDismiss: () -> Unit,
) {
    val c = RcqTheme.colors
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else 0.9f, label = "preview")

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .scale(scale)
                .padding(28.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.bgSecondary)
                .clickable(enabled = false) {}
                .fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                avatar()
                Column {
                    Text(title, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
            actions.forEach { a ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { a.onClick(); onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val tint = if (a.destructive) Color(0xFFE5484D) else c.textPrimary
                    Icon(a.icon, null, tint = tint, modifier = Modifier.size(20.dp))
                    Text(a.title, color = tint, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun SearchOverlay(contacts: List<Contact>, onClose: () -> Unit, onSelect: (Contact) -> Unit) {
    val c = RcqTheme.colors
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, contacts) {
        if (query.isBlank()) contacts
        else contacts.filter { it.nickname.contains(query, true) || it.uin.toString().contains(query) }
    }
    Column(Modifier.fillMaxSize().background(c.bgPrimary).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search contacts", color = c.textSecondary) },
                singleLine = true,
            )
            Text("Cancel", color = c.accent, modifier = Modifier.clickable(onClick = onClose).padding(12.dp))
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(filtered, key = { it.uin }) { ct ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(ct) }.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatusIcon(ct.presence, size = 26.dp)
                    Column {
                        Text(ct.nickname, color = c.textPrimary, fontSize = 15.sp)
                        Text("#${ct.uin}", color = c.textMono, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddContactDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = RcqTheme.colors
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Add contact", color = c.textPrimary) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter(Char::isDigit) },
                label = { Text("UIN", color = c.textSecondary) },
                singleLine = true,
            )
        },
        confirmButton = {
            val target = input.toIntOrNull()
            TextButton(enabled = target != null, onClick = { target?.let(onAdd) }) {
                Text("Send request", color = if (target != null) c.accent else c.textSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}

@Composable
private fun CreateGroupDialog(contacts: List<Contact>, onCreate: (String, List<Int>) -> Unit, onDismiss: () -> Unit) {
    val c = RcqTheme.colors
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateMapOf<Int, Boolean>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("New group", color = c.textPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name", color = c.textSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Add members", color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(contacts, key = { it.uin }) { ct ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected[ct.uin] = !(selected[ct.uin] ?: false) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = selected[ct.uin] ?: false, onCheckedChange = { selected[ct.uin] = it })
                            StatusIcon(ct.presence, size = 22.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(ct.nickname, color = c.textPrimary, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            val members = selected.filterValues { it }.keys.toList()
            val ok = name.isNotBlank()
            TextButton(enabled = ok, onClick = { onCreate(name.trim(), members) }) {
                Text("Create", color = if (ok) c.accent else c.textSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}

@Composable
private fun ReportDialog(name: String, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    val c = RcqTheme.colors
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Report $name", color = c.textPrimary) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason", color = c.textSecondary) },
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(enabled = reason.isNotBlank(), onClick = { onSubmit(reason.trim()) }) {
                Text("Submit", color = if (reason.isNotBlank()) Color(0xFFE5484D) else c.textSecondary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}
