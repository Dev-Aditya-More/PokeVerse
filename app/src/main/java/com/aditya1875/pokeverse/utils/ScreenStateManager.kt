package com.aditya1875.pokeverse.utils

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object ScreenStateManager {
    val Context.dataStore by preferencesDataStore(name = "settings")

    val LAST_ROUTE = stringPreferencesKey("last_route")
    val INTRO_SEEN = booleanPreferencesKey("intro_seen")
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    val SPECIAL_EFFECTS_ENABLED = booleanPreferencesKey("special_effects_enabled")

    val ASSETS_SHOWN = booleanPreferencesKey("assets_shown")
    val RATING_SHOWN = booleanPreferencesKey("rating_shown")
    val PREMIUM_SHOWN = booleanPreferencesKey("premium_shown")
    val UPDATE_SHOWN_VERSION = longPreferencesKey("update_shown_version")

    val LAST_LEADERBOARD_RANK = intPreferencesKey("last_leaderboard_rank")

    val LAST_SEEN_WEEKLY_RESET = longPreferencesKey("last_seen_weekly_reset")

    suspend fun setLastCelebratedRank(context: Context, rank: Int) {
        context.dataStore.edit {
            it[LAST_LEADERBOARD_RANK] = rank
        }
    }

    suspend fun getLastSeenReset(context: Context): Long {
        return context.dataStore.data.first()[LAST_SEEN_WEEKLY_RESET] ?: 0L
    }

    suspend fun setLastSeenReset(context: Context, value: Long) {
        context.dataStore.edit {
            it[LAST_SEEN_WEEKLY_RESET] = value
        }
    }

    suspend fun getLastCelebratedRank(context: Context): Int {
        return context.dataStore.data.first()[LAST_LEADERBOARD_RANK] ?: -1
    }

    suspend fun saveCurrentRoute(context: Context, route: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_ROUTE] = route
        }
    }

    suspend fun getLastRoute(context: Context): String? {
        val prefs = context.dataStore.data.first()
        return prefs[LAST_ROUTE]
    }

    suspend fun markIntroSeen(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[INTRO_SEEN] = true
        }
    }

    suspend fun isIntroSeen(context: Context): Boolean {
        return context.dataStore.data.first()[INTRO_SEEN] ?: false
    }

    suspend fun setSpecialEffectsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SPECIAL_EFFECTS_ENABLED] = enabled
        }
    }

    fun specialEffectsEnabledFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SPECIAL_EFFECTS_ENABLED] ?: false
        }
    }

    suspend fun markAssetsShown(context: Context) {
        context.dataStore.edit { it[ASSETS_SHOWN] = true }
    }

    suspend fun markRatingShown(context: Context) {
        context.dataStore.edit { it[RATING_SHOWN] = true }
    }

    suspend fun markPremiumShown(context: Context) {
        context.dataStore.edit { it[PREMIUM_SHOWN] = true }
    }

    suspend fun markUpdateShown(context: Context, version: Long) {
        context.dataStore.edit { it[UPDATE_SHOWN_VERSION] = version }
    }

    suspend fun isAssetsShown(context: Context): Boolean {
        return context.dataStore.data.first()[ASSETS_SHOWN] ?: false
    }

    suspend fun isRatingShown(context: Context): Boolean {
        return context.dataStore.data.first()[RATING_SHOWN] ?: false
    }

    suspend fun isPremiumShown(context: Context): Boolean {
        return context.dataStore.data.first()[PREMIUM_SHOWN] ?: false
    }

    suspend fun getUpdateShownVersion(context: Context): Long {
        return context.dataStore.data.first()[UPDATE_SHOWN_VERSION] ?: 0L
    }
}
