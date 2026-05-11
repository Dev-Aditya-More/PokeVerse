package com.aditya1875.pokeverse.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberIsWideScreen(): Boolean =
    LocalConfiguration.current.screenWidthDp >= 600

@Composable
fun rememberAdaptiveHPadding(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> 48.dp
        w >= 600 -> 32.dp
        else -> 16.dp
    }
}

@Composable
fun rememberAdaptiveHPaddingProfile(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> 56.dp
        w >= 600 -> 36.dp
        else -> 20.dp
    }
}

@Composable
fun rememberDetailHeaderMaxWidth(): Dp {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w >= 840 -> 480.dp
        w >= 600 -> 420.dp
        else -> 4000.dp
    }
}
