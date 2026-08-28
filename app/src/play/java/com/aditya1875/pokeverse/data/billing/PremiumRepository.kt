package com.aditya1875.pokeverse.data.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.aditya1875.pokeverse.feature.game.core.data.billing.PremiumPlan
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PremiumRepository(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    private val functions = FirebaseFunctions.getInstance()
    private val auth = FirebaseAuth.getInstance()

    object Keys {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_PLAN = stringPreferencesKey("premium_plan")
        val EXPIRY_TIME = longPreferencesKey("premium_expiry")
    }

    val isPremium: Flow<Boolean> = dataStore.data.map { it[Keys.IS_PREMIUM] ?: false }
    val premiumPlan: Flow<PremiumPlan?> = dataStore.data.map {
        val name = it[Keys.PREMIUM_PLAN] ?: return@map null
        runCatching { PremiumPlan.valueOf(name) }.getOrNull()
    }

    suspend fun verifyPurchase(
        purchaseToken: String,
        productId: String,
        isSubscription: Boolean
    ): Boolean {
        val uid = auth.currentUser?.uid
        val data = hashMapOf(
            "purchaseToken" to purchaseToken,
            "productId" to productId,
            "isSubscription" to isSubscription,
            "uid" to uid
        )

        return try {
            val result = functions
                .getHttpsCallable("verifyPurchase")
                .call(data)
                .await()

            val response = result.data as? Map<*, *>
            val success = response?.get("success") as? Boolean ?: false

            if (success) {
                val entitlement = response?.get("entitlement") as? Map<*, *>
                // expiryTimeMillis comes back as a Number from Firebase Functions
                val expiryTime = (entitlement?.get("expiryTimeMillis") as? Number)?.toLong() ?: 0L
                val plan = when (productId) {
                    "dexverse_premium_yearly", "pokeverse_premium_yearly" -> PremiumPlan.YEARLY
                    "dexverse_premium_lifetime" -> PremiumPlan.LIFETIME
                    else -> PremiumPlan.MONTHLY
                }
                updateLocalPremium(true, plan, expiryTime)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateLocalPremium(isPremium: Boolean, plan: PremiumPlan? = null, expiryTime: Long = 0L) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = isPremium
            if (plan != null) {
                prefs[Keys.PREMIUM_PLAN] = plan.name
            }
            prefs[Keys.EXPIRY_TIME] = expiryTime
        }
    }

    suspend fun clearPremium() {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PREMIUM] = false
            prefs.remove(Keys.PREMIUM_PLAN)
            prefs.remove(Keys.EXPIRY_TIME)
        }
    }
}
