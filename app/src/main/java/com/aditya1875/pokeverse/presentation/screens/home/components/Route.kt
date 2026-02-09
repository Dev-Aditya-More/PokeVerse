package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val route: String) {

    object Splash : Route("splash")
    object Onboarding : Route("intro")

    sealed class BottomBar(route: String, val icon: ImageVector) : Route(route) {
        object Home : BottomBar("home", Icons.Default.Home)
        object Team : BottomBar("dream_team", Icons.Default.Star)
        object Settings : BottomBar("settings", Icons.Default.Settings)
    }

    // Other screens
    object Analysis : Route("team_analysis")
    object ThemeSelector : Route("theme_selector")

    object Details : Route("pokemon_detail/{pokemonName}") {
        fun createDetails(pokemonName: String): String = "pokemon_detail/$pokemonName"
    }
}
