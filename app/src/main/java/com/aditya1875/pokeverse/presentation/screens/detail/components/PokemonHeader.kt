package com.aditya1875.pokeverse.presentation.screens.detail.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.presentation.screens.settings.components.ResponsiveMetaballSwitch
import com.aditya1875.pokeverse.presentation.specialscreens.ParticleBackground
import com.aditya1875.pokeverse.presentation.specialscreens.getParticleTypeFor
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun PokemonDetailHeader(
    pokemon: PokemonResponse?,
    bgColor: Color,
    specialEffectsEnabled: Boolean,
    spriteEffectsEnabled: Boolean,
    spriteEffectsEnabledState: MutableState<Boolean>,
    isSpeaking: Boolean,
    spriteVisible: Boolean,
    evolutionUi: EvolutionChainUi?,
    onPokemonClick: (String) -> Unit,
    isShinyEnabled: Boolean,
    onShinyToggle: (Boolean) -> Unit,
    currentSpriteUrl: String?,
    onSpriteLoaded: (Boolean) -> Unit,
    onSpriteError: (Boolean) -> Unit,
    showLoader: Boolean,
    modifier: Modifier = Modifier
) {
    val typeList = pokemon?.types?.map { it.type.name } ?: emptyList()
    val context = LocalContext.current

    val pressScale = remember { Animatable(1f) }
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(ImageDecoderDecoder.Factory())
        }
        .build()

    val animatedAlpha by animateFloatAsState(
        targetValue = if (showLoader || !spriteVisible) 0f else 1f,
        animationSpec = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing
        ),
        label = "SpriteFade"
    )

    var spriteLoaded by remember { mutableStateOf(false) }
    var spriteError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        // Background radial glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradientBrush = Brush.radialGradient(
                colors = listOf(
                    bgColor.copy(alpha = 0.55f),
                    bgColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = center,
                radius = size.maxDimension * 0.7f
            )
            drawCircle(
                brush = gradientBrush,
                radius = size.maxDimension * 0.7f,
                center = center
            )
        }

        // Waveform visualizer
        LayeredWaveformVisualizer(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(140.dp)
                .zIndex(0f),
            color = bgColor,
            isSpeaking = isSpeaking
        )

        // Particle effects
        if (specialEffectsEnabled && spriteVisible && spriteEffectsEnabled) {
            val particleType = getParticleTypeFor(typeList)
            ParticleBackground(particleType, pokemon?.name.toString())
        }

        // Evolution chain
        if (evolutionUi != null) {
            AnimatedVisibility(
                visible = spriteVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EvolutionChainRow(
                    evolution = evolutionUi,
                    onPokemonClick = onPokemonClick,
                    modifier = Modifier.zIndex(10f)
                )
            }
        }

        // Pokemon sprite
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .zIndex(4f)
        ) {
            val hasValidSprite = currentSpriteUrl != null && !spriteError

            if (hasValidSprite) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentSpriteUrl)
                        .decoderFactory(SvgDecoder.Factory())
                        .crossfade(true)
                        .allowHardware(false)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = pokemon?.name,
                    contentScale = ContentScale.Fit,
                    imageLoader = imageLoader,
                    onLoading = {
                        onSpriteLoaded(false)
                        onSpriteError(false)
                    },
                    onSuccess = {
                        spriteLoaded = true
                        onSpriteLoaded(true)
                        onSpriteError(false)
                    },
                    onError = {
                        spriteLoaded = false
                        onSpriteLoaded(false)
                        onSpriteError(true)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = animatedAlpha
                            scaleX = pressScale.value
                            scaleY = pressScale.value
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    pressScale.animateTo(1.1f, tween(150))
                                    spriteEffectsEnabledState.value = true
                                    tryAwaitRelease()
                                    pressScale.animateTo(1.0f, tween(150))
                                    spriteEffectsEnabledState.value = false
                                }
                            )
                        }
                )
            } else {
                // Placeholder when sprite is not available
                SpritePlaceholder(
                    isShiny = isShinyEnabled,
                    bgColor = bgColor,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (!spriteVisible) 0f else 1f
                            scaleX = pressScale.value
                            scaleY = pressScale.value
                        }
                )

                LaunchedEffect(Unit) {
                    spriteLoaded = true
                }
            }

            // Loading animation
            AnimatedVisibility(
                visible = showLoader,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LottieAnimation(
                    composition = rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.shine)
                    ).value,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                )
            }
        }

        // Shiny toggle
        AnimatedVisibility(
            visible = spriteLoaded && spriteVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 140.dp)
                .zIndex(4f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isShinyEnabled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = if (isShinyEnabled) "Shiny" else "Default",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                ResponsiveMetaballSwitch(
                    checked = isShinyEnabled,
                    onCheckedChange = onShinyToggle
                )
            }
        }
    }
}
