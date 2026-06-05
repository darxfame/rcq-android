package app.rcq.android.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The classic KOLOBOK emoticon set, bundled in `assets/emoticons/<name>.gif`
 * (copied byte-for-byte from the iOS app's Resources/Emoticons). Reaction
 * asset names match iOS exactly, so a reaction renders identically on both
 * clients — iOS sends e.g. "smile", and we render the same GIF (and vice
 * versa) instead of the old mismatch where Android sent a system emoji that
 * iOS couldn't render and iOS sent an emoticon name Android showed as text.
 */
internal object Emoticons {
    /** The six offered as message reactions (iOS MessageActionSheet parity). */
    val reactions = listOf("smile", "biggrin", "shok", "cray", "good", "heart")

    /** The composer palette: (asset, display name), in the exact iOS
     *  `Emoticons.entries` order. Codes are the `:asset:` form. */
    val palette: List<Pair<String, String>> = listOf(
        "smile" to "Happy", "sad" to "Sad", "wink" to "Winking", "blum" to "Tongue",
        "tease" to "Joking", "cray" to "Crying", "air_kiss" to "Kissed", "kiss2" to "Kiss",
        "blush" to "Embarrassed", "angel" to "Angel", "secret" to "Silent", "wacko" to "Confused",
        "aggressive" to "Angry", "biggrin" to "Laughing", "nea" to "Pensive", "shok" to "Shocked",
        "dirol" to "Cool", "dance" to "Headphones", "boredom" to "Yawning", "bad" to "Sick",
        "stop" to "Stop", "kissing" to "Two Kissing", "diablo" to "Devil", "give_rose" to "Red Rose",
        "bomb" to "Bomb", "good" to "Thumbs Up", "drinks" to "Drink", "heart" to "In Love",
    )

    /** Asset names that have a `:code:` (for tokenizing message bodies). */
    private val codes: Set<String> = palette.map { it.first }.toSet()

    private val cache = HashMap<String, ByteArray?>()

    /** Raw GIF bytes for an emoticon [name] from assets (cached). Null when
     *  there's no such asset (e.g. a plain-emoji reaction from an old client). */
    fun bytes(context: Context, name: String): ByteArray? {
        synchronized(cache) { if (cache.containsKey(name)) return cache[name] }
        val b = runCatching {
            context.applicationContext.assets.open("emoticons/$name.gif").use { it.readBytes() }
        }.getOrNull()
        synchronized(cache) { cache[name] = b }
        return b
    }

    fun isEmoticon(context: Context, name: String): Boolean = bytes(context, name) != null

    /** A run of a tokenized message body: literal text or an emoticon. */
    sealed interface Token {
        data class Text(val text: String) : Token
        data class Emo(val asset: String, val code: String) : Token
    }

    // `:asset:` codes only (iOS parity — short shortcuts like :) are NOT parsed,
    // they collide with URLs/math). Asset names use [A-Za-z0-9_!-].
    private val TOKEN_RE = Regex(":([A-Za-z0-9_!-]+):")

    /** A `#<uin>` mention in a message body (3+ digits to avoid matching `#1`
     *  or `#ff0000`-style tokens). Rendered as the user's clickable nick when a
     *  resolver is supplied to [EmoticonText]. */
    val MENTION_RE = Regex("#(\\d{3,})")

    /** An `@nickname` mention in a message body. Resolved against the group
     *  roster (case-insensitive) by [EmoticonText]; an unmatched `@foo` (or an
     *  email's `@domain`) stays plain text. Mirrors the iOS MentionParser
     *  pattern: letters/digits/underscore/dot/hyphen (so ".Dev" resolves). */
    val MENTION_AT_RE = Regex("@([\\p{L}\\p{N}_.\\-]+)")

    /** Split [text] into text runs + known `:asset:` emoticons. Returns a single
     *  Text token when there are no emoticons (the common case). */
    fun tokenize(text: String): List<Token> {
        if (!text.contains(':')) return listOf(Token.Text(text))
        val out = ArrayList<Token>()
        var last = 0
        for (m in TOKEN_RE.findAll(text)) {
            val asset = m.groupValues[1]
            if (asset !in codes) continue
            if (m.range.first > last) out.add(Token.Text(text.substring(last, m.range.first)))
            out.add(Token.Emo(asset, m.value))
            last = m.range.last + 1
        }
        if (out.isEmpty()) return listOf(Token.Text(text))
        if (last < text.length) out.add(Token.Text(text.substring(last)))
        return out
    }

    fun hasEmoticon(text: String): Boolean =
        text.contains(':') && TOKEN_RE.findAll(text).any { it.groupValues[1] in codes }
}

/** Render a bundled emoticon GIF by [name] — animated on API 28+, a frozen
 *  first frame below that (minSdk is 26). Renders nothing if the asset is
 *  missing. */
@Composable
internal fun EmoticonGif(name: String, modifier: Modifier, animate: Boolean = true) {
    val context = LocalContext.current
    val bytes by produceState<ByteArray?>(initialValue = null, name) {
        value = Emoticons.bytes(context, name)
    }
    val b = bytes ?: return
    // [animate]=false (e.g. the 28-emoticon picker grid) shows a frozen frame
    // to avoid running dozens of AnimatedImageDrawables at once.
    if (animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AnimatedGif(b, modifier)
    } else {
        val img = remember(b) { runCatching { BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap() }.getOrNull() }
        if (img != null) Image(bitmap = img, contentDescription = null, modifier = modifier)
    }
}

/** A reaction chip under a message bubble: a small emoticon GIF when the
 *  reaction is a known KOLOBOK asset, else the raw string (plain-emoji
 *  reactions from older clients still show). */
@Composable
internal fun ReactionChip(asset: String) {
    val c = RcqTheme.colors
    val context = LocalContext.current
    val isEmoticon = remember(asset) { Emoticons.isEmoticon(context, asset) }
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isEmoticon) EmoticonGif(asset, Modifier.size(18.dp))
        else Text(asset, fontSize = 13.sp, color = c.textPrimary)
    }
}

/** A message/caption body with inline `:asset:` emoticons rendered as small
 *  GIFs (iOS EmoticonText parity). Falls back to a plain [Text] when the body
 *  has no emoticon codes (the common path — no inline-content overhead). */
@Composable
internal fun EmoticonText(
    body: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    // When supplied, a `#<uin>` in the body whose uin resolves to a nick renders
    // as the clickable accent nick (tap → [onMentionClick]); else it stays plain
    // digits. Null = no mention handling (the default for non-message text).
    mentionNick: ((Int) -> String?)? = null,
    onMentionClick: ((Int) -> Unit)? = null,
    // When supplied, an `@nickname` whose nick resolves to a group member's uin
    // renders as the clickable accent nick (tap → [onMentionClick]); else plain.
    mentionUin: ((String) -> Int?)? = null,
) {
    val tokens = remember(body) { Emoticons.tokenize(body) }
    val accent = RcqTheme.colors.accent
    val hasMention =
        (mentionNick != null && body.contains('#') && Emoticons.MENTION_RE.containsMatchIn(body)) ||
        (mentionUin != null && body.contains('@') && Emoticons.MENTION_AT_RE.containsMatchIn(body))
    // Fast path: a pure-text body with no resolvable mentions.
    if (tokens.size == 1 && tokens[0] is Emoticons.Token.Text && !hasMention) {
        Text(body, color = color, fontSize = fontSize, lineHeight = lineHeight, modifier = modifier)
        return
    }
    val inline = HashMap<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        for (t in tokens) when (t) {
            is Emoticons.Token.Text -> appendWithMentions(t.text, mentionNick, mentionUin, onMentionClick, accent)
            is Emoticons.Token.Emo -> {
                appendInlineContent(t.asset, t.code)
                if (t.asset !in inline) {
                    val asset = t.asset
                    inline[asset] = InlineTextContent(
                        Placeholder(width = 1.4.em, height = 1.4.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
                    ) { EmoticonGif(asset, Modifier.fillMaxSize()) }
                }
            }
        }
    }
    Text(annotated, color = color, fontSize = fontSize, lineHeight = lineHeight, inlineContent = inline, modifier = modifier)
}

/** Append [text], turning each resolvable mention into a clickable accent nick
 *  (tap → [onMentionClick]): `#<uin>` via [mentionNick] (renders the nick), and
 *  `@nickname` via [mentionUin] (renders the typed `@nick`). Unresolved or
 *  no-resolver tokens stay plain. Both kinds are merged in source order. */
private fun AnnotatedString.Builder.appendWithMentions(
    text: String,
    mentionNick: ((Int) -> String?)?,
    mentionUin: ((String) -> Int?)?,
    onMentionClick: ((Int) -> Unit)?,
    accent: Color,
) {
    if (mentionNick == null && mentionUin == null) { append(text); return }
    data class Hit(val range: IntRange, val uin: Int, val display: String)
    val hits = ArrayList<Hit>()
    if (mentionNick != null) {
        for (m in Emoticons.MENTION_RE.findAll(text)) {
            val uin = m.groupValues[1].toIntOrNull() ?: continue
            val nick = mentionNick(uin) ?: continue
            hits.add(Hit(m.range, uin, nick))
        }
    }
    if (mentionUin != null) {
        for (m in Emoticons.MENTION_AT_RE.findAll(text)) {
            val uin = mentionUin(m.groupValues[1]) ?: continue
            hits.add(Hit(m.range, uin, m.value)) // keep the typed "@nick"
        }
    }
    if (hits.isEmpty()) { append(text); return }
    hits.sortBy { it.range.first }
    var cursor = 0
    for (h in hits) {
        if (h.range.first < cursor) continue // skip overlaps
        if (h.range.first > cursor) append(text.substring(cursor, h.range.first))
        withLink(LinkAnnotation.Clickable(tag = "m${h.uin}", linkInteractionListener = { onMentionClick?.invoke(h.uin) })) {
            withStyle(SpanStyle(color = accent)) { append(h.display) }
        }
        cursor = h.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

/** The composer smiley panel: a scrollable grid of the palette emoticons.
 *  Tapping one calls [onPick] with its `:asset:` code to splice into the draft. */
@Composable
internal fun EmoticonPanel(onPick: (String) -> Unit) {
    val c = RcqTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 46.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(c.bgSecondary)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(Emoticons.palette.size) { i ->
            val asset = Emoticons.palette[i].first
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).clickable { onPick(":$asset:") },
                contentAlignment = Alignment.Center,
            ) { EmoticonGif(asset, Modifier.size(30.dp), animate = false) }
        }
    }
}
