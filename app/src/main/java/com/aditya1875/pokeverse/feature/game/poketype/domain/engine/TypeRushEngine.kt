package com.aditya1875.pokeverse.feature.game.poketype.domain.engine

import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushQuestion
import com.aditya1875.pokeverse.feature.game.poketype.domain.model.TypeRushRoundResult

class TypeRushEngine {

    fun evaluateAnswer(
        question: TypeRushQuestion,
        selected: Set<String>,
        timeRemaining: Int
    ): TypeRushRoundResult {

        val correct = question.correctTypes.toSet()

        val allCorrectSelected = correct.all { it in selected }
        val noWrongSelected = selected.none { it !in correct }

        val isFullyCorrect = allCorrectSelected && noWrongSelected
        val isPartiallyCorrect = correct.any { it in selected }

        val basePoints = when {
            isFullyCorrect -> 100
            isPartiallyCorrect -> 40
            else -> 0
        }

        val timeBonus = if (isFullyCorrect) timeRemaining * 5 else 0

        return TypeRushRoundResult(
            question = question,
            selectedTypes = selected,
            isFullyCorrect = isFullyCorrect,
            isPartiallyCorrect = isPartiallyCorrect,
            pointsEarned = basePoints,
            timeBonus = timeBonus
        )
    }
}