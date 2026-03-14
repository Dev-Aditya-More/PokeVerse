package com.aditya1875.pokeverse.presentation.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val supportsShaders =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val _specialEffectsEnabled = MutableStateFlow(false)
    val specialEffectsEnabled: StateFlow<Boolean> = _specialEffectsEnabled.asStateFlow()

    private val BANNER_SEEN_KEY = booleanPreferencesKey("assets_banner_seen")

    // Expose as StateFlow
    val assetsBannerSeen: StateFlow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[BANNER_SEEN_KEY] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissAssetsBanner() {
        viewModelScope.launch {
            context.dataStore.edit { it[BANNER_SEEN_KEY] = true }
        }
    }

    private val _originalAssetsEnabled = MutableStateFlow(false)
    val originalAssetsEnabled: StateFlow<Boolean> = _originalAssetsEnabled.asStateFlow()

    fun toggleOriginalAssetsEnabled() {
        _originalAssetsEnabled.value = !originalAssetsEnabled.value
    }

    init {
        viewModelScope.launch {
            ScreenStateManager.specialEffectsEnabledFlow(context).collect { persisted ->
                _specialEffectsEnabled.value = persisted && supportsShaders
            }
        }
    }
    fun toggleSpecialEffects(enabled: Boolean) {
        val safeValue = enabled && supportsShaders
        _specialEffectsEnabled.value = safeValue

        viewModelScope.launch {
            ScreenStateManager.setSpecialEffectsEnabled(context, safeValue)
        }
    }
}