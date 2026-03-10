package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResult

object TeamMapper {
    fun PokemonResponse.toEntity(): PokemonResult{
        return PokemonResult(
            name = this.name
        )
    }
}