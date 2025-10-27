package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.evolutionModels.Chain
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionStage
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.Species

fun mapEvolutionChain(chain: Chain): List<EvolutionStage> {
    val stages = mutableListOf<Species>()
    collectSpecies(chain, stages)

    return stages.mapIndexed { index, species ->
        val id = species.url.trimEnd('/').substringAfterLast('/').toInt()
        EvolutionStage(
            id = id,
            name = species.name.replaceFirstChar { it.uppercase() },
            imageUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png",
            hasPrev = index > 0,
            hasNext = index < stages.lastIndex
        )
    }
}

private fun collectSpecies(chain: Chain, list: MutableList<Species>) {
    list.add(chain.species)
    chain.evolvesTo.forEach { evo ->
        collectSpecies(evo, list)
    }
}
