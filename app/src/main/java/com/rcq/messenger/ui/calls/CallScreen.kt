package com.rcq.messenger.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.rcq.messenger.service.CallState
import com.rcq.messenger.ui.theme.*

@Composable
fun CallScreen(
    chatId: String,
    targetNickname: String,
    onBack: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()

    LaunchedEffect(chatId) {
        // TODO: Get target user ID from chat
        val targetUin = 0L // Placeholder - should get from chat participants
        viewModel.startCall(chatId, targetUin)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Profile
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (callState == CallState.CONNECTING || callState == CallState.RINGING) {
                            Icons.Default.Call
                        } else {
                            Icons.Default.Person
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = targetNickname,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (callState) {
                        CallState.IDLE -> "Ready to call"
                        CallState.CONNECTING -> "Calling..."
                        CallState.RINGING -> "Ringing..."
                        CallState.CONNECTED -> "Connected"
                        CallState.DISCONNECTED -> "Disconnected"
                        CallState.ENDED -> "Call ended"
                        CallState.ERROR -> "Connection error"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }

            // Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                FilledIconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isMuted) Error else SurfaceVariant
                    )
                ) {
                    Icon(
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) TextPrimary else TextPrimary
                    )
                }

                // End call
                FilledIconButton(
                    onClick = {
                        viewModel.endCall()
                        onBack()
                    },
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Error
                    )
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End call",
                        modifier = Modifier.size(32.dp),
                        tint = TextPrimary
                    )
                }

                // Speaker
                FilledIconButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSpeakerOn) Primary else SurfaceVariant
                    )
                ) {
                    Icon(
                        if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Back button
            TextButton(onClick = onBack) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

@Composable
fun IncomingCallScreen(
    callerName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Incoming Call",
                style = MaterialTheme.typography.bodyLarge,
                color = Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = callerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Decline
                FilledIconButton(
                    onClick = onDecline,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Error
                    )
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Decline",
                        modifier = Modifier.size(32.dp),
                        tint = TextPrimary
                    )
                }

                // Accept
                FilledIconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Primary
                    )
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Accept",
                        modifier = Modifier.size(32.dp),
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}