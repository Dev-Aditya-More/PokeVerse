package com.aditya1875.pokeverse.feature.game.survivor.domain.engine

import android.util.Log
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.DifficultyTier
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.MatchupPokemon
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.Modifier
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.SurvivorRound
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.WeatherKind
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.utils.pokemonTypeEffectiveness

/**
 * Fetches a random living-dex Pokémon and builds a playable [SurvivorRound]
 * around it, escalating modifiers and pace via [DifficultyTier.forStreak].
 *
 * [fetchDefender] (the slow network part) and [buildRound] (instant, pure) are
 * exposed separately so a caller can prefetch the next defender while the
 * current round is still being played, then build the actual round once the
 * next round's difficulty tier is known — see `SurvivorViewModel`'s prefetch.
 *
 * No not-recently-seen weighting yet (pure `.random()`) — acceptable for v1,
 * flagged as a follow-up once the loop itself is validated.
 */
class SurvivorRoundGenerator(
    private val pokemonRepo: PokemonDetailRepo
) {

    /** The slow part — fetches one random living-dex Pokémon's data over the network. */
    suspend fun fetchDefender(maxId: Int = 1010, attempts: Int = 5): MatchupPokemon? {
        repeat(attempts) {
            val id = (1..maxId).random()
            try {
                val pokemon = pokemonRepo.getPokemonByName(id.toString())
                val sprite = pokemon.sprites.other?.officialArtwork?.frontDefault
                    ?: pokemon.sprites.front_default ?: return@repeat

                return MatchupPokemon(
                    id = pokemon.id,
                    name = pokemon.name,
                    spriteUrl = sprite,
                    types = pokemon.types.map { it.type.name }
                )
            } catch (e: Exception) {
                Log.w("SurvivorRoundGenerator", "Skip $id: ${e.message}")
            }
        }
        return null
    }

    /** The fast part — pure, no I/O. Returns null if [defender] has no super-effective answer under [tier]. */
    fun buildRound(defender: MatchupPokemon, tier: DifficultyTier): SurvivorRound? {
        val modifiers = rollModifiers(tier)
        val effectiveness = SurvivorRuleEngine.resolve(defender, modifiers)
        val correctTypes = SurvivorRuleEngine.correctAnswers(effectiveness)

        // A round with no super-effective option isn't playable — the caller
        // should fetch a different defender rather than surface a no-win round.
        if (correctTypes.isEmpty()) return null

        val wrongTypes = pokemonTypeEffectiveness.keys
            .filter { it !in correctTypes }
            .shuffled()
            .take((tier.optionCount - correctTypes.size).coerceAtLeast(0))

        val options = (correctTypes + wrongTypes).shuffled()

        return SurvivorRound(
            defender = defender,
            activeModifiers = modifiers,
            options = options,
            correctTypes = correctTypes,
            timeBudgetMs = tier.timeBudgetMs
        )
    }

    /** Convenience wrapper for when there's no prefetched defender to build from. */
    suspend fun generate(streak: Int, maxId: Int = 1010, attempts: Int = 5): SurvivorRound? {
        val tier = DifficultyTier.forStreak(streak)
        repeat(attempts) {
            val defender = fetchDefender(maxId, attempts = 1) ?: return@repeat
            buildRound(defender, tier)?.let { return it }
        }
        return null
    }

    private fun rollModifiers(tier: DifficultyTier): List<Modifier> {
        if (tier.weatherChance <= 0f || Math.random() > tier.weatherChance) return emptyList()
        return listOf(Modifier.Weather(WeatherKind.entries.random()))
    }
}
