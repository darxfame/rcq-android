package app.rcq.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.view.Gravity
import android.widget.EditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The composer text field, but emoticon `:codes:` render as inline GIF (first
 * frame) images right in the field — the way iOS does it via UIKit. Compose's
 * BasicTextField can't draw inline images in editable text, so this wraps a
 * native [EditText] and paints an [ImageSpan] over each known `:code:` run.
 *
 * The underlying text still holds the literal `:code:` characters (the span only
 * COVERS them visually), so `value` / send keep working unchanged — a sent
 * message carries the code and renders as the GIF on both clients.
 */
@Composable
internal fun EmoticonInputField(
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val context = LocalContext.current
    // Guard so our own span-painting / programmatic setText doesn't re-enter the
    // TextWatcher and loop.
    val suppress = remember { booleanArrayOf(false) }
    val sizePx = with(LocalDensity.current) { 20.dp.toPx().toInt() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            EditText(ctx).apply {
                background = null
                setPadding(0, 0, 0, 0)
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(textColor.toArgb())
                textSize = 15f // sp
                inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE
                isSingleLine = false
                maxLines = 5
                setOnFocusChangeListener { _, has -> if (has) onFocused() }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (suppress[0]) return
                        suppress[0] = true
                        paintEmoticonSpans(context, this@apply.text, sizePx)
                        suppress[0] = false
                        onValueChange(s?.toString() ?: "")
                    }
                })
            }
        },
        update = { et ->
            if (et.text?.toString() != value) {
                suppress[0] = true
                et.setText(value)
                paintEmoticonSpans(context, et.text, sizePx)
                et.setSelection(et.text?.length ?: 0)
                suppress[0] = false
            }
        },
    )
}

// First-frame bitmaps, scaled to the field's line size, cached by asset@size.
private val spanBitmaps = HashMap<String, Bitmap?>()

private fun emoticonBitmap(context: android.content.Context, asset: String, sizePx: Int): Bitmap? {
    val key = "$asset@$sizePx"
    synchronized(spanBitmaps) { if (spanBitmaps.containsKey(key)) return spanBitmaps[key] }
    val src = Emoticons.bytes(context, asset)?.let {
        runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull()
    }
    // Scale to fit sizePx on the LONGER side so the kolobok keeps its aspect
    // ratio (they aren't square — forcing sizePx x sizePx squashed them).
    val scaled = src?.let {
        val s = sizePx.toFloat() / maxOf(it.width, it.height).coerceAtLeast(1)
        val w = (it.width * s).toInt().coerceAtLeast(1)
        val h = (it.height * s).toInt().coerceAtLeast(1)
        runCatching { Bitmap.createScaledBitmap(it, w, h, true) }.getOrNull()
    }
    synchronized(spanBitmaps) { spanBitmaps[key] = scaled }
    return scaled
}

/** Replace every existing emoticon ImageSpan, then paint one over each known
 *  `:code:` run in [editable]. */
private fun paintEmoticonSpans(context: android.content.Context, editable: Editable?, sizePx: Int) {
    if (editable == null) return
    editable.getSpans(0, editable.length, ImageSpan::class.java).forEach { editable.removeSpan(it) }
    val text = editable.toString()
    for ((start, end, asset) in Emoticons.codeSpans(text)) {
        val bmp = emoticonBitmap(context, asset, sizePx) ?: continue
        editable.setSpan(ImageSpan(context, bmp, ImageSpan.ALIGN_BOTTOM), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
