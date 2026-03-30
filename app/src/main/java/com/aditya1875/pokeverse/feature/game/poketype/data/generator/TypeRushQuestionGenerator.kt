package com.aditya1875.pokeverse.feature.game.poketype.data.generator

import android.util.Log
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.PokemonTypes
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushDifficulty
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushQuestion
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

class TypeRushQuestionGenerator(
    private val pokemonRepo: PokemonDetailRepo
) {

    suspend fun generate(difficulty: TypeRushDifficulty): List<TypeRushQuestion> {
        val questions = mutableListOf<TypeRushQuestion>()
        val usedIds = mutableSetOf<Int>()
        val maxId = when (difficulty) {
            TypeRushDifficulty.EASY   -> 151
            TypeRushDifficulty.MEDIUM -> 493
            TypeRushDifficulty.HARD   -> 1010
        }

        repeat(difficulty.rounds) {
            var attempts = 0
            while (attempts < 50) {
                val id = (1..maxId).random()
                if (id in usedIds) { attempts++; continue }
                usedIds.add(id)
                try {
                    val pokemon = pokemonRepo.getPokemonByName(id.toString())
                    val sprite = if (difficulty.showName) {
                        pokemon.sprites.other?.officialArtwork?.frontDefault
                            ?: pokemon.sprites.front_default
                    } else {
                        // silhouette — still use same sprite, screen will darken it
                        pokemon.sprites.other?.officialArtwork?.frontDefault
                            ?: pokemon.sprites.front_default
                    } ?: run { attempts++; continue }

                    val correctTypes = pokemon.types.map { it.type.name }

                    val wrongTypes = PokemonTypes.ALL_TYPES
                        .filter { it !in correctTypes }
                        .shuffled()
                        .take(difficulty.optionCount - correctTypes.size)

                    val options = (correctTypes + wrongTypes).shuffled()

                    questions.add(
                        TypeRushQuestion(
                            pokemonId = id,
                            pokemonName = pokemon.name,
                            spriteUrl = sprite,
                            correctTypes = correctTypes,
                            options = options,
                        )
                    )
                    break
                } catch (e: Exception) {
                    Log.w("TypeRush", "Skip $id: ${e.message}")
                }
                attempts++
            }
        }
        return questions
    }
}