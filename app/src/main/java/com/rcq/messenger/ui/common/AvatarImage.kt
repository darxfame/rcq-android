package com.rcq.messenger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rcq.messenger.ui.theme.LocalRCQColors

private val AVATAR_PALETTE = listOf(
    Color(0xFF4FA3E3),
    Color(0xFFF26B6B),
    Color(0xFF6BBE6B),
    Color(0xFFE0A53A),
    Color(0xFFA479D9),
    Color(0xFF57C7BC),
    Color(0xFFE2769B)
)

fun avatarColor(uin: Long): Color {
    val index = (uin % AVATAR_PALETTE.size).toInt().let { if (it < 0) it + AVATAR_PALETTE.size else it }
    return AVATAR_PALETTE[index]
}

@Composable
fun AvatarImage(
    avatarUrl: String?,
    displayName: String,
    uin: Long = 0L,
    size: Dp = 40.dp,
    hasStory: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rcq = LocalRCQColors.current
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val renderedSize = if (hasStory) size + 4.dp else size

    Box(modifier = modifier.size(renderedSize), contentAlignment = Alignment.Center) {
        if (hasStory) {
            Box(
                modifier = Modifier
                    .size(size + 4.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(rcq.accent, rcq.accentPressed, rcq.accent)
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .then(if (hasStory) Modifier.padding(2.dp) else Modifier)
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(avatarColor(uin)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = rcq.bgPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        }
    }
}
