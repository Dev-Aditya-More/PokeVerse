package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.local.entity.TeamMemberEntity
import com.aditya1875.pokeverse.data.remote.model.PokemonResponse
import com.aditya1875.pokeverse.data.remote.model.PokemonResult
import com.aditya1875.pokeverse.data.remote.model.PokemonSearchIndex

object TeamMapper {
    fun PokemonResult.toEntity(): TeamMemberEntity {
        return TeamMemberEntity(
            name = this.name,
            imageUrl = this.url
        )
    }

    fun PokemonResponse.toEntity(): PokemonResult{
        return PokemonResult(
            name = this.name
        )
    }

}