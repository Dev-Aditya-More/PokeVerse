package com.aditya1875.pokeverse.feature.referral.data

import android.content.Context
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class ReferralInfo(val code: String, val joined: Int, val redeemed: Boolean)

sealed interface RedeemResult {
    data object Success : RedeemResult
    data object NotSignedIn : RedeemResult
    data object UnknownCode : RedeemResult
    data object OwnCode : RedeemResult
    data object AlreadyRedeemed : RedeemResult
    data object TooLate : RedeemResult
    data object Failed : RedeemResult
}

/**
 * Client side of the referral Cloud Functions (getReferralCode / redeemReferral /
 * claimReferralRewards). The server only counts friends; XP is awarded locally by the caller.
 */
class ReferralRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val functions get() = FirebaseFunctions.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    private val _pendingInstallCode = MutableStateFlow(prefs.getString(KEY_PENDING_CODE, null))
    /** A friend's code carried in through the Play Store install link, not yet redeemed. */
    val pendingInstallCode: StateFlow<String?> = _pendingInstallCode.asStateFlow()

    val isSignedIn: Boolean get() = auth.currentUser != null

    val hasRedeemed: Boolean get() = prefs.getBoolean(KEY_REDEEMED, false)

    suspend fun myReferral(): ReferralInfo? {
        if (!isSignedIn) return null
        return try {
            val res = functions.getHttpsCallable("getReferralCode").call().await()
            @Suppress("UNCHECKED_CAST")
            val data = res.getData() as? Map<String, Any?> ?: return null
            val info = ReferralInfo(
                code = data["code"] as? String ?: return null,
                joined = (data["joined"] as? Number)?.toInt() ?: 0,
                redeemed = data["redeemed"] as? Boolean ?: false
            )
            if (info.redeemed) markRedeemed()
            info
        } catch (e: Exception) {
            Log.w(TAG, "getReferralCode failed", e)
            null
        }
    }

    suspend fun redeem(rawCode: String): RedeemResult {
        if (!isSignedIn) return RedeemResult.NotSignedIn
        val code = rawCode.trim().uppercase()
        return try {
            functions.getHttpsCallable("redeemReferral").call(mapOf("code" to code)).await()
            markRedeemed()
            RedeemResult.Success
        } catch (e: FirebaseFunctionsException) {
            val result = when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> RedeemResult.UnknownCode
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> { markRedeemed(); RedeemResult.AlreadyRedeemed }
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> { markRedeemed(); RedeemResult.TooLate }
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    if (e.message == "own-code") RedeemResult.OwnCode else RedeemResult.UnknownCode
                else -> RedeemResult.Failed
            }
            // A link code the server rejected will never work — stop offering it on every launch.
            if (result != RedeemResult.Failed && code == _pendingInstallCode.value) clearPendingCode()
            result
        } catch (e: Exception) {
            Log.w(TAG, "redeemReferral failed", e)
            RedeemResult.Failed
        }
    }

    /** Number of newly-joined friends to pay XP for (server marks them paid). Throttled locally. */
    suspend fun claimRewards(force: Boolean = false): Int {
        if (!isSignedIn) return 0
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CLAIM, 0L) < CLAIM_INTERVAL_MS) return 0
        return try {
            val res = functions.getHttpsCallable("claimReferralRewards").call().await()
            prefs.edit().putLong(KEY_LAST_CLAIM, now).apply()
            @Suppress("UNCHECKED_CAST")
            ((res.getData() as? Map<String, Any?>)?.get("newly") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "claimReferralRewards failed", e)
            0
        }
    }

    fun shareLink(code: String): String =
        "https://play.google.com/store/apps/details?id=${context.packageName}&referrer=ref%3D$code"

    /** Reads the Play install referrer once per install and keeps any "ref=CODE" for later. */
    fun captureInstallReferrer() {
        if (prefs.getBoolean(KEY_REFERRER_CHECKED, false)) return
        val client = InstallReferrerClient.newBuilder(context).build()
        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        val referrer = runCatching { client.installReferrer.installReferrer }.getOrNull()
                        parseCode(referrer)?.let { code ->
                            prefs.edit().putString(KEY_PENDING_CODE, code).apply()
                            _pendingInstallCode.value = code
                        }
                    }
                    if (responseCode != InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED) {
                        prefs.edit().putBoolean(KEY_REFERRER_CHECKED, true).apply()
                    }
                    runCatching { client.endConnection() }
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            })
        }
    }

    fun clearPendingCode() {
        prefs.edit().remove(KEY_PENDING_CODE).apply()
        _pendingInstallCode.value = null
    }

    // ── Popup pacing ─────────────────────────────────────────────────────────

    fun invitePromptHistory(): Pair<Int, Long> =
        prefs.getInt(KEY_INVITE_ASKS, 0) to prefs.getLong(KEY_INVITE_LAST_MS, 0L)

    fun markInvitePromptShown() {
        prefs.edit()
            .putInt(KEY_INVITE_ASKS, prefs.getInt(KEY_INVITE_ASKS, 0) + 1)
            .putLong(KEY_INVITE_LAST_MS, System.currentTimeMillis())
            .apply()
    }

    private fun markRedeemed() {
        prefs.edit().putBoolean(KEY_REDEEMED, true).remove(KEY_PENDING_CODE).apply()
        _pendingInstallCode.value = null
    }

    private fun parseCode(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null
        return referrer.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == "ref" }
            ?.get(1)
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.matches(Regex("[A-Z0-9]{4,10}")) }
    }

    private companion object {
        const val TAG = "Referral"
        const val PREFS = "referrals"
        const val KEY_PENDING_CODE = "pending_install_code"
        const val KEY_REFERRER_CHECKED = "install_referrer_checked"
        const val KEY_REDEEMED = "redeemed"
        const val KEY_LAST_CLAIM = "last_claim_ms"
        const val KEY_INVITE_ASKS = "invite_prompt_asks"
        const val KEY_INVITE_LAST_MS = "invite_prompt_last_ms"
        const val CLAIM_INTERVAL_MS = 30L * 60 * 1000
    }
}
