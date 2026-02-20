package com.aditya1875.pokeverse.presentation.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val route: String) {

    object Splash : Route("splash")
    object Onboarding : Route("intro")

    sealed class BottomBar(route: String, val icon: ImageVector) : Route(route) {
        object Home : BottomBar("home", Icons.Default.Home)
        object Team : BottomBar("dream_team", Icons.Default.Star)
        object Game : BottomBar("game", Icons.Default.SportsEsports)
        object Settings : BottomBar("settings", Icons.Default.Settings)
    }

    object GameDifficulty : Route("game/difficulty")

    object GamePlay : Route("game/play/{difficulty}") {
        fun createRoute(difficulty: String) = "game/play/$difficulty"
    }

    object QuizDifficulty : Route("quiz/difficulty")

    object QuizPlay : Route("quiz/play/{difficulty}") {
        fun createRoute(difficulty: String) = "quiz/play/$difficulty"
    }

    object GuessDifficulty : Route("guess/difficulty")

    object GuessPlay : Route("guess/play/{difficulty}") {
        fun createRoute(difficulty: String) = "guess/play/$difficulty"
    }

    // Other screens
    object Analysis : Route("team_analysis")
    object ThemeSelector : Route("theme_selector")

    object Details : Route("pokemon_detail/{pokemonName}") {
        fun createDetails(pokemonName: String): String = "pokemon_detail/$pokemonName"
    }
}
