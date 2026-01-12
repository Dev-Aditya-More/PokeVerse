package com.aditya1875.pokeverse.utils

import android.os.Build

object EffectCapabilities {
    val supportsShaders: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
