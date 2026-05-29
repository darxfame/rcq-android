package com.rcq.messenger.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Classic ICQ emoticons sliced from allicons.bmp (JIMM-style 52 icons, 16x16 each)
data class Emoticon(val name: String, val resIndex: Int, val text: String)

val ICQ_EMOTICONS: List<Emoticon> = listOf(
    Emoticon("smile",      0,  ":)"),
    Emoticon("sad",        1,  ":("),
    Emoticon("wink",       2,  ";)"),
    Emoticon("biggrin",    3,  ":D"),
    Emoticon("blum",       4,  ":P"),
    Emoticon("shok",       5,  "8o"),
    Emoticon("angel",      6,  "O:)"),
    Emoticon("diablo",     7,  ">:)"),
    Emoticon("nea",        8,  ":-|"),
    Emoticon("lol",        9,  ":-))"),
    Emoticon("crazy",      10, ":crazy"),
    Emoticon("secret",     11, ":-X"),
    Emoticon("stop",       12, ":stop"),
    Emoticon("kiss",       13, ":-*"),
    Emoticon("blush",      14, ":blush"),
    Emoticon("cray",       15, ":cry"),
    Emoticon("good",       16, ":+1"),
    Emoticon("bad",        17, ":-1"),
    Emoticon("dance",      18, ":dance"),
    Emoticon("heart",      19, "<3"),
    Emoticon("drinks",     20, ":beer"),
    Emoticon("dirol",      21, "8-)"),
    Emoticon("boredom",    22, ":-/"),
    Emoticon("kissing",    23, ":-{}"),
    Emoticon("air_kiss",   24, ":kiss"),
    Emoticon("give_rose",  25, ":rose"),
    Emoticon("ROFL",       26, ":ROFL"),
    Emoticon("aggressive", 27, ">:-("),
    Emoticon("wacko",      28, ":wacko"),
    Emoticon("yahoo",      29, ":yahoo"),
    Emoticon("pardon",     30, ":?"),
    Emoticon("tease",      31, ":tease"),
    Emoticon("pleasantry", 32, ":pleasantry"),
    Emoticon("bomb",       33, ":bomb"),
    Emoticon("mega_chok",  34, ":O"),
    Emoticon("happy",      35, ":-D"),
    Emoticon("emo36",      36, ":36"),
    Emoticon("emo37",      37, ":37"),
    Emoticon("emo38",      38, ":38"),
    Emoticon("emo39",      39, ":39"),
    Emoticon("emo40",      40, ":40"),
    Emoticon("emo41",      41, ":41"),
    Emoticon("emo42",      42, ":42"),
    Emoticon("emo43",      43, ":43"),
    Emoticon("emo44",      44, ":44"),
    Emoticon("emo45",      45, ":45"),
    Emoticon("emo46",      46, ":46"),
    Emoticon("emo47",      47, ":47"),
    Emoticon("emo48",      48, ":48"),
    Emoticon("emo49",      49, ":49"),
    Emoticon("emo50",      50, ":50"),
    Emoticon("emo51",      51, ":51")
)

// Cached drawable IDs resolved once via reflection
private val emoDrawableIds: Map<Int, Int> by lazy {
    val cls = com.rcq.messenger.R.drawable::class.java
    (0..51).associate { idx ->
        val fieldName = "emo_%02d".format(idx)
        val resId = runCatching { cls.getField(fieldName).getInt(null) }.getOrDefault(0)
        idx to resId
    }
}

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
            items(ICQ_EMOTICONS) { emo ->
                EmoticonCell(emo = emo, onClick = { onEmoticonSelected(emo.text) })
            }
        }
    }
}

@Composable
private fun EmoticonCell(emo: Emoticon, onClick: () -> Unit) {
    val resId = emoDrawableIds[emo.resIndex] ?: 0
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = emo.name,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
