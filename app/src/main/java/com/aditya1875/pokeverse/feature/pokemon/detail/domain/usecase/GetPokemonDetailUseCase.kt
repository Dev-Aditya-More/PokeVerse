package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.feature.pokemon.home.domain.model.PokemonDetailDomainModel

class GetPokemonDetailUseCase(
    private val repo: PokemonDetailRepo
) {

    suspend operator fun invoke(name: String): PokemonDetailDomainModel {
        val pokemon = repo.getPokemonByName(name)
        val species = repo.getPokemonSpeciesByName(name)

        val description = species.flavorTextEntries.firstOrNull {
            it.language.name == "en"
        }?.flavorText
            ?.replace("\n", " ")
            ?.replace("\u000c", " ")
            ?: "No description"

        return PokemonDetailDomainModel(
            pokemon = pokemon,
            description = description,
            varieties = species.varieties,
            evolutionChainUrl = species.evolutionChain?.url
        )
    }
}