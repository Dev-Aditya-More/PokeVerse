package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

interface IBillingManager {
    val subscriptionState: StateFlow<SubscriptionState>
    val monthlyProduct: StateFlow<ProductDetails?>
    val yearlyProduct: StateFlow<ProductDetails?>
    val billingError: StateFlow<String?>

    fun startConnection()
    suspend fun queryExistingPurchases()
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        isYearly: Boolean
    )
    fun clearError()
    fun endConnection()
}

// Shared sealed class
sealed class SubscriptionState {
    object Loading : SubscriptionState()
    object Free : SubscriptionState()
    data class Premium(val plan: PremiumPlan = PremiumPlan.MONTHLY) : SubscriptionState()
}

enum class PremiumPlan {
    MONTHLY,
    YEARLY
}
