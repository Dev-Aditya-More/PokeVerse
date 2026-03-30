package com.aditya1875.pokeverse.feature.pokemon.home.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import com.aditya1875.pokeverse.feature.item.presentation.screens.ItemListScreen

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