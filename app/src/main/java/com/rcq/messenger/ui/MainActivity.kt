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
import com.rcq.messenger.ui.theme.RCQTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingChatId = MutableStateFlow<String?>(null)
    private val pendingScreen = MutableStateFlow<String?>(null)

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

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra("chat_id")?.let { pendingChatId.value = it }
        intent.getStringExtra("screen")?.let { pendingScreen.value = it }
    }
}