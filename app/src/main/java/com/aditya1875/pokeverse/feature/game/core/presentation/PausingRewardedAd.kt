package com.aditya1875.pokeverse.feature.game.core.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

enum class AdRequestPhase { Idle, Waiting, Unavailable }

/**
 * A rewarded ad for real-time games: the run pauses the instant it's requested
 * (never while an ad loads in the background and the player dies), a loader
 * shows while it's fetched, and it plays as soon as it's ready. If nothing
 * arrives in [LOAD_TIMEOUT_MS] the player is told plainly and stays paused.
 *
 * Obtain one with [rememberPausingRewardedAd]; draw [AdRequestOverlay] for its UI.
 */
@Stable
class PausingRewardedAd internal constructor(
    private val adManager: IRewardedAdManager,
    private val context: Context
) {
    var phase by mutableStateOf(AdRequestPhase.Idle)
        private set

    internal var onPause: () -> Unit = {}
    internal var onRewarded: () -> Unit = {}

    fun request() {
        onPause()
        if (adManager.adState.value is RewardedAdState.Ready) {
            show()
        } else {
            phase = AdRequestPhase.Waiting
            adManager.loadAd(context)
        }
    }

    fun dismiss() {
        phase = AdRequestPhase.Idle
    }

    internal fun onAdStateChanged(state: RewardedAdState) {
        if (phase != AdRequestPhase.Waiting) return
        when (state) {
            RewardedAdState.Ready -> show()
            // The manager drops back to Idle when a load fails.
            RewardedAdState.Idle -> phase = AdRequestPhase.Unavailable
            else -> Unit
        }
    }

    internal fun onTimeout() {
        if (phase == AdRequestPhase.Waiting) phase = AdRequestPhase.Unavailable
    }

    private fun show() {
        val activity = context.findActivity()
        if (activity == null) {
            phase = AdRequestPhase.Unavailable
            return
        }
        phase = AdRequestPhase.Idle
        adManager.showAd(activity) { onRewarded() }
    }

    internal companion object {
        const val LOAD_TIMEOUT_MS = 10_000L
    }
}

/** Walks ContextWrappers (e.g. a locale-wrapped context) to the hosting Activity. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberPausingRewardedAd(onPause: () -> Unit, onRewarded: () -> Unit): PausingRewardedAd {
    val adManager: IRewardedAdManager = koinInject()
    val context = LocalContext.current
    val ad = remember(adManager, context) { PausingRewardedAd(adManager, context) }
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnRewarded by rememberUpdatedState(onRewarded)
    ad.onPause = { currentOnPause() }
    ad.onRewarded = { currentOnRewarded() }

    val adState by adManager.adState.collectAsState()
    LaunchedEffect(adState) { ad.onAdStateChanged(adState) }
    LaunchedEffect(ad.phase) {
        if (ad.phase == AdRequestPhase.Waiting) {
            delay(PausingRewardedAd.LOAD_TIMEOUT_MS)
            ad.onTimeout()
        }
    }
    return ad
}

/**
 * Keeps a rewarded ad warm while [enabled], so most requests play instantly.
 * Failed loads retry after [retryAfterMs] instead of hammering the ad network.
 */
@Composable
fun PreloadRewardedAd(enabled: Boolean, retryAfterMs: Long = 20_000L) {
    val adManager: IRewardedAdManager = koinInject()
    val context = LocalContext.current
    val adState by adManager.adState.collectAsState()
    var attempts by remember { mutableStateOf(0) }
    LaunchedEffect(enabled, adState) {
        if (!enabled || adState !is RewardedAdState.Idle) return@LaunchedEffect
        if (attempts > 0) delay(retryAfterMs)
        attempts++
        adManager.loadAd(context)
    }
}

/** Loader while the ad is fetched, or a plain "no ad right now" note. Blocks touches behind it. */
@Composable
fun AdRequestOverlay(ad: PausingRewardedAd, loadingText: String) {
    if (ad.phase == AdRequestPhase.Idle) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (ad.phase) {
                    AdRequestPhase.Waiting -> {
                        CircularProgressIndicator()
                        Text(
                            loadingText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            stringResource(R.string.ad_loading_paused),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = ad::dismiss) { Text(stringResource(R.string.cancel)) }
                    }
                    AdRequestPhase.Unavailable -> {
                        Text("📺", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            stringResource(R.string.ad_unavailable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = ad::dismiss) { Text(stringResource(R.string.ad_unavailable_ok)) }
                    }
                    AdRequestPhase.Idle -> Unit
                }
            }
        }
    }
}
