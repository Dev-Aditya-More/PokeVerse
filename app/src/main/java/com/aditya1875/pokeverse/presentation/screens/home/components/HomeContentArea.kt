package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.aditya1875.pokeverse.presentation.screens.item.ItemListScreen

// ─────────────────────────────────────────────────────────────────────────────
// Drop this where your Pokemon list currently renders in HomeScreen.
// It handles the animated crossfade between Pokémon and Items content.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeContentArea(
    mode: HomeContentMode,
    pokemonContent: @Composable () -> Unit,
    onItemClick: (String) -> Unit,
) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val enterDir = if (targetState == HomeContentMode.ITEMS) 1 else -1
            (slideInHorizontally(
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
                initialOffsetX = { it * enterDir }
            ) + fadeIn(tween(250)))
                .togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(220),
                        targetOffsetX = { -it * enterDir }
                    ) + fadeOut(tween(200))
                )
        },
        label = "contentMode"
    ) { targetMode ->
        when (targetMode) {
            HomeContentMode.POKEMON -> pokemonContent()
            HomeContentMode.ITEMS   -> ItemListScreen(
                onItemClick = onItemClick
            )
        }
    }
}