package com.aditya1875.pokeverse.feature.game.pokematch.domain.engine

import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.Difficulty
import com.aditya1875.pokeverse.feature.game.pokematch.domain.model.GameState

class MatchGameEngine {

    fun onCardFlipped(
        state: GameState.Playing,
        cardIndex: Int
    ): GameState.Playing {

        val card = state.cards[cardIndex]

        if (card.isFlipped || card.isMatched) return state
        if (state.flippedIndices.size >= 2) return state

        val newFlipped = state.flippedIndices + cardIndex
        val newCards = state.cards.toMutableList()
        newCards[cardIndex] = card.copy(isFlipped = true)

        var updatedState = state.copy(
            cards = newCards,
            flippedIndices = newFlipped
        )

        if (newFlipped.size == 2) {
            updatedState = resolveMatch(updatedState, newFlipped[0], newFlipped[1])
        }

        return updatedState
    }

    private fun resolveMatch(
        state: GameState.Playing,
        firstIndex: Int,
        secondIndex: Int
    ): GameState.Playing {

        val firstCard = state.cards[firstIndex]
        val secondCard = state.cards[secondIndex]

        val newMoves = state.moves + 1

        return if (firstCard.pairId == secondCard.pairId) {

            val newCards = state.cards.toMutableList()
            newCards[firstIndex] = firstCard.copy(isMatched = true)
            newCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = state.matchedPairs + firstCard.pairId

            val newScore = calculateScore(
                moves = newMoves,
                matchesFound = newMatchedPairs.size,
                totalPairs = state.difficulty.pairs,
                timeRemaining = state.timeRemaining,
                difficulty = state.difficulty
            )

            state.copy(
                cards = newCards,
                flippedIndices = emptyList(),
                matchedPairs = newMatchedPairs,
                moves = newMoves,
                score = newScore
            )

        } else {
            val newCards = state.cards.toMutableList()
            newCards[firstIndex] = firstCard.copy(isFlipped = false)
            newCards[secondIndex] = secondCard.copy(isFlipped = false)

            state.copy(
                cards = newCards,
                flippedIndices = emptyList(),
                moves = newMoves
            )
        }
    }

    private fun calculateScore(
        moves: Int,
        matchesFound: Int,
        totalPairs: Int,
        timeRemaining: Int,
        difficulty: Difficulty
    ): Int {
        val baseScore = matchesFound * 100
        val timeBonus = timeRemaining * 2
        val multiplier = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
        }
        val penalty = (moves - matchesFound).coerceAtLeast(0) * 5

        return ((baseScore + timeBonus - penalty) * multiplier).coerceAtLeast(0)
    }

    fun calculateStars(
        moves: Int,
        totalPairs: Int,
        timeRemaining: Int,
        totalTime: Int
    ): Int {
        val timePercent = timeRemaining.toFloat() / totalTime
        val moveRatio = totalPairs.toFloat() / moves.coerceAtLeast(1)

        return when {
            timePercent > 0.5f && moveRatio > 0.7f -> 3
            timePercent > 0.2f && moveRatio > 0.4f -> 2
            else -> 1
        }
    }


}