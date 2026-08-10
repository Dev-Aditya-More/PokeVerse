package com.aditya1875.pokeverse.feature.game.core.presentation

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.aditya1875.pokeverse.R
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState

/**
 * Shows the rewarded ad immediately if it's already loaded. If not, kicks
 * off a load and lets the player know via a toast rather than blocking
 * gameplay behind a "Watch Ad?" confirmation dialog.
 */
fun requestRewardedAd(
    context: Context,
    activity: Activity?,
    adManager: IRewardedAdManager,
    adState: RewardedAdState,
    onAdWillShow: () -> Unit = {},
    onRewarded: () -> Unit
) {
    if (adState is RewardedAdState.Ready && activity != null) {
        onAdWillShow()
        adManager.showAd(activity, onRewarded)
    } else {
        adManager.loadAd(context)
        Toast.makeText(context, R.string.ad_not_ready_toast, Toast.LENGTH_SHORT).show()
    }
}
