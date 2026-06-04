package com.rcq.messenger.ui.contacts

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.domain.model.Group
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.ui.common.StatusIndicator
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQFontSize
import com.rcq.messenger.ui.theme.RCQMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactInfoScreen(
    userId: Long,
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    onCall: (Long) -> Unit,
    viewModel: ContactInfoViewModel = hiltViewModel()
) {
    val contact by viewModel.contact.collectAsState()
    val commonGroups by viewModel.commonGroups.collectAsState()
    val error by viewModel.error.collectAsState()
    val rcq = LocalRCQColors.current

    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact", color = rcq.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = rcq.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
            )
        },
        containerColor = rcq.bgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                ContactHeader(contact = contact, userId = userId)
            }

            if (error != null) {
                item {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowVPad)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RCQMetrics.screenHPad),
                    horizontalArrangement = Arrangement.spacedBy(RCQMetrics.rowHPad)
                ) {
                    ActionChip(Icons.Default.Chat, "Message", Modifier.weight(1f)) {
                        viewModel.openChat(userId, onChat)
                    }
                    ActionChip(Icons.Default.Call, "Call", Modifier.weight(1f)) {
                        onCall(userId)
                    }
                    ActionChip(Icons.Default.Videocam, "Video", Modifier.weight(1f)) {
                        onCall(userId)
                    }
                    ActionChip(Icons.Default.Block, "Block", Modifier.weight(1f), destructive = true) {
                        viewModel.blockContact(userId)
                    }
                }
                Spacer(Modifier.height(RCQMetrics.rowHPad))
            }

            item { SectionHeader("Mutual Contacts") }
            item {
                InfoRow(
                    icon = Icons.Default.Groups,
                    title = "Mutual contacts",
                    subtitle = "0 contacts"
                )
            }

            item { SectionHeader("Common Groups") }
            if (commonGroups.isEmpty()) {
                item {
                    Text(
                        text = "No common groups",
                        color = rcq.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowHPad)
                    )
                }
            } else {
                items(commonGroups, key = { it.id }) { group ->
                    CommonGroupRow(group)
                    HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)
                }
            }

            item {
                Spacer(Modifier.height(RCQMetrics.rowHPad))
                HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)
                DestructiveRow(Icons.Default.Block, "Block") {
                    viewModel.blockContact(userId)
                }
                DestructiveRow(Icons.Default.Flag, "Report") {
                    viewModel.blockContact(userId)
                }
                DestructiveRow(Icons.Default.PersonRemove, "Remove contact") {
                    viewModel.removeContact(userId, onBack)
                }
            }
        }
    }
}

@Composable
private fun ContactHeader(contact: Contact?, userId: Long) {
    val rcq = LocalRCQColors.current
    val context = LocalContext.current
    val nickname = contact?.customNickname?.takeIf { it.isNotBlank() }
        ?: contact?.nickname?.takeIf { it.isNotBlank() }
        ?: userId.toString()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(RCQMetrics.screenHPad * 2)
    ) {
        Box(Modifier.size(RCQMetrics.avatarLg * 2.2f)) {
            Box(
                modifier = Modifier
                    .size(RCQMetrics.avatarLg * 2.2f)
                    .clip(CircleShape)
                    .background(rcq.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nickname.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = RCQFontSize.bubble * 2,
                    fontWeight = FontWeight.Bold
                )
            }
            StatusIndicator(
                status = contact?.status ?: UserStatus.OFFLINE,
                size = 14,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.height(RCQMetrics.rowHPad))
        Text(
            text = nickname,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = rcq.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        contact?.statusMessage?.takeIf { it.isNotBlank() }?.let { msg ->
            Text(
                text = msg,
                color = rcq.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(RCQMetrics.rowVPad))
        Row(
            modifier = Modifier.clickable {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("UIN", userId.toString()))
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UIN: $userId",
                color = rcq.textSecondary,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.width(RCQMetrics.rowVPad))
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UIN", tint = rcq.textSecondary, modifier = Modifier.size(RCQMetrics.avatarSm))
        }
        Text(
            text = lastSeenText(contact),
            color = rcq.textSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val rcq = LocalRCQColors.current
    val tint = if (destructive) MaterialTheme.colorScheme.error else rcq.accent
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(RCQMetrics.bubbleRadius),
        color = rcq.bgSecondary
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = RCQMetrics.rowHPad)
        ) {
            Icon(icon, contentDescription = label, tint = tint)
            Spacer(Modifier.height(RCQMetrics.rowVPad))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val rcq = LocalRCQColors.current
    Text(
        text = title.uppercase(),
        color = rcq.textSecondary,
        fontSize = RCQFontSize.sectionLabel,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(rcq.bgSecondary)
            .padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowVPad)
    )
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    val rcq = LocalRCQColors.current
    ListItem(
        headlineContent = { Text(title, color = rcq.textPrimary) },
        supportingContent = { Text(subtitle, color = rcq.textSecondary) },
        leadingContent = { Icon(icon, contentDescription = null, tint = rcq.accent) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
}

@Composable
private fun CommonGroupRow(group: Group) {
    val rcq = LocalRCQColors.current
    ListItem(
        headlineContent = { Text(group.name, color = rcq.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${group.memberCount} members", color = rcq.textSecondary) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(RCQMetrics.avatarLg)
                    .clip(CircleShape)
                    .background(rcq.bgSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text(group.name.firstOrNull()?.uppercase() ?: "#", color = rcq.accent, fontWeight = FontWeight.Bold)
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
}

@Composable
private fun DestructiveRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    val rcq = LocalRCQColors.current
    ListItem(
        headlineContent = { Text(title, color = MaterialTheme.colorScheme.error) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
}

private fun lastSeenText(contact: Contact?): String {
    return when {
        contact?.status == UserStatus.ONLINE -> "Online"
        !contact?.lastSeen.isNullOrBlank() -> "Last seen ${contact?.lastSeen}"
        else -> "Offline"
    }
}
