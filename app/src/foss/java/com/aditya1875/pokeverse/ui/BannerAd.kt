package com.aditya1875.pokeverse.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object BannerAdUnitIds {
    const val GAME_HUB    = ""
    const val LEADERBOARD = ""
}

@Composable
fun BannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    // No-op in FOSS build
}
