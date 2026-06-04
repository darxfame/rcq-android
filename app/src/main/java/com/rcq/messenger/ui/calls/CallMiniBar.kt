package com.rcq.messenger.ui.calls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.RCQMetrics

@Composable
fun CallMiniBar(
    calleeName: String,
    isVisible: Boolean,
    onTap: () -> Unit,
    onHangup: () -> Unit
) {
    val rcq = LocalRCQColors.current
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rcq.accent)
                .clickable(onClick = onTap)
                .padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowVPad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RCQMetrics.rowHPad)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = rcq.bgPrimary)
                Text(
                    text = calleeName,
                    color = rcq.bgPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onHangup) {
                Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = rcq.bgPrimary)
            }
        }
    }
}
