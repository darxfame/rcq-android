package com.rcq.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcq.messenger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUin: Long?,
    nickname: String,
    recoveryPhrase: List<String>,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRecoveryPhrase by remember { mutableStateOf(false) }

    if (showRecoveryPhrase && recoveryPhrase.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showRecoveryPhrase = false },
            title = { Text("Recovery Phrase") },
            text = {
                Column {
                    Text(
                        "Your recovery phrase. Keep it safe!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    recoveryPhrase.forEachIndexed { index, word ->
                        Text(
                            "${index + 1}. $word",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecoveryPhrase = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out? Make sure you have saved your recovery phrase.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Log Out", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SettingsHeader(currentUin = currentUin, nickname = nickname)
            }

            item {
                SettingsSection(title = "Account") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Profile",
                        subtitle = "Manage your profile",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "Recovery Phrase",
                        subtitle = "View your recovery phrase",
                        onClick = { showRecoveryPhrase = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Privacy",
                        subtitle = "Last seen, read receipts",
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Preferences") {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Message, call, group sounds",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Appearance",
                        subtitle = "Theme, colors",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Storage",
                        subtitle = "Auto-download, data usage",
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Support") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version 1.0.0",
                        onClick = { }
                    )
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Help",
                        subtitle = "FAQ, contact support",
                        onClick = { }
                    )
                }
            }

            item {
                SettingsSection(title = "Danger Zone") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Log Out",
                        subtitle = "Sign out of your account",
                        onClick = { showLogoutDialog = true },
                        tint = Error
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettingsHeader(currentUin: Long?, nickname: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nickname.take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.displaySmall,
                color = Primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nickname.ifEmpty { "No Name" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "RCQ ID: ${currentUin ?: "..."}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "100 TOKENS",
                style = MaterialTheme.typography.labelMedium,
                color = Primary
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = Primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary
        )
    }
}