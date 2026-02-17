package com.aditya1875.pokeverse.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.data.billing.BillingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: BillingManager
) : ViewModel() {

    val subscriptionState = billingManager.subscriptionState
    val monthlyProduct    = billingManager.monthlyProduct
    val yearlyProduct     = billingManager.yearlyProduct
    val billingError      = billingManager.billingError

    // Formatted prices from Play Console (auto handles currency/locale)
    val monthlyPrice: StateFlow<String> = monthlyProduct.map { product ->
        product?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice ?: "₹49"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹49")

    val yearlyPrice: StateFlow<String> = yearlyProduct.map { product ->
        product?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice ?: "₹399"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹399")

    fun purchaseMonthly(activity: Activity) {
        val product = monthlyProduct.value ?: return
        billingManager.launchPurchaseFlow(
            activity = activity,
            productDetails = product,
            isYearly = false
        )
    }

    fun purchaseYearly(activity: Activity) {
        val product = yearlyProduct.value ?: return
        billingManager.launchPurchaseFlow(
            activity = activity,
            productDetails = product,
            isYearly = true
        )
    }

    fun refreshPurchases() {
        viewModelScope.launch {
            billingManager.queryExistingPurchases()
        }
    }

    fun clearError() = billingManager.clearError()

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }
}