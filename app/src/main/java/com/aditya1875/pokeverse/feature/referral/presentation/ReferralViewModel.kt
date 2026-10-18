package com.aditya1875.pokeverse.feature.referral.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPEvent
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPManager
import com.aditya1875.pokeverse.feature.leaderboard.domain.xp.XPResult
import com.aditya1875.pokeverse.feature.referral.data.RedeemResult
import com.aditya1875.pokeverse.feature.referral.data.ReferralInfo
import com.aditya1875.pokeverse.feature.referral.data.ReferralRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferralUiState(
    val signedIn: Boolean = false,
    val loading: Boolean = false,
    val info: ReferralInfo? = null,
    val loadFailed: Boolean = false,
    val codeInput: String = "",
    val redeeming: Boolean = false,
    val redeemResult: RedeemResult? = null,
    val canRedeem: Boolean = false
)

class ReferralViewModel(
    private val repo: ReferralRepository,
    private val xpManager: XPManager
) : ViewModel() {

    private val _state = MutableStateFlow(ReferralUiState())
    val state: StateFlow<ReferralUiState> = _state.asStateFlow()

    private val _xp = MutableSharedFlow<XPResult>(extraBufferCapacity = 4)
    val xp: SharedFlow<XPResult> = _xp.asSharedFlow()

    val pendingInstallCode: StateFlow<String?> = repo.pendingInstallCode

    fun load() {
        val signedIn = repo.isSignedIn
        _state.update { it.copy(signedIn = signedIn, canRedeem = signedIn && !repo.hasRedeemed) }
        if (!signedIn || _state.value.loading) return
        _state.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            val info = repo.myReferral()
            _state.update {
                it.copy(
                    loading = false,
                    info = info ?: it.info,
                    loadFailed = info == null && it.info == null,
                    canRedeem = it.canRedeem && info?.redeemed != true
                )
            }
        }
    }

    fun onCodeChange(value: String) {
        _state.update { it.copy(codeInput = value.uppercase().filter { c -> c.isLetterOrDigit() }.take(10), redeemResult = null) }
    }

    fun redeem(code: String = _state.value.codeInput) {
        if (code.isBlank() || _state.value.redeeming) return
        _state.update { it.copy(codeInput = code, redeeming = true, redeemResult = null) }
        viewModelScope.launch {
            val result = repo.redeem(code)
            _state.update {
                it.copy(
                    redeeming = false,
                    redeemResult = result,
                    canRedeem = result is RedeemResult.UnknownCode || result is RedeemResult.OwnCode || result is RedeemResult.Failed
                )
            }
            if (result is RedeemResult.Success) {
                val award = xpManager.awardGameXP(XPEvent.ReferralWelcome)
                if (award.xpGained > 0) _xp.emit(award)
            }
        }
    }

    /** Pays out XP for friends who joined since the last check. */
    fun claimRewards(force: Boolean = false) {
        viewModelScope.launch {
            val newly = repo.claimRewards(force)
            if (newly > 0) {
                val award = xpManager.awardGameXP(XPEvent.ReferralReward(newly))
                if (award.xpGained > 0) _xp.emit(award)
            }
        }
    }

    fun captureInstallReferrer() = repo.captureInstallReferrer()

    fun dismissPendingCode() = repo.clearPendingCode()

    fun shareLink(code: String) = repo.shareLink(code)
}
