package app.rcq.android.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

/** Coarse "last seen" buckets — minutes / hours / days, else a date.
 *  Mirrors iOS ContactRow.relativeLastSeen. */
internal fun relativeLastSeen(ts: Long): String {
    val secs = ((System.currentTimeMillis() - ts) / 1000).toInt()
    if (secs < 60) return "just now"
    val mins = secs / 60
    if (mins < 60) return "${mins}m ago"
    val hours = mins / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 7) return "${days}d ago"
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
}

/** Chevron that points right when collapsed, down when expanded. */
@Composable
internal fun CollapseChevron(collapsed: Boolean) {
    val rotation by animateFloatAsState(if (collapsed) 0f else 90f, label = "chevron")
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = RcqTheme.colors.textSecondary,
        modifier = Modifier.size(16.dp).rotate(rotation),
    )
}

/** ICQ-style collapsible section header: chevron · TITLE · (count) · trailing. */
@Composable
internal fun SectionHeader(
    title: String,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val c = RcqTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(c.bgSecondary.copy(alpha = 0.7f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollapseChevron(collapsed)
        Spacer(Modifier.size(6.dp))
        Text(title.uppercase(), color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(4.dp))
        Text("($count)", color = c.textSecondary, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

@Composable
internal fun GenderIcon(gender: String?) {
    when (gender?.lowercase()) {
        "m", "male" -> Icon(Icons.Filled.Male, null, tint = Color(0xFF4A90D9), modifier = Modifier.size(12.dp))
        "f", "female" -> Icon(Icons.Filled.Female, null, tint = Color(0xFFD96BA6), modifier = Modifier.size(12.dp))
        else -> Unit
    }
}

@Composable
internal fun CapsuleButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = RcqTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) c.accent else c.bgSecondary)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 40.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}
