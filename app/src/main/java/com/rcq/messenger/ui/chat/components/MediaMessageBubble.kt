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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.rcq.messenger.BuildConfig
import com.rcq.messenger.domain.model.Message
import com.rcq.messenger.domain.model.MessageKind
import com.rcq.messenger.domain.model.MessageStatus
import com.rcq.messenger.media.PlaybackState
import com.rcq.messenger.ui.common.EmoticonText
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
    val rcq = LocalRCQColors.current
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
                        .background(rcq.bgSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("U", color = rcq.accent, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
            ) {
                when (message.kind) {
                    MessageKind.PHOTO, MessageKind.PREMIUM_PHOTO -> {
                        ImageMessageContent(
                            message = message,
                            isOwnMessage = isOwnMessage,
                            onImageClick = { onMediaClick(message.mediaId ?: "") }
                        )
                    }
                    MessageKind.VIDEO, MessageKind.PREMIUM_VIDEO -> {
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
                                    if (isOwnMessage) rcq.bubbleSelf else rcq.bubbleOther,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            EmoticonText(
                                text = message.content,
                                color = rcq.textPrimary,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
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
                        .background(rcq.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("M", color = rcq.bgPrimary, style = MaterialTheme.typography.labelMedium)
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
    val rcq = LocalRCQColors.current
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()
    val imageModel = message.mediaId?.takeIf { it.isNotBlank() }?.let { mediaUrl(it) }
        ?: message.thumbnailB64?.takeIf { it.isNotBlank() }
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(rcq.bgSecondary)
            .clickable { onImageClick() }
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MediaPlaceholder(status = message.status, contentDescription = "Image unavailable")
        }

        // Caption overlay if present
        if (message.content.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        rcq.textPrimary.copy(alpha = 0.7f),
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(8.dp)
            ) {
                EmoticonText(
                    text = message.content,
                    color = rcq.bgPrimary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
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
    val rcq = LocalRCQColors.current
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()
    val thumbnailModel = message.mediaId?.takeIf { it.isNotBlank() }?.let { mediaUrl(it) }
        ?: message.thumbnailB64?.takeIf { it.isNotBlank() }
    Box(
        modifier = Modifier
            .width(240.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(rcq.bgSecondary)
            .clickable { onVideoClick() }
    ) {
        if (thumbnailModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailModel)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            MediaPlaceholder(status = message.status, contentDescription = "Video unavailable")
        }

        // Play button overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .background(rcq.textPrimary.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = rcq.bgPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Duration overlay
        if (message.durationSec > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(rcq.textPrimary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatDuration(message.durationSec.toLong()),
                    color = rcq.bgPrimary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun mediaUrl(mediaId: String): String =
    BuildConfig.API_BASE_URL.trimEnd('/') + "/media/$mediaId"

@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }
}

@Composable
private fun MediaPlaceholder(status: MessageStatus, contentDescription: String) {
    val rcq = LocalRCQColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (status == MessageStatus.SENDING) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = rcq.accent,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
                tint = rcq.textSecondary
            )
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
    val rcq = LocalRCQColors.current
    val activeProgress = when (playbackState) {
        PlaybackState.PLAYING, PlaybackState.PAUSED -> 0.35f
        else -> 0f
    }
    Row(
        modifier = Modifier
            .width(190.dp)
            .background(
                if (isOwnMessage) rcq.bubbleSelf else rcq.bubbleOther,
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isOwnMessage) 18.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 18.dp
                )
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
                tint = rcq.accent
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(rcq.textSecondary.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(activeProgress)
                        .fillMaxHeight()
                        .background(rcq.accent)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(message.durationSec.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = rcq.textSecondary
                )
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = rcq.textSecondary
                )
            }
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
