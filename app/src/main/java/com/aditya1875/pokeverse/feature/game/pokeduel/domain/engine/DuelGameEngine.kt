package com.aditya1875.pokeverse.feature.game.pokeduel.domain.engine

import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelOutcome
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelPokemon
import com.aditya1875.pokeverse.feature.game.pokeduel.domain.model.DuelResult

class DuelGameEngine {

    // Full gen 1-9 type chart: attacker -> defender -> multiplier
    private val typeChart: Map<String, Map<String, Float>> = mapOf(
        "normal" to mapOf("rock" to 0.5f, "ghost" to 0f, "steel" to 0.5f),
        "fire" to mapOf(
            "fire" to 0.5f, "water" to 0.5f, "grass" to 2f, "ice" to 2f,
            "bug" to 2f, "rock" to 0.5f, "dragon" to 0.5f, "steel" to 2f
        ),
        "water" to mapOf(
            "fire" to 2f, "water" to 0.5f, "grass" to 0.5f, "ground" to 2f,
            "rock" to 2f, "dragon" to 0.5f
        ),
        "grass" to mapOf(
            "fire" to 0.5f, "water" to 2f, "grass" to 0.5f, "poison" to 0.5f,
            "ground" to 2f, "flying" to 0.5f, "bug" to 0.5f, "rock" to 2f,
            "dragon" to 0.5f, "steel" to 0.5f
        ),
        "electric" to mapOf(
            "water" to 2f, "electric" to 0.5f, "grass" to 0.5f, "ground" to 0f,
            "flying" to 2f, "dragon" to 0.5f
        ),
        "ice" to mapOf(
            "fire" to 0.5f, "water" to 0.5f, "grass" to 2f, "ice" to 0.5f,
            "ground" to 2f, "flying" to 2f, "dragon" to 2f, "steel" to 0.5f
        ),
        "fighting" to mapOf(
            "normal" to 2f, "ice" to 2f, "poison" to 0.5f, "flying" to 0.5f,
            "psychic" to 0.5f, "bug" to 0.5f, "rock" to 2f, "ghost" to 0f,
            "dark" to 2f, "steel" to 2f, "fairy" to 0.5f
        ),
        "poison" to mapOf(
            "grass" to 2f, "poison" to 0.5f, "ground" to 0.5f, "rock" to 0.5f,
            "ghost" to 0.5f, "steel" to 0f, "fairy" to 2f
        ),
        "ground" to mapOf(
            "fire" to 2f, "electric" to 2f, "grass" to 0.5f, "poison" to 2f,
            "flying" to 0f, "bug" to 0.5f, "rock" to 2f, "steel" to 2f
        ),
        "flying" to mapOf(
            "electric" to 0.5f, "grass" to 2f, "fighting" to 2f, "bug" to 2f,
            "rock" to 0.5f, "steel" to 0.5f
        ),
        "psychic" to mapOf(
            "fighting" to 2f, "poison" to 2f, "psychic" to 0.5f,
            "dark" to 0f, "steel" to 0.5f
        ),
        "bug" to mapOf(
            "fire" to 0.5f, "grass" to 2f, "fighting" to 0.5f, "poison" to 0.5f,
            "flying" to 0.5f, "psychic" to 2f, "ghost" to 0.5f, "dark" to 2f,
            "steel" to 0.5f, "fairy" to 0.5f
        ),
        "rock" to mapOf(
            "fire" to 2f, "ice" to 2f, "fighting" to 0.5f, "ground" to 0.5f,
            "flying" to 2f, "bug" to 2f, "steel" to 0.5f
        ),
        "ghost" to mapOf("normal" to 0f, "psychic" to 2f, "ghost" to 2f, "dark" to 0.5f),
        "dragon" to mapOf("dragon" to 2f, "steel" to 0.5f, "fairy" to 0f),
        "dark" to mapOf(
            "fighting" to 0.5f, "psychic" to 2f, "ghost" to 2f,
            "dark" to 0.5f, "fairy" to 0.5f
        ),
        "steel" to mapOf(
            "fire" to 0.5f, "water" to 0.5f, "electric" to 0.5f, "ice" to 2f,
            "rock" to 2f, "steel" to 0.5f, "fairy" to 2f
        ),
        "fairy" to mapOf(
            "fire" to 0.5f, "fighting" to 2f, "poison" to 0.5f, "dragon" to 2f,
            "dark" to 2f, "steel" to 0.5f
        )
    )

    // Compute total offensive multiplier of attacker's types against defender's types
    fun computeAdvantage(attacker: DuelPokemon, defender: DuelPokemon): Float {
        var best = 1f
        attacker.types.forEach { atkType ->
            val atkRow = typeChart[atkType.lowercase()] ?: return@forEach
            var multiplier = 1f
            defender.types.forEach { defType ->
                multiplier *= atkRow[defType.lowercase()] ?: 1f
            }
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