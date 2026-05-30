package com.rcq.messenger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.BuildConfig
import com.rcq.messenger.service.ProxyManager
import com.rcq.messenger.service.SingBoxTransport
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DiagLine(val label: String, val value: String, val ok: Boolean)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val singBox: SingBoxTransport,
    private val proxyManager: ProxyManager
) : ViewModel() {

    val lines = MutableStateFlow<List<DiagLine>>(emptyList())
    val running = MutableStateFlow(false)

    fun run() {
        viewModelScope.launch {
            running.value = true
            val out = mutableListOf<DiagLine>()

            out += DiagLine(
                label = "Транспорт",
                value = "enabled=${singBox.isEnabled}  active=${singBox.isActive}  port=${SingBoxTransport.LOCAL_PORT}",
                ok = true
            )
            singBox.lastStartError?.let {
                out += DiagLine("Ошибка транспорта", it, ok = false)
            }
            out += DiagLine("Прокси", proxyManager.stealthStatusLabel(), ok = true)
            lines.value = out.toList()

            val t0 = System.currentTimeMillis()
            val (healthMsg, healthOk) = runCatching {
                withContext(Dispatchers.IO) {
                    val conn = java.net.URL("${BuildConfig.API_BASE_URL}health")
                        .openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "HEAD"
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    val code = conn.responseCode
                    val ms = System.currentTimeMillis() - t0
                    if (code in 200..299) "HTTP $code — ${ms}ms" to true
                    else "HTTP $code — ${ms}ms" to false
                }
            }.getOrElse { e -> "FAIL — ${e.message}" to false }
            out += DiagLine("GET /health", healthMsg, healthOk)
            lines.value = out.toList()

            running.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val lines by viewModel.lines.collectAsState()
    val running by viewModel.running.collectAsState()

    LaunchedEffect(Unit) { viewModel.run() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диагностика", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.run() }, enabled = !running) {
                        Text("Запустить", color = Primary)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            lines.forEach { line ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (line.ok) "✓" else "✗",
                        color = if (line.ok) Online else Error,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column {
                        Text(line.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(
                            line.value,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextSecondary
                        )
                    }
                }
            }
            if (running) {
                CircularProgressIndicator(Modifier.padding(top = 8.dp).size(24.dp), color = Primary, strokeWidth = 2.dp)
            }
        }
    }
}
