package com.aditya1875.pokeverse.feature.pokemon.detail.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

private val SineEaseInOut = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
fun Pokemon3DModelViewer(
    pokemonName: String,
    isShiny: Boolean,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val spriteUrl = remember(pokemonName, isShiny) {
        val name = pokemonName.lowercase()
        if (isShiny)
            "https://img.pokemondb.net/sprites/go/shiny/$name.png"
        else
            "https://img.pokemondb.net/sprites/go/normal/$name.png"
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }

    var isLoading by remember(spriteUrl) { mutableStateOf(true) }
    var hasError by remember(spriteUrl) { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = SineEaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatFraction"
    )
    val floatOffsetPx = with(density) { 12.dp.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!hasError) {
            // Shadow ellipse — shrinks and fades as sprite rises
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(width = 72.dp, height = 10.dp)
            ) {
                val shadowAlpha = 0.28f - 0.18f * floatFraction
                val widthFraction = 1f - 0.32f * floatFraction
                val ovalWidth = size.width * widthFraction
                drawOval(
                    color = Color.Black,
                    topLeft = Offset((size.width - ovalWidth) / 2f, 0f),
                    size = Size(ovalWidth, size.height),
                    alpha = shadowAlpha
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spriteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = pokemonName,
                contentScale = ContentScale.Fit,
                imageLoader = imageLoader,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { hasError = true; isLoading = false },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = -floatOffsetPx * floatFraction }
            )
        }

        if (isLoading && !hasError) {
            CircularProgressIndicator(
                color = bgColor,
                modifier = Modifier.size(40.dp)
            )
        }

        if (hasError) {
            Text(
                text = "GO sprite unavailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
