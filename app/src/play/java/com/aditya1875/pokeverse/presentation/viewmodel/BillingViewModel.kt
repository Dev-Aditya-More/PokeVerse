package com.aditya1875.pokeverse.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class BillingViewModel(
    private val billingManager: IBillingManager
) : ViewModel() {

    val subscriptionState = billingManager.subscriptionState
    val monthlyProduct = billingManager.monthlyProduct
    val yearlyProduct = billingManager.yearlyProduct

    init {
        billingManager.startConnection()
    }

    val monthlyPrice: StateFlow<String> =
        monthlyProduct
            .map { product ->
                product?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice ?: ""
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val yearlyPrice: StateFlow<String> =
        yearlyProduct
            .map { product ->
                product?.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice ?: ""
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun purchaseMonthly(activity: Activity) {
        val product = monthlyProduct.value ?: return
        billingManager.launchPurchaseFlow(activity, product)
    }

    fun purchaseYearly(activity: Activity) {
        val product = yearlyProduct.value ?: return
        billingManager.launchPurchaseFlow(activity, product)
    }

    override fun onCleared() {
        billingManager.endConnection()
        super.onCleared()
    }
}