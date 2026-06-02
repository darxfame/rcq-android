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
import com.rcq.messenger.di.PreferencesKeys
import kotlinx.coroutines.flow.map
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.domain.model.User
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val messagePreview: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.MESSAGE_PREVIEW] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val soundEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.SOUND_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val vibrationEnabled: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.VIBRATION_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val readReceipts: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.READ_RECEIPTS] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val lastSeenVisible: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.LAST_SEEN_VISIBLE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val onlineVisible: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.ONLINE_VISIBLE] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setNotificationsEnabled(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = v } } }
    fun setMessagePreview(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.MESSAGE_PREVIEW] = v } } }
    fun setSoundEnabled(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = v } } }
    fun setVibrationEnabled(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = v } } }
    fun setReadReceipts(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.READ_RECEIPTS] = v } } }
    fun setLastSeenVisible(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.LAST_SEEN_VISIBLE] = v } } }
    fun setOnlineVisible(v: Boolean) { viewModelScope.launch { dataStore.edit { it[PreferencesKeys.ONLINE_VISIBLE] = v } } }

    val darkTheme: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.DARK_THEME] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val retroMode: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.RETRO_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val amoledTheme: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.AMOLED_THEME] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val highContrast: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.HIGH_CONTRAST] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val compactMode: StateFlow<Boolean> = dataStore.data
        .map { it[PreferencesKeys.COMPACT_MODE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.DARK_THEME] = enabled } }
    }

    fun setRetroMode(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.RETRO_MODE] = enabled } }
    }

    fun setCompactMode(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.COMPACT_MODE] = enabled } }
    }

    fun setAmoledTheme(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.AMOLED_THEME] = enabled } }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.HIGH_CONTRAST] = enabled } }
    }

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
    onNavigateToStealth: () -> Unit = {},
    onNavigateToPin: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
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
    val retroMode by viewModel.retroMode.collectAsState()
    val amoledTheme by viewModel.amoledTheme.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val compactMode by viewModel.compactMode.collectAsState()

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

    val rcq = LocalRCQColors.current
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
                    containerColor = rcq.bgPrimary,
                    titleContentColor = rcq.textPrimary
                )
            )
        },
        containerColor = rcq.bgPrimary
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
                SettingsSection(title = "Notifications") {
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Preview,
                        title = "Message preview",
                        checked = messagePreview,
                        onCheckedChange = { viewModel.setMessagePreview(it) },
                        enabled = notificationsEnabled
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound",
                        checked = soundEnabled,
                        onCheckedChange = { viewModel.setSoundEnabled(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.setVibrationEnabled(it) }
                    )
                }
            }

            item {
                SettingsSection(title = "Privacy") {
                    SettingsToggleItem(
                        icon = Icons.Default.DoneAll,
                        title = "Read receipts",
                        checked = readReceipts,
                        onCheckedChange = { viewModel.setReadReceipts(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.AccessTime,
                        title = "Show last seen",
                        checked = lastSeenVisible,
                        onCheckedChange = { viewModel.setLastSeenVisible(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Circle,
                        title = "Show online status",
                        checked = onlineVisible,
                        onCheckedChange = { viewModel.setOnlineVisible(it) }
                    )
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsToggleItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark theme",
                        checked = darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.History,
                        title = "JIMM Retro Mode",
                        subtitle = "ICQ-style contact groups & layout",
                        checked = retroMode,
                        onCheckedChange = { viewModel.setRetroMode(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.BrightnessLow,
                        title = "AMOLED Black",
                        subtitle = "Pure black background (OLED screens)",
                        checked = amoledTheme,
                        onCheckedChange = { viewModel.setAmoledTheme(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.Contrast,
                        title = "High Contrast",
                        subtitle = "WCAG AAA — maximum readability",
                        checked = highContrast,
                        onCheckedChange = { viewModel.setHighContrast(it) }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.ViewCompact,
                        title = "Compact Mode",
                        subtitle = "Уменьшенные отступы и строки",
                        checked = compactMode,
                        onCheckedChange = { viewModel.setCompactMode(it) }
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
                SettingsSection(title = "Stealth Mode") {
                    SettingsItem(
                        icon = Icons.Default.VpnKey,
                        title = "Proxy & SingBox",
                        subtitle = "Configure stealth transport",
                        onClick = onNavigateToStealth
                    )
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Panic PIN",
                        subtitle = "Decoy and wipe PIN codes",
                        onClick = onNavigateToPin
                    )
                    SettingsItem(
                        icon = Icons.Default.NetworkCheck,
                        title = "Connection Diagnostics",
                        subtitle = "Check proxy and relay status",
                        onClick = onNavigateToDiagnostics
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
    val rcq = LocalRCQColors.current
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
                .background(rcq.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nickname.take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.displaySmall,
                color = rcq.accent
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

/** Flat QIP-style section — no card, just label + divider-separated rows */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val rcq = LocalRCQColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = RCQMetrics.dividerThick, color = rcq.divider)
        Text(
            text = title.uppercase(),
            fontSize = RCQFontSize.sectionLabel,
            fontWeight = FontWeight.SemiBold,
            color = rcq.accent,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
        HorizontalDivider(thickness = RCQMetrics.dividerThick, color = rcq.divider)
        Column(modifier = Modifier.fillMaxWidth().background(rcq.bgSecondary)) {
            content()
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
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
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = RCQFontSize.nickname, color = TextPrimary)
            Text(subtitle, fontSize = RCQFontSize.caption, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(thickness = RCQMetrics.dividerThick, color = SurfaceVariant.copy(alpha = 0.4f))
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
    val rcq = LocalRCQColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = { onCheckedChange(!checked) })
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (enabled) rcq.accent else rcq.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = RCQFontSize.nickname, color = if (enabled) rcq.textPrimary else rcq.textSecondary)
                if (subtitle != null) Text(subtitle, fontSize = RCQFontSize.caption, color = rcq.textSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedThumbColor = rcq.accent, checkedTrackColor = rcq.accent.copy(alpha = 0.3f))
            )
        }
        HorizontalDivider(thickness = RCQMetrics.dividerThick, color = rcq.divider)
    }
}
