package com.rcq.messenger.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
class NotificationsSettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {
    val messages = dataStore.data
        .map { it[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val groups = dataStore.data
        .map { it[PreferencesKeys.GROUP_NOTIFICATIONS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val contactOnline = dataStore.data
        .map { it[PreferencesKeys.CONTACT_ONLINE_NOTIFICATIONS] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val sounds = dataStore.data
        .map { it[PreferencesKeys.SOUND_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val vibration = dataStore.data
        .map { it[PreferencesKeys.VIBRATION_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val preview = dataStore.data
        .map { it[PreferencesKeys.MESSAGE_PREVIEW] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val led = dataStore.data
        .map { it[PreferencesKeys.NOTIFICATION_LED] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setMessages(enabled: Boolean) = set(PreferencesKeys.NOTIFICATIONS_ENABLED, enabled)
    fun setGroups(enabled: Boolean) = set(PreferencesKeys.GROUP_NOTIFICATIONS, enabled)
    fun setContactOnline(enabled: Boolean) = set(PreferencesKeys.CONTACT_ONLINE_NOTIFICATIONS, enabled)
    fun setSounds(enabled: Boolean) = set(PreferencesKeys.SOUND_ENABLED, enabled)
    fun setVibration(enabled: Boolean) = set(PreferencesKeys.VIBRATION_ENABLED, enabled)
    fun setPreview(enabled: Boolean) = set(PreferencesKeys.MESSAGE_PREVIEW, enabled)
    fun setLed(enabled: Boolean) = set(PreferencesKeys.NOTIFICATION_LED, enabled)

    private fun set(key: Preferences.Key<Boolean>, enabled: Boolean) {
        viewModelScope.launch { dataStore.edit { it[key] = enabled } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit,
    vm: NotificationsSettingsViewModel = hiltViewModel()
) {
    val rcq = LocalRCQColors.current
    val messages by vm.messages.collectAsState()
    val groups by vm.groups.collectAsState()
    val contactOnline by vm.contactOnline.collectAsState()
    val sounds by vm.sounds.collectAsState()
    val vibration by vm.vibration.collectAsState()
    val preview by vm.preview.collectAsState()
    val led by vm.led.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = rcq.textPrimary) },
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
                NotificationSection("Notifications") {
                    NotificationSwitchTile(Icons.Default.Notifications, "Messages", "Notify for new messages", messages, vm::setMessages)
                    NotificationSwitchTile(Icons.Default.Groups, "Groups", "Notify for group messages", groups, vm::setGroups)
                    NotificationSwitchTile(Icons.Default.Person, "Contact online", "When a contact comes online", contactOnline, vm::setContactOnline)
                }
            }
            item {
                NotificationSection("Sound & Haptics") {
                    NotificationSwitchTile(Icons.Default.VolumeUp, "Sounds", "Play notification sounds", sounds, vm::setSounds)
                    NotificationSwitchTile(Icons.Default.Vibration, "Vibration", "Vibrate on notification", vibration, vm::setVibration)
                }
            }
            item {
                NotificationSection("Preview") {
                    NotificationSwitchTile(Icons.Default.Preview, "Show preview", "Show message text in notifications", preview, vm::setPreview)
                    NotificationSwitchTile(Icons.Default.Lightbulb, "Notification LED", "Use notification light when available", led, vm::setLed)
                }
            }
            item { Spacer(Modifier.height(RCQMetrics.rowHPad)) }
        }
    }
}

@Composable
private fun NotificationSection(
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
private fun NotificationSwitchTile(
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
