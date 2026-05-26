package com.rcq.messenger.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountRecoveryScreen(
    onBack: () -> Unit,
    onRecoveryComplete: () -> Unit,
    viewModel: AccountRecoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Восстановление аккаунта") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.step) {
                RecoveryStep.INFO -> {
                    RecoveryInfoStep(
                        onContinue = { viewModel.nextStep() }
                    )
                }
                RecoveryStep.UIN_INPUT -> {
                    UinInputStep(
                        uin = uiState.uin,
                        onUinChange = viewModel::updateUin,
                        onContinue = { viewModel.checkAccount() },
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                }
                RecoveryStep.BACKUP_KEYS -> {
                    BackupKeysStep(
                        backupData = uiState.backupData,
                        onBackupDataChange = viewModel::updateBackupData,
                        onRestore = { viewModel.restoreAccount() },
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                }
                RecoveryStep.SUCCESS -> {
                    RecoverySuccessStep(
                        onComplete = onRecoveryComplete
                    )
                }
                RecoveryStep.FAILED -> {
                    RecoveryFailedStep(
                        error = uiState.error ?: "Неизвестная ошибка",
                        onRetry = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

@Composable
fun RecoveryInfoStep(
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Primary
        )

        Text(
            text = "Восстановление аккаунта RCQ",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Важно: RCQ использует сквозное шифрование",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "• Ваши приватные ключи хранятся только на устройстве\n" +
                            "• Сервер не может восстановить ваш аккаунт\n" +
                            "• Без резервной копии ключей восстановление невозможно\n" +
                            "• Все сообщения будут потеряны навсегда",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Text(
            text = "Для восстановления вам понадобится:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecoveryRequirementItem(
                icon = Icons.Default.Person,
                text = "Ваш 9-значный UIN"
            )
            RecoveryRequirementItem(
                icon = Icons.Default.Key,
                text = "Резервная копия приватных ключей"
            )
            RecoveryRequirementItem(
                icon = Icons.Default.Fingerprint,
                text = "Данные для подтверждения личности"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Продолжить")
        }
    }
}

@Composable
fun RecoveryRequirementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun UinInputStep(
    uin: String,
    onUinChange: (String) -> Unit,
    onContinue: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Введите ваш UIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "9-значный идентификатор вашего аккаунта RCQ",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = uin,
            onValueChange = onUinChange,
            label = { Text("UIN") },
            placeholder = { Text("123456789") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = error != null
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = uin.length == 9 && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Проверить аккаунт")
            }
        }
    }
}

@Composable
fun BackupKeysStep(
    backupData: String,
    onBackupDataChange: (String) -> Unit,
    onRestore: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Восстановление ключей",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Вставьте резервную копию ваших приватных ключей",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = backupData,
            onValueChange = onBackupDataChange,
            label = { Text("Резервная копия ключей") },
            placeholder = { Text("Вставьте данные резервной копии...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            isError = error != null
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            enabled = backupData.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Восстановить аккаунт")
            }
        }
    }
}

@Composable
fun RecoverySuccessStep(
    onComplete: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Аккаунт восстановлен!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Ваш аккаунт RCQ успешно восстановлен. Теперь вы можете пользоваться мессенджером.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти в аккаунт")
        }
    }
}

@Composable
fun RecoveryFailedStep(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Восстановление не удалось",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Попробовать снова")
        }
    }
}

@HiltViewModel
class AccountRecoveryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AccountRecoveryUiState())
    val uiState: StateFlow<AccountRecoveryUiState> = _uiState.asStateFlow()

    fun updateUin(uin: String) {
        if (uin.length <= 9 && uin.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(uin = uin, error = null)
        }
    }

    fun updateBackupData(data: String) {
        _uiState.value = _uiState.value.copy(backupData = data, error = null)
    }

    fun nextStep() {
        _uiState.value = _uiState.value.copy(step = RecoveryStep.UIN_INPUT)
    }

    fun checkAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // TODO: Implement actual account check via API
                kotlinx.coroutines.delay(2000) // Simulate API call

                // For now, always proceed to backup keys step
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = RecoveryStep.BACKUP_KEYS
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Аккаунт с таким UIN не найден"
                )
            }
        }
    }

    fun restoreAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // TODO: Implement actual key restoration
                kotlinx.coroutines.delay(3000) // Simulate restoration

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = RecoveryStep.SUCCESS
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = RecoveryStep.FAILED,
                    error = "Не удалось восстановить аккаунт. Проверьте правильность резервной копии."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = AccountRecoveryUiState()
    }
}

data class AccountRecoveryUiState(
    val step: RecoveryStep = RecoveryStep.INFO,
    val uin: String = "",
    val backupData: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class RecoveryStep {
    INFO,
    UIN_INPUT,
    BACKUP_KEYS,
    SUCCESS,
    FAILED
}