package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Direct Google Play Billing Library implementation — RevenueCat is detached for now (its
// getOfferings()/StoreProduct pricing wasn't resolving in production and it was costing real
// subscribers a working paywall). This is the pre-RevenueCat implementation, restored as-is,
// adapted only where needed to satisfy IBillingManager's current shape (formatted price
// Strings instead of raw ProductDetails, PremiumPlan-based purchase flow, restorePurchases())
// so nothing in the UI layer built since had to change.
class BillingManager(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val premiumRepository: PremiumRepository
) : IBillingManager, PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "dexverse_premium_monthly"
        const val PRODUCT_YEARLY = "dexverse_premium_yearly"
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

    private var monthlyProduct: ProductDetails? = null
    private var yearlyProduct: ProductDetails? = null
    private var lifetimeProduct: ProductDetails? = null

    private val _monthlyPrice = MutableStateFlow("")
    override val monthlyPrice: StateFlow<String> = _monthlyPrice

    private val _yearlyPrice = MutableStateFlow("")
    override val yearlyPrice: StateFlow<String> = _yearlyPrice

    private val _lifetimePrice = MutableStateFlow("")
    override val lifetimePrice: StateFlow<String> = _lifetimePrice

    private val _billingError = MutableStateFlow<String?>(null)
    override val billingError: StateFlow<String?> = _billingError

    init {
        // Observe verified premium state from repository
        coroutineScope.launch {
            premiumRepository.isPremium.collect { isPremium ->
                if (isPremium) {
                    val plan = premiumRepository.premiumPlan.first() ?: PremiumPlan.MONTHLY
                    _subscriptionState.value = SubscriptionState.Premium(plan)
                } else if (_subscriptionState.value !is SubscriptionState.Loading) {
                    _subscriptionState.value = SubscriptionState.Free
                }
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                coroutineScope.launch { queryExistingPurchases() }
            }
        })
    }

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
                    PRODUCT_MONTHLY -> {
                        monthlyProduct = product
                        _monthlyPrice.value = product.formattedPrice()
                    }
                    PRODUCT_YEARLY -> {
                        yearlyProduct = product
                        _yearlyPrice.value = product.formattedPrice()
                    }
                    PRODUCT_LIFETIME -> {
                        lifetimeProduct = product
                        _lifetimePrice.value = product.formattedPrice()
                    }
                }
            }
        } else {
            Log.e("Billing", "Product query failed: ${result.billingResult.debugMessage}")
            _billingError.value = "Product query failed: ${result.billingResult.debugMessage}"
        }
    }

    // Subscriptions carry their price in the first pricing phase of the first offer (this app
    // doesn't configure trials/intro pricing); one-time products (Lifetime) carry it directly.
    private fun ProductDetails.formattedPrice(): String =
        oneTimePurchaseOfferDetails?.formattedPrice
            ?: subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            ?: ""

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

        val subsOk = subs.billingResult.responseCode == BillingResponseCode.OK
        val inAppsOk = inApps.billingResult.responseCode == BillingResponseCode.OK

        val allPurchases = subs.purchasesList + inApps.purchasesList

        if (allPurchases.isEmpty()) {
            if (subsOk && inAppsOk) {
                premiumRepository.clearPremium()
            }
            return
        }

        allPurchases.forEach { purchase ->
            if (purchase.purchaseState == PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }

                val productId = purchase.products.firstOrNull() ?: return@forEach
                val isSubscription = productId != PRODUCT_LIFETIME

                coroutineScope.launch {
                    premiumRepository.verifyPurchase(
                        purchaseToken = purchase.purchaseToken,
                        productId = productId,
                        isSubscription = isSubscription
                    )
                }
            }
        }
    }

    override suspend fun restorePurchases(): Boolean {
        queryExistingPurchases()
        delay(1000)
        return premiumRepository.isPremium.first()
    }

    override fun launchPurchaseFlow(activity: Activity, plan: PremiumPlan) {
        val productDetails = when (plan) {
            PremiumPlan.MONTHLY -> monthlyProduct
            PremiumPlan.YEARLY -> yearlyProduct
            PremiumPlan.LIFETIME -> lifetimeProduct
        } ?: return

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

                                val productId = purchase.products.firstOrNull() ?: return@launch
                                val isSubscription = productId != PRODUCT_LIFETIME

                                premiumRepository.verifyPurchase(
                                    purchaseToken = purchase.purchaseToken,
                                    productId = productId,
                                    isSubscription = isSubscription
                                )
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
                _billingError.value = "Purchase failed: ${result.debugMessage}"
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
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
