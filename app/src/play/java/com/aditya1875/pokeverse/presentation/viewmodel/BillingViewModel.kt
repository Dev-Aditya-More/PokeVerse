package com.aditya1875.pokeverse.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: IBillingManager
) : ViewModel() {

    val subscriptionState = billingManager.subscriptionState
    val monthlyPrice = billingManager.monthlyPrice
    val yearlyPrice = billingManager.yearlyPrice
    val lifetimePrice = billingManager.lifetimePrice
    val billingError = billingManager.billingError
    val purchaseInProgress = billingManager.purchaseInProgress

    init {
        billingManager.startConnection()
    }

    fun purchaseMonthly(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, PremiumPlan.MONTHLY)
    }

    fun purchaseYearly(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, PremiumPlan.YEARLY)
    }

    fun purchaseLifetime(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, PremiumPlan.LIFETIME)
    }

    suspend fun restorePurchases(): Boolean {
        return billingManager.restorePurchases()
    }

    /** (Re)loads plans/prices if they're missing; cheap no-op otherwise. */
    fun retryLoadPlans() = billingManager.startConnection()

    /** Re-reads the entitlement from RevenueCat (e.g. right after the paywall completes a purchase). */
    fun refreshEntitlement() {
        viewModelScope.launch { billingManager.queryExistingPurchases() }
    }

    fun clearError() {
        billingManager.clearError()
    }

    override fun onCleared() {
        billingManager.endConnection()
        super.onCleared()
    }
}
