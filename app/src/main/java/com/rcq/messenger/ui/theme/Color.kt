package com.rcq.messenger.ui.theme

import androidx.compose.ui.graphics.Color

// ── Light palette (ICQ 2002 classic white) ───────────────────────────────────
object LightColors {
    val bgPrimary       = Color(0xFFFFFFFF)
    val bgSecondary     = Color(0xFFF2F2F2)
    val bgRowHover      = Color(0xFFE6EFFA)
    val textPrimary     = Color(0xFF000000)
    val textSecondary   = Color(0xFF555555)
    val textMono        = Color(0xFF222222)
    val accent          = Color(0xFF6BB12C)   // ICQ "flower" green
    val accentPressed   = Color(0xFF4F8E1C)
    val bubbleSelf      = Color(0xFFDCEEFC)   // light blue — own messages
    val bubbleOther     = Color(0xFFF2F2F2)   // gray — incoming
    val divider         = Color(0xFFCFCFCF)
    val inputBg         = Color(0xFFF5F5F5)
    val navBar          = Color(0xFFF7F7F7)
}

// ── Dark palette (night mode) ─────────────────────────────────────────────────
object DarkColors {
    val bgPrimary       = Color(0xFF1A1A1A)
    val bgSecondary     = Color(0xFF222222)
    val bgRowHover      = Color(0xFF2A2A2A)
    val textPrimary     = Color(0xFFEDEDED)
    val textSecondary   = Color(0xFF9A9A9A)
    val textMono        = Color(0xFFB8B8B8)
    val accent          = Color(0xFF84C32C)
    val accentPressed   = Color(0xFF6BB12C)
    val bubbleSelf      = Color(0xFF2E3A4A)   // dark blue-gray — own
    val bubbleOther     = Color(0xFF222222)   // dark — incoming
    val divider         = Color(0xFF303030)
    val inputBg         = Color(0xFF2A2A2A)
    val navBar          = Color(0xFF1A1A1A)
}

// ── Status colors — identical across themes ───────────────────────────────────
val StatusOnline    = Color(0xFF4CAF50)
val StatusAway      = Color(0xFFFFC107)
val StatusBusy      = Color(0xFFF44336)
val StatusInvisible = Color(0xFF9C27B0)
val StatusOffline   = Color(0xFF9E9E9E)

// ── Utility ───────────────────────────────────────────────────────────────────
val ColorError      = Color(0xFFEF4444)
val ColorSuccess    = Color(0xFF4CAF50)

// ── Backward-compat aliases (старые имена → новые токены) ─────────────────────
// Постепенно мигрируем экраны на прямое использование LightColors/DarkColors
// через LocalRCQColors.current. Эти aliases — временный мост.
val Background      = DarkColors.bgPrimary
val Surface         = DarkColors.bgSecondary
val SurfaceVariant  = DarkColors.bgRowHover
val Primary         = DarkColors.accent
val PrimaryVariant  = DarkColors.accentPressed
val OnPrimary       = androidx.compose.ui.graphics.Color.White
val Secondary       = DarkColors.accent
val OnSecondary     = androidx.compose.ui.graphics.Color.White
val OnBackground    = DarkColors.textPrimary
val OnSurface       = DarkColors.textPrimary
val TextPrimary     = DarkColors.textPrimary
val TextSecondary   = DarkColors.textSecondary
val TextTertiary    = DarkColors.textMono
val TextOnPrimary   = androidx.compose.ui.graphics.Color.White
val MessageSent     = DarkColors.bubbleSelf
val MessageReceived = DarkColors.bubbleOther
val MessageBackground = DarkColors.bgPrimary
val Online          = StatusOnline
val Offline         = StatusOffline
val Typing          = StatusAway
val Error           = ColorError
val Success         = ColorSuccess
val Warning         = StatusAway
val Info            = androidx.compose.ui.graphics.Color(0xFF3B82F6)
val IMAGE           = androidx.compose.ui.graphics.Color(0xFF3B82F6)
