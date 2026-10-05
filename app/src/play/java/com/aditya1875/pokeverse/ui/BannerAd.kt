package com.aditya1875.pokeverse.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import org.koin.compose.koinInject

// ── Replace these with your real AdMob banner unit IDs before release ─────────
object BannerAdUnitIds {
    const val GAME_HUB = "ca-app-pub-5302526681326969/9951979383" // TODO: replace
    const val LEADERBOARD = "ca-app-pub-5302526681326969/9117409862" // TODO: replace
}

/**
 * Anchored banner ad. Premium users never see one — this is the single choke
 * point every banner goes through, so gating it here covers every screen.
 */
@Composable
fun BannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    val billingManager: IBillingManager = koinInject()
    val subscriptionState by billingManager.subscriptionState.collectAsStateWithLifecycle()
    if (subscriptionState is SubscriptionState.Premium) return

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
        },
        // Release the ad's WebView when the banner leaves composition (e.g. on upgrade).
        onRelease = { it.destroy() }
    )
}
