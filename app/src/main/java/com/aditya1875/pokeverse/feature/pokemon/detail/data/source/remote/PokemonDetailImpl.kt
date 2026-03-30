package com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.PokemonSpeciesResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.TypeResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainResponse
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo


class PokemonDetailImpl(
    private val api: PokemonDetailsApi
) : PokemonDetailRepo {
    override suspend fun getPokemonByName(name: String): PokemonResponse {
        return api.getPokemonByName(name)
    }

    override suspend fun getPokemonSpeciesByName(name: String): PokemonSpeciesResponse {
        return api.getPokemonSpeciesByName(name)
    }

    override suspend fun getEvolutionChain(id: Int): EvolutionChainResponse {
        return api.getEvolutionChain(id)
    }
}