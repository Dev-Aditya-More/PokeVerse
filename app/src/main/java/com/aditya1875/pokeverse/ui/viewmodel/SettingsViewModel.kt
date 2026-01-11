package com.aditya1875.pokeverse.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya1875.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val supportsShaders =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private val _specialEffectsEnabled = MutableStateFlow(false)
    val specialEffectsEnabled: StateFlow<Boolean> = _specialEffectsEnabled.asStateFlow()

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