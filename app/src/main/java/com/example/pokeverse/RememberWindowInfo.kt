package com.example.pokeverse

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp

class RememberWindowInfo {

    data class WindowType(
        val width: Dp,
        val height: Dp
    ) {
        sealed class WindowType {
            object Compact : WindowType()
            object Medium : WindowType()
            object Expanded : WindowType()
        }
    }

}