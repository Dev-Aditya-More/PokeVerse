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

    val APP_VERSION = intPreferencesKey("app_version")
    private const val CURRENT_APP_VERSION = 1

    suspend fun isFirstLaunch(context: Context): Boolean {
        return context.dataStore.data.first()[FIRST_LAUNCH] ?: true
    }

    suspend fun markFirstLaunchShown(context: Context) {
        context.dataStore.edit { prefs ->
            prefs[FIRST_LAUNCH] = false
        }
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

    suspend fun getValidatedLastRoute(context: Context, validRoutes: Set<String>): String? {
        val lastRoute = getLastRoute(context)
        return if (lastRoute != null && lastRoute in validRoutes) {
            lastRoute
        } else {
            null
        }
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

    suspend fun isSpecialEffectsEnabled(context: Context): Boolean {
        return context.dataStore.data.first()[SPECIAL_EFFECTS_ENABLED] ?: false
    }

    fun specialEffectsEnabledFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SPECIAL_EFFECTS_ENABLED] ?: false
        }
    }

    suspend fun clearNavigationState(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(LAST_ROUTE)
        }
    }

    suspend fun resetToDefaults(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun validateAppState(context: Context): Boolean {
        return try {
            context.dataStore.data.first()
            true
        } catch (e: Exception) {
            Log.e("ScreenStateManager", "DataStore corrupted", e)
            false
        }
    }
}
