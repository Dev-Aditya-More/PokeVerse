package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GlowGold = Color(0xFFFFD54A)

@Composable
fun DailyHoppingPokemon(
    onClicked: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var hasBeenClicked by remember { mutableStateOf(false) }

    // Pick today's Pokemon deterministically from the date, so it's the same
    // "mystery" Pokemon all day (even across app restarts) but a different one
    // tomorrow — rather than a fresh random pick every time this composes.
    val pokemonId = remember {
        val ids = listOf(25, 133, 1, 4, 7, 10, 16, 39, 54, 152, 155, 158, 252, 255, 258, 387, 390, 393)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        ids[Math.floorMod(today.hashCode(), ids.size)]
    }

    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp.dp
    val screenHeight = config.screenHeightDp.dp

    // Positioned exactly above the bottom bar.
    // Assuming standard BottomAppBar height (~80dp) + some buffer.
    val fixedY = remember { (config.screenHeightDp - 140).dp }
    
    val xAnim = remember { Animatable(-100f) }
    
    // Jump animation
    val infiniteTransition = rememberInfiniteTransition(label = "hop")
    val hopY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hopY"
    )

    // Pulsing glow behind the Pokemon so it actually catches the eye against
    // the list/grid content instead of blending in.
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        // Keep giving the user chances to spot it throughout the session —
        // a single 8-second pass is too easy to miss entirely. It stops for
        // good once clicked (or claimed, which unmounts this composable).
        while (!hasBeenClicked) {
            delay(Random.nextLong(15_000L, 30_000L))
            if (hasBeenClicked) break

            xAnim.snapTo(-100f)
            isVisible = true

            // Animate across the screen
            xAnim.animateTo(
                targetValue = config.screenWidthDp + 100f,
                animationSpec = tween(8000, easing = LinearEasing)
            )
            isVisible = false
        }
    }

    if (isVisible && !hasBeenClicked) {
        Box(
            // fillMaxSize() here was the bug: it made this Box as tall as the whole
            // screen, so the content — centered *within* the box — ended up at
            // fixedY + screenHeight/2, off the bottom of the screen on every device.
            // fillMaxWidth() lets the box wrap to just the content's height, so the
            // y-offset lands the sprite where fixedY actually says.
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = xAnim.value.dp, y = fixedY + hopY.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow sits behind the sprite and pulses independently of the hop,
            // so the whole thing reads as "something is happening here" even
            // out of the corner of your eye.
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(glowScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GlowGold.copy(alpha = glowAlpha),
                                GlowGold.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            AsyncImage(
                model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png",
                contentDescription = "Surprise Pokemon",
                modifier = Modifier
                    .size(80.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        hasBeenClicked = true
                        onClicked()
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}
