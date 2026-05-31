package com.rcq.messenger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class RCQColorScheme(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgRowHover: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMono: Color,
    val accent: Color,
    val accentPressed: Color,
    val bubbleSelf: Color,
    val bubbleOther: Color,
    val divider: Color,
    val inputBg: Color,
    val navBar: Color,
)

private val LightScheme = RCQColorScheme(
    bgPrimary     = LightColors.bgPrimary,
    bgSecondary   = LightColors.bgSecondary,
    bgRowHover    = LightColors.bgRowHover,
    textPrimary   = LightColors.textPrimary,
    textSecondary = LightColors.textSecondary,
    textMono      = LightColors.textMono,
    accent        = LightColors.accent,
    accentPressed = LightColors.accentPressed,
    bubbleSelf    = LightColors.bubbleSelf,
    bubbleOther   = LightColors.bubbleOther,
    divider       = LightColors.divider,
    inputBg       = LightColors.inputBg,
    navBar        = LightColors.navBar,
)

private val DarkScheme = RCQColorScheme(
    bgPrimary     = DarkColors.bgPrimary,
    bgSecondary   = DarkColors.bgSecondary,
    bgRowHover    = DarkColors.bgRowHover,
    textPrimary   = DarkColors.textPrimary,
    textSecondary = DarkColors.textSecondary,
    textMono      = DarkColors.textMono,
    accent        = DarkColors.accent,
    accentPressed = DarkColors.accentPressed,
    bubbleSelf    = DarkColors.bubbleSelf,
    bubbleOther   = DarkColors.bubbleOther,
    divider       = DarkColors.divider,
    inputBg       = DarkColors.inputBg,
    navBar        = DarkColors.navBar,
)

private val AmoledScheme = RCQColorScheme(
    bgPrimary     = AmoledColors.bgPrimary,
    bgSecondary   = AmoledColors.bgSecondary,
    bgRowHover    = AmoledColors.bgRowHover,
    textPrimary   = AmoledColors.textPrimary,
    textSecondary = AmoledColors.textSecondary,
    textMono      = AmoledColors.textMono,
    accent        = AmoledColors.accent,
    accentPressed = AmoledColors.accentPressed,
    bubbleSelf    = AmoledColors.bubbleSelf,
    bubbleOther   = AmoledColors.bubbleOther,
    divider       = AmoledColors.divider,
    inputBg       = AmoledColors.inputBg,
    navBar        = AmoledColors.navBar,
)

private val HighContrastScheme = RCQColorScheme(
    bgPrimary     = HighContrastColors.bgPrimary,
    bgSecondary   = HighContrastColors.bgSecondary,
    bgRowHover    = HighContrastColors.bgRowHover,
    textPrimary   = HighContrastColors.textPrimary,
    textSecondary = HighContrastColors.textSecondary,
    textMono      = HighContrastColors.textMono,
    accent        = HighContrastColors.accent,
    accentPressed = HighContrastColors.accentPressed,
    bubbleSelf    = HighContrastColors.bubbleSelf,
    bubbleOther   = HighContrastColors.bubbleOther,
    divider       = HighContrastColors.divider,
    inputBg       = HighContrastColors.inputBg,
    navBar        = HighContrastColors.navBar,
)

val LocalRCQColors = compositionLocalOf { DarkScheme }

/** True when the user has enabled JIMM retro mode */
val LocalRetroMode = compositionLocalOf { false }

/** Access current theme colors in any composable: `LocalRCQColors.current.accent` */
val rcqColors @Composable get() = LocalRCQColors.current

private fun materialLight(c: RCQColorScheme) = lightColorScheme(
    primary         = c.accent,
    onPrimary       = Color.White,
    secondary       = c.accent,
    onSecondary     = Color.White,
    background      = c.bgPrimary,
    onBackground    = c.textPrimary,
    surface         = c.bgPrimary,
    onSurface       = c.textPrimary,
    surfaceVariant  = c.bgSecondary,
    onSurfaceVariant= c.textSecondary,
    error           = ColorError,
    outline         = c.divider,
)

private fun materialDark(c: RCQColorScheme) = darkColorScheme(
    primary         = c.accent,
    onPrimary       = Color.White,
    secondary       = c.accent,
    onSecondary     = Color.White,
    background      = c.bgPrimary,
    onBackground    = c.textPrimary,
    surface         = c.bgPrimary,
    onSurface       = c.textPrimary,
    surfaceVariant  = c.bgSecondary,
    onSurfaceVariant= c.textSecondary,
    error           = ColorError,
    outline         = c.divider,
)

@Composable
fun RCQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean = false,
    highContrast: Boolean = false,
    retroMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val rcq = when {
        highContrast -> HighContrastScheme
        amoledTheme  -> AmoledScheme
        darkTheme    -> DarkScheme
        else         -> LightScheme
    }
    val isDark = darkTheme || amoledTheme || highContrast
    val material = if (isDark) materialDark(rcq) else materialLight(rcq)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = rcq.bgPrimary.toArgb()
            window.navigationBarColor = rcq.navBar.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalRCQColors provides rcq,
        LocalRetroMode provides retroMode,
    ) {
        MaterialTheme(
            colorScheme = material,
            typography = Typography,
            content = content
        )
    }
}
