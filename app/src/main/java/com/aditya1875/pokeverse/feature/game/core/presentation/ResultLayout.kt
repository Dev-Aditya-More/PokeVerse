package com.aditya1875.pokeverse.feature.game.core.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotSpeed: Float,
    val shape: Int,
)

private fun generateParticles(heroColor: Color, count: Int = 60): List<Particle> {
    val palette = listOf(
        heroColor,
        heroColor.copy(red = (heroColor.red + 0.3f).coerceIn(0f,1f)),
        Color(0xFFFFD700),
        Color(0xFFFFFFFF).copy(alpha = 0.8f),
        heroColor.copy(blue = (heroColor.blue + 0.4f).coerceIn(0f,1f)),
    )
    return List(count) {
        val angle = (it.toFloat() / count) * 2 * PI.toFloat() + Random.nextFloat() * 0.5f
        val speed = 3f + Random.nextFloat() * 4f
        Particle(
            x = 0.5f, y = 0.35f,
            vx = cos(angle) * speed,
            vy = sin(angle) * speed - 2f,
            color = palette[it % palette.size],
            size = 4f + Random.nextFloat() * 6f,
            rotation = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 15f,
            shape = it % 3,
        )
    }
}

private fun DrawScope.drawParticle(p: Particle, progress: Float, gravity: Float = 0.15f) {
    val t = progress
    val px = (p.x + p.vx * t * 0.06f) * size.width
    val py = (p.y + p.vy * t * 0.06f + 0.5f * gravity * t * t * 0.06f) * size.height
    val rot = p.rotation + p.rotSpeed * t
    val alpha = (1f - (t * 0.7f)).coerceIn(0f, 1f)
    val paint = p.color.copy(alpha = p.color.alpha * alpha)

    withTransform({
        translate(px, py)
        rotate(rot, Offset.Zero)
    }) {
        when (p.shape) {
            0 -> drawCircle(paint, p.size * (1f - t * 0.3f))
            1 -> drawRect(paint, topLeft = Offset(-p.size, -p.size / 2),
                size = Size(p.size * 2, p.size))
            else -> {
                val path = Path().apply {
                    moveTo(0f, -p.size)
                    lineTo(p.size, 0f)
                    lineTo(0f, p.size)
                    lineTo(-p.size, 0f)
                    close()
                }
                drawPath(path, paint)
            }
        }
    }
}

@Composable
fun GameResultLayout(
    title: String,
    subtitle: String,
    score: String,
    scoreLabel: String,
    heroColor: Color,
    stars: Int = -1,
    isNewBest: Boolean = false,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    heroContent: @Composable () -> Unit = {},
    statsContent: @Composable ColumnScope.() -> Unit,
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    // ── Particle system ───────────────────────────────────────────────────────
    val particles = remember(heroColor) { generateParticles(heroColor) }
    val particleProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300)
        particleProgress.animateTo(1f, tween(2200, easing = LinearEasing))
    }

    // ── Star pop animation ────────────────────────────────────────────────────
    val starScales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(stars) {
        if (stars >= 0) {
            delay(600)
            starScales.forEachIndexed { i, anim ->
                delay(150L * i)
                anim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }
        }
    }

    // ── Score count-up ────────────────────────────────────────────────────────
    val targetScore = score.toIntOrNull() ?: 0
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(targetScore) {
        delay(500)
        animatedScore.animateTo(targetScore.toFloat(), tween(900, easing = LinearEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background gradient ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to heroColor.copy(alpha = 0.18f),
                        0.45f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background,
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { drawParticle(it, particleProgress.value) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            AnimatedVisibility(
                visible = isNewBest && entrance.value > 0.3f,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🏆", fontSize = 16.sp)
                        Text(
                            "NEW BEST",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .scale(
                        lerp(0.6f, 1f, entrance.value.coerceIn(0f, 1f))
                    )
                    .alpha(entrance.value.coerceIn(0f, 1f)),
                contentAlignment = Alignment.Center
            ) {
                // Glowing halo ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(heroColor.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                )
                heroContent()
            }

            Spacer(Modifier.height(20.dp))

            // ── Stars row ─────────────────────────────────────────────────────
            if (stars >= 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { i ->
                        val filled = i < stars
                        Icon(
                            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (filled) Color(0xFFFFD700)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(36.dp)
                                .scale(starScales[i].value)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Title ─────────────────────────────────────────────────────────
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(((entrance.value - 0.2f) * 2f).coerceIn(0f, 1f))
                    .offset(y = lerp(12f, 0f, entrance.value).dp)
            )

            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(((entrance.value - 0.3f) * 2f).coerceIn(0f, 1f))
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Score — large, count-up animated ──────────────────────────────
            val displayScore = if (score.toIntOrNull() != null)
                animatedScore.value.toInt().toString()
            else score

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(((entrance.value - 0.35f) * 2.5f).coerceIn(0f, 1f))
            ) {
                Text(
                    text = displayScore,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = heroColor,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Stats block ───────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(((entrance.value - 0.5f) * 3f).coerceIn(0f, 1f))
                    .offset(y = lerp(20f, 0f, ((entrance.value - 0.5f) * 3f).coerceIn(0f, 1f)).dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = heroColor.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    statsContent()
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Actions ───────────────────────────────────────────────────────
            val btnAlpha = ((entrance.value - 0.7f) * 4f).coerceIn(0f, 1f)

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(btnAlpha),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = heroColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Replay, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Play Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(btnAlpha),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, heroColor.copy(alpha = 0.4f))
            ) {
                Text(
                    "Back to Menu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = heroColor
                )
            }
        }
    }
}

// ─── Shared stat row inside stats block ──────────────────────────────────────
@Composable
fun ResultStatRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    isLast: Boolean = false,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        icon, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (valueColor != Color.Unspecified) valueColor
                else MaterialTheme.colorScheme.onSurface
            )
        }
        if (!isLast) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }
    }
}

// ─── Inline stat chips row (3 across) ────────────────────────────────────────
@Composable
fun ResultStatChips(vararg chips: Pair<String, String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.forEach { (label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)