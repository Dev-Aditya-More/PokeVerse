package com.example.pokeverse.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object ScreenStateManager {
    private val Context.dataStore by preferencesDataStore("screen_state")
    private val CURRENT_ROUTE_KEY = stringPreferencesKey("current_route")

    suspend fun saveCurrentRoute(context: Context, route: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_ROUTE_KEY] = route
        }
    }

    suspend fun getLastRoute(context: Context): String? {
        val preferences = context.dataStore.data.first()
        return preferences[CURRENT_ROUTE_KEY]
    }
}