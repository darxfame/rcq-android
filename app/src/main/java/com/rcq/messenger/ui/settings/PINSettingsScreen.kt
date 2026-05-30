package com.rcq.messenger.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.service.PanicPINManager
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PINSettingsViewModel @Inject constructor(val panicPIN: PanicPINManager) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val success = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)

    fun setRealPIN(pin: String) = run("PIN установлен") { panicPIN.setRealPIN(pin) }
    fun setDecoyPIN(pin: String) = run("PIN-приманка установлен") { panicPIN.setDecoyPIN(pin) }
    fun setWipePIN(pin: String) = run("PIN-стирание установлен") { panicPIN.setWipePIN(pin) }
    fun removeAllPINs() = run("Все PIN удалены") { panicPIN.removeAllPINs() }

    private fun run(successMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            loading.value = true; error.value = null; success.value = null
            runCatching { block() }
                .onSuccess { success.value = successMsg }
                .onFailure { error.value = it.message }
            loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PINSettingsScreen(
    onBack: () -> Unit,
    viewModel: PINSettingsViewModel = hiltViewModel()
) {
    val mode by viewModel.panicPIN.mode.collectAsState()
    val isConfigured = viewModel.panicPIN.isConfigured
    val hasDecoy = viewModel.panicPIN.hasDecoyPIN
    val hasWipe = viewModel.panicPIN.hasWipePIN
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var realPin by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var wipePin by remember { mutableStateOf("") }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Удалить все PIN?", color = TextPrimary) },
            text = { Text("Все PIN-коды будут удалены. Доступ без PIN.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showRemoveConfirm = false; viewModel.removeAllPINs() }) {
                    Text("Удалить", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) { Text("Отмена", color = Primary) }
            },
            containerColor = com.rcq.messenger.ui.theme.Surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panic PIN", color = TextPrimary) },
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
            // Status card
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(com.rcq.messenger.ui.theme.Surface, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Статус: ${if (isConfigured) "настроен" else "не настроен"}",
                        style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Режим сессии: ${mode.name}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    if (isConfigured) {
                        Text("Приманка: ${if (hasDecoy) "✓" else "—"}  |  Стирание: ${if (hasWipe) "✓" else "—"}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            error?.let { Text(it, color = Error, style = MaterialTheme.typography.bodySmall) }
            success?.let { Text(it, color = Online, style = MaterialTheme.typography.bodySmall) }

            PinSection(
                title = "Основной PIN",
                description = if (!isConfigured) "Создать PIN для входа" else "Изменить основной PIN",
                value = realPin, onValueChange = { realPin = it },
                onSave = { viewModel.setRealPIN(realPin); realPin = "" },
                loading = loading
            )

            if (mode == PanicPINManager.SessionMode.real) {
                PinSection(
                    title = "PIN-приманка",
                    description = if (hasDecoy) "Изменить PIN-приманку" else "Открывает пустой аккаунт",
                    value = decoyPin, onValueChange = { decoyPin = it },
                    onSave = { viewModel.setDecoyPIN(decoyPin); decoyPin = "" },
                    loading = loading
                )
                PinSection(
                    title = "PIN-стирание",
                    description = if (hasWipe) "Изменить PIN-стирание" else "Удаляет все данные",
                    value = wipePin, onValueChange = { wipePin = it },
                    onSave = { viewModel.setWipePIN(wipePin); wipePin = "" },
                    loading = loading, accentColor = Error
                )
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                ) { Text("Удалить все PIN") }
            }
        }
    }
}

@Composable
private fun PinSection(
    title: String, description: String,
    value: String, onValueChange: (String) -> Unit,
    onSave: () -> Unit, loading: Boolean,
    accentColor: Color = Primary
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(com.rcq.messenger.ui.theme.Surface, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            OutlinedTextField(
                value = value, onValueChange = onValueChange,
                placeholder = { Text("≥ 4 цифры", color = TextSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )
            Button(
                onClick = onSave, enabled = value.length >= 4 && !loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Сохранить")
            }
        }
    }
}
