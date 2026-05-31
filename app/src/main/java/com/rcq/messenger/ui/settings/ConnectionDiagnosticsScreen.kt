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
import com.rcq.messenger.data.api.RCQApiService
import com.rcq.messenger.data.websocket.WebSocketService
import com.rcq.messenger.service.ProxyManager
import com.rcq.messenger.service.SingBoxTransport
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class DiagLine(val label: String, val value: String, val ok: Boolean)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val singBox: SingBoxTransport,
    private val proxyManager: ProxyManager,
    private val api: RCQApiService,
    private val wsService: WebSocketService
) : ViewModel() {

    val lines = MutableStateFlow<List<DiagLine>>(emptyList())
    val running = MutableStateFlow(false)

    fun run() {
        viewModelScope.launch {
            running.value = true
            val out = mutableListOf<DiagLine>()

            // Active server domain — verify endpoints point at the right host
            out += DiagLine("Сервер", BuildConfig.API_BASE_URL, ok = true)
            lines.value = out.toList()

            // Transport / proxy status
            out += DiagLine(
                label = "Транспорт",
                value = "enabled=${singBox.isEnabled}  active=${singBox.isActive}  port=${SingBoxTransport.LOCAL_PORT}",
                ok = true
            )
            singBox.lastStartError?.let {
                out += DiagLine("Ошибка транспорта", it, ok = false)
            }
            out += DiagLine("Прокси", proxyManager.stealthStatusLabel(), ok = true)

            // WebSocket connection state
            val wsState = wsService.connectionState.value
            out += DiagLine(
                label = "WebSocket",
                value = wsState.name.lowercase().replace('_', ' '),
                ok = wsState.name == "CONNECTED"
            )
            lines.value = out.toList()

            // HTTP health (no auth — direct TCP probe)
            out += apiTest("GET /health (прямой)") {
                withContext(Dispatchers.IO) {
                    val conn = java.net.URL("${BuildConfig.API_BASE_URL}health")
                        .openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 5_000; conn.readTimeout = 5_000
                    val code = conn.responseCode
                    conn.disconnect()
                    code to null
                }
            }
            lines.value = out.toList()

            // Authenticated endpoints via Retrofit (uses proxy + auth interceptor)
            out += apiTest("GET /users/me/info") { api.getCurrentUser().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /contacts") { api.getContacts().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /contacts/pending") { api.getContactRequests().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /chats") { api.getChats().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /messages/queue") { api.getMessageQueue().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /settings") { api.getSettings().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /groups") { api.getGroups().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /stories") { api.getStories().code() to null }
            lines.value = out.toList()

            out += apiTest("GET /rooms") { api.getAudioRooms().code() to null }
            lines.value = out.toList()

            running.value = false
        }
    }

    private suspend fun apiTest(label: String, block: suspend () -> Pair<Int, Any?>): DiagLine {
        val t0 = System.currentTimeMillis()
        val (msg, ok) = withTimeoutOrNull(8_000L) {
            runCatching {
                val (code, _) = block()
                val ms = System.currentTimeMillis() - t0
                when (code) {
                    in 200..299 -> "HTTP $code — ${ms}ms" to true
                    // 401/403 prove the connection works end-to-end — only auth is missing.
                    401, 403 -> "HTTP $code соединение OK (нужна авторизация) — ${ms}ms" to true
                    404, 405 -> "HTTP $code эндпоинт недоступен — ${ms}ms" to false
                    else -> "HTTP $code — ${ms}ms" to false
                }
            }.getOrElse { e -> "FAIL — ${e.message?.take(80)}" to false }
        } ?: ("Таймаут 8s" to false)
        return DiagLine(label, msg, ok)
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
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 8.dp).size(24.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
