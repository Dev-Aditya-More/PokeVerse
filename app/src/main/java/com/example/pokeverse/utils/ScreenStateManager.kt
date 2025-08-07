package com.example.pokeverse.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object ScreenStateManager {
    val Context.dataStore by preferencesDataStore(name = "settings")

    val LAST_ROUTE = stringPreferencesKey("last_route")
    val INTRO_SEEN = booleanPreferencesKey("intro_seen")

    val SPECIAL_EFFECTS_ENABLED = booleanPreferencesKey("special_effects_enabled")

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

    suspend fun isSpecialEffectsEnabled(context: Context): Boolean {
        return context.dataStore.data.first()[SPECIAL_EFFECTS_ENABLED] ?: false
    }

    // Read as Flow (optional)
    fun specialEffectsEnabledFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[SPECIAL_EFFECTS_ENABLED] ?: false
        }
    }
}
