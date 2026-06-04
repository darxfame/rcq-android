package com.rcq.messenger.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.di.PreferencesKeys
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQFontSize
import com.rcq.messenger.ui.theme.RCQMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    val readReceipts = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.READ_RECEIPTS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val typingIndicator = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.TYPING_INDICATOR] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val lastSeenVisibility = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.LAST_SEEN_VISIBILITY] ?: "contacts" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "contacts")

    val onlineVisibility = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.ONLINE_VISIBILITY] ?: "contacts" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "contacts")

    val profilePhotoVisibility = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] ?: "everyone" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "everyone")

    val groupInvitePolicy = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.GROUP_INVITE_POLICY] ?: "contacts" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "contacts")

    val genderVisibility = dataStore.data
        .map { prefs -> prefs[PreferencesKeys.GENDER_VISIBILITY] ?: "nobody" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "nobody")

    fun setReadReceipts(on: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.READ_RECEIPTS] = on } }
    }

    fun setTypingIndicator(on: Boolean) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.TYPING_INDICATOR] = on } }
    }

    fun setLastSeenVisibility(value: String) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.LAST_SEEN_VISIBILITY] = value } }
    }

    fun setOnlineVisibility(value: String) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.ONLINE_VISIBILITY] = value } }
    }

    fun setProfilePhotoVisibility(value: String) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = value } }
    }

    fun setGroupInvitePolicy(value: String) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.GROUP_INVITE_POLICY] = value } }
    }

    fun setGenderVisibility(value: String) {
        viewModelScope.launch { dataStore.edit { it[PreferencesKeys.GENDER_VISIBILITY] = value } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    onBlockedUsers: () -> Unit = {},
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val rcq = LocalRCQColors.current
    val readReceipts by viewModel.readReceipts.collectAsState()
    val typingIndicator by viewModel.typingIndicator.collectAsState()
    val lastSeenVisibility by viewModel.lastSeenVisibility.collectAsState()
    val onlineVisibility by viewModel.onlineVisibility.collectAsState()
    val profilePhotoVisibility by viewModel.profilePhotoVisibility.collectAsState()
    val groupInvitePolicy by viewModel.groupInvitePolicy.collectAsState()
    val genderVisibility by viewModel.genderVisibility.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy", color = rcq.textPrimary) },
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
                PrivacySection("Messages") {
                    SwitchTile(
                        icon = Icons.Default.DoneAll,
                        title = "Read receipts",
                        subtitle = "Show when you've read messages",
                        checked = readReceipts,
                        onToggle = viewModel::setReadReceipts
                    )
                    SwitchTile(
                        icon = Icons.Default.Visibility,
                        title = "Typing indicator",
                        subtitle = "Show when you're typing",
                        checked = typingIndicator,
                        onToggle = viewModel::setTypingIndicator
                    )
                }
            }
            item {
                PrivacySection("Profile") {
                    VisibilityTile(Icons.Default.Schedule, "Last seen", lastSeenVisibility, viewModel::setLastSeenVisibility)
                    VisibilityTile(Icons.Default.Visibility, "Online status", onlineVisibility, viewModel::setOnlineVisibility)
                    VisibilityTile(Icons.Default.PhotoCamera, "Profile photo", profilePhotoVisibility, viewModel::setProfilePhotoVisibility)
                    VisibilityTile(Icons.Default.Wc, "Gender", genderVisibility, viewModel::setGenderVisibility)
                }
            }
            item {
                PrivacySection("Groups") {
                    VisibilityTile(Icons.Default.Groups, "Group invites", groupInvitePolicy, viewModel::setGroupInvitePolicy)
                }
            }
            item {
                PrivacySection("Safety") {
                    NavigationTile(Icons.Default.Block, "Blocked users", "Manage blocked contacts", onBlockedUsers)
                }
            }
            item { Spacer(Modifier.height(RCQMetrics.rowHPad)) }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val rcq = LocalRCQColors.current
    Column {
        Text(
            text = title.uppercase(),
            color = rcq.accent,
            fontSize = RCQFontSize.sectionLabel,
            modifier = Modifier.padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowVPad)
        )
        Column(content = content)
    }
}

@Composable
private fun SwitchTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val rcq = LocalRCQColors.current
    ListItem(
        headlineContent = { Text(title, color = rcq.textPrimary) },
        supportingContent = { Text(subtitle, color = rcq.textSecondary) },
        leadingContent = { Icon(icon, contentDescription = null, tint = rcq.accent) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = rcq.accent, checkedTrackColor = rcq.accent.copy(alpha = 0.3f))
            )
        },
        colors = ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
    HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)
}

@Composable
private fun VisibilityTile(
    icon: ImageVector,
    title: String,
    current: String,
    onChange: (String) -> Unit
) {
    val rcq = LocalRCQColors.current
    var show by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title, color = rcq.textPrimary) },
        supportingContent = { Text(current.visibilityLabel(), color = rcq.textSecondary) },
        leadingContent = { Icon(icon, contentDescription = null, tint = rcq.accent) },
        trailingContent = { Text(current.visibilityLabel(), color = rcq.accent, style = MaterialTheme.typography.labelMedium) },
        modifier = Modifier.clickable { show = true },
        colors = ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
    HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(title, color = rcq.textPrimary) },
            text = {
                Column {
                    listOf("everyone", "contacts", "nobody").forEach { option ->
                        ListItem(
                            headlineContent = { Text(option.visibilityLabel(), color = rcq.textPrimary) },
                            modifier = Modifier.clickable {
                                onChange(option)
                                show = false
                            },
                            colors = ListItemDefaults.colors(containerColor = rcq.bgPrimary)
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = rcq.bgPrimary
        )
    }
}

@Composable
private fun NavigationTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val rcq = LocalRCQColors.current
    ListItem(
        headlineContent = { Text(title, color = rcq.textPrimary) },
        supportingContent = { Text(subtitle, color = rcq.textSecondary) },
        leadingContent = { Icon(icon, contentDescription = null, tint = rcq.accent) },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = rcq.bgPrimary)
    )
    HorizontalDivider(color = rcq.divider, thickness = RCQMetrics.dividerThick)
}

private fun String.visibilityLabel(): String = replaceFirstChar { it.uppercase() }
