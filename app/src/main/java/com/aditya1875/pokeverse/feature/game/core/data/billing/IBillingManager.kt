package com.aditya1875.pokeverse.feature.game.core.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface IBillingManager {

    val subscriptionState: StateFlow<SubscriptionState>

    /**
     * Formatted prices for the UI
     */
    val monthlyPrice: StateFlow<String>
    val yearlyPrice: StateFlow<String>
    val lifetimePrice: StateFlow<String>

    val billingError: StateFlow<String?>

    fun startConnection()

    suspend fun queryExistingPurchases()
    
    suspend fun restorePurchases(): Boolean

    fun launchPurchaseFlow(
        activity: Activity,
        plan: PremiumPlan
    )

    fun clearError()

    fun endConnection()
}

sealed class SubscriptionState {

    object Loading : SubscriptionState()

    object Free : SubscriptionState()

    object Pending : SubscriptionState()

    data class Premium(
        val plan: PremiumPlan = PremiumPlan.MONTHLY
    ) : SubscriptionState()
}

enum class PremiumPlan {
    MONTHLY,
    YEARLY,
    LIFETIME
}
