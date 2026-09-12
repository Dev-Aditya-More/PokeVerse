package com.aditya1875.pokeverse.feature.game.survivor.domain.engine

import com.aditya1875.pokeverse.feature.game.survivor.domain.model.MatchupPokemon
import com.aditya1875.pokeverse.feature.game.survivor.domain.model.Modifier
import com.aditya1875.pokeverse.utils.pokemonTypeEffectiveness
import com.aditya1875.pokeverse.utils.typeEffectivenessMultiplier

/**
 * Pure resolution logic for a Survivor round: given a defender and the active
 * modifier stack, works out every attacking type's effectiveness, and which of
 * those types count as a correct answer.
 *
 * Deliberately free of Android/Compose imports and of any round-generation or
 * scoring concerns — those live in [SurvivorRoundGenerator] and
 * `EndlessRoundLoop` respectively, so this class stays trivially unit-testable.
 */
object SurvivorRuleEngine {

    /** Effectiveness of every attacking type against [defender], with [modifiers] applied in order. */
    fun resolve(defender: MatchupPokemon, modifiers: List<Modifier>): Map<String, Float> {
        val base = pokemonTypeEffectiveness.keys.associateWith { attackType ->
            typeEffectivenessMultiplier(attackType, defender.types)
        }
        return modifiers.fold(base) { effectiveness, modifier -> modifier.apply(effectiveness) }
    }

    /**
     * Every type sitting at the max multiplier counts as correct — ties are
     * intentional (a round can have more than one right answer once modifiers
     * stack) rather than picking one arbitrary "canonical" answer.
     */
    fun correctAnswers(effectiveness: Map<String, Float>): Set<String> {
        val best = effectiveness.values.maxOrNull() ?: return emptySet()
        if (best <= 1f) return emptySet() // no super-effective option this round
        return effectiveness.filterValues { it == best }.keys
    }
}
