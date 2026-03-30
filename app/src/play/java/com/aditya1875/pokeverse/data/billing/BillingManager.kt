package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BillingManager(
    context: Context,
    private val coroutineScope: CoroutineScope
) : IBillingManager, PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "pokeverse_premium_monthly"
        const val PRODUCT_YEARLY = "pokeverse_premium_yearly"
    }

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

    private val _subscriptionState =
        MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _monthlyProduct = MutableStateFlow<ProductDetails?>(null)
    override val monthlyProduct: StateFlow<ProductDetails?> = _monthlyProduct

    private val _yearlyProduct = MutableStateFlow<ProductDetails?>(null)
    override val yearlyProduct: StateFlow<ProductDetails?> = _yearlyProduct

    private val _billingError = MutableStateFlow<String?>(null)
    override val billingError: StateFlow<String?> = _billingError

    override fun startConnection() {
        if (billingClient.isReady) return

        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    coroutineScope.launch {
                        queryProducts()
                        queryExistingPurchases()
                    }
                } else {
                    Log.e("Billing", "Setup failed: ${result.debugMessage}")
                    _subscriptionState.value = SubscriptionState.Free
                }
            }

            override fun onBillingServiceDisconnected() {
                coroutineScope.launch {
                    delay(2000)
                    startConnection()
                }
            }
        })
    }

    private suspend fun queryProducts() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingResponseCode.OK) {

            result.productDetailsList?.forEach { product ->
                when (product.productId) {
                    PRODUCT_MONTHLY -> _monthlyProduct.value = product
                    PRODUCT_YEARLY -> _yearlyProduct.value = product
                }
            }

        } else {
            _billingError.value =
                "Product query failed: ${result.billingResult.debugMessage}"
        }
    }

    override suspend fun queryExistingPurchases() {

        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Log.e("Billing", "Query purchases failed: ${result.billingResult.debugMessage}")
            return
        }

        val activePurchase = result.purchasesList.firstOrNull { purchase ->
            purchase.purchaseState == PurchaseState.PURCHASED &&
                    (purchase.products.contains(PRODUCT_MONTHLY)
                            || purchase.products.contains(PRODUCT_YEARLY))
        }

        _subscriptionState.value =
            if (activePurchase != null) {
                val plan =
                    if (activePurchase.products.contains(PRODUCT_YEARLY))
                        PremiumPlan.YEARLY
                    else
                        PremiumPlan.MONTHLY

                SubscriptionState.Premium(plan)

            } else {
                SubscriptionState.Free
            }

        result.purchasesList
            .filter { it.purchaseState == PurchaseState.PURCHASED }
            .filter { !it.isAcknowledged }
            .forEach { acknowledgePurchase(it) }
    }

    override fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
    ) {

        val offer = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.pricingPhases.pricingPhaseList.isNotEmpty() }

        val offerToken = offer?.offerToken ?: run {
            _billingError.value = "No offer available"
            return
        }

        val productParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

        val billingParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

        val result = billingClient.launchBillingFlow(activity, billingParams)

        if (result.responseCode != BillingResponseCode.OK) {
            _billingError.value =
                "Billing flow failed: ${result.debugMessage}"
        }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: List<Purchase>?
    ) {

        when (result.responseCode) {

            BillingResponseCode.OK -> {

                purchases?.forEach { purchase ->

                    when (purchase.purchaseState) {

                        PurchaseState.PURCHASED -> {
                            coroutineScope.launch {

                                Log.d("Billing", "Purchase token: ${purchase.purchaseToken}")

                                acknowledgePurchase(purchase)

                                val plan =
                                    if (purchase.products.contains(PRODUCT_YEARLY))
                                        PremiumPlan.YEARLY
                                    else
                                        PremiumPlan.MONTHLY

                                _subscriptionState.value =
                                    SubscriptionState.Premium(plan)
                            }
                        }

                        PurchaseState.PENDING -> {
                            _subscriptionState.value = SubscriptionState.Pending
                        }

                        else -> Unit
                    }
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                Log.d("Billing", "User cancelled purchase")
            }

            else -> {
                _billingError.value =
                    "Purchase failed: ${result.debugMessage}"
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {

        if (purchase.isAcknowledged) return

        val params =
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

        billingClient.acknowledgePurchase(params) { result ->

            if (result.responseCode != BillingResponseCode.OK) {
                Log.e("Billing", "Acknowledge failed: ${result.debugMessage}")
            } else {
                Log.d("Billing", "Purchase acknowledged")
            }
        }
    }

    override fun clearError() {
        _billingError.value = null
    }

    override fun endConnection() {
        billingClient.endConnection()
    }
}