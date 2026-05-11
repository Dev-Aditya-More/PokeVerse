package com.aditya1875.pokeverse.feature.game.core.data.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface IRewardedAdManager {
    val adState: StateFlow<RewardedAdState>
    fun loadAd(context: Context)
    fun showAd(activity: Activity, onRewarded: () -> Unit)
}

sealed class RewardedAdState {
    object Idle : RewardedAdState()
    object Loading : RewardedAdState()
    object Ready : RewardedAdState()
    object Showing : RewardedAdState()
}
