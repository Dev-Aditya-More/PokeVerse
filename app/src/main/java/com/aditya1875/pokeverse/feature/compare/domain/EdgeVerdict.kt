package com.aditya1875.pokeverse.feature.compare.domain

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.utils.FanFavoritePokemon
import com.aditya1875.pokeverse.utils.pokemonTypeEffectiveness
import com.aditya1875.pokeverse.utils.typeEffectivenessMultiplier

/**
 * A soft, non-authoritative "who edges ahead" read on two Pokémon — combining
 * their type matchup with the curated fan-favorite list. Deliberately never a
 * hard "wins/loses" declaration: [leaningTo] is a lean, not a verdict, and can
 * be null ("too close to call").
 */
data class EdgeVerdict(
    val leaningTo: Side?,
    val reason: String
) {
    enum class Side { LEFT, RIGHT }
}

fun computeEdgeVerdict(left: PokemonResponse, right: PokemonResponse): EdgeVerdict {
    val leftTypes = left.types.map { it.type.name }
    val rightTypes = right.types.map { it.type.name }

    // Best multiplier each side's typing lands against the other's.
    val leftTypeScore = pokemonTypeEffectiveness.keys.maxOf { typeEffectivenessMultiplier(it, rightTypes) }
    val rightTypeScore = pokemonTypeEffectiveness.keys.maxOf { typeEffectivenessMultiplier(it, leftTypes) }

    val leftFavorite = FanFavoritePokemon.isFanFavorite(left.id)
    val rightFavorite = FanFavoritePokemon.isFanFavorite(right.id)

    val typeLean = when {
        leftTypeScore > rightTypeScore -> EdgeVerdict.Side.LEFT
        rightTypeScore > leftTypeScore -> EdgeVerdict.Side.RIGHT
        else -> null
    }
    val favoriteLean = when {
        leftFavorite && !rightFavorite -> EdgeVerdict.Side.LEFT
        rightFavorite && !leftFavorite -> EdgeVerdict.Side.RIGHT
        else -> null
    }

    val leftName = left.name.replaceFirstChar { it.uppercase() }
    val rightName = right.name.replaceFirstChar { it.uppercase() }

    return when {
        typeLean != null && typeLean == favoriteLean -> {
            val name = if (typeLean == EdgeVerdict.Side.LEFT) leftName else rightName
            EdgeVerdict(typeLean, "$name edges ahead here — type advantage and fan-favorite energy")
        }
        typeLean != null -> {
            val name = if (typeLean == EdgeVerdict.Side.LEFT) leftName else rightName
            EdgeVerdict(typeLean, "$name edges ahead here — type advantage")
        }
        favoriteLean != null -> {
            val name = if (favoriteLean == EdgeVerdict.Side.LEFT) leftName else rightName
            EdgeVerdict(favoriteLean, "$name edges ahead here — fan-favorite energy")
        }
        else -> EdgeVerdict(null, "Too close to call — evenly matched")
    }
}
