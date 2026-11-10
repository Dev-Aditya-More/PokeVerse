package com.aditya1875.pokeverse.feature.challenges.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.challenges.domain.BallReward
import com.aditya1875.pokeverse.feature.game.wildcatch.domain.model.BallType
import com.aditya1875.pokeverse.feature.game.wildcatch.presentation.components.BallIcon
import com.aditya1875.pokeverse.utils.Haptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val Gold = Color(0xFFFFC94A)

private class Spark(val angle: Float, val distance: Float, val size: Float, val color: Color, val delay: Float)

/**
 * The payoff for claiming: a dimmed screen, a flash, sparks flying outwards and the balls popping in
 * one after another with a bounce and their counts, then it clears itself (or on tap).
 */
@Composable
fun RewardBurst(reward: BallReward, onDone: () -> Unit) {
    val haptics: Haptics = koinInject()
    val scrim = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val sparks = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val ballPop = remember { listOf(Animatable(0f), Animatable(0f)) }
    val particles = remember {
        val rng = Random(System.nanoTime())
        List(34) {
            Spark(
                angle = rng.nextFloat() * 6.2832f,
                distance = 90f + rng.nextFloat() * 170f,
                size = 3f + rng.nextFloat() * 5f,
                color = listOf(Gold, Color.White, Color(0xFF64B5F6), Color(0xFFFFE082))[rng.nextInt(4)],
                delay = rng.nextFloat() * 0.18f,
            )
        }
    }
    val kinds = listOfNotNull(
        reward.great.takeIf { it > 0 }?.let { BallType.GREAT to it },
        reward.ultra.takeIf { it > 0 }?.let { BallType.ULTRA to it },
    )

    LaunchedEffect(Unit) {
        haptics.play(Haptics.Haptic.SUCCESS)
        coroutineScope {
            launch { scrim.animateTo(1f, tween(180)) }
            launch { flash.animateTo(1f, tween(120)); flash.animateTo(0f, tween(420)) }
            launch { sparks.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.1f, 0.7f, 0.2f, 1f))) }
            launch { delay(120); title.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            kinds.indices.forEach { i ->
                launch {
                    delay(220L + i * 180L)
                    ballPop[i].animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                    if (i == 0) haptics.play(Haptics.Haptic.POP)
                }
            }
        }
        delay(1500)
        onDone()
    }

    Dialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f * scrim.value))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
            contentAlignment = Alignment.Center,
        ) {
            // Glow behind the balls, and the sparks that fly out of it.
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f - 30.dp.toPx())
                val glowR = 150.dp.toPx() * (0.5f + 0.5f * scrim.value)
                drawCircle(
                    Brush.radialGradient(listOf(Gold.copy(alpha = 0.55f * scrim.value + 0.35f * flash.value), Color.Transparent), c, glowR),
                    radius = glowR, center = c,
                )
                val t = sparks.value
                particles.forEach { s ->
                    val local = ((t - s.delay) / (1f - s.delay)).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEach
                    val d = s.distance * local * density
                    val alpha = (1f - local).coerceIn(0f, 1f)
                    drawCircle(
                        s.color.copy(alpha = alpha),
                        radius = s.size * density * (1f - local * 0.5f),
                        center = Offset(c.x + cos(s.angle) * d, c.y + sin(s.angle) * d),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(
                    stringResource(R.string.challenges_reward_title),
                    color = Gold,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 0.4f + 0.6f * title.value
                        scaleY = 0.4f + 0.6f * title.value
                        alpha = title.value.coerceIn(0f, 1f)
                    },
                )
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    kinds.forEachIndexed { i, (ball, count) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BallIcon(
                                ball, 84.dp,
                                Modifier.graphicsLayer {
                                    val p = ballPop[i].value
                                    scaleX = p; scaleY = p
                                    rotationZ = (1f - p) * -140f
                                    alpha = p.coerceIn(0f, 1f)
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "x$count",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.graphicsLayer {
                                    val p = ballPop[i].value
                                    scaleX = p; scaleY = p
                                    alpha = p.coerceIn(0f, 1f)
                                },
                            )
                            Text(
                                stringResource(if (ball == BallType.GREAT) R.string.item_great_ball else R.string.item_ultra_ball),
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.graphicsLayer { alpha = ballPop[i].value.coerceIn(0f, 1f) },
                            )
                        }
                    }
                }
            }
        }
    }
}
