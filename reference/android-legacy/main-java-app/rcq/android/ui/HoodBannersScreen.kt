package app.rcq.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.rcq.android.R
import app.rcq.android.Session
import kotlinx.coroutines.launch

/** District announcements (Hood Banners) for [bucket] — a paid board (mock IAP
 *  today). List + post composer (text + duration/price + anonymous). */
@Composable
fun HoodBannersScreen(session: Session, bucket: String, onBack: () -> Unit) {
    val c = RcqTheme.colors
    val hood = session.hood
    val scope = rememberCoroutineScope()
    val banners by hood.banners.collectAsState()
    val canPost by hood.canPostBanner.collectAsState()
    val pricing by hood.pricing.collectAsState()
    var showCompose by remember { mutableStateOf(false) }

    LaunchedEffect(bucket) { hood.loadBanners(bucket) }

    Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary.copy(alpha = 0.6f)).padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back), tint = c.accent,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.banners_title), color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (canPost) {
                Icon(Icons.Filled.Add, stringResource(R.string.banners_post), tint = c.accent,
                    modifier = Modifier.size(26.dp).clip(CircleShape).clickable { showCompose = true })
            }
        }

        if (banners.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.banners_empty), color = c.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(banners, key = { it.id }) { b ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(12.dp)) {
                        Text(b.text, color = c.textPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (b.is_anonymous) stringResource(R.string.banners_anon) else (b.owner_nickname ?: "#${b.owner_uin}"),
                                color = c.textSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f),
                            )
                            if (b.is_mine) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.rooms_delete), tint = c.textSecondary,
                                    modifier = Modifier.size(18.dp).clip(CircleShape).clickable { hood.deleteBanner(b.id, bucket) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompose) {
        BannerComposer(
            pricing = pricing,
            onDismiss = { showCompose = false },
            onPost = { text, anon, duration ->
                showCompose = false
                scope.launch { hood.postBanner(bucket, text, anon, duration) }
            },
        )
    }
}

@Composable
private fun BannerComposer(
    pricing: List<app.rcq.android.net.RcqApi.BannerPricing>,
    onDismiss: () -> Unit,
    onPost: (text: String, anonymous: Boolean, duration: String) -> Unit,
) {
    val c = RcqTheme.colors
    var text by remember { mutableStateOf("") }
    var anon by remember { mutableStateOf(false) }
    val options = pricing.ifEmpty {
        listOf(app.rcq.android.net.RcqApi.BannerPricing("1h", "1 hour", 99, "$0.99"))
    }
    var duration by remember(options) { mutableStateOf(options.first().duration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.banners_post), color = c.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = text, onValueChange = { text = it.take(500) },
                    placeholder = { Text(stringResource(R.string.banners_text_hint)) }, modifier = Modifier.fillMaxWidth())
                options.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { duration = p.duration }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (duration == p.duration) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                            null, tint = if (duration == p.duration) c.accent else c.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${p.label} · ${p.price_display}", color = c.textPrimary, fontSize = 14.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.banners_anon_post), color = c.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(checked = anon, onCheckedChange = { anon = it })
                }
                Text(stringResource(R.string.banners_mock_note), color = c.textSecondary, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onPost(text.trim(), anon, duration) }) {
                Text(stringResource(R.string.banners_post), color = c.accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = c.textSecondary) } },
        containerColor = c.bgSecondary,
    )
}
