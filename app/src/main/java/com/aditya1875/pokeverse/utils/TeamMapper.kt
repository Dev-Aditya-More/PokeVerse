package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.home.data.source.remote.model.PokemonResult

object TeamMapper {
    fun PokemonResponse.toEntity(): PokemonResult{
        return PokemonResult(
            name = this.name
        )
    }
}