package com.rcq.messenger.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.CallRepository
import com.rcq.messenger.domain.model.Call
import com.rcq.messenger.domain.model.CallStatus
import com.rcq.messenger.domain.model.CallType
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {

    val calls: StateFlow<List<Call>> = callRepository.getCalls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initiateCall(targetId: Long, type: CallType) {
        viewModelScope.launch {
            _isLoading.value = true
            callRepository.initiateCall(targetId, type)
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    viewModel: CallsViewModel = hiltViewModel(),
    onCallClick: (String) -> Unit
) {
    val calls by viewModel.calls.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Calls",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        if (calls.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No calls yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your call history will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(calls) { call ->
                    CallItem(
                        call = call,
                        onClick = { onCallClick(call.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CallItem(
    call: Call,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = call.targetNickname.firstOrNull()?.uppercase() ?: "?",
                color = Primary,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = call.targetNickname,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (call.type) {
                        CallType.AUDIO -> Icons.Default.Call
                        CallType.VIDEO -> Icons.Default.Videocam
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when (call.status) {
                        CallStatus.MISSED -> Error
                        CallStatus.DECLINED -> Error
                        else -> TextSecondary
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (call.status) {
                        CallStatus.MISSED -> "Missed"
                        CallStatus.DECLINED -> "Declined"
                        CallStatus.ENDED -> formatDuration(call.duration)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (call.status) {
                        CallStatus.MISSED -> Error
                        CallStatus.DECLINED -> Error
                        else -> TextSecondary
                    }
                )
            }
        }

        Text(
            text = call.startedAt?.let { formatDate(it) } ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { /* Call back */ }) {
            Icon(
                Icons.Default.Call,
                contentDescription = "Call back",
                tint = Primary
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        minutes > 0 -> String.format("%d:%02d", minutes, seconds)
        else -> "0:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604800000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun IncomingCallScreen(
    call: Call,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = call.targetNickname.firstOrNull()?.uppercase() ?: "?",
                color = Primary,
                style = MaterialTheme.typography.displayMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = call.targetNickname,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Incoming ${if (call.type == CallType.VIDEO) "video" else "audio"} call...",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(64.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FloatingActionButton(
                onClick = onDecline,
                containerColor = Error,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    modifier = Modifier.size(32.dp),
                    tint = OnPrimary
                )
            }

            FloatingActionButton(
                onClick = onAccept,
                containerColor = Success,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    when (call.type) {
                        CallType.AUDIO -> Icons.Default.Call
                        CallType.VIDEO -> Icons.Default.Videocam
                    },
                    contentDescription = "Accept",
                    modifier = Modifier.size(32.dp),
                    tint = OnPrimary
                )
            }
        }
    }
}