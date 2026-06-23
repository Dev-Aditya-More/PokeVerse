package com.aditya1875.pokeverse.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// ── Replace these with your real AdMob banner unit IDs before release ─────────
object BannerAdUnitIds {
    const val GAME_HUB = "ca-app-pub-5302526681326969/9951979383" // TODO: replace
    const val LEADERBOARD = "ca-app-pub-5302526681326969/9117409862" // TODO: replace
}

@Composable
fun BannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adSize = remember(configuration.orientation) {
        val density = context.resources.displayMetrics.density
        val adWidth = (context.resources.displayMetrics.widthPixels / density).toInt()
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
