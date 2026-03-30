package com.aditya1875.pokeverse.feature.game.pokeguess.domain.usecases

import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.PokeGuessQuestion
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo

class GeneratePokeGuessQuestionsUseCase(
    private val repo: PokemonDetailRepo
) {

    suspend operator fun invoke(
        difficulty: GuessDifficulty
    ): List<PokeGuessQuestion> {

        val questions = mutableListOf<PokeGuessQuestion>()
        val usedIds = mutableSetOf<Int>()

        val maxPokemonId = when (difficulty) {
            GuessDifficulty.EASY -> 151
            GuessDifficulty.MEDIUM -> 493
            GuessDifficulty.HARD -> 1010
        }

        repeat(difficulty.questionsPerGame) {
            var attempts = 0

            while (attempts < 50) {
                val randomId = (1..maxPokemonId).random()

                if (randomId !in usedIds) {
                    usedIds.add(randomId)

                    try {
                        val pokemon = repo.getPokemonByName(randomId.toString())

                        val spriteUrl =
                            pokemon.sprites.other?.officialArtwork?.frontDefault
                                ?: pokemon.sprites.front_default

                        if (spriteUrl != null) {

                            val wrongOptions = generateWrongOptions(
                                correctName = pokemon.name,
                                count = difficulty.optionCount - 1,
                                maxId = maxPokemonId,
                                usedIds = usedIds
                            )

                            val allOptions = (wrongOptions + pokemon.name).shuffled()
                            val correctIndex = allOptions.indexOf(pokemon.name)

                            questions.add(
                                PokeGuessQuestion(
                                    pokemonId = randomId,
                                    pokemonName = pokemon.name,
                                    spriteUrl = spriteUrl,
                                    options = allOptions,
                                    correctIndex = correctIndex,
                                    generation = getGeneration(randomId),
                                    types = pokemon.types.map { it.type.name }
                                )
                            )

                            break
                        }

                    } catch (_: Exception) {
                        // skip silently
                    }
                }
                attempts++
            }
        }

        return questions
    }

    // -------------------------
    // Helpers (PRIVATE)
    // -------------------------

    private suspend fun generateWrongOptions(
        correctName: String,
        count: Int,
        maxId: Int,
        usedIds: Set<Int>
    ): List<String> {

        val wrongOptions = mutableListOf<String>()
        var attempts = 0

        while (wrongOptions.size < count && attempts < 100) {
            val randomId = (1..maxId).random()

            if (randomId !in usedIds) {
                try {
                    val pokemon = repo.getPokemonByName(randomId.toString())

                    if (pokemon.name != correctName && pokemon.name !in wrongOptions) {
                        wrongOptions.add(pokemon.name)
                    }

                } catch (_: Exception) {
                }
            }

            attempts++
        }

        return wrongOptions
    }

    private fun getGeneration(id: Int): Int {
        return when (id) {
            in 1..151 -> 1
            in 152..251 -> 2
            in 252..386 -> 3
            in 387..493 -> 4
            in 494..649 -> 5
            in 650..721 -> 6
            in 722..809 -> 7
            in 810..905 -> 8
            else -> 9
        }
    }
}