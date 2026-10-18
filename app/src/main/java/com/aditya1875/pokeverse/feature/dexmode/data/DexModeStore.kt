package com.aditya1875.pokeverse.feature.dexmode.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only Dex Mode progress: which entries the user has opened ("seen"), device
 * preferences, and whether the mode has been discovered (drives the Home nudges).
 */
class DexModeStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _seen = MutableStateFlow(
        prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    )
    val seen: StateFlow<Set<Int>> = _seen.asStateFlow()

    private val _backlit = MutableStateFlow(prefs.getBoolean(KEY_BACKLIT, false))
    val backlit: StateFlow<Boolean> = _backlit.asStateFlow()

    private val _muted = MutableStateFlow(prefs.getBoolean(KEY_MUTED, false))
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _discovered = MutableStateFlow(prefs.getBoolean(KEY_DISCOVERED, false))
    val discovered: StateFlow<Boolean> = _discovered.asStateFlow()

    val isAnnounced: Boolean get() = prefs.getBoolean(KEY_ANNOUNCED, false)

    fun markSeen(id: Int) {
        if (id in _seen.value) return
        val updated = _seen.value + id
        _seen.value = updated
        prefs.edit().putStringSet(KEY_SEEN, updated.map(Int::toString).toSet()).apply()
    }

    fun toggleBacklit() {
        val v = !_backlit.value
        _backlit.value = v
        prefs.edit().putBoolean(KEY_BACKLIT, v).apply()
    }

    fun toggleMuted() {
        val v = !_muted.value
        _muted.value = v
        prefs.edit().putBoolean(KEY_MUTED, v).apply()
    }

    fun markDiscovered() {
        if (_discovered.value) return
        _discovered.value = true
        prefs.edit().putBoolean(KEY_DISCOVERED, true).putBoolean(KEY_ANNOUNCED, true).apply()
    }

    fun markAnnounced() {
        prefs.edit().putBoolean(KEY_ANNOUNCED, true).apply()
    }

    private companion object {
        const val PREFS = "dex_mode"
        const val KEY_SEEN = "seen_ids"
        const val KEY_BACKLIT = "backlit"
        const val KEY_MUTED = "muted"
        const val KEY_DISCOVERED = "discovered"
        const val KEY_ANNOUNCED = "announced"
    }
}
