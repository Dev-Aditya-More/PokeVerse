package com.aditya1875.pokeverse.feature.game.core.data.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

interface IBillingManager {

    val subscriptionState: StateFlow<SubscriptionState>

    val monthlyProduct: StateFlow<ProductDetails?>

    val yearlyProduct: StateFlow<ProductDetails?>

    val lifetimeProduct: StateFlow<ProductDetails?>

    val billingError: StateFlow<String?>

    fun startConnection()

    suspend fun queryExistingPurchases()

    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
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
