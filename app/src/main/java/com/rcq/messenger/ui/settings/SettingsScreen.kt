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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.User
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val KEY_NICKNAME = stringPreferencesKey("nickname")

    val editNickname = MutableStateFlow("")
    val editBio = MutableStateFlow("")
    val showEditDialog = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val error: MutableStateFlow<String?> = MutableStateFlow(null)

    val notificationsEnabled = MutableStateFlow(true)
    val messagePreview = MutableStateFlow(true)
    val soundEnabled = MutableStateFlow(true)
    val vibrationEnabled = MutableStateFlow(true)
    val readReceipts = MutableStateFlow(true)
    val lastSeenVisible = MutableStateFlow(true)
    val onlineVisible = MutableStateFlow(true)
    val darkTheme = MutableStateFlow(false)

    private var currentUser: User? = null

    fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().onSuccess { user ->
                currentUser = user
            }
        }
    }

    fun openEditDialog(currentNickname: String, currentBio: String) {
        editNickname.value = currentNickname
        editBio.value = currentBio
        error.value = null
        showEditDialog.value = true
    }

    fun saveProfile() {
        val user = currentUser ?: return
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            userRepository.updateProfile(
                user.copy(nickname = editNickname.value, bio = editBio.value)
            ).onSuccess { updated ->
                dataStore.edit { prefs ->
                    prefs[KEY_NICKNAME] = updated.nickname
                }
                showEditDialog.value = false
            }.onFailure { e ->
                error.value = e.message ?: "Failed to update profile"
            }
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUin: Long?,
    nickname: String,
    recoveryPhrase: List<String>,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRecoveryPhrase by remember { mutableStateOf(false) }

    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val editNickname by viewModel.editNickname.collectAsState()
    val editBio by viewModel.editBio.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val messagePreview by viewModel.messagePreview.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val readReceipts by viewModel.readReceipts.collectAsState()
    val lastSeenVisible by viewModel.lastSeenVisible.collectAsState()
    val onlineVisible by viewModel.onlineVisible.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showEditDialog.value = false },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editNickname,
                        onValueChange = { viewModel.editNickname.value = it },
                        label = { Text("Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { viewModel.editBio.value = it },
                        label = { Text("Bio") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            color = Error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveProfile() },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEditDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                        icon = Icons.Default.Edit,
                        title = "Edit Profile",
                        subtitle = "Change nickname and bio",
                        onClick = { viewModel.openEditDialog(nickname, "") }
                    )
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
                SettingsSection(title = "Notifications") {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.notificationsEnabled.value = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Preview,
                        title = "Message preview",
                        checked = messagePreview,
                        onCheckedChange = { viewModel.messagePreview.value = it },
                        enabled = notificationsEnabled
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.soundEnabled.value = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.vibrationEnabled.value = it }
                    )
                }
            }

            item {
                SettingsSection(title = "Privacy") {
                    SettingsToggleItem(
                        icon = Icons.Default.DoneAll,
                        title = "Read receipts",
                        checked = readReceipts,
                        onCheckedChange = { viewModel.readReceipts.value = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.AccessTime,
                        title = "Show last seen",
                        checked = lastSeenVisible,
                        onCheckedChange = { viewModel.lastSeenVisible.value = it }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Circle,
                        title = "Show online status",
                        checked = onlineVisible,
                        onCheckedChange = { viewModel.onlineVisible.value = it }
                    )
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsToggleItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark theme",
                        checked = darkTheme,
                        onCheckedChange = { viewModel.darkTheme.value = it }
                    )
                }
            }

            item {
                SettingsSection(title = "Storage") {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "Cache",
                        subtitle = "Calculating...",
                        onClick = {}
                    )
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Clear cache",
                        subtitle = "Free up storage space",
                        onClick = {}
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

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Primary else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) TextPrimary else TextSecondary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
