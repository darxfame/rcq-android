package com.rcq.messenger.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.ui.theme.*

// Classic ICQ status icons from statuses.bmp (JIMM-style sprite, 19 icons at 16x16)
// status_00=online, status_01=away, status_02=occupied, status_03=dnd,
// status_07=invisible, status_08=offline
private val statusIconIds: Map<UserStatus, Int> by lazy {
    val cls = com.rcq.messenger.R.drawable::class.java
    fun id(name: String) = runCatching { cls.getField(name).getInt(null) }.getOrDefault(0)
    mapOf(
        UserStatus.ONLINE    to id("status_00"),
        UserStatus.AWAY      to id("status_01"),
        UserStatus.BUSY      to id("status_02"),
        UserStatus.DND       to id("status_03"),
        UserStatus.INVISIBLE to id("status_07"),
        UserStatus.OFFLINE   to id("status_08")
    )
}

@Composable
fun StatusIndicator(
    status: UserStatus,
    size: Int = 16,
    modifier: Modifier = Modifier
) {
    val resId = statusIconIds[status] ?: 0
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = status.name,
            modifier = modifier.size(size.dp)
        )
    } else {
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
}
