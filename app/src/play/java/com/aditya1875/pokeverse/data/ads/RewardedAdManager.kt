package com.aditya1875.pokeverse.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aditya1875.pokeverse.feature.game.core.data.ads.IRewardedAdManager
import com.aditya1875.pokeverse.feature.game.core.data.ads.RewardedAdState
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RewardedAdManager : IRewardedAdManager {

    companion object {
        // TODO: Replace with your production ad unit ID from admob.google.com
        // Test ID is safe to use during development — it won't generate real revenue
        private const val AD_UNIT_ID = "ca-app-pub-5302526681326969/7195763651"
    }

    private val _adState = MutableStateFlow<RewardedAdState>(RewardedAdState.Idle)
    override val adState: StateFlow<RewardedAdState> = _adState

    private var rewardedAd: RewardedAd? = null

    override fun loadAd(context: Context) {
        if (_adState.value == RewardedAdState.Loading || _adState.value == RewardedAdState.Ready) return

        _adState.value = RewardedAdState.Loading

        RewardedAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _adState.value = RewardedAdState.Ready
                    Log.d("RewardedAd", "Ad loaded successfully")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            _adState.value = RewardedAdState.Idle
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            rewardedAd = null
                            _adState.value = RewardedAdState.Idle
                            Log.e("RewardedAd", "Failed to show: ${error.message}")
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    _adState.value = RewardedAdState.Idle
                    Log.w("RewardedAd", "Failed to load: ${error.message}")
                }
            }
        )
    }

    override fun showAd(activity: Activity, onRewarded: () -> Unit) {
        val ad = rewardedAd
        if (ad == null || _adState.value != RewardedAdState.Ready) return

        _adState.value = RewardedAdState.Showing
        ad.show(activity) { _ -> onRewarded() }
    }
}
