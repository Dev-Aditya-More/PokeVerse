package com.aditya1875.pokeverse

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