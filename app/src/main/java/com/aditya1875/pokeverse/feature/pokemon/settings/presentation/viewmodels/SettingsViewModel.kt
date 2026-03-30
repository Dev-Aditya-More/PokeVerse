package com.aditya1875.pokeverse.feature.pokemon.settings.presentation.viewmodels

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

    private val supportsShaders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private val ORIGINAL_ASSETS_KEY = booleanPreferencesKey("original_assets_enabled")
    val ASSETS_BANNER_SEEN = booleanPreferencesKey("assets_banner_seen")
    val RATING_PROMPT_SEEN = booleanPreferencesKey("rating_prompt_seen")
    val PREMIUM_PROMPT_SHOWN = booleanPreferencesKey("premium_prompt_shown")
    val TOTAL_SESSION_MINUTES = longPreferencesKey("total_session_minutes")
    val UPDATE_DIALOG_SHOWN_VERSION = longPreferencesKey("update_dialog_shown_version")

    // ── Special effects (synced with ScreenStateManager) ─────────────────────
    private val _specialEffectsEnabled = MutableStateFlow(false)
    val specialEffectsEnabled: StateFlow<Boolean> = _specialEffectsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            ScreenStateManager.specialEffectsEnabledFlow(context).collect { persisted ->
                _specialEffectsEnabled.value = persisted && supportsShaders
            }
        }
    }

    // ── FIX: originalAssetsEnabled now reads from DataStore directly ──────────
    // Before: MutableStateFlow(false) in memory — wiped on every process death.
    // Now: backed by DataStore → survives process death, app kill, and low-memory kills.
    val originalAssetsEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { it[ORIGINAL_ASSETS_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

    // FIX: toggleOriginalAssetsEnabled now writes to DataStore
    // Before: only flipped the in-memory MutableStateFlow
    fun toggleOriginalAssetsEnabled() {
        viewModelScope.launch {
            val current = originalAssetsEnabled.value
            context.dataStore.edit { it[ORIGINAL_ASSETS_KEY] = !current }
        }
    }

    // ── Popup state flows from DataStore ──────────────────────────────────────
    val assetsBannerSeen: StateFlow<Boolean> = context.dataStore.data
        .map { it[ASSETS_BANNER_SEEN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

    val ratingPromptSeen: StateFlow<Boolean> = context.dataStore.data
        .map { it[RATING_PROMPT_SEEN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

    val premiumPromptShown: StateFlow<Boolean> = context.dataStore.data
        .map { it[PREMIUM_PROMPT_SHOWN] ?: false }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, false)

    val totalSessionMinutes: StateFlow<Long> = context.dataStore.data
        .map { it[TOTAL_SESSION_MINUTES] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, 0L)

    val updateDialogShownVersion: StateFlow<Long> = context.dataStore.data
        .map { it[UPDATE_DIALOG_SHOWN_VERSION] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Companion.Eagerly, 0L)

    // ── Popup actions ─────────────────────────────────────────────────────────
    fun dismissAssetsBanner() {
        viewModelScope.launch { context.dataStore.edit { it[ASSETS_BANNER_SEEN] = true } }
    }

    fun markRatingPromptSeen() {
        viewModelScope.launch { context.dataStore.edit { it[RATING_PROMPT_SEEN] = true } }
    }

    fun markPremiumPromptShown() {
        viewModelScope.launch { context.dataStore.edit { it[PREMIUM_PROMPT_SHOWN] = true } }
    }

    fun markUpdateDialogShown(versionCode: Long) {
        viewModelScope.launch {
            context.dataStore.edit {
                it[UPDATE_DIALOG_SHOWN_VERSION] = versionCode
            }
        }
    }

    // Called from MainActivity.onStop() to accumulate real usage time
    fun recordSessionMinutes(minutes: Long) {
        viewModelScope.launch {
            val current = totalSessionMinutes.value
            context.dataStore.edit { it[TOTAL_SESSION_MINUTES] = current + minutes }
        }
    }   // ← FIX: was missing closing brace here, causing toggleOriginalAssetsEnabled
    //   and toggleSpecialEffects to be nested INSIDE recordSessionMinutes as dead code

    fun toggleSpecialEffects(enabled: Boolean) {
        val safeValue = enabled && supportsShaders
        _specialEffectsEnabled.value = safeValue
        viewModelScope.launch {
            ScreenStateManager.setSpecialEffectsEnabled(context, safeValue)
        }
    }
}