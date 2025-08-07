package com.example.pokeverse.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokeverse.utils.ScreenStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val _specialEffectsEnabled = MutableStateFlow(false)
    val specialEffectsEnabled: StateFlow<Boolean> = _specialEffectsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            ScreenStateManager.specialEffectsEnabledFlow(context).collect {
                _specialEffectsEnabled.value = it
            }
        }
    }

    fun toggleSpecialEffects(enabled: Boolean) {
        _specialEffectsEnabled.value = enabled
        viewModelScope.launch {
            ScreenStateManager.setSpecialEffectsEnabled(context, enabled)
        }
    }
}