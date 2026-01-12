package com.aditya1875.pokeverse.screens.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    // Infinite transition for smooth looping
    val infiniteTransition = rememberInfiniteTransition()
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Interpolating colors subtly
    val topColor = lerp(Color(0xFF2E2E2E), Color(0xFF3A3A3A), animatedOffset)
    val bottomColor = lerp(Color(0xFF1A1A1A), Color(0xFF262626), animatedOffset)

    val gradient = Brush.verticalGradient(listOf(topColor, bottomColor))

    Box(modifier = modifier.fillMaxSize().background(gradient))
}
