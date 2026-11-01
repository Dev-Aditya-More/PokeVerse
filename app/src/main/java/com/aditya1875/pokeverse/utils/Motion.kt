package com.aditya1875.pokeverse.utils

import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * False when the player turned system animations off or Battery Saver is on. Decorative,
 * always-running motion (auras, shimmer, rotating rings) should then hold still: it's an
 * accessibility setting, and continuous redraws cost battery the player asked us to save.
 */
@Composable
fun rememberDecorativeMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val animationsOn = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
        val batterySaver = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        animationsOn && !batterySaver
    }
}
