package com.rcq.messenger.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.rcq.messenger.ui.theme.IMAGE
import com.rcq.messenger.ui.theme.TextOnPrimary
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.media.PlaybackState
import com.rcq.messenger.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaMessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onMediaClick: (String) -> Unit,
    onVoicePlay: (String) -> Unit,
    onVoicePause: () -> Unit,
    playbackState: PlaybackState = PlaybackState.IDLE,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            if (!isOwnMessage) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = Primary, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
            ) {
                when (message.type) {
                    "image" -> {
                        ImageMessageContent(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onImageClick = { onMediaClick(message.mediaId ?: "") }
                        )
                    }
                    MessageKind.VIDEO -> {
                        VideoMessageContent(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onVideoClick = { onMediaClick(message.mediaId ?: "") }
                        )
                    }
                    MessageKind.VOICE -> {
                        VoiceMessageContent(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onVoicePlay = { onVoicePlay(message.mediaId ?: "") },
                            onVoicePause = onVoicePause,
                            playbackState = playbackState
                        )
                    }
                    MessageKind.FILE -> {
                        FileMessageContent(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onFileClick = { onMediaClick(message.mediaId ?: "") }
                        )
                    }
                    else -> {
                        // Regular text message
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isOwnMessage) MessageSent else MessageReceived,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = if (isOwnMessage) TextOnPrimary else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (isOwnMessage) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = TextOnPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ImageMessageContent(
    message: Message,
    isOwnMessage: Boolean,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOwnMessage) MessageSent else MessageReceived)
            .clickable { onImageClick() }
    ) {
        if (message.thumbnailB64?.isNotEmpty() == true) {
            // TODO: Load image from base64 thumbnail or media URL
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.thumbnailB64)
                    .build(),
                contentDescription = "Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Image",
                    modifier = Modifier.size(48.dp),
                    tint = TextSecondary
                )
            }
        }

        // Caption overlay if present
        if (message.content.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun VideoMessageContent(
    message: Message,
    isOwnMessage: Boolean,
    onVideoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOwnMessage) MessageSent else MessageReceived)
            .clickable { onVideoClick() }
    ) {
        if (message.thumbnailB64?.isNotEmpty() == true) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(message.thumbnailB64)
                    .build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video",
                    modifier = Modifier.size(48.dp),
                    tint = TextSecondary
                )
            }
        }

        // Play button overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Duration overlay
        if (message.durationSec > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(message.durationSec.toLong()),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun VoiceMessageContent(
    message: Message,
    isOwnMessage: Boolean,
    onVoicePlay: () -> Unit,
    onVoicePause: () -> Unit,
    playbackState: PlaybackState
) {
    Row(
        modifier = Modifier
            .background(
                if (isOwnMessage) MessageSent else MessageReceived,
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                when (playbackState) {
                    PlaybackState.PLAYING -> onVoicePause()
                    else -> onVoicePlay()
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = when (playbackState) {
                    PlaybackState.PLAYING -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause" else "Play",
                tint = if (isOwnMessage) TextOnPrimary else Primary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            // Waveform placeholder
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(20) { index ->
                    val height = (8..24).random().dp
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(height)
                            .background(
                                if (isOwnMessage) TextOnPrimary.copy(alpha = 0.7f)
                                else TextSecondary,
                                RoundedCornerShape(1.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDuration(message.durationSec.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOwnMessage) TextOnPrimary else TextSecondary
            )
        }
    }
}

@Composable
fun FileMessageContent(
    message: Message,
    isOwnMessage: Boolean,
    onFileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                if (isOwnMessage) MessageSent else MessageReceived,
                RoundedCornerShape(16.dp)
            )
            .clickable { onFileClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = "File",
            tint = if (isOwnMessage) TextOnPrimary else Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = message.fileName ?: "Файл",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isOwnMessage) TextOnPrimary else TextPrimary
            )

            if (message.fileSizeBytes != null) {
                Text(
                    text = formatFileSize(message.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOwnMessage) TextOnPrimary.copy(alpha = 0.7f) else TextSecondary
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0

    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}