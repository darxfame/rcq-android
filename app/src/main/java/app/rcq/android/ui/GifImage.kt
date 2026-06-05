package app.rcq.android.ui

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer

/** GIF magic bytes — "GIF8" (both 87a and 89a start with this). Lets the photo
 *  bubble (and avatars) detect an animated blob in the same "photo" media path
 *  iOS uses, and render it animated instead of as a frozen first frame. */
internal fun ByteArray.isGif(): Boolean =
    size >= 4 && this[0] == 0x47.toByte() && this[1] == 0x49.toByte() &&
        this[2] == 0x46.toByte() && this[3] == 0x38.toByte()

/** Renders an animated GIF/WebP via ImageDecoder → AnimatedImageDrawable
 *  (API 28+). Below 28 the caller falls back to a static first frame. */
@RequiresApi(Build.VERSION_CODES.P)
@Composable
internal fun AnimatedGif(bytes: ByteArray, modifier: Modifier) {
    val drawable = remember(bytes) {
        runCatching {
            ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
        }.getOrNull()
    }
    // If the animated decode failed (some OEM/OS ImageDecoder builds choke on
    // particular GIFs), fall back to a static first frame. A bad emoticon must
    // never take down a whole chat.
    val staticFrame = remember(bytes, drawable) {
        if (drawable != null) null
        else runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
    }
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { iv ->
            runCatching {
                when {
                    drawable != null -> {
                        iv.setImageDrawable(drawable)
                        (drawable as? AnimatedImageDrawable)?.start()
                    }
                    staticFrame != null -> iv.setImageBitmap(staticFrame)
                    else -> iv.setImageDrawable(null)
                }
            }
        },
        modifier = modifier,
    )
}
