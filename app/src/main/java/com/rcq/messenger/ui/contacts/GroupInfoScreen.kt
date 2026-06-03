package com.rcq.messenger.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.ui.theme.LocalRCQColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    onBack: () -> Unit,
    onMemberClick: (Long) -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val group by viewModel.group.collectAsState()
    val isOwnerOrAdmin by viewModel.isOwnerOrAdmin.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val rcq = LocalRCQColors.current

    LaunchedEffect(groupId) { viewModel.load(groupId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    if (isOwnerOrAdmin) {
                        IconButton(onClick = {
                            newName = group?.name.orEmpty()
                            showRenameDialog = true
                        }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(rcq.bgSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            group?.name?.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.headlineLarge,
                            color = rcq.accent
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        group?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = rcq.textPrimary
                    )
                    Text(
                        "${group?.memberIds?.size ?: 0} members",
                        style = MaterialTheme.typography.bodyMedium,
                        color = rcq.textSecondary
                    )
                    group?.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                description,
                                style = MaterialTheme.typography.bodySmall,
                                color = rcq.textSecondary
                            )
                        }
                    }
                }
            }

            group?.pinnedText?.let { pinned ->
                if (pinned.isNotBlank()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = rcq.bgSecondary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PushPin, null, tint = rcq.accent)
                                Spacer(Modifier.width(8.dp))
                                Text(pinned, style = MaterialTheme.typography.bodySmall, color = rcq.textPrimary)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Members",
                    style = MaterialTheme.typography.labelLarge,
                    color = rcq.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(group?.memberIds ?: emptyList()) { memberId ->
                val isAdmin = group?.adminIds?.contains(memberId) == true
                val isOwner = group?.ownerId == memberId
                ListItem(
                    headlineContent = { Text(memberId.toString()) },
                    supportingContent = {
                        if (isOwner) {
                            Text("Owner", color = rcq.accent)
                        } else if (isAdmin) {
                            Text("Admin", color = rcq.accent)
                        }
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(rcq.bgSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(memberId.toString().take(2), color = rcq.accent)
                        }
                    },
                    trailingContent = {
                        if (isOwnerOrAdmin && memberId != viewModel.ownUin) {
                            IconButton(onClick = { viewModel.removeMember(memberId) }) {
                                Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    modifier = Modifier.clickable { onMemberClick(memberId) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Leave group", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showLeaveDialog = true }
                )
            }
            if (isOwnerOrAdmin) {
                item {
                    ListItem(
                        headlineContent = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { showDeleteDialog = true }
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename group") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameGroup(newName)
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave group?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveGroup()
                        showLeaveDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete group?") },
            text = { Text("This will permanently delete the group for all members.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup()
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
