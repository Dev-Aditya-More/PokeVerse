package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aditya1875.pokeverse.utils.SubscriptionState
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// data/billing/BillingManager.kt
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "pokeverse_premium_monthly"
        const val PRODUCT_YEARLY  = "pokeverse_premium_yearly"
    }

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    // ── State Flows ───────────────────────────────────────────────
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(
        SubscriptionState.Loading
    )
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _monthlyProduct = MutableStateFlow<ProductDetails?>(null)
    val monthlyProduct: StateFlow<ProductDetails?> = _monthlyProduct

    private val _yearlyProduct = MutableStateFlow<ProductDetails?>(null)
    val yearlyProduct: StateFlow<ProductDetails?> = _yearlyProduct

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError

    // ── Connection ────────────────────────────────────────────────
    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
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
                // Retry connection
                coroutineScope.launch {
                    delay(2000)
                    startConnection()
                }
            }
        })
    }

    // ── Query available products from Play Console ────────────────
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

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            result.productDetailsList?.forEach { product ->
                when (product.productId) {
                    PRODUCT_MONTHLY -> _monthlyProduct.value = product
                    PRODUCT_YEARLY  -> _yearlyProduct.value  = product
                }
            }
        }
    }

    // ── Check if user already has active subscription ─────────────
    suspend fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        val hasActiveSub = result.purchasesList.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    (purchase.products.contains(PRODUCT_MONTHLY) ||
                            purchase.products.contains(PRODUCT_YEARLY))
        }

        _subscriptionState.value = if (hasActiveSub) {
            SubscriptionState.Premium
        } else {
            SubscriptionState.Free
        }

        // Acknowledge any unacknowledged purchases
        result.purchasesList
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .filter { !it.isAcknowledged }
            .forEach { acknowledgePurchase(it) }
    }

    // ── Launch purchase flow ──────────────────────────────────────
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        isYearly: Boolean
    ) {
        val offerToken = productDetails
            .subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken ?: return

        val productDetailsParams = BillingFlowParams.ProductDetailsParams
            .newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    // ── Called when purchase completes (success/fail/cancel) ──────
    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                coroutineScope.launch {
                    purchases?.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            acknowledgePurchase(purchase)
                            _subscriptionState.value = SubscriptionState.Premium
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d("Billing", "User cancelled purchase")
            }
            else -> {
                _billingError.value = "Purchase failed: ${result.debugMessage}"
                Log.e("Billing", "Purchase error: ${result.debugMessage}")
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        val result = billingClient.acknowledgePurchase(params)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d("Billing", "Purchase acknowledged: ${purchase.products}")
        }
    }

    fun clearError() {
        _billingError.value = null
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}