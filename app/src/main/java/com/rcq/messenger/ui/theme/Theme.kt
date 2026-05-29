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

val LocalRCQColors = compositionLocalOf { DarkScheme }

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
    content: @Composable () -> Unit
) {
    val rcq = if (darkTheme) DarkScheme else LightScheme
    val material = if (darkTheme) materialDark(rcq) else materialLight(rcq)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = rcq.bgPrimary.toArgb()
            window.navigationBarColor = rcq.navBar.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalRCQColors provides rcq) {
        MaterialTheme(
            colorScheme = material,
            typography = Typography,
            content = content
        )
    }
}
