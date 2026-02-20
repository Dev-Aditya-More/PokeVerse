package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _monthlyProduct = MutableStateFlow<ProductDetails?>(null)
    override val monthlyProduct: StateFlow<ProductDetails?> = _monthlyProduct

    private val _yearlyProduct = MutableStateFlow<ProductDetails?>(null)
    override val yearlyProduct: StateFlow<ProductDetails?> = _yearlyProduct

    private val _billingError = MutableStateFlow<String?>(null)
    override val billingError: StateFlow<String?> = _billingError

    override fun startConnection() {
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
            _billingError.value = "Product query failed: ${result.billingResult.debugMessage}"
        }
    }

    override suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        val activePurchase = result.purchasesList.firstOrNull { purchase ->
            purchase.purchaseState == PurchaseState.PURCHASED &&
                    (purchase.products.contains(PRODUCT_MONTHLY) ||
                            purchase.products.contains(PRODUCT_YEARLY))
        }

        _subscriptionState.value =
            if (activePurchase != null) {
                val plan = when {
                    activePurchase.products.contains(PRODUCT_YEARLY) -> PremiumPlan.YEARLY
                    else -> PremiumPlan.MONTHLY
                }
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
        productDetails: ProductDetails,
        isYearly: Boolean
    ) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: run {
                _billingError.value = "No offer available for this product"
                return
            }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)

        if (result.responseCode != BillingResponseCode.OK) {
            _billingError.value = "Billing flow failed: ${result.debugMessage}"
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                coroutineScope.launch {
                    purchases?.forEach { purchase ->
                        if (purchase.purchaseState == PurchaseState.PURCHASED) {
                            acknowledgePurchase(purchase)

                            val plan = when {
                                purchase.products.contains(PRODUCT_YEARLY) -> PremiumPlan.YEARLY
                                else -> PremiumPlan.MONTHLY
                            }

                            _subscriptionState.value = SubscriptionState.Premium(plan)
                        }
                    }
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                Log.d("Billing", "User cancelled purchase")
            }
            else -> {
                _billingError.value = "Purchase failed: ${result.debugMessage}"
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params)
    }

    override fun clearError() {
        _billingError.value = null
    }

    override fun endConnection() {
        billingClient.endConnection()
    }
}