package com.aditya1875.pokeverse.feature.dexdaily.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyCodec
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyPuzzle
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyPuzzles
import com.aditya1875.pokeverse.feature.dexdaily.domain.DexDailyRecord
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Backed up with the player's progress (listed in backup_rules / data_extraction_rules).
private val Context.dexDailyStore by preferencesDataStore("dex_daily")

/** A puzzle plus the display data the UI needs (names, artwork, clues). */
data class DexDailyContent(
    val puzzle: DexDailyPuzzle,
    val answerName: String,
    val artworkUrl: String,
    val types: List<String>,
    val generation: Int,
    /** optionId -> display name, in the puzzle's option order. */
    val options: List<Pair<Int, String>>,
)

class DexDailyRepository(
    private val context: Context,
    private val pokemonRepo: PokemonDetailRepo,
) {
    private val recordsKey = stringPreferencesKey("records")
    private val contentCache = mutableMapOf<String, DexDailyContent>()
    private val loadLock = Mutex()

    val records: Flow<Map<String, DexDailyRecord>> =
        context.dexDailyStore.data.map { DexDailyCodec.decode(it[recordsKey]) }

    /** Atomic read-modify-write of one date's record; returns the stored result. */
    suspend fun update(date: String, transform: (DexDailyRecord) -> DexDailyRecord): DexDailyRecord {
        var result = DexDailyRecord()
        context.dexDailyStore.edit { prefs ->
            val all = DexDailyCodec.decode(prefs[recordsKey])
            result = transform(all[date] ?: DexDailyRecord())
            prefs[recordsKey] = DexDailyCodec.encode(all + (date to result))
        }
        return result
    }

    /**
     * Loads names/artwork for a date's puzzle. One request per option, all in parallel and
     * served from the HTTP cache after the first time; held in memory for the session.
     */
    suspend fun content(date: String): DexDailyContent = loadLock.withLock {
        contentCache[date]?.let { return it }
        val puzzle = DexDailyPuzzles.forDate(date)
        val loaded = coroutineScope {
            val calls = puzzle.optionIds.associateWith { id -> async { runCatching { pokemonRepo.getPokemonByName(id.toString()) }.getOrNull() } }
            calls.mapValues { it.value.await() }
        }
        val answer = loaded[puzzle.answerId] ?: error("Couldn't load today's Pokémon")
        DexDailyContent(
            puzzle = puzzle,
            answerName = displayName(answer.name),
            artworkUrl = answer.sprites.other?.officialArtwork?.frontDefault
                ?: DexDailyPuzzles.artworkUrl(puzzle.answerId),
            types = answer.types.map { it.type.name },
            generation = DexDailyPuzzles.generationOf(puzzle.answerId),
            // A decoy that failed to load still needs a label; its dex number is honest and rare.
            options = puzzle.optionIds.map { id -> id to (loaded[id]?.name?.let(::displayName) ?: "#$id") },
        ).also { contentCache[date] = it }
    }

    /** "giratina-altered" -> "Giratina", "tapu-koko" -> "Tapu Koko", "ho-oh" -> "Ho-Oh". */
    private fun displayName(apiName: String): String {
        SPECIAL_NAMES[apiName]?.let { return it }
        val base = FORM_SUFFIXES.firstOrNull { apiName.endsWith(it) }?.let { apiName.removeSuffix(it) } ?: apiName
        return base.split("-").joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
    }

    private companion object {
        // Default forms PokéAPI names after the form (every one that occurs in the puzzle pool).
        val FORM_SUFFIXES = listOf(
            "-altered", "-normal", "-land", "-incarnate", "-ordinary", "-aria", "-shield",
            "-midday", "-solo", "-single-strike", "-50",
        )
        val SPECIAL_NAMES = mapOf("ho-oh" to "Ho-Oh")
    }
}
