package com.aditya1875.pokeverse.feature.pokemon.dex

import androidx.datastore.preferences.core.booleanPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dexStore: DataStore<Preferences> by preferencesDataStore("pokedex_progress")

/**
 * What this trainer has encountered. Every caught Pokémon is also seen, so [seen] is a superset of
 * [caught]. Ids are national-dex numbers (1..[TOTAL]); alternate forms are not tracked.
 */
data class DexProgress(
    val seen: Set<Int> = emptySet(),
    val caught: Set<Int> = emptySet(),
    /** False only for the placeholder emitted before the stored progress has been read. */
    val loaded: Boolean = true,
) {
    val seenCount: Int get() = seen.size
    val caughtCount: Int get() = caught.size

    companion object {
        const val TOTAL = 1025

        /** Wild Catch spawns #1..this, and it is the only way to catch, so "caught" is out of this. */
        const val CATCHABLE = 809

        /** A full team; all six caught earns the "Trainer's party" mark. */
        const val PARTY_SIZE = 6

        /** Pikachu: the starter for anyone who skips or predates the professor intro. */
        const val DEFAULT_STARTER_ID = 25
        val EMPTY = DexProgress(loaded = false)
    }
}

/**
 * Device-local Pokédex progress. Seen = opened its detail page, met in Wild Catch, favourited,
 * put on a team, or chosen as the starter. Caught = caught in Wild Catch, plus the starter you begin with.
 */
class PokedexRepository(context: Context) {

    private val ds = context.applicationContext.dexStore
    private val seenKey = stringSetPreferencesKey("seen")
    // "caught_wild": earlier builds also counted teams and starters under "caught"; a new key drops that.
    private val caughtKey = stringSetPreferencesKey("caught_wild")
    private val starterChosenKey = booleanPreferencesKey("starter_chosen")

    val progress: Flow<DexProgress> = ds.data.map { p ->
        DexProgress(
            seen = p[seenKey].toIds(),
            caught = p[caughtKey].toIds(),
        )
    }

    /** Returns true only the first time this Pokémon is seen, so callers can celebrate the new entry. */
    suspend fun markSeen(id: Int): Boolean {
        if (id !in 1..DexProgress.TOTAL) return false
        var isNew = false
        ds.edit { p ->
            val seen = p[seenKey].orEmpty()
            if (id.toString() !in seen) {
                p[seenKey] = seen + id.toString()
                isNew = true
            }
        }
        return isNew
    }

    /** The starter is the one Pokémon you start out with, so it is caught from day one. */
    suspend fun chooseStarter(id: Int) {
        if (id !in 1..DexProgress.TOTAL) return
        ds.edit { p ->
            p[starterChosenKey] = true
            p[caughtKey] = p[caughtKey].orEmpty() + id.toString()
            p[seenKey] = p[seenKey].orEmpty() + id.toString()
        }
    }

    /** Anyone who skipped the intro, or installed before it existed, starts with Pikachu. Runs once. */
    suspend fun ensureDefaultStarter() {
        val id = DexProgress.DEFAULT_STARTER_ID.toString()
        ds.edit { p ->
            if (p[starterChosenKey] != true) {
                p[starterChosenKey] = true
                p[caughtKey] = p[caughtKey].orEmpty() + id
                p[seenKey] = p[seenKey].orEmpty() + id
            }
        }
    }

    suspend fun markCaught(id: Int) {
        if (id !in 1..DexProgress.TOTAL) return
        ds.edit { p ->
            p[caughtKey] = p[caughtKey].orEmpty() + id.toString()
            p[seenKey] = p[seenKey].orEmpty() + id.toString()
        }
    }

    private fun Set<String>?.toIds(): Set<Int> = orEmpty().mapNotNullTo(HashSet()) { it.toIntOrNull() }
}

class PokedexViewModel(private val repository: PokedexRepository) : ViewModel() {
    val progress: StateFlow<DexProgress> = repository.progress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DexProgress.EMPTY)

    fun markSeen(id: Int) {
        viewModelScope.launch { repository.markSeen(id) }
    }

    fun markCaught(id: Int) {
        viewModelScope.launch { repository.markCaught(id) }
    }
}

/** National-dex number from a PokeAPI resource url (".../pokemon/25/"), or 0 when it can't be read. */
fun pokemonIdFromUrl(url: String): Int =
    url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: 0

/** Home filter: everything, only what has been seen, or only what has been caught. */
enum class DexFilter { ALL, SEEN, CAUGHT }

/** Dex number from a PokeAPI sprite url (".../pokemon/25.png"), or null for forms and empty urls. */
fun pokemonIdFromSpriteUrl(url: String): Int? =
    url.substringAfterLast('/').substringBefore('.').toIntOrNull()?.takeIf { it in 1..DexProgress.TOTAL }

/** True when [ids] is a full party and every member has been caught. */
fun isTrainersParty(ids: List<Int>, caught: Set<Int>): Boolean =
    ids.size == DexProgress.PARTY_SIZE && ids.all { it in caught }
