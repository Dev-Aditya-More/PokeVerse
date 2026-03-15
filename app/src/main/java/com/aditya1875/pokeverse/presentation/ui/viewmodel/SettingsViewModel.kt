package com.aditya1875.pokeverse.presentation.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.utils.ScreenStateManager
import com.aditya1875.pokeverse.utils.ScreenStateManager.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context
) : ViewModel() {

    private val supportsShaders =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val _specialEffectsEnabled = MutableStateFlow(false)
    val specialEffectsEnabled: StateFlow<Boolean> = _specialEffectsEnabled.asStateFlow()

    val ASSETS_BANNER_SEEN = booleanPreferencesKey("assets_banner_seen")
    val RATING_PROMPT_SEEN = booleanPreferencesKey("rating_prompt_seen")
    val SESSION_START_MS = longPreferencesKey("session_start_ms")
    val PREMIUM_PROMPT_SHOWN = booleanPreferencesKey("premium_prompt_shown")
    val TOTAL_SESSION_MINUTES = longPreferencesKey("total_session_minutes")

    private val _originalAssetsEnabled = MutableStateFlow(false)
    val originalAssetsEnabled: StateFlow<Boolean> = _originalAssetsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            ScreenStateManager.specialEffectsEnabledFlow(context).collect { persisted ->
                _specialEffectsEnabled.value = persisted && supportsShaders
            }
        }
    }

    val assetsBannerSeen: StateFlow<Boolean> = context.dataStore.data
        .map { it[ASSETS_BANNER_SEEN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val ratingPromptSeen: StateFlow<Boolean> = context.dataStore.data
        .map { it[RATING_PROMPT_SEEN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val premiumPromptShown: StateFlow<Boolean> = context.dataStore.data
        .map { it[PREMIUM_PROMPT_SHOWN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val totalSessionMinutes: StateFlow<Long> = context.dataStore.data
        .map { it[TOTAL_SESSION_MINUTES] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    fun dismissAssetsBanner() {
        viewModelScope.launch {
            context.dataStore.edit { it[ASSETS_BANNER_SEEN] = true }
        }
    }

    fun markRatingPromptSeen() {
        viewModelScope.launch {
            context.dataStore.edit { it[RATING_PROMPT_SEEN] = true }
        }
    }

    fun markPremiumPromptShown() {
        viewModelScope.launch {
            context.dataStore.edit { it[PREMIUM_PROMPT_SHOWN] = true }
        }
    }

    fun toggleOriginalAssetsEnabled() {
        _originalAssetsEnabled.value = !originalAssetsEnabled.value
    }
    fun toggleSpecialEffects(enabled: Boolean) {
        val safeValue = enabled && supportsShaders
        _specialEffectsEnabled.value = safeValue

        viewModelScope.launch {
            ScreenStateManager.setSpecialEffectsEnabled(context, safeValue)
        }
    }

    fun recordSessionMinutes(minutes: Long) {
        viewModelScope.launch {
            val current = totalSessionMinutes.value
            context.dataStore.edit { it[TOTAL_SESSION_MINUTES] = current + minutes }
        }

        fun toggleOriginalAssetsEnabled() {
            _originalAssetsEnabled.value = !originalAssetsEnabled.value
        }

        fun toggleSpecialEffects(enabled: Boolean) {
            val safeValue = enabled && supportsShaders
            _specialEffectsEnabled.value = safeValue

            viewModelScope.launch {
                ScreenStateManager.setSpecialEffectsEnabled(context, safeValue)
            }
        }
    }
}