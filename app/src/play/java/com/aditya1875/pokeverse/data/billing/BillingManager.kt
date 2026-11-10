package com.aditya1875.pokeverse.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aditya1875.pokeverse.BuildConfig
import com.aditya1875.pokeverse.feature.game.core.data.billing.IBillingManager
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.aditya1875.pokeverse.feature.game.core.data.billing.SubscriptionState
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.google.firebase.auth.FirebaseAuth
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * RevenueCat-backed billing.
 *
 * Identity: premium belongs to the **Dexverse account**, not the device. While someone is signed
 * in, RevenueCat's App User ID is their Firebase uid; signed out, it's an anonymous ID.
 * - Buying as a guest still works; `logIn` merges that anonymous purchase into the account on
 *   its first sign-in. The same merge moves existing (pre-account-tied) buyers onto the first
 *   account they're signed into after updating.
 * - A second Dexverse account on the same phone (same Google Play account) does NOT get Pro.
 *   That needs the dashboard's Transfer Behavior set to "Keep with original App User ID";
 *   with the default ("Transfer"), a restore would move Pro to whoever restored it.
 *
 * Entitlement: everything premium is gated on the single [ENTITLEMENT_ID] entitlement, so
 * monthly / yearly / lifetime (and legacy `pokeverse_premium_*` products, if attached to that
 * entitlement in the dashboard) all unlock the same thing.
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val premiumRepository: PremiumRepository
) : IBillingManager {

    companion object {
        private const val TAG = "Billing"

        /** Entitlement *identifier* in the RevenueCat dashboard (case-sensitive). */
        const val ENTITLEMENT_ID = "dexverse_pro"

        // Package identifiers inside the current Offering in the RevenueCat dashboard.
        private const val PACKAGE_ID_MONTHLY = "monthly"
        private const val PACKAGE_ID_YEARLY = "yearly"
        private const val PACKAGE_ID_LIFETIME = "lifetime"
    }

    private val _subscriptionState =
        MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    override val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState

    private val _monthlyPrice = MutableStateFlow("")
    override val monthlyPrice: StateFlow<String> = _monthlyPrice

    private val _yearlyPrice = MutableStateFlow("")
    override val yearlyPrice: StateFlow<String> = _yearlyPrice

    private val _lifetimePrice = MutableStateFlow("")
    override val lifetimePrice: StateFlow<String> = _lifetimePrice

    private val _billingError = MutableStateFlow<String?>(null)
    override val billingError: StateFlow<String?> = _billingError

    private val _purchaseInProgress = MutableStateFlow(false)
    override val purchaseInProgress: StateFlow<Boolean> = _purchaseInProgress

    /** Firebase uid the current [subscriptionState] belongs to (null = guest). */
    private val _premiumOwner = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid)
    override val premiumOwner: StateFlow<String?> = _premiumOwner
    private val identityLock = Mutex()

    private var offerings: Offerings? = null

    init {
        configureIfNeeded()

        // Push updates (purchase made in the paywall, renewal, expiry, refund, restore on another
        // screen...) straight into app state.
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { info -> coroutineScope.launch { applyCustomerInfo(info) } }

        // Show the last known state immediately (no Free flash / no lockout while offline),
        // but only if it was cached for the account signed in right now.
        coroutineScope.launch {
            val cached = premiumRepository.cachedPlan(_premiumOwner.value.orEmpty())
            if (cached != null && _subscriptionState.value is SubscriptionState.Loading) {
                _subscriptionState.value = SubscriptionState.Premium(cached)
            }
        }

        // Follow sign-in / sign-out / account switches. Fires once right away with the current user.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            coroutineScope.launch { syncIdentity(uid) }
        }

        // Re-verify every time the app returns to the foreground so an expired/cancelled/refunded
        // subscription stops unlocking premium without needing a cold start.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                coroutineScope.launch { refreshCustomerInfo() }
            }
        })
    }

    private fun configureIfNeeded() {
        if (Purchases.isConfigured) return
        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        Purchases.configure(
            // No explicit appUserID on purpose: the SDK resumes its last identity (the uid after
            // the first logIn). Configuring straight AS the uid would skip logIn's merge step, and
            // existing buyers' anonymous purchases would be stranded on the old anonymous ID.
            PurchasesConfiguration.Builder(context.applicationContext, BuildConfig.REVENUECAT_API_KEY)
                .build()
        )
    }

    /**
     * Points RevenueCat at [uid] (or back to anonymous) and reloads that account's entitlement.
     * Until it lands, state is Loading, so nothing (theme, flair, gates) acts on the previous
     * account's premium.
     */
    private suspend fun syncIdentity(uid: String?) = identityLock.withLock {
        val purchases = Purchases.sharedInstance
        val current = if (purchases.isAnonymous) null else purchases.appUserID
        if (current == uid) {
            _premiumOwner.value = uid
            return@withLock
        }
        _subscriptionState.value = SubscriptionState.Loading
        try {
            val info = if (uid != null) purchases.awaitLogIn(uid).customerInfo else purchases.awaitLogOut()
            _premiumOwner.value = uid
            applyCustomerInfo(info)
        } catch (e: PurchasesException) {
            Log.e(TAG, "Switching billing identity failed: ${e.error}")
            _premiumOwner.value = uid
            // Fail closed: never carry the previous account's premium over to this one.
            _subscriptionState.value = premiumRepository.cachedPlan(uid.orEmpty())
                ?.let { SubscriptionState.Premium(it) } ?: SubscriptionState.Free
        }
    }

    // ─── IBillingManager ─────────────────────────────────────────────────────

    /** Idempotent and cheap, called from several places; only does work that's still missing. */
    override fun startConnection() {
        coroutineScope.launch {
            refreshCustomerInfo()
            // Also retries when a previous load left the prices blank (paywall's Retry button)
            if (offerings == null || _monthlyPrice.value.isEmpty() || _yearlyPrice.value.isEmpty()) {
                loadOfferings()
            }
            syncLegacyPurchasesOnce()
        }
    }

    override suspend fun queryExistingPurchases() {
        refreshCustomerInfo()
    }

    override suspend fun restorePurchases(): Boolean = try {
        applyCustomerInfo(Purchases.sharedInstance.awaitRestore())
        _subscriptionState.value is SubscriptionState.Premium
    } catch (e: PurchasesException) {
        Log.e(TAG, "Restore failed: ${e.error}")
        _billingError.value = e.error.userMessage()
        false
    }

    override fun launchPurchaseFlow(activity: Activity, plan: PremiumPlan) {
        if (_purchaseInProgress.value) return
        _purchaseInProgress.value = true
        coroutineScope.launch {
            try {
                purchase(activity, plan)
            } finally {
                _purchaseInProgress.value = false
            }
        }
    }

    private suspend fun purchase(activity: Activity, plan: PremiumPlan) {
        run {
            if (offerings == null) loadOfferings()
            val pkg = packageFor(plan)
            if (pkg == null) {
                _billingError.value = "This plan isn't available right now. Please try again in a moment."
                return
            }
            try {
                val result = Purchases.sharedInstance.awaitPurchase(
                    PurchaseParams.Builder(activity, pkg).build()
                )
                applyCustomerInfo(result.customerInfo)
                if (result.customerInfo.entitlements[ENTITLEMENT_ID]?.isActive != true) {
                    // The store accepted the purchase but RevenueCat didn't grant our entitlement:
                    // a dashboard mapping problem, not an app bug. Say so instead of failing silently.
                    Log.e(
                        TAG,
                        "Purchase of ${pkg.product.id} succeeded but '$ENTITLEMENT_ID' is not active. " +
                            "Active entitlements: ${result.customerInfo.entitlements.active.keys}. " +
                            "Attach this product to the entitlement in the RevenueCat dashboard."
                    )
                    _billingError.value =
                        "Purchase went through but Premium didn't unlock (entitlement '$ENTITLEMENT_ID' " +
                            "isn't linked to this product in RevenueCat)."
                }
            } catch (e: PurchasesTransactionException) {
                when {
                    e.userCancelled -> Log.d(TAG, "User cancelled purchase")
                    e.error.code == PurchasesErrorCode.PaymentPendingError -> {
                        if (_subscriptionState.value !is SubscriptionState.Premium) {
                            _subscriptionState.value = SubscriptionState.Pending
                        }
                    }
                    // Already owned (e.g. bought on a previous install): just sync it back.
                    e.error.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> {
                        restorePurchases()
                    }
                    else -> {
                        Log.e(TAG, "Purchase failed: ${e.error}")
                        _billingError.value = e.error.userMessage()
                    }
                }
            }
        }
    }

    override fun clearError() {
        _billingError.value = null
    }

    /** RevenueCat owns the BillingClient connection lifecycle; nothing to tear down. */
    override fun endConnection() = Unit

    // ─── Customer info / entitlement ─────────────────────────────────────────

    // Same lock as identity switches: a refresh started for the previous account can't land
    // after a switch and hand its premium to the new one.
    private suspend fun refreshCustomerInfo() = identityLock.withLock {
        try {
            applyCustomerInfo(
                // FETCH_CURRENT hits the network; falls back to the SDK's cache when offline
                Purchases.sharedInstance.awaitCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
            )
        } catch (e: PurchasesException) {
            Log.e(TAG, "Customer info fetch failed: ${e.error}")
            // Never downgrade a known-premium user over a transient failure; never leave anyone
            // stuck on Loading either.
            if (_subscriptionState.value is SubscriptionState.Loading) {
                _subscriptionState.value = SubscriptionState.Free
            }
        }
    }

    private suspend fun applyCustomerInfo(info: CustomerInfo) {
        val entitlement = info.entitlements[ENTITLEMENT_ID]
        if (entitlement?.isActive == true) {
            val plan = entitlement.toPlan()
            _subscriptionState.value = SubscriptionState.Premium(plan)
            premiumRepository.cachePremium(plan, _premiumOwner.value.orEmpty())
        } else {
            _subscriptionState.value = SubscriptionState.Free
            premiumRepository.clearPremium()
        }
    }

    private fun EntitlementInfo.toPlan(): PremiumPlan {
        val id = productIdentifier.lowercase()
        return when {
            expirationDate == null || "lifetime" in id -> PremiumPlan.LIFETIME
            "year" in id || "annual" in id -> PremiumPlan.YEARLY
            else -> PremiumPlan.MONTHLY
        }
    }

    /**
     * Anyone who bought through the previous direct-Play-Billing implementation already has a
     * Play purchase; ask RevenueCat to import it once so those subscribers keep premium. (The
     * legacy product IDs must be attached to the entitlement in the dashboard for it to grant.)
     */
    private suspend fun syncLegacyPurchasesOnce() {
        if (premiumRepository.legacyPurchasesSynced()) return
        try {
            applyCustomerInfo(Purchases.sharedInstance.awaitRestore())
            premiumRepository.markLegacyPurchasesSynced()
        } catch (e: PurchasesException) {
            Log.w(TAG, "Legacy purchase sync will retry next launch: ${e.error}")
        }
    }

    // ─── Offerings / prices ──────────────────────────────────────────────────

    private suspend fun loadOfferings() {
        try {
            val loaded = Purchases.sharedInstance.awaitOfferings()
            offerings = loaded
            val current = loaded.current
            if (current == null) {
                Log.e(TAG, "No current Offering, set one as 'Current' in the RevenueCat dashboard")
                _billingError.value = "Plans are unavailable right now."
                return
            }
            _monthlyPrice.value = packageFor(PremiumPlan.MONTHLY)?.product?.price?.formatted.orEmpty()
            _yearlyPrice.value = packageFor(PremiumPlan.YEARLY)?.product?.price?.formatted.orEmpty()
            _lifetimePrice.value = packageFor(PremiumPlan.LIFETIME)?.product?.price?.formatted.orEmpty()
        } catch (e: PurchasesException) {
            Log.e(TAG, "Offerings fetch failed: ${e.error}")
            _billingError.value = e.error.userMessage()
        }
    }

    private fun packageFor(plan: PremiumPlan): Package? {
        val packages = offerings?.current?.availablePackages ?: return null
        val (id, type) = when (plan) {
            PremiumPlan.MONTHLY -> PACKAGE_ID_MONTHLY to PackageType.MONTHLY
            PremiumPlan.YEARLY -> PACKAGE_ID_YEARLY to PackageType.ANNUAL
            PremiumPlan.LIFETIME -> PACKAGE_ID_LIFETIME to PackageType.LIFETIME
        }
        return packages.firstOrNull { it.identifier == id }
            ?: packages.firstOrNull { it.packageType == type }
    }

    private fun PurchasesError.userMessage(): String = when (code) {
        PurchasesErrorCode.NetworkError -> "No internet connection. Please try again."
        PurchasesErrorCode.StoreProblemError -> "Google Play had a problem. Please try again later."
        PurchasesErrorCode.PurchaseNotAllowedError -> "Purchases aren't allowed on this device."
        PurchasesErrorCode.PurchaseInvalidError -> "The purchase couldn't be completed."
        // Transfer Behavior "Keep with original": this Play account's Pro is someone else's.
        PurchasesErrorCode.ReceiptAlreadyInUseError ->
            "This Google Play account's Pro belongs to another Dexverse account. Sign in with that account to use it."
        else -> message
    }
}
