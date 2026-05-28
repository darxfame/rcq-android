package com.rcq.messenger.ui.common

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

val EMOTICONS = listOf(
    "smile", "heart", "lol", "wink", "sad", "biggrin", "blum", "shok",
    "angel", "dirol", "nea", "kissing", "air_kiss", "kiss2", "blush",
    "dance", "good", "bad", "boredom", "cray", "crazy", "diablo",
    "drinks", "give_rose", "pardon", "pleasantry", "secret", "stop",
    "tease", "wacko", "ROFL", "aggressive", "bomb", "i-m_so_happy",
    "mega_chok", "yahoo!"
)

@Composable
fun EmoticonPicker(
    onEmoticonSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.height(220.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 52.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(EMOTICONS) { name ->
                EmoticonCell(name = name, onClick = { onEmoticonSelected(name) })
            }
        }
    }
}

@Composable
private fun EmoticonCell(name: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
        },
        update = { view ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data("file:///android_asset/emoticons/$name.gif")
                    .target(view)
                    .build()
            )
        },
        modifier = Modifier.size(48.dp).clickable(onClick = onClick)
    )
}
