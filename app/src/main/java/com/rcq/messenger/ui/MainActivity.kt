package com.rcq.messenger.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.rcq.messenger.data.repository.UserRepository
import com.rcq.messenger.data.websocket.WebSocketService
import com.rcq.messenger.ui.theme.RCQTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingChatId = MutableStateFlow<String?>(null)
    private val pendingScreen = MutableStateFlow<String?>(null)
    private var stoppedAt: Long = 0L

    @Inject lateinit var webSocketService: WebSocketService
    @Inject lateinit var userRepository: UserRepository

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silently proceed — UI degrades gracefully without notifications */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val appPrefs: AppPrefsViewModel = hiltViewModel()
            val retroMode by appPrefs.retroMode.collectAsState()
            val darkTheme by appPrefs.darkTheme.collectAsState()
            val amoledTheme by appPrefs.amoledTheme.collectAsState()
            val highContrast by appPrefs.highContrast.collectAsState()
            val compactMode by appPrefs.compactMode.collectAsState()

            RCQTheme(
                darkTheme = darkTheme,
                amoledTheme = amoledTheme,
                highContrast = highContrast,
                retroMode = retroMode,
                compactMode = compactMode,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val chatId by pendingChatId.collectAsState()
                    val screen by pendingScreen.collectAsState()
                    RCQApp(initialChatId = chatId, initialScreen = screen)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val bgMs = System.currentTimeMillis() - stoppedAt
        if (stoppedAt > 0L && bgMs > 4_000L) {
            lifecycleScope.launch {
                webSocketService.reconnectIfNeeded()
            }
        }
        lifecycleScope.launch {
            runCatching { userRepository.updatePresence("online") }
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
        lifecycleScope.launch {
            runCatching { userRepository.updatePresence("away") }
        }
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra("chat_id")?.let { pendingChatId.value = it }
        intent.getStringExtra("screen")?.let { pendingScreen.value = it }
        intent.data?.let { uri ->
            when {
                (uri.scheme == "rcq" && uri.host == "add") ||
                    (uri.host == "rcq.app" && uri.pathSegments.getOrNull(0) == "u") -> {
                    uri.lastPathSegment?.toLongOrNull()?.let { uin ->
                        pendingScreen.value = "add_contact_$uin"
                    }
                }
                (uri.scheme == "rcq" && uri.host == "group") ||
                    (uri.host == "rcq.app" && uri.pathSegments.getOrNull(0) == "g") -> {
                    uri.lastPathSegment?.let { groupId ->
                        pendingScreen.value = "join_group_$groupId"
                    }
                }
                else -> Unit
            }
        }
    }
}
