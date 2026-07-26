package com.aditya1875.pokeverse.feature.game.poketype.data.generator

import android.util.Log
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.PokemonTypes
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushQuestion
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TypeRushQuestionGenerator(
    private val pokemonRepo: PokemonDetailRepo
) {

    // Wrong options here are just type names (no network needed), so the only
    // network cost per round is fetching that round's Pokémon. Fetching the
    // whole batch concurrently instead of one-at-a-time in a retry loop is
    // what actually made "loading" feel slow.
    suspend fun generate(difficulty: TypeRushDifficulty): List<TypeRushQuestion> = coroutineScope {
        val maxId = when (difficulty) {
            TypeRushDifficulty.EASY   -> 151
            TypeRushDifficulty.MEDIUM -> 493
            TypeRushDifficulty.HARD   -> 1010
        }

        // Small overfetch buffer — a handful of random IDs 404 or lack a sprite and get dropped
        val questions = (1..maxId).shuffled().take(difficulty.rounds + 5).map { id ->
            async {
                try {
                    val pokemon = pokemonRepo.getPokemonByName(id.toString())
                    val sprite = pokemon.sprites.other?.officialArtwork?.frontDefault
                        ?: pokemon.sprites.front_default ?: return@async null

                    val correctTypes = pokemon.types.map { it.type.name }
                    val wrongTypes = PokemonTypes.ALL_TYPES
                        .filter { it !in correctTypes }
                        .shuffled()
                        .take(difficulty.optionCount - correctTypes.size)

                    TypeRushQuestion(
                        pokemonId = pokemon.id,
                        pokemonName = pokemon.name,
                        spriteUrl = sprite,
                        correctTypes = correctTypes,
                        options = (correctTypes + wrongTypes).shuffled(),
                    )
                } catch (e: Exception) {
                    Log.w("TypeRush", "Skip $id: ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull()

        questions.take(difficulty.rounds)
    }
}
