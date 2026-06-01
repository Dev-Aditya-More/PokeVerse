package com.aditya1875.pokeverse.feature.pokemon.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BgTop = Color(0xFF060D1F)
private val BgBottom = Color(0xFF0A1628)
private val CyanGlow = Color(0xFF29C6E0)
private val RedGlow = Color(0xFFE05A29)
private val White = Color(0xFFEEF4FF)

@Preview(showBackground = true, showSystemUi = false)
@Composable
fun SplashScreen() {

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // dual-color ambient glow
        Canvas(modifier = Modifier.size(340.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = size.minDimension / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx - base * 0.35f, cy),
                    radius = base * 0.9f
                ),
                radius = base * 0.9f,
                center = Offset(cx - base * 0.35f, cy),
                blendMode = BlendMode.Screen
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(RedGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx + base * 0.35f, cy),
                    radius = base * 0.9f
                ),
                radius = base * 0.9f,
                center = Offset(cx + base * 0.35f, cy),
                blendMode = BlendMode.Screen
            )
        }

        // rotating accent ring
        Canvas(modifier = Modifier.size(220.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 6f
            rotate(ringRotation, Offset(cx, cy)) {
                for (i in 0 until 24) {
                    val angle = (i / 24f) * 2f * PI.toFloat()
                    val dotAlpha = if (i % 3 == 0) 0.9f else 0.25f
                    drawCircle(
                        color = if (i < 12) CyanGlow else RedGlow,
                        radius = 3f,
                        center = Offset(cx + cos(angle) * r, cy + sin(angle) * r),
                        alpha = dotAlpha
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DEXVERSE",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CyanGlow, White, RedGlow)
                    )
                )
            )
        }
    }
}
