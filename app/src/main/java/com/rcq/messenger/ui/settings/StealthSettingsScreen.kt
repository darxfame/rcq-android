package com.rcq.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.service.ProxyManager
import com.rcq.messenger.service.RelayConfigRepository
import com.rcq.messenger.service.SingBoxTransport
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StealthViewModel @Inject constructor(
    val proxyManager: ProxyManager,
    val singBox: SingBoxTransport,
    val relayRepo: RelayConfigRepository
) : ViewModel() {

    val singboxEnabled = MutableStateFlow(singBox.isEnabled)
    val singboxActive = MutableStateFlow(singBox.isActive)
    val lastError = MutableStateFlow(singBox.lastStartError)
    val manualProxy = MutableStateFlow(proxyManager.manualProxyUrl)
    val autoDisabled = MutableStateFlow(proxyManager.singboxAutoDisabled)
    val refreshing = MutableStateFlow(false)
    private val _statusLabel = MutableStateFlow(proxyManager.stealthStatusLabel())
    val statusLabel: StateFlow<String> = _statusLabel

    fun refreshRelays() {
        viewModelScope.launch {
            refreshing.value = true
            relayRepo.refreshInBackground()
            refreshing.value = false
        }
    }

    fun setSingboxEnabled(on: Boolean) {
        viewModelScope.launch {
            singBox.setEnabled(on)
            singboxEnabled.value = on
            singboxActive.value = singBox.isActive
            lastError.value = singBox.lastStartError
            updateStatus()
        }
    }

    fun setAutoDisabled(on: Boolean) {
        proxyManager.singboxAutoDisabled = on
        autoDisabled.value = on
        updateStatus()
    }

    fun saveManualProxy(url: String) {
        proxyManager.manualProxyUrl = url
        manualProxy.value = url
        updateStatus()
    }

    private fun updateStatus() { _statusLabel.value = proxyManager.stealthStatusLabel() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthSettingsScreen(
    onBack: () -> Unit,
    viewModel: StealthViewModel = hiltViewModel()
) {
    val singboxEnabled by viewModel.singboxEnabled.collectAsState()
    val singboxActive by viewModel.singboxActive.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val manualProxy by viewModel.manualProxy.collectAsState()
    val autoDisabled by viewModel.autoDisabled.collectAsState()
    val statusLabel by viewModel.statusLabel.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()

    var draftProxy by remember(manualProxy) { mutableStateOf(manualProxy) }
    val relayCount = remember { viewModel.relayRepo.currentRelays().size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Режим стелс", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Surface, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val statusColor = when {
                    manualProxy.isNotBlank() -> Primary
                    singboxActive -> Warning
                    else -> Online
                }
                Box(Modifier.size(8.dp).background(statusColor, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextPrimary
                )
            }

            // Sing-box toggle
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sing-box (авто)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("VLESS+Reality / Hysteria2 — встроенный прокси",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = singboxEnabled,
                        onCheckedChange = { viewModel.setSingboxEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = Primary.copy(alpha = 0.4f)
                        )
                    )
                }
                if (lastError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(lastError!!, style = MaterialTheme.typography.bodySmall, color = Error)
                }
                if (singboxActive) {
                    Spacer(Modifier.height(4.dp))
                    Text("Активен: 127.0.0.1:${SingBoxTransport.LOCAL_PORT}",
                        style = MaterialTheme.typography.bodySmall, color = Online)
                }
            }

            // Auto-disable toggle
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Отключить авто-включение", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Для пользователей с собственным VPN",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = autoDisabled,
                        onCheckedChange = { viewModel.setAutoDisabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = Primary.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Manual proxy
            SettingsCard {
                Text("Ручной прокси", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftProxy,
                    onValueChange = { draftProxy = it },
                    placeholder = { Text("socks5://127.0.0.1:1080", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    trailingIcon = {
                        if (draftProxy != manualProxy) {
                            IconButton(onClick = { viewModel.saveManualProxy(draftProxy) }) {
                                Icon(Icons.Default.Check, null, tint = Primary)
                            }
                        }
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text("socks5:// или http://. Пусто — прямое подключение.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            // Relay refresh
            SettingsCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Ретрансляторы", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("$relayCount реле доступно",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                    } else {
                        TextButton(onClick = { viewModel.refreshRelays() }) {
                            Text("Обновить", color = Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(com.rcq.messenger.ui.theme.Surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}
