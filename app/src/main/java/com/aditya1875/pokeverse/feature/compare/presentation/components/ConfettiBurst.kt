package com.aditya1875.pokeverse.feature.compare.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aditya1875.pokeverse.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * A one-shot confetti burst (reuses the same Lottie asset [ConfettiOverlay]
 * uses for top-3 leaderboard finishes) for the Compare tool's edge reveal —
 * plays once instead of looping, triggered by [visible] rather than a rank.
 */
@Composable
fun ConfettiBurst(visible: Boolean) {
    if (!visible) return

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti))
    val progress by animateLottieCompositionAsState(composition = composition, iterations = 1)

    Box(modifier = Modifier.fillMaxSize()) {
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = Modifier.fillMaxSize()
        )
    }
}
