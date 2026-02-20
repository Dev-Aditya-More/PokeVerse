package com.aditya1875.pokeverse.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.billing.IBillingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: IBillingManager
) : ViewModel() {

    val subscriptionState = billingManager.subscriptionState
    val monthlyProduct = billingManager.monthlyProduct
    val yearlyProduct = billingManager.yearlyProduct
    val billingError = billingManager.billingError

    val monthlyPrice: StateFlow<String> = monthlyProduct.map { product ->
        product?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val yearlyPrice: StateFlow<String> = yearlyProduct.map { product ->
        product?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun purchaseMonthly(activity: Activity) {
        val product = monthlyProduct.value ?: return
        billingManager.launchPurchaseFlow(activity, product, isYearly = false)
    }

    fun purchaseYearly(activity: Activity) {
        val product = yearlyProduct.value ?: return
        billingManager.launchPurchaseFlow(activity, product, isYearly = true)
    }

    fun refreshPurchases() {
        viewModelScope.launch {
            billingManager.queryExistingPurchases()
        }
    }

    fun clearError() = billingManager.clearError()
}
