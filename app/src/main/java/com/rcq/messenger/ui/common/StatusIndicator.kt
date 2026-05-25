package com.rcq.messenger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.ui.theme.*

@Composable
fun StatusIndicator(
    status: UserStatus,
    size: Int = 12,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                when (status) {
                    UserStatus.ONLINE -> Online
                    UserStatus.AWAY -> Warning
                    UserStatus.BUSY, UserStatus.DND -> Error
                    UserStatus.INVISIBLE, UserStatus.OFFLINE -> Offline
                }
            )
    )
}