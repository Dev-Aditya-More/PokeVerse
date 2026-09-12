package com.aditya1875.pokeverse.feature.game.survivor.domain.model

data class SurvivorRound(
    val defender: MatchupPokemon,
    val activeModifiers: List<Modifier>,
    val options: List<String>,
    val correctTypes: Set<String>,
    val timeBudgetMs: Int
)
