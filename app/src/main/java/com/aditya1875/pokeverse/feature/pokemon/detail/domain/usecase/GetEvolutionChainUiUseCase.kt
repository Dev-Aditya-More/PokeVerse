package com.aditya1875.pokeverse.feature.pokemon.detail.domain.usecase

import com.aditya1875.pokeverse.feature.pokemon.detail.data.source.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.feature.pokemon.detail.domain.repository.PokemonDetailRepo
import com.aditya1875.pokeverse.utils.EvolutionChainMapper

class GetEvolutionChainUiUseCase(
    private val repo: PokemonDetailRepo
) {

    suspend operator fun invoke(name: String): EvolutionChainUi? {
        return try {
            val species = repo.getPokemonSpeciesByName(name)
            val chainId = species.evolutionChain?.url
                ?.trimEnd('/')
                ?.split("/")
                ?.lastOrNull()
                ?.toIntOrNull()
                ?: return null

            val chain = repo.getEvolutionChain(chainId)

            val linear = EvolutionChainMapper.extractLinearChain(chain.chain)
            EvolutionChainMapper.toUiChain(linear, name)

        } catch (e: Exception) {
            null
        }
    }
}