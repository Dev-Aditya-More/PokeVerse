package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : IBillingManager {

    companion object {
        private const val REVENUECAT_API_KEY = "goog_duxRMtKZsyJxonRlLspFTPWswFY"

        // Must match the *Identifier* field of the entitlement in the RevenueCat
        // dashboard exactly (case + spacing sensitive) — not just its display name.
        private const val ENTITLEMENT_ID = "Dexverse Premium"

        // Package identifiers as configured in the RevenueCat dashboard's default Offering
        private const val PACKAGE_ID_MONTHLY = "monthly"
        private const val PACKAGE_ID_YEARLY = "yearly"
        private const val PACKAGE_ID_LIFETIME = "lifetime"
    }

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _monthlyPrice = MutableStateFlow("")
    override val monthlyPrice: StateFlow<String> = _monthlyPrice

    private val _yearlyPrice = MutableStateFlow("")
    override val yearlyPrice: StateFlow<String> = _yearlyPrice

    private val _lifetimePrice = MutableStateFlow("")
    override val lifetimePrice: StateFlow<String> = _lifetimePrice

    private val _billingError = MutableStateFlow<String?>(null)
    override val billingError: StateFlow<String?> = _billingError

    private var currentOfferings: Offerings? = null

    init {
        if (!Purchases.isConfigured) {
            Purchases.debugLogsEnabled = BuildConfig.DEBUG
            Purchases.configure(PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY).build())
        }
        // RevenueCat pushes fresh CustomerInfo whenever it changes (purchase, renewal,
        // cancellation, or a periodic background refresh) — listening here keeps
        // subscriptionState current without needing to manually re-poll on foreground.
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
            updateSubscriptionState(info)
        }
    }

    override fun startConnection() {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateSubscriptionState(customerInfo)
                loadOfferings()
            }

            override fun onError(error: PurchasesError) {
                _subscriptionState.value = SubscriptionState.Free
                Log.e("RevenueCat", "Error fetching customer info: ${error.message}")
            }
        })
    }

    private fun loadOfferings() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                Log.e("RevenueCat", "Error fetching offerings: ${error.message}")
            },
            onSuccess = { offerings ->
                currentOfferings = offerings
                val current = offerings.current ?: return@getOfferingsWith

                _monthlyPrice.value = findPackage(current, PACKAGE_ID_MONTHLY, current.monthly)?.product?.price?.formatted ?: ""
                _yearlyPrice.value = findPackage(current, PACKAGE_ID_YEARLY, current.annual)?.product?.price?.formatted ?: ""
                _lifetimePrice.value = findPackage(current, PACKAGE_ID_LIFETIME, current.lifetime)?.product?.price?.formatted ?: ""
            }
        )
    }

    override suspend fun queryExistingPurchases() {
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updateSubscriptionState(customerInfo)
            }
            override fun onError(error: PurchasesError) {}
        })
    }

    override suspend fun restorePurchases(): Boolean {
        var success = false
        try {
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { success = false },
                onSuccess = { customerInfo ->
                    updateSubscriptionState(customerInfo)
                    success = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
                }
            )
        } catch (e: Exception) {
            success = false
        }
        return success
    }

    override fun launchPurchaseFlow(activity: Activity, plan: PremiumPlan) {
        val current = currentOfferings?.current ?: return
        val packageToPurchase = when (plan) {
            PremiumPlan.MONTHLY -> findPackage(current, PACKAGE_ID_MONTHLY, current.monthly)
            PremiumPlan.YEARLY -> findPackage(current, PACKAGE_ID_YEARLY, current.annual)
            PremiumPlan.LIFETIME -> findPackage(current, PACKAGE_ID_LIFETIME, current.lifetime)
        } ?: return

        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (!userCancelled) {
                    _billingError.value = error.message
                }
            },
            onSuccess = { _, customerInfo ->
                updateSubscriptionState(customerInfo)
            }
        )
    }

    // The dashboard uses custom package identifiers ("monthly"/"yearly"/"lifetime")
    // rather than RevenueCat's reserved $rc_* ones, so the .monthly/.annual/.lifetime
    // convenience getters (which only match packages typed by those reserved
    // identifiers) may return null. Look up by the actual identifier first, and
    // only fall back to the convenience getter if the dashboard naming changes later.
    private fun findPackage(offering: Offering, identifier: String, standard: Package?): Package? =
        offering.availablePackages.firstOrNull { it.identifier == identifier } ?: standard

    private fun updateSubscriptionState(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]
        if (entitlement?.isActive == true) {
            // Google Play subscriptions with base plans report productIdentifier as
            // "productId:basePlanId" — match by prefix so both the bare and composite
            // forms (and the pre-rebrand legacy IDs) resolve correctly.
            val productId = entitlement.productIdentifier
            val plan = when {
                productId.startsWith("dexverse_premium_lifetime") || productId == PACKAGE_ID_LIFETIME ->
                    PremiumPlan.LIFETIME
                productId.startsWith("dexverse_premium_yearly") ||
                        productId.startsWith("pokeverse_premium_yearly") ||
                        productId.startsWith("dexverse-premium-yearly") ||
                        productId == PACKAGE_ID_YEARLY ->
                    PremiumPlan.YEARLY
                else -> PremiumPlan.MONTHLY
            }
            _subscriptionState.value = SubscriptionState.Premium(plan)
        } else {
            _subscriptionState.value = SubscriptionState.Free
        }
    }

    override fun clearError() {
        _billingError.value = null
    }

    override fun endConnection() {
        // RevenueCat handles this automatically
    }
}
