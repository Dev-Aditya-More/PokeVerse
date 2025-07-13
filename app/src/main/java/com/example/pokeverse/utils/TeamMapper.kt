package com.example.pokeverse.utils

import androidx.compose.runtime.Composable
import com.example.pokeverse.data.local.entity.TeamMemberEntity
import com.example.pokeverse.data.remote.model.PokemonResult

object TeamMapper {
    fun PokemonResult.toEntity(): TeamMemberEntity {
        return TeamMemberEntity(
            name = this.name,
            imageUrl = this.url
        )
    }
}