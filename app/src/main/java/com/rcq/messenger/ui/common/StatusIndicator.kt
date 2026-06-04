package com.rcq.messenger.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rcq.messenger.R
import com.rcq.messenger.domain.model.UserStatus

// Classic ICQ status icons from statuses.bmp (JIMM-style sprite, 19 icons at 16x16)
// status_00=online, status_01=away, status_02=occupied, status_03=dnd,
// status_07=invisible, status_08=offline
private fun statusDrawableId(status: UserStatus): Int = when (status) {
    UserStatus.ONLINE -> R.drawable.status_00
    UserStatus.AWAY -> R.drawable.status_01
    UserStatus.BUSY -> R.drawable.status_02
    UserStatus.DND -> R.drawable.status_03
    UserStatus.INVISIBLE -> R.drawable.status_07
    UserStatus.OFFLINE -> R.drawable.status_08
}

@Composable
fun StatusIndicator(
    status: UserStatus,
    size: Int = 16,
    modifier: Modifier = Modifier
) {
    val resId = statusDrawableId(status)
    Image(
        painter = painterResource(id = resId),
        contentDescription = status.name,
        modifier = modifier.size(size.dp)
    )
}
