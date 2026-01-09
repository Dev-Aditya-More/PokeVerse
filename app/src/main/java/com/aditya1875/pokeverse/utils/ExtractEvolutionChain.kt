package com.aditya1875.pokeverse.utils

import com.aditya1875.pokeverse.data.remote.model.evolutionModels.Chain
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionChainUi
import com.aditya1875.pokeverse.data.remote.model.evolutionModels.EvolutionNode

object EvolutionChainMapper {

    fun extractLinearChain(chain: Chain): List<EvolutionNode> {
        val result = mutableListOf<EvolutionNode>()

        fun dfs(node: Chain) {
            val minLevel =
                node.species.minLevel

            result.add(
                EvolutionNode(
                    name = node.species.name,
                    minLevel = minLevel
                )
            )

            // v1: take first branch only
            node.evolves_to.firstOrNull()?.let { next ->
                dfs(
                    Chain(
                        evolution_details = next.evolution_details,
                        evolves_to = next.evolves_to,
                        is_baby = next.is_baby,
                        species = next.species
                    )
                )
            }
        }

        dfs(chain)
        return result
    }

    fun toUiChain(
        linear: List<EvolutionNode>,
        currentName: String
    ): EvolutionChainUi? {
        val index = linear.indexOfFirst {
            it.name.equals(currentName, ignoreCase = true)
        }

        if (index == -1) return null

        return EvolutionChainUi(
            previous = linear.getOrNull(index - 1),
            current = linear[index],
            next = linear.getOrNull(index + 1)
        )
    }
}
