package com.aditya1875.pokeverse.data.remote.model.gameModels

import com.aditya1875.pokeverse.utils.Difficulty

data class GameScore(
    val moves: Int,           // total flips / 2
    val timeSeconds: Int,     // time taken
    val matchesFound: Int,    // correct pairs
    val difficulty: Difficulty,
    val stars: Int,           // 1-3 based on performance
    val score: Int            // calculated final score
)
