package com.aditya1875.pokeverse.feature.game.pokeguess.domain.usecases

import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.GuessDifficulty
import com.aditya1875.pokeverse.feature.game.pokeguess.domain.model.PokeGuessQuestion
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GeneratePokeGuessQuestionsUseCase(
    private val repo: PokemonDetailRepo
) {

    // A 10-question game used to mean ~40 sequential network round trips
    // (1 correct + 3 wrong-option fetches per question, one at a time) before
    // the game even started. Instead we fetch one big batch of unique
    // Pokémon concurrently, then assign correct answers + wrong options from
    // it locally — same total fetch count, but concurrent instead of serial,
    // and wrong-option names are reused across questions instead of being
    // fetched again per question.
    suspend operator fun invoke(
        difficulty: GuessDifficulty
    ): List<PokeGuessQuestion> = coroutineScope {
        val maxPokemonId = when (difficulty) {
            GuessDifficulty.EASY -> 151
            GuessDifficulty.MEDIUM -> 493
            GuessDifficulty.HARD -> 1010
        }

        val needed = difficulty.questionsPerGame * difficulty.optionCount
        // Small overfetch buffer — a handful of random IDs 404 or lack a sprite and get dropped
        val pool = fetchPokemonBatch(maxPokemonId, needed + difficulty.questionsPerGame)
        if (pool.isEmpty()) return@coroutineScope emptyList()

        val correctPicks = pool.filter { it.spriteUrl() != null }.take(difficulty.questionsPerGame)
        val namePool = pool.map { it.name }
        var nameCursor = 0

        correctPicks.map { pokemon ->
            val wrongNames = mutableListOf<String>()
            while (wrongNames.size < difficulty.optionCount - 1 && nameCursor < namePool.size) {
                val candidate = namePool[nameCursor++]
                if (candidate != pokemon.name && candidate !in wrongNames) wrongNames.add(candidate)
            }
            val allOptions = (wrongNames + pokemon.name).shuffled()

            PokeGuessQuestion(
                pokemonId = pokemon.id,
                pokemonName = pokemon.name,
                spriteUrl = pokemon.spriteUrl()!!,
                options = allOptions,
                correctIndex = allOptions.indexOf(pokemon.name),
                generation = getGeneration(pokemon.id),
                types = pokemon.types.map { it.type.name }
            )
        }
    }

    private suspend fun fetchPokemonBatch(maxId: Int, count: Int): List<PokemonResponse> = coroutineScope {
        (1..maxId).shuffled().take(count).map { id ->
            async {
                try {
                    repo.getPokemonByName(id.toString())
                } catch (_: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun PokemonResponse.spriteUrl(): String? =
        sprites.other?.officialArtwork?.frontDefault ?: sprites.front_default

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
