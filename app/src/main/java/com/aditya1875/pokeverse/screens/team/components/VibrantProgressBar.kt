package com.aditya1875.pokeverse.screens.team.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VibrantProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    progressColor: Color = Color(0xFFD62828), // vibrant red
    glow: Boolean = true
) {
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(progressColor)
                .then(
                    if (glow) Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = progressColor.copy(alpha = 0.7f),
                        spotColor = progressColor.copy(alpha = 0.7f)
                    ) else Modifier
                )
        )
    }
}
