package com.rcq.messenger.ui.common

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.rcq.messenger.ui.theme.LocalRCQColors
import java.nio.ByteBuffer

private val EMOTICON_CODE_TO_ASSET = mapOf(
    ":smile:" to "smile",
    "(smile)" to "smile",
    ":-)" to "smile",
    ":)" to "smile",
    ":sad:" to "sad",
    "(sad)" to "sad",
    ":-(" to "sad",
    ":(" to "sad",
    ":wink:" to "wink",
    "(wink)" to "wink",
    ";-)" to "wink",
    ";)" to "wink",
    ":biggrin:" to "biggrin",
    "(biggrin)" to "biggrin",
    ":-D" to "biggrin",
    ":D" to "biggrin",
    ":blum:" to "blum",
    ":P" to "blum",
    ":-P" to "blum",
    ":cray:" to "cray",
    ":cry" to "cray",
    ":heart:" to "heart",
    "(heart)" to "heart",
    "<3" to "heart",
    ":good:" to "good",
    ":+1" to "good",
    ":bad:" to "bad",
    ":-1" to "bad",
    ":shok:" to "shok",
    ":O" to "mega_chok",
    "8o" to "shok",
    ":angel:" to "angel",
    "O:)" to "angel",
    ":diablo:" to "diablo",
    ">:)" to "diablo",
    ":rose:" to "give_rose",
    ":dance:" to "dance",
    ":beer" to "drinks",
    ":bomb:" to "bomb",
    ":stop:" to "stop",
    ":wacko:" to "wacko"
)

private val EMOTICON_RE = Regex(
    EMOTICON_CODE_TO_ASSET.keys
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }
)

private val MENTION_RE = Regex("(^|\\s)(@[A-Za-z0-9_]{2,32})")

private sealed interface EmoticonSegment {
    data class Text(val value: String) : EmoticonSegment
    data class Image(val code: String, val asset: String) : EmoticonSegment
}

private fun parseEmoticons(text: String): List<EmoticonSegment> {
    val matches = EMOTICON_RE.findAll(text).toList()
    if (matches.isEmpty()) return listOf(EmoticonSegment.Text(text))

    val out = mutableListOf<EmoticonSegment>()
    var last = 0
    matches.forEach { match ->
        if (match.range.first > last) {
            out += EmoticonSegment.Text(text.substring(last, match.range.first))
        }
        val code = match.value
        val asset = EMOTICON_CODE_TO_ASSET[code]
        if (asset != null) out += EmoticonSegment.Image(code, asset)
        last = match.range.last + 1
    }
    if (last < text.length) out += EmoticonSegment.Text(text.substring(last))
    return out
}

@Composable
fun EmoticonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val rcq = LocalRCQColors.current
    val segments = remember(text) { parseEmoticons(text) }
    val hasImages = segments.any { it is EmoticonSegment.Image }
    val hasMentions = remember(text) { MENTION_RE.containsMatchIn(text) }

    if (!hasImages && !hasMentions) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
        return
    }

    val inlineContent = remember(segments) {
        segments
            .filterIsInstance<EmoticonSegment.Image>()
            .distinctBy { it.asset }
            .associate { segment ->
                segment.asset to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.35.em,
                        height = 1.35.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    )
                ) {
                    EmoticonGif(segment.asset, Modifier.fillMaxSize())
                }
            }
    }

    val annotated = buildAnnotatedString {
        segments.forEach { segment ->
            when (segment) {
                is EmoticonSegment.Image -> appendInlineContent(segment.asset, segment.code)
                is EmoticonSegment.Text -> appendMentionStyled(segment.value, rcq.accent)
            }
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        inlineContent = inlineContent
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendMentionStyled(
    value: String,
    mentionColor: Color
) {
    var last = 0
    MENTION_RE.findAll(value).forEach { match ->
        val mentionStart = match.range.first + match.groupValues[1].length
        val mentionEnd = match.range.last + 1
        if (mentionStart > last) append(value.substring(last, mentionStart))
        withStyle(SpanStyle(color = mentionColor)) {
            append(value.substring(mentionStart, mentionEnd))
        }
        last = mentionEnd
    }
    if (last < value.length) append(value.substring(last))
}

@Composable
private fun EmoticonGif(asset: String, modifier: Modifier) {
    val context = LocalContext.current
    val bytes by produceState<ByteArray?>(initialValue = null, asset) {
        value = runCatching {
            context.applicationContext.assets.open("emoticons/$asset.gif").use { it.readBytes() }
        }.getOrNull()
    }
    val data = bytes ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AnimatedGif(data, modifier)
    } else {
        val image = remember(data) {
            runCatching {
                BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = modifier)
        }
    }
}

@Composable
private fun AnimatedGif(bytes: ByteArray, modifier: Modifier) {
    val drawable = remember(bytes) {
        runCatching {
            ImageDecoder.decodeDrawable(ImageDecoder.createSource(ByteBuffer.wrap(bytes)))
        }.getOrNull()
    }
    AndroidView(
        factory = { context ->
            ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE }
        },
        update = { view ->
            view.setImageDrawable(drawable)
            (drawable as? AnimatedImageDrawable)?.start()
        },
        modifier = modifier
    )
}
