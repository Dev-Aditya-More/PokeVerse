package com.aditya1875.pokeverse.feature.analysis.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya1875.pokeverse.feature.analysis.presentation.screens.AnalysisColors
import com.aditya1875.pokeverse.utils.localizedTypeName
import com.aditya1875.pokeverse.utils.pokemonTypeColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Offensive coverage as a radar over all 18 types: the shape grows out from the centre, each type is a
 * dot in its own colour, and types nobody on the team can hit super-effectively blink red at the
 * middle, so the gaps in the team are the first thing you see.
 */
@Composable
fun CoverageRadar(coverage: Map<String, Int>, modifier: Modifier = Modifier) {
    val types = PokemonTypeData.allTypes
    val labels = types.map { localizedTypeName(it).take(3).uppercase() }
    val measurer = rememberTextMeasurer()

    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val blink by rememberInfiniteTransition(label = "gapBlink").animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "blink",
    )

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val labelRoom = 22.dp.toPx()
            val radius = min(size.width, size.height) / 2f - labelRoom
            val n = types.size
            fun angle(i: Int) = (-PI / 2.0 + 2.0 * PI * i / n).toFloat()
            fun point(i: Int, r: Float) = Offset(c.x + cos(angle(i)) * r, c.y + sin(angle(i)) * r)

            // Rings at 1, 2 and 3+ members, plus a faint spoke to every type.
            listOf(1f / 3f, 2f / 3f, 1f).forEach { f ->
                drawCircle(Color.White.copy(alpha = 0.07f), radius * f, c, style = Stroke(1.dp.toPx()))
            }
            for (i in 0 until n) drawLine(Color.White.copy(alpha = 0.05f), c, point(i, radius), 1.dp.toPx())

            // The coverage shape: 0 members sits near the centre, 3 or more reaches the outer ring.
            val shape = Path()
            types.forEachIndexed { i, type ->
                val level = min(coverage[type] ?: 0, 3) / 3f
                val p = point(i, radius * (0.1f + 0.9f * level) * grow.value)
                if (i == 0) shape.moveTo(p.x, p.y) else shape.lineTo(p.x, p.y)
            }
            shape.close()
            drawPath(shape, AnalysisColors.BLUE.copy(alpha = 0.22f))
            drawPath(shape, AnalysisColors.BLUE, style = Stroke(2.dp.toPx()))

            types.forEachIndexed { i, type ->
                val count = coverage[type] ?: 0
                val level = min(count, 3) / 3f
                val at = point(i, radius * (0.1f + 0.9f * level) * grow.value)
                if (count == 0) {
                    // A gap: blinking red ring at the centre end of the spoke.
                    drawCircle(AnalysisColors.RED.copy(alpha = blink), 5.dp.toPx(), at, style = Stroke(2.dp.toPx()))
                } else {
                    drawCircle(pokemonTypeColor(type), 4.5.dp.toPx(), at)
                }
                // Type label just outside the outer ring.
                val label = measurer.measure(
                    labels[i],
                    TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count == 0) AnalysisColors.RED.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.7f),
                    ),
                )
                val lp = point(i, radius + 13.dp.toPx())
                drawText(label, topLeft = Offset(lp.x - label.size.width / 2f, lp.y - label.size.height / 2f))
            }
        }
    }
}
