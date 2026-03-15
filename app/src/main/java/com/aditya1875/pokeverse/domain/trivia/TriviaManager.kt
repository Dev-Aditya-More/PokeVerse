package com.aditya1875.pokeverse.domain.trivia

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.domain.repository.PokemonRepo
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

private val Context.triviaDataStore by preferencesDataStore("daily_trivia")

data class DailyTriviaState(
    val pokemonId: Int,
    val pokemonName: String,
    val spriteUrl: String,
    val types: List<String>,
    val height: Int,        // in decimetres
    val weight: Int,        // in hectograms
    val baseStats: Map<String, Int>,   // hp, attack, defense, speed, etc.
    val generation: Int,
    val date: String,       // "yyyy-MM-dd"
    val isAnswered: Boolean = false,
    val wasCorrect: Boolean = false,
)

// These are "special" Pokémon — legendaries, mythicals, pseudo-legendaries
// Hand-picked for the daily trivia to make it feel premium
private val SPECIAL_POKEMON_IDS = listOf(
    // Gen 1 legendaries + starters
    6, 9, 3, 65, 68, 94, 130, 131, 143, 144, 145, 146, 149, 150, 151,
    // Gen 2
    157, 160, 196, 197, 212, 230, 243, 244, 245, 248, 249, 250, 251,
    // Gen 3
    254, 257, 260, 282, 306, 373, 376, 380, 381, 382, 383, 384, 385, 386,
    // Gen 4
    392, 395, 398, 445, 448, 461, 462, 463, 464, 466, 467, 483, 484, 485,
    486, 487, 491, 492, 493,
    // Gen 5
    497, 500, 503, 534, 609, 637, 641, 642, 643, 644, 645, 646, 647, 648, 649,
    // Gen 6
    654, 658, 681, 700, 716, 717, 718, 719, 720, 721,
    // Gen 7 + 8 highlights
    724, 727, 730, 745, 746, 785, 786, 787, 788, 800, 801, 802,
    812, 818, 823, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898
)

class DailyTriviaManager(
    private val context: Context,
    private val pokemonRepo: PokemonRepo
) {
    private val ds = context.triviaDataStore
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private object Keys {
        val DATE = stringPreferencesKey("trivia_date")
        val POKEMON_ID = intPreferencesKey("trivia_pokemon_id")
        val POKEMON_NAME = stringPreferencesKey("trivia_pokemon_name")
        val SPRITE_URL = stringPreferencesKey("trivia_sprite_url")
        val TYPES = stringPreferencesKey("trivia_types")       // comma-separated
        val HEIGHT = intPreferencesKey("trivia_height")
        val WEIGHT = intPreferencesKey("trivia_weight")
        val STATS = stringPreferencesKey("trivia_stats")       // "hp:45,attack:49,..."
        val GENERATION = intPreferencesKey("trivia_generation")
        val IS_ANSWERED = booleanPreferencesKey("trivia_answered")
        val WAS_CORRECT = booleanPreferencesKey("trivia_correct")
    }

    // ── Main entry point: get today's trivia (cache or fetch) ─────────────────
    suspend fun getDailyTrivia(): Result<DailyTriviaState> {
        val today = dateFormat.format(Date())
        val prefs = ds.data.first()

        // Cache hit: same day and we have a sprite
        if (prefs[Keys.DATE] == today && !prefs[Keys.SPRITE_URL].isNullOrEmpty()) {
            return Result.success(prefsToState(prefs, today))
        }

        // Cache miss: fetch new Pokémon for today
        return fetchTodaysPokemon(today)
    }

    // ── Deterministic daily selection: same Pokémon for everyone on same day ──
    private suspend fun fetchTodaysPokemon(today: String): Result<DailyTriviaState> {
        return try {
            // Use date as seed so everyone gets the same Pokémon
            val seed = today.replace("-", "").toLong()
            val rng = Random(seed)
            val pokemonId = SPECIAL_POKEMON_IDS[rng.nextInt(SPECIAL_POKEMON_IDS.size)]

            val pokemon = pokemonRepo.getPokemonByName(pokemonId.toString())
            val spriteUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.front_default
                ?: ""

            val stats = pokemon.stats.associate { it.stat.name to it.base_stat }
            val statsString = stats.entries.joinToString(",") { "${it.key}:${it.value}" }
            val types = pokemon.types.map { it.type.name }
            val generation = getGeneration(pokemonId)

            val state = DailyTriviaState(
                pokemonId = pokemonId,
                pokemonName = pokemon.name,
                spriteUrl = spriteUrl,
                types = types,
                height = pokemon.height,
                weight = pokemon.weight,
                baseStats = stats,
                generation = generation,
                date = today,
                isAnswered = false,
                wasCorrect = false,
            )

            // Cache it
            ds.edit { p ->
                p[Keys.DATE] = today
                p[Keys.POKEMON_ID] = pokemonId
                p[Keys.POKEMON_NAME] = pokemon.name
                p[Keys.SPRITE_URL] = spriteUrl
                p[Keys.TYPES] = types.joinToString(",")
                p[Keys.HEIGHT] = pokemon.height
                p[Keys.WEIGHT] = pokemon.weight
                p[Keys.STATS] = statsString
                p[Keys.GENERATION] = generation
                p[Keys.IS_ANSWERED] = false
                p[Keys.WAS_CORRECT] = false
            }

            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark today's trivia as answered ───────────────────────────────────────
    suspend fun markAnswered(correct: Boolean) {
        ds.edit { p ->
            p[Keys.IS_ANSWERED] = true
            p[Keys.WAS_CORRECT] = correct
        }
    }

    // ── Check if today is already answered ────────────────────────────────────
    suspend fun isTodayAnswered(): Boolean {
        val today = dateFormat.format(Date())
        val prefs = ds.data.first()
        return prefs[Keys.DATE] == today && prefs[Keys.IS_ANSWERED] == true
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun prefsToState(
        prefs: Preferences,
        today: String
    ): DailyTriviaState {
        val statsString = prefs[Keys.STATS] ?: ""
        val stats: Map<String, Int> =
            statsString
                .split(",")
                .mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0)
                    else null
                }
                .toMap()

        return DailyTriviaState(
            pokemonId = prefs[Keys.POKEMON_ID] ?: 0,
            pokemonName = prefs[Keys.POKEMON_NAME] ?: "",
            spriteUrl = prefs[Keys.SPRITE_URL] ?: "",
            types = prefs[Keys.TYPES]?.split(",") ?: emptyList(),
            height = prefs[Keys.HEIGHT] ?: 0,
            weight = prefs[Keys.WEIGHT] ?: 0,
            baseStats = stats,
            generation = prefs[Keys.GENERATION] ?: 1,
            date = today,
            isAnswered = prefs[Keys.IS_ANSWERED] ?: false,
            wasCorrect = prefs[Keys.WAS_CORRECT] ?: false,
        )
    }

    private fun getGeneration(id: Int) = when (id) {
        in 1..151 -> 1; in 152..251 -> 2; in 252..386 -> 3
        in 387..493 -> 4; in 494..649 -> 5; in 650..721 -> 6
        in 722..809 -> 7; in 810..905 -> 8; else -> 9
    }
}