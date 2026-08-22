package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

private val Context.billingDataStore: DataStore<Preferences> by preferencesDataStore(name = "billing_prefs")

// Direct Google Play Billing Library implementation — RevenueCat is detached for now (its
// getOfferings()/StoreProduct pricing wasn't resolving in production and it was costing real
// subscribers a working paywall). This is the pre-RevenueCat implementation, restored as-is,
// adapted only where needed to satisfy IBillingManager's current shape (formatted price
// Strings instead of raw ProductDetails, PremiumPlan-based purchase flow, restorePurchases())
// so nothing in the UI layer built since had to change.
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : IBillingManager, PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_MONTHLY = "dexverse_premium_monthly"
        const val PRODUCT_YEARLY = "dexverse_premium_yearly"
        const val PRODUCT_LIFETIME = "dexverse_premium_lifetime"
        // Legacy IDs from before the rebrand; existing subscribers may still hold these
        private const val PRODUCT_MONTHLY_LEGACY = "pokeverse_premium_monthly"
        private const val PRODUCT_YEARLY_LEGACY = "pokeverse_premium_yearly"

        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_PREMIUM_PLAN = stringPreferencesKey("premium_plan")
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
        // Google Play only reflects a cancellation once the paid period actually ends —
        // until then the purchase legitimately stays PURCHASED, that part is correct.
        // But this app only re-verified against Play's live purchase state on cold start,
        // so a user whose period *had* already ended kept local Premium access for as
        // long as the process stayed alive. Re-checking on every foreground return closes
        // that staleness window down to "since last backgrounded" instead of "since last
        // full app restart" — matches Google's own recommended practice of re-querying
        // purchases in onResume.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                coroutineScope.launch { queryExistingPurchases() }
            }
        })
    }

    override fun startConnection() {
        // Restore cached premium state immediately so the UI never flashes Free on cold start
        coroutineScope.launch { loadCachedPremiumState() }

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
                    // Don't downgrade a known-premium user on a transient billing setup failure
                    if (_subscriptionState.value !is SubscriptionState.Premium) {
                        _subscriptionState.value = SubscriptionState.Free
                    }
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

        val activePurchase = allPurchases.firstOrNull { purchase ->
            purchase.purchaseState == PurchaseState.PURCHASED &&
                    (
                            purchase.products.contains(PRODUCT_MONTHLY) ||
                                    purchase.products.contains(PRODUCT_MONTHLY_LEGACY) ||
                                    purchase.products.contains(PRODUCT_YEARLY) ||
                                    purchase.products.contains(PRODUCT_YEARLY_LEGACY) ||
                                    purchase.products.contains(PRODUCT_LIFETIME)
                            )
        }

        if (activePurchase != null) {
            // Acknowledge purchases that survived a crash/kill before acknowledgment.
            // Google Play refunds unacknowledged purchases within 3 days.
            if (!activePurchase.isAcknowledged) {
                acknowledgePurchase(activePurchase)
            }
            val plan = when {
                activePurchase.products.contains(PRODUCT_LIFETIME) ->
                    PremiumPlan.LIFETIME

                activePurchase.products.contains(PRODUCT_YEARLY) ||
                        activePurchase.products.contains(PRODUCT_YEARLY_LEGACY) ->
                    PremiumPlan.YEARLY

                else ->
                    PremiumPlan.MONTHLY
            }
            _subscriptionState.value = SubscriptionState.Premium(plan)
            cachePremium(plan)
        } else if (subsOk && inAppsOk) {
            // Both queries returned OK with zero purchases — this is a definitive non-premium result
            _subscriptionState.value = SubscriptionState.Free
            clearPremiumCache()
        }
        // If either query failed (e.g. Play Services unavailable), preserve current state
    }

    override suspend fun restorePurchases(): Boolean {
        queryExistingPurchases()
        return _subscriptionState.value is SubscriptionState.Premium
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

                                val plan = when {
                                    purchase.products.contains(PRODUCT_LIFETIME) -> PremiumPlan.LIFETIME
                                    purchase.products.contains(PRODUCT_YEARLY) ||
                                            purchase.products.contains(PRODUCT_YEARLY_LEGACY) -> PremiumPlan.YEARLY
                                    else -> PremiumPlan.MONTHLY
                                }

                                _subscriptionState.value = SubscriptionState.Premium(plan)
                                cachePremium(plan)
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

    private suspend fun loadCachedPremiumState() {
        val prefs = context.billingDataStore.data.first()
        if (prefs[KEY_IS_PREMIUM] == true) {
            val planName = prefs[KEY_PREMIUM_PLAN] ?: PremiumPlan.MONTHLY.name
            val plan = runCatching { PremiumPlan.valueOf(planName) }.getOrDefault(PremiumPlan.MONTHLY)
            _subscriptionState.value = SubscriptionState.Premium(plan)
        }
    }

    private suspend fun cachePremium(plan: PremiumPlan) {
        context.billingDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = true
            prefs[KEY_PREMIUM_PLAN] = plan.name
        }
    }

    private suspend fun clearPremiumCache() {
        context.billingDataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = false
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
