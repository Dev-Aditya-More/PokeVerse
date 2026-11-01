package com.aditya1875.pokeverse.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Local last-known-premium cache only. The source of truth is RevenueCat (see [BillingManager]).
 * The cache exists so a returning premium user doesn't see a "Free" flash (or lose access while
 * offline) before RevenueCat's first customer-info callback.
 *
 * It remembers WHICH account it belongs to (the Firebase uid, or "" for a guest), so signing a
 * different account in on this device never inherits someone else's cached premium.
 */
class PremiumRepository(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_PLAN = stringPreferencesKey("premium_plan")
        val PREMIUM_OWNER = stringPreferencesKey("premium_owner")
        // One-time sync of purchases made through the old direct-Play-Billing implementation
        val LEGACY_PURCHASES_SYNCED = booleanPreferencesKey("revenuecat_legacy_synced")
    }

    val isPremium: Flow<Boolean> = dataStore.data.map { it[Keys.IS_PREMIUM] ?: false }

    val premiumPlan: Flow<PremiumPlan?> = dataStore.data.map {
        val name = it[Keys.PREMIUM_PLAN] ?: return@map null
        runCatching { PremiumPlan.valueOf(name) }.getOrNull()
    }

    /** Last plan we saw as active for [owner] ("" = guest), or null if not premium / someone else's. */
    suspend fun cachedPlan(owner: String): PremiumPlan? {
        val prefs = dataStore.data.first()
        if (prefs[Keys.IS_PREMIUM] != true) return null
        // Caches written before premium followed the account have no owner: trust none of them.
        if (prefs[Keys.PREMIUM_OWNER] != owner) return null
        return prefs[Keys.PREMIUM_PLAN]
            ?.let { name -> runCatching { PremiumPlan.valueOf(name) }.getOrNull() }
            ?: PremiumPlan.MONTHLY
    }

    suspend fun cachePremium(plan: PremiumPlan, owner: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = true
            prefs[Keys.PREMIUM_PLAN] = plan.name
            prefs[Keys.PREMIUM_OWNER] = owner
        }
    }

    suspend fun clearPremium() {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = false
            prefs.remove(Keys.PREMIUM_PLAN)
            prefs.remove(Keys.PREMIUM_OWNER)
        }
    }

    suspend fun legacyPurchasesSynced(): Boolean =
        dataStore.data.first()[Keys.LEGACY_PURCHASES_SYNCED] == true

    suspend fun markLegacyPurchasesSynced() {
        dataStore.edit { it[Keys.LEGACY_PURCHASES_SYNCED] = true }
    }
}
