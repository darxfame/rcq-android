package app.rcq.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Branded loading indicator: the eight ICQ-flower petals light up one after
 *  another around the circle (tester #13's "lepestki" animation). Drawn with a
 *  Canvas so no per-petal asset is needed; uses the accent colour. */
@Composable
fun PetalLoader(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val accent = RcqTheme.colors.accent
    val transition = rememberInfiniteTransition(label = "petals")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val dim = this.size.minDimension
        val r = dim * 0.30f
        val petalW = dim * 0.20f
        val petalH = dim * 0.34f
        for (i in 0 until 8) {
            // Angular distance from the moving highlight to this petal (0..4),
            // so a single bright spot sweeps around the ring.
            var d = ((phase - i) % 8f + 8f) % 8f
            if (d > 4f) d = 8f - d
            val lit = (1f - d / 4f).coerceIn(0f, 1f)
            val a = 0.22f + 0.78f * lit
            rotate(degrees = i * 45f, pivot = Offset(cx, cy)) {
                drawOval(
                    color = accent.copy(alpha = a),
                    topLeft = Offset(cx - petalW / 2f, cy - r - petalH / 2f),
                    size = Size(petalW, petalH),
                )
            }
        }
        drawCircle(color = accent, radius = dim * 0.11f, center = Offset(cx, cy))
    }
}
