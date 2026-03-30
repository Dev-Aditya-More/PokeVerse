package com.aditya1875.pokeverse.feature.pokemon.home.domain.trivia

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.model.DailyTriviaState
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

private val Context.triviaDataStore by preferencesDataStore("daily_trivia")
private val SPECIAL_POKEMON_IDS = listOf(
    6, 9, 3, 65, 68, 94, 130, 131, 143, 144, 145, 146, 149, 150, 151,
    157, 160, 196, 197, 212, 230, 243, 244, 245, 248, 249, 250, 251,
    254, 257, 260, 282, 306, 373, 376, 380, 381, 382, 383, 384, 385, 386,
    392, 395, 398, 445, 448, 461, 462, 463, 464, 466, 467, 483, 484, 485,
    486, 487, 491, 492, 493,
    497, 500, 503, 534, 609, 637, 641, 642, 643, 644, 645, 646, 647, 648, 649,
    654, 658, 681, 700, 716, 717, 718, 719, 720, 721,
    724, 727, 730, 745, 746, 785, 786, 787, 788, 800, 801, 802,
    812, 818, 823, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898
)

class DailyTriviaManager(
    private val context: Context,
    private val pokemonRepo: PokemonDetailRepo
) {
    private val ds = context.triviaDataStore
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private object Keys {
        val DATE = stringPreferencesKey("trivia_date")
        val POKEMON_ID = intPreferencesKey("trivia_pokemon_id")
        val POKEMON_NAME = stringPreferencesKey("trivia_pokemon_name")
        val SPRITE_URL = stringPreferencesKey("trivia_sprite_url")
        val TYPES = stringPreferencesKey("trivia_types")
        val HEIGHT = intPreferencesKey("trivia_height")
        val WEIGHT = intPreferencesKey("trivia_weight")
        val STATS = stringPreferencesKey("trivia_stats")
        val GENERATION = intPreferencesKey("trivia_generation")
        val IS_ANSWERED = booleanPreferencesKey("trivia_answered")
        val WAS_CORRECT = booleanPreferencesKey("trivia_correct")
        val OPTIONS = stringPreferencesKey("trivia_options")
    }

    suspend fun getDailyTrivia(): Result<DailyTriviaState> {
        val today = dateFormat.format(Date())
        val prefs = ds.data.first()

        if (prefs[Keys.DATE] == today && !prefs[Keys.SPRITE_URL].isNullOrEmpty()) {
            return Result.success(prefsToState(prefs, today))
        }

        return fetchTodaysPokemon(today)
    }

    private suspend fun fetchTodaysPokemon(today: String): Result<DailyTriviaState> {
        return try {
            // Seed with today's date — same Pokémon + same options for everyone on the same day
            val seed = today.replace("-", "").toLong()
            val rng = Random(seed)

            // ── Today's Pokémon ───────────────────────────────────────────────
            val pokemonId = SPECIAL_POKEMON_IDS[rng.nextInt(SPECIAL_POKEMON_IDS.size)]
            val pokemon = pokemonRepo.getPokemonByName(pokemonId.toString())
            val spriteUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.front_default ?: ""
            val stats = pokemon.stats.associate { it.stat.name to it.base_stat }
            val types = pokemon.types.map { it.type.name }
            val generation = getGeneration(pokemonId)

            // ── 3 wrong options (deterministic with same seed) ────────────────
            // Shuffle the pool excluding today's ID, pick first 3.
            // This is one deterministic operation — no extra randomness.
            val wrongIds = SPECIAL_POKEMON_IDS
                .filter { it != pokemonId }
                .shuffled(rng)
                .take(3)

            // Fetch names only (no sprites needed for option buttons)
            val wrongNames = wrongIds.mapNotNull { id ->
                try {
                    pokemonRepo.getPokemonByName(id.toString()).name
                } catch (e: Exception) {
                    null
                }
            }.toMutableList()

            // Fallback if a fetch fails (offline edge case)
            val fallbacks = listOf("Snorlax", "Gengar", "Machamp")
            while (wrongNames.size < 3) {
                wrongNames.add(fallbacks[wrongNames.size % fallbacks.size])
            }

            // Shuffle all 4 with the same RNG — deterministic order per day
            val options = (wrongNames.take(3) + pokemon.name).shuffled(rng)

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
                options = options,
            )

            val statsString = stats.entries.joinToString(",") { "${it.key}:${it.value}" }

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
                p[Keys.OPTIONS] = options.joinToString(",")
            }

            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAnswered(correct: Boolean) {
        ds.edit { p ->
            p[Keys.IS_ANSWERED] = true
            p[Keys.WAS_CORRECT] = correct
        }
    }

    suspend fun isTodayAnswered(): Boolean {
        val today = dateFormat.format(Date())
        val prefs = ds.data.first()
        return prefs[Keys.DATE] == today && prefs[Keys.IS_ANSWERED] == true
    }

    private fun prefsToState(prefs: Preferences, today: String): DailyTriviaState {
        val stats = (prefs[Keys.STATS] ?: "").split(",").mapNotNull {
            val p = it.split(":")
            if (p.size == 2) p[0] to (p[1].toIntOrNull() ?: 0) else null
        }.toMap()

        val optionsRaw = prefs[Keys.OPTIONS] ?: ""
        val options = if (optionsRaw.isBlank()) emptyList()
        else optionsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }

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
            options = options,
        )
    }

    private fun getGeneration(id: Int) = when (id) {
        in 1..151 -> 1; in 152..251 -> 2; in 252..386 -> 3
        in 387..493 -> 4; in 494..649 -> 5; in 650..721 -> 6
        in 722..809 -> 7; in 810..905 -> 8; else -> 9
    }
}