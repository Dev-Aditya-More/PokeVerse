package com.aditya1875.pokeverse.feature.game.pokeduel.domain.engine

import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelOutcome
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelPokemon
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelResult
import com.aditya1875.pokeverse.utils.typeEffectivenessMultiplier

class DuelGameEngine {

    // Compute total offensive multiplier of attacker's types against defender's types
    fun computeAdvantage(attacker: DuelPokemon, defender: DuelPokemon): Float {
        var best = 1f
        attacker.types.forEach { atkType ->
            val multiplier = typeEffectivenessMultiplier(atkType, defender.types)
            if (multiplier > best) best = multiplier
        }
        return best
    }

    fun evaluate(left: DuelPokemon, right: DuelPokemon): DuelResult {
        val leftAdv = computeAdvantage(left, right)
        val rightAdv = computeAdvantage(right, left)

        val outcome = when {
            leftAdv > rightAdv -> DuelOutcome.LEFT_WINS
            rightAdv > leftAdv -> DuelOutcome.RIGHT_WINS
            else -> DuelOutcome.DRAW
        }

        val explanation = buildExplanation(left, right, leftAdv, rightAdv, outcome)

        return DuelResult(
            outcome = outcome,
            leftAdvantage = leftAdv,
            rightAdvantage = rightAdv,
            explanation = explanation
        )
    }

    private fun buildExplanation(
        left: DuelPokemon,
        right: DuelPokemon,
        leftAdv: Float,
        rightAdv: Float,
        outcome: DuelOutcome
    ): String {
        val leftTypes = left.types.joinToString("/") { it.replaceFirstChar { c -> c.uppercase() } }
        val rightTypes =
            right.types.joinToString("/") { it.replaceFirstChar { c -> c.uppercase() } }
        return when (outcome) {
            DuelOutcome.LEFT_WINS ->
                "$leftTypes is super effective against $rightTypes! (${leftAdv}x vs ${rightAdv}x)"

            DuelOutcome.RIGHT_WINS ->
                "$rightTypes overpowers $leftTypes! (${rightAdv}x vs ${leftAdv}x)"

            DuelOutcome.DRAW ->
                "Equal matchup between $leftTypes and $rightTypes!"
        }
    }

    fun calculateScore(baseScore: Int, streak: Int, isCorrect: Boolean): Int {
        if (!isCorrect) return 0
        val streakBonus = when {
            streak >= 5 -> 50
            streak >= 3 -> 25
            streak >= 2 -> 10
            else -> 0
        }
        return baseScore + streakBonus
    }
}