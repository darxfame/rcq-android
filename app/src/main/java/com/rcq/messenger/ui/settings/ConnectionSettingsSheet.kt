package com.rcq.messenger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.service.BypassMode
import com.rcq.messenger.service.SingBoxTransport
import com.rcq.messenger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSettingsSheet(
    onDismiss: () -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Background,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Подключение", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            SettingsCard {
                Text("Режим", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                BypassModeSelector(selected = bypassMode, onSelect = viewModel::setBypassMode)
                Spacer(Modifier.height(8.dp))
                Text(
                    when (bypassMode) {
                        BypassMode.OFF -> "Только прямое подключение."
                        BypassMode.AUTO -> "Сначала прямое подключение; встроенный relay включится при сбоях."
                        BypassMode.BUILT_IN -> "Всегда использовать встроенный relay."
                        BypassMode.MANUAL -> "Всегда использовать указанный proxy."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (bypassMode == BypassMode.MANUAL) {
                SettingsCard {
                    Text("Свой proxy", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
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
                    Text("Поддерживаются socks5:// и http://", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            if (bypassMode == BypassMode.AUTO || bypassMode == BypassMode.BUILT_IN) {
                SettingsCard {
                    Text("Встроенный relay", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (singboxActive) "Активен: 127.0.0.1:${SingBoxTransport.LOCAL_PORT}"
                        else "Не активен",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (singboxActive) Online else TextSecondary
                    )
                    if (lastError != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(lastError!!, style = MaterialTheme.typography.bodySmall, color = Error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${relays.size} relay", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        TextButton(onClick = { viewModel.refreshRelays() }, enabled = !refreshing) {
                            Text(if (refreshing) "Обновляю..." else "Обновить", color = Primary)
                        }
                    }
                    if (!singboxActive && bypassMode == BypassMode.BUILT_IN) {
                        Button(
                            onClick = { viewModel.setBypassMode(BypassMode.BUILT_IN) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Запустить relay")
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
