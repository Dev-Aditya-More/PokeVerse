package com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType

/** A colour for chips and glows that goes with the ball (not the paint on the ball itself). */
fun BallType.accent(): Color = when (this) {
    BallType.POKE -> Color(0xFFE53935)
    BallType.GREAT -> Color(0xFF2F7BE0)
    BallType.ULTRA -> Color(0xFFFFC107)
    BallType.MASTER -> Color(0xFFAB47BC)
}

@Composable
fun BallIcon(ball: BallType, size: Dp, modifier: Modifier = Modifier) {
    BallIcon(ball, modifier.size(size))
}

/**
 * The balls as they look in the games, drawn in code so they stay sharp at any size:
 *  - Poké Ball: red top, white bottom.
 *  - Great Ball: blue top with a red band on each side.
 *  - Ultra Ball: black top with yellow "H" marks and side bands.
 *  - Master Ball: purple top with a pink bump on each side and an "M" across the top.
 */
@Composable
fun BallIcon(ball: BallType, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = minOf(size.width, size.height) / 2f * 0.92f
        val belt = r * 0.1f
        val corner = Offset(cx - r, cy - r)
        val box = Size(r * 2, r * 2)

        // Lower half, then the upper half in the ball's own colour.
        drawArc(Color(0xFFF5F5F5), 0f, 180f, useCenter = true, topLeft = corner, size = box)
        drawArc(shell(ball), 180f, 180f, useCenter = true, topLeft = corner, size = box)

        when (ball) {
            BallType.POKE -> Unit
            BallType.GREAT -> sideBands(cx, cy, r, Color(0xFFE53935))
            BallType.ULTRA -> {
                sideBands(cx, cy, r, Color(0xFFFFD54F))
                // The "H": two uprights joined by a bar.
                val yellow = Color(0xFFFFD54F)
                val w = r * 0.1f
                drawRect(yellow, Offset(cx - r * 0.24f - w / 2, cy - r * 0.86f), Size(w, r * 0.5f))
                drawRect(yellow, Offset(cx + r * 0.24f - w / 2, cy - r * 0.86f), Size(w, r * 0.5f))
                drawRect(yellow, Offset(cx - r * 0.24f, cy - r * 0.64f - w / 2), Size(r * 0.48f, w))
            }
            BallType.MASTER -> {
                val pink = Color(0xFFF06292)
                drawCircle(pink, r * 0.2f, Offset(cx - r * 0.56f, cy - r * 0.46f))
                drawCircle(pink, r * 0.2f, Offset(cx + r * 0.56f, cy - r * 0.46f))
                // The "M".
                val m = Path().apply {
                    moveTo(cx - r * 0.26f, cy - r * 0.34f)
                    lineTo(cx - r * 0.26f, cy - r * 0.8f)
                    lineTo(cx, cy - r * 0.52f)
                    lineTo(cx + r * 0.26f, cy - r * 0.8f)
                    lineTo(cx + r * 0.26f, cy - r * 0.34f)
                }
                drawPath(m, Color(0xFFF8BBD0), style = Stroke(width = r * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        // Belt and button.
        drawLine(Color(0xFF1B1B1B), Offset(cx - r, cy), Offset(cx + r, cy), belt * 1.4f)
        drawCircle(Color(0xFF1B1B1B), r * 0.25f, Offset(cx, cy))
        drawCircle(Color(0xFFF5F5F5), r * 0.17f, Offset(cx, cy))
        drawCircle(Color(0xFFCFCFCF), r * 0.09f, Offset(cx - r * 0.03f, cy - r * 0.03f))
        // A little gloss on the shoulder, then the outline.
        drawArc(
            Color.White.copy(alpha = 0.28f), startAngle = 205f, sweepAngle = 26f, useCenter = false,
            topLeft = Offset(cx - r * 0.86f, cy - r * 0.86f), size = Size(r * 1.72f, r * 1.72f),
            style = Stroke(width = r * 0.07f, cap = StrokeCap.Round),
        )
        drawCircle(Color(0xFF1B1B1B), r, Offset(cx, cy), style = Stroke(r * 0.09f))
    }
}

private fun shell(ball: BallType): Color = when (ball) {
    BallType.POKE -> Color(0xFFE53935)
    BallType.GREAT -> Color(0xFF2F7BE0)
    BallType.ULTRA -> Color(0xFF2B2B2B)
    BallType.MASTER -> Color(0xFF7B1FA2)
}

/** The curved bands on each shoulder of the upper half. */
private fun DrawScope.sideBands(cx: Float, cy: Float, r: Float, color: Color) {
    val ring = Size(r * 1.5f, r * 1.5f)
    val corner = Offset(cx - r * 0.75f, cy - r * 0.75f)
    val stroke = Stroke(width = r * 0.3f)
    drawArc(color, startAngle = 196f, sweepAngle = 34f, useCenter = false, topLeft = corner, size = ring, style = stroke)
    drawArc(color, startAngle = 310f, sweepAngle = 34f, useCenter = false, topLeft = corner, size = ring, style = stroke)
}
