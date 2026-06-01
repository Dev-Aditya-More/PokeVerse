package com.aditya1875.pokeverse.data.ads

import android.app.Activity
import android.content.Context
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NoOpRewardedAdManager : IRewardedAdManager {
    override val adState: StateFlow<RewardedAdState> = MutableStateFlow(RewardedAdState.Idle)
    override fun loadAd(context: Context) {}
    override fun showAd(activity: Activity, onRewarded: () -> Unit) {}
}
