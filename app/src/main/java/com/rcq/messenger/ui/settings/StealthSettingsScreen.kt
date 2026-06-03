package com.rcq.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.rcq.messenger.service.BypassMode
import com.rcq.messenger.service.ProxyManager
import com.rcq.messenger.service.RelayEntry
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

    val bypassMode = MutableStateFlow(proxyManager.bypassMode)
    val singboxActive = MutableStateFlow(singBox.isActive)
    val lastError = MutableStateFlow(singBox.lastStartError)
    val manualProxy = MutableStateFlow(proxyManager.manualProxyUrl)
    val refreshing = MutableStateFlow(false)
    val relays = MutableStateFlow(relayRepo.currentRelays())
    val selectedRelayTag = MutableStateFlow(relayRepo.selectedRelayTag)
    val customRelayError = MutableStateFlow<String?>(null)

    val statusLabel: StateFlow<String> = proxyManager.statusLabel

    // Совместимость со старым кодом
    val singboxEnabled = MutableStateFlow(singBox.isEnabled)
    val autoDisabled = MutableStateFlow(proxyManager.singboxAutoDisabled)

    fun setBypassMode(mode: BypassMode) {
        proxyManager.bypassMode = mode
        bypassMode.value = mode
        autoDisabled.value = proxyManager.singboxAutoDisabled
        when (mode) {
            BypassMode.BUILT_IN -> viewModelScope.launch {
                proxyManager.forceEnableNow()
                singboxEnabled.value = singBox.isEnabled
                singboxActive.value = singBox.isActive
                lastError.value = singBox.lastStartError
            }
            BypassMode.AUTO, BypassMode.OFF, BypassMode.MANUAL -> {
                if (singBox.isActive && mode != BypassMode.AUTO) {
                    proxyManager.stopSingBox()
                }
                if (mode != BypassMode.BUILT_IN) {
                    viewModelScope.launch {
                        singBox.setEnabled(false)
                        singboxEnabled.value = singBox.isEnabled
                        singboxActive.value = singBox.isActive
                        lastError.value = singBox.lastStartError
                    }
                }
            }
        }
    }

    fun refreshRelays() {
        viewModelScope.launch {
            refreshing.value = true
            relayRepo.refreshInBackground()
            relays.value = relayRepo.currentRelays()
            refreshing.value = false
        }
    }

    fun forceEnableBypass() {
        viewModelScope.launch {
            proxyManager.forceEnableNow()
            singboxActive.value = singBox.isActive
            singboxEnabled.value = singBox.isEnabled
            lastError.value = singBox.lastStartError
        }
    }

    private fun restartBuiltInBypassIfNeeded() {
        if (proxyManager.bypassMode != BypassMode.BUILT_IN) return
        proxyManager.stopSingBox()
        forceEnableBypass()
    }

    fun setSingboxEnabled(on: Boolean) {
        viewModelScope.launch {
            singBox.setEnabled(on)
            singboxEnabled.value = on
            singboxActive.value = singBox.isActive
            lastError.value = singBox.lastStartError
        }
    }

    fun saveManualProxy(url: String) {
        proxyManager.manualProxyUrl = url
        manualProxy.value = url
    }

    fun selectRelay(tag: String?) {
        if (tag.isNullOrBlank()) relayRepo.clearRelaySelection() else relayRepo.selectRelay(tag)
        selectedRelayTag.value = relayRepo.selectedRelayTag
        relays.value = relayRepo.currentRelays()
        restartBuiltInBypassIfNeeded()
    }

    fun addCustomVless(url: String) {
        relayRepo.addCustomVless(url)
            .onSuccess {
                customRelayError.value = null
                selectedRelayTag.value = relayRepo.selectedRelayTag
                relays.value = relayRepo.currentRelays()
                if (proxyManager.bypassMode != BypassMode.BUILT_IN) setBypassMode(BypassMode.BUILT_IN)
                else restartBuiltInBypassIfNeeded()
            }
            .onFailure { customRelayError.value = it.message ?: "Не удалось разобрать VLESS URL" }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealthSettingsScreen(
    onBack: () -> Unit,
    viewModel: StealthViewModel = hiltViewModel()
) {
    val bypassMode by viewModel.bypassMode.collectAsState()
    val singboxActive by viewModel.singboxActive.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val manualProxy by viewModel.manualProxy.collectAsState()
    val statusLabel by viewModel.statusLabel.collectAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val relays by viewModel.relays.collectAsState()
    val selectedRelayTag by viewModel.selectedRelayTag.collectAsState()
    val customRelayError by viewModel.customRelayError.collectAsState()

    var draftProxy by remember(manualProxy) { mutableStateOf(manualProxy) }

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
                val statusColor = when (bypassMode) {
                    BypassMode.OFF -> TextSecondary
                    BypassMode.MANUAL -> Primary
                    BypassMode.AUTO -> if (singboxActive) Warning else Online
                    BypassMode.BUILT_IN -> if (singboxActive) Online else Warning
                }
                Box(Modifier.size(8.dp).background(statusColor, RoundedCornerShape(50)))
                Spacer(Modifier.width(8.dp))
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextPrimary
                )
            }

            // Bypass mode selector
            SettingsCard {
                Text(
                    "Режим обхода блокировок",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                BypassModeSelector(
                    selected = bypassMode,
                    onSelect = { viewModel.setBypassMode(it) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (bypassMode) {
                        BypassMode.OFF -> "Прямое подключение без прокси."
                        BypassMode.AUTO -> "Сначала прямое подключение; встроенный relay включается только после ошибок."
                        BypassMode.MANUAL -> "Всегда использует указанный прокси-адрес."
                        BypassMode.BUILT_IN -> "Всегда использует встроенный relay."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Manual proxy input (only in MANUAL mode)
            if (bypassMode == BypassMode.MANUAL) {
                SettingsCard {
                    Text("Адрес прокси", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
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
                    Text(
                        "socks5:// или http://",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Sing-box status card (AUTO mode)
            if (bypassMode == BypassMode.AUTO || bypassMode == BypassMode.BUILT_IN) {
                SettingsCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Sing-box", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(
                                if (singboxActive) "Активен: 127.0.0.1:${SingBoxTransport.LOCAL_PORT}"
                                else if (bypassMode == BypassMode.AUTO) "Ожидает ошибок соединения"
                                else "Включается как основной маршрут",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (singboxActive) Online else TextSecondary
                            )
                        }
                        if (singboxActive) {
                            Surface(
                                color = Online.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "ON",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Online
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.forceEnableBypass() },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text("Включить сейчас", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (lastError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(lastError!!, style = MaterialTheme.typography.bodySmall, color = Error)
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Surface)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Ретрансляторы", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("${relays.size} доступно", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
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

                RelaySelectionCard(
                    relays = relays,
                    selectedTag = selectedRelayTag,
                    customRelayError = customRelayError,
                    onSelect = viewModel::selectRelay,
                    onAddCustom = viewModel::addCustomVless
                )
            }
        }
    }
}

@Composable
fun RelaySelectionCard(
    relays: List<RelayEntry>,
    selectedTag: String,
    customRelayError: String?,
    onSelect: (String?) -> Unit,
    onAddCustom: (String) -> Unit,
) {
    var draftVless by remember { mutableStateOf("") }
    SettingsCard {
        Text("Выбор обхода", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        RelayOptionRow(
            title = "Автоматически",
            subtitle = "Выбрать лучший доступный relay",
            selected = selectedTag.isBlank(),
            onClick = { onSelect(null) }
        )
        relays.forEach { relay ->
            RelayOptionRow(
                title = relay.displayName(),
                subtitle = relay.description(),
                selected = selectedTag == relay.tag,
                onClick = { onSelect(relay.tag) }
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = draftVless,
            onValueChange = { draftVless = it },
            placeholder = { Text("vless://...", color = TextSecondary) },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        if (customRelayError != null) {
            Spacer(Modifier.height(4.dp))
            Text(customRelayError, style = MaterialTheme.typography.bodySmall, color = Error)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onAddCustom(draftVless) },
            enabled = draftVless.trim().startsWith("vless://", ignoreCase = true),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Добавить VLESS")
        }
    }
}

@Composable
private fun RelayOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private fun RelayEntry.displayName(): String =
    tag.removePrefix("relay-").removePrefix("custom-")
        .replace('-', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun RelayEntry.description(): String {
    val transport = when {
        transport_type.equals("xhttp", ignoreCase = true) -> "xHTTP"
        proto.equals("hysteria2", ignoreCase = true) -> "Hysteria2"
        else -> "VLESS Reality TCP"
    }
    return "$transport · $server · SNI $sni"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassModeSelector(selected: BypassMode, onSelect: (BypassMode) -> Unit) {
    val options = listOf(
        BypassMode.OFF to "Выкл",
        BypassMode.AUTO to "Авто",
        BypassMode.BUILT_IN to "Relay",
        BypassMode.MANUAL to "Ручной"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onSelect(mode) },
                selected = selected == mode,
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Primary.copy(alpha = 0.15f),
                    activeContentColor = Primary,
                    inactiveContainerColor = Surface,
                    inactiveContentColor = TextSecondary
                )
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
