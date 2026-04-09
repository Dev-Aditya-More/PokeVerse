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
        const val PRODUCT_LIFETIME = "dexverse_premium_lifetime"
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

    private val _lifetimeProduct = MutableStateFlow<ProductDetails?>(null)
    override val lifetimeProduct: StateFlow<ProductDetails?> = _lifetimeProduct

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

        // Separate products by type as queryProductDetails requires all products in a list to be of the same type
        val subProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        queryProductDetailsByType(subProducts)
        queryProductDetailsByType(inAppProducts)
    }

    private suspend fun queryProductDetailsByType(productList: List<QueryProductDetailsParams.Product>) {
        if (productList.isEmpty()) return

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            result.productDetailsList?.forEach { product ->
                when (product.productId) {
                    PRODUCT_MONTHLY -> _monthlyProduct.value = product
                    PRODUCT_YEARLY -> _yearlyProduct.value = product
                    PRODUCT_LIFETIME -> _lifetimeProduct.value = product
                }
            }
        } else {
            Log.e("Billing", "Product query failed: ${result.billingResult.debugMessage}")
            _billingError.value = "Product query failed: ${result.billingResult.debugMessage}"
        }
    }

    override suspend fun queryExistingPurchases() {

        if (!billingClient.isReady) return

        val subs = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inApps = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val allPurchases = subs.purchasesList + inApps.purchasesList

        val activePurchase = allPurchases.firstOrNull { purchase ->
            purchase.purchaseState == PurchaseState.PURCHASED &&
                    (
                            purchase.products.contains(PRODUCT_MONTHLY) ||
                                    purchase.products.contains(PRODUCT_YEARLY) ||
                                    purchase.products.contains(PRODUCT_LIFETIME)
                            )
        }

        _subscriptionState.value =
            if (activePurchase != null) {
                when {
                    activePurchase.products.contains(PRODUCT_LIFETIME) ->
                        SubscriptionState.Premium(PremiumPlan.LIFETIME)

                    activePurchase.products.contains(PRODUCT_YEARLY) ->
                        SubscriptionState.Premium(PremiumPlan.YEARLY)

                    else ->
                        SubscriptionState.Premium(PremiumPlan.MONTHLY)
                }
            } else {
                SubscriptionState.Free
            }
    }

    override fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails
    ) {
        val productParamsBuilder =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

        productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
            productParamsBuilder.setOfferToken(offer.offerToken)
        }

        val billingParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
                .build()

        val result = billingClient.launchBillingFlow(activity, billingParams)

        if (result.responseCode != BillingResponseCode.OK) {
            _billingError.value = "Billing flow failed: ${result.debugMessage}"
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

                                val plan = when {
                                    purchase.products.contains(PRODUCT_LIFETIME) -> PremiumPlan.LIFETIME
                                    purchase.products.contains(PRODUCT_YEARLY) -> PremiumPlan.YEARLY
                                    else -> PremiumPlan.MONTHLY
                                }

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
